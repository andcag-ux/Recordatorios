package com.example.recordatorios.data

import com.example.recordatorios.Recordatorio
import com.example.recordatorios.data.local.CodigoOperacion
import com.example.recordatorios.data.local.CodigoSync
import com.example.recordatorios.data.local.RecordatorioDao
import com.example.recordatorios.data.local.RecordatorioEntity
import com.example.recordatorios.data.prefs.OrdenListado
import com.example.recordatorios.data.prefs.Preferencias
import com.example.recordatorios.data.remote.FuenteRemotaRecordatorios
import com.example.recordatorios.data.remote.RecordatorioRemoto
import com.example.recordatorios.data.remote.ResultadoRed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Repositorio local-first: coordina Room, DataStore y Firestore.
 *
 * - Room es la fuente de verdad: la interfaz solo observa la base local.
 * - Toda escritura se aplica primero en Room y queda marcada como pendiente.
 * - [sincronizar] envía lo pendiente a Firestore y luego descarga los datos de la cuenta.
 * - Todo se hace en nombre de la cuenta activa ([uidActivo]): las consultas de
 *   Room filtran por uid y las rutas de Firestore se arman con ese mismo uid.
 * - Para evitar duplicados: (1) un Mutex impide dos sincronizaciones simultáneas,
 *   (2) el id del documento en Firestore es el clientId, así un reintento
 *   sobrescribe el mismo documento, (3) Room tiene un índice único sobre clientId.
 */
class RecordatorioRepositoryImpl(
    private val dao: RecordatorioDao,
    private val remota: FuenteRemotaRecordatorios,
    private val preferencias: Flow<Preferencias>,
    private val uidActivo: StateFlow<String?>,
    private val scope: CoroutineScope,
    private val hayConexion: () -> Boolean = { true },
    private val ahora: () -> Long = { System.currentTimeMillis() },
    private val nuevoClientId: () -> String = { UUID.randomUUID().toString() }
) : RecordatorioRepository {

    private val mutex = Mutex()
    private val _estado = MutableStateFlow<EstadoSincronizacion>(EstadoSincronizacion.Inactiva)
    override val estadoSincronizacion: StateFlow<EstadoSincronizacion> = _estado.asStateFlow()

    private fun uidRequerido(): String =
        uidActivo.value ?: throw IllegalStateException("No hay una sesión activa.")

    // ------------------------------------------------------------------
    // Lectura (siempre desde Room, siempre de la cuenta activa)
    // ------------------------------------------------------------------

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observarListado(): Flow<List<Recordatorio>> =
        combine(uidActivo, preferencias.distinctUntilChanged()) { uid, prefs -> uid to prefs }
            .flatMapLatest { (uid, prefs) ->
                if (uid == null) {
                    flowOf(emptyList<Recordatorio>())
                } else {
                    // La preferencia "mostrar completados" decide qué consulta de Room se usa.
                    val fuente =
                        if (prefs.mostrarCompletados) dao.observarTodos(uid) else dao.observarNoCompletados(uid)
                    fuente.map { lista -> ordenar(lista.map { it.aDominio() }, prefs.orden) }
                }
            }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observarRecordatorio(id: Long): Flow<Recordatorio?> =
        uidActivo.flatMapLatest { uid ->
            if (uid == null) flowOf<Recordatorio?>(null) else dao.observarPorId(id, uid).map { it?.aDominio() }
        }

    private fun ordenar(lista: List<Recordatorio>, orden: OrdenListado): List<Recordatorio> =
        when (orden) {
            OrdenListado.Recientes -> lista // Room ya devuelve más recientes primero
            OrdenListado.Prioridad -> lista.sortedByDescending { it.prioridad.peso } // orden estable
            OrdenListado.Titulo -> lista.sortedBy { it.titulo.lowercase() }
        }

    // ------------------------------------------------------------------
    // Escritura: primero Room, luego se intenta sincronizar
    // ------------------------------------------------------------------

    override suspend fun crear(recordatorio: Recordatorio): Long {
        val uid = uidRequerido()
        val id = dao.insertar(recordatorio.aEntidadNueva(uid, nuevoClientId(), ahora()))
        sincronizarEnSegundoPlano()
        return id
    }

    override suspend fun actualizar(recordatorio: Recordatorio) {
        val uid = uidRequerido()
        // La búsqueda incluye el uid: no se puede modificar un registro de otra cuenta.
        val actual = dao.obtenerPorId(recordatorio.id, uid) ?: return
        if (actual.operacion == CodigoOperacion.ELIMINAR) return
        // Si el registro nunca llegó a Firestore sigue siendo una creación pendiente.
        val operacion =
            if (actual.enServidor) CodigoOperacion.ACTUALIZAR else CodigoOperacion.CREAR
        dao.actualizar(
            recordatorio.aplicarSobre(actual).copy(
                actualizadoEn = ahora(),
                syncEstado = CodigoSync.PENDIENTE,
                operacion = operacion,
                version = actual.version + 1
            )
        )
        sincronizarEnSegundoPlano()
    }

    override suspend fun eliminar(id: Long) {
        val uid = uidRequerido()
        val actual = dao.obtenerPorId(id, uid) ?: return
        if (!actual.enServidor) {
            // Nunca llegó a Firestore: alcanza con borrarlo localmente.
            dao.eliminar(id)
        } else {
            // Se oculta de la interfaz y queda pendiente de borrarse en Firestore.
            dao.actualizar(
                actual.copy(
                    syncEstado = CodigoSync.PENDIENTE,
                    operacion = CodigoOperacion.ELIMINAR,
                    version = actual.version + 1
                )
            )
        }
        sincronizarEnSegundoPlano()
    }

    // ------------------------------------------------------------------
    // Sincronización
    // ------------------------------------------------------------------

    override fun sincronizarEnSegundoPlano() {
        scope.launch { sincronizar() }
    }

    override suspend fun sincronizar() {
        val uid = uidActivo.value ?: return
        // Si ya hay una sincronización en curso, este disparo se descarta:
        // no se envía dos veces la misma operación.
        if (!mutex.tryLock()) return
        var resultado: EstadoSincronizacion = EstadoSincronizacion.Inactiva
        try {
            _estado.value = EstadoSincronizacion.EnCurso
            val falla = if (!hayConexion()) {
                ResultadoRed.SinConexion // sin red no se intenta: evita esperas inútiles
            } else {
                enviarPendientes(uid) ?: descargar(uid)
            }
            resultado = if (falla == null) {
                EstadoSincronizacion.Correcta(ahora())
            } else {
                EstadoSincronizacion.Fallida(falla.mensaje, dao.contarPendientes(uid))
            }
        } finally {
            // Si la sesión cambió mientras se sincronizaba, no se informa el resultado de la anterior.
            _estado.value = if (uidActivo.value == uid) resultado else EstadoSincronizacion.Inactiva
            mutex.unlock()
        }
    }

    /** Envía cada operación pendiente. Devuelve la falla que obliga a cortar, o null. */
    private suspend fun enviarPendientes(uid: String): ResultadoRed.Falla? {
        for (item in dao.obtenerPendientes(uid)) {
            val falla: ResultadoRed.Falla? = when (item.operacion) {
                CodigoOperacion.CREAR, CodigoOperacion.ACTUALIZAR -> enviarGuardado(uid, item)
                CodigoOperacion.ELIMINAR -> enviarEliminacion(uid, item)
                else -> null
            }
            if (falla != null) {
                // Sin red, timeout o sesión inválida: se corta y se reintenta después.
                if (falla.esTransitoria || falla == ResultadoRed.SesionInvalida) return falla
                dao.marcarError(item.id) // rechazo puntual: queda marcado y se sigue con el resto
            }
        }
        return null
    }

    /** CREAR y ACTUALIZAR usan la misma escritura: set() sobre el documento con id = clientId. */
    private suspend fun enviarGuardado(uid: String, item: RecordatorioEntity): ResultadoRed.Falla? =
        when (val r = remota.guardar(uid, item.aRemoto())) {
            is ResultadoRed.Exito -> {
                val filas = dao.confirmarSincronizado(item.id, item.version)
                // Si se editó mientras se enviaba, no se pisa el cambio: queda pendiente de actualizar.
                if (filas == 0) dao.marcarEnServidor(item.id)
                null
            }
            is ResultadoRed.Falla -> r
        }

    private suspend fun enviarEliminacion(uid: String, item: RecordatorioEntity): ResultadoRed.Falla? {
        if (!item.enServidor) {
            dao.eliminar(item.id)
            return null
        }
        return when (val r = remota.eliminar(uid, item.clientId)) {
            is ResultadoRed.Exito -> { dao.eliminar(item.id); null }
            is ResultadoRed.Falla -> r
        }
    }

    /** Descarga los documentos de la cuenta, los transforma y actualiza Room sin duplicar registros. */
    private suspend fun descargar(uid: String): ResultadoRed.Falla? =
        when (val r = remota.listar(uid)) {
            is ResultadoRed.Falla -> r
            is ResultadoRed.Exito -> {
                incorporar(uid, r.datos.mapNotNull { it.aEntidadONull(uid, ahora()) })
                null
            }
        }

    private suspend fun incorporar(uid: String, desdeServidor: List<RecordatorioEntity>) {
        for (nuevo in desdeServidor) {
            val existente = dao.obtenerPorClientId(uid, nuevo.clientId)
            if (existente == null) {
                dao.insertar(nuevo) // por ejemplo, creado desde otro dispositivo
            } else if (existente.operacion == CodigoOperacion.NINGUNA) {
                dao.actualizar(nuevo.copy(id = existente.id, version = existente.version))
            }
            // Si hay cambios locales sin enviar, gana la versión local: no se pisa nada.
        }
        // Lo que estaba sincronizado y Firestore ya no tiene (borrado en otro dispositivo) se elimina localmente.
        val idsServidor = desdeServidor.map { it.clientId }.toSet()
        val huerfanos = dao.clientIdsSincronizados(uid).filter { it !in idsServidor }
        huerfanos.chunked(500).forEach { dao.eliminarSincronizadosPorClientIds(uid, it) }
    }

    // ------------------------------------------------------------------
    // Sesión
    // ------------------------------------------------------------------

    override suspend fun pendientesDeSincronizar(): Int {
        val uid = uidActivo.value ?: return 0
        return dao.contarPendientes(uid)
    }

    override suspend fun alCerrarSesion(uid: String) {
        // Espera a que termine una sincronización en curso de esa cuenta antes de limpiar.
        mutex.withLock { dao.eliminarSincronizadosDeUsuario(uid) }
        _estado.value = EstadoSincronizacion.Inactiva
    }

    // ------------------------------------------------------------------
    // Prueba de seguridad (solo se ofrece en la variante debug)
    // ------------------------------------------------------------------

    override suspend fun intentarLeerCuentaAjena(uidAjeno: String): ResultadoRed<Int> =
        when (val r = remota.listar(uidAjeno.trim())) {
            is ResultadoRed.Exito -> ResultadoRed.Exito(r.datos.size)
            is ResultadoRed.Falla -> r
        }

    override suspend fun intentarEscribirEnCuentaAjena(uidAjeno: String): ResultadoRed<Unit> =
        remota.guardar(
            uidAjeno.trim(),
            RecordatorioRemoto(id = "prueba-acceso-${nuevoClientId()}", titulo = "Intento de escritura ajena")
        )
}
