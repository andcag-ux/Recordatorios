package com.example.recordatorios.data

import com.example.recordatorios.Recordatorio
import com.example.recordatorios.data.local.CodigoOperacion
import com.example.recordatorios.data.local.CodigoSync
import com.example.recordatorios.data.local.RecordatorioDao
import com.example.recordatorios.data.local.RecordatorioEntity
import com.example.recordatorios.data.prefs.OrdenListado
import com.example.recordatorios.data.prefs.Preferencias
import com.example.recordatorios.data.remote.RecordatorioApi
import com.example.recordatorios.data.remote.ResultadoRed
import com.example.recordatorios.data.remote.llamadaSegura
import com.example.recordatorios.data.remote.llamadaSeguraSinCuerpo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.util.UUID

/**
 * Repositorio local-first: coordina Room, DataStore y el servicio remoto.
 *
 * - Room es la fuente de verdad: la interfaz solo observa la base local.
 * - Toda escritura se aplica primero en Room y queda marcada como pendiente.
 * - [sincronizar] envía lo pendiente y luego descarga el estado del servicio.
 * - Para evitar duplicados: (1) un Mutex impide dos sincronizaciones simultáneas,
 *   (2) cada registro tiene un clientId que el servicio usa como clave de
 *   idempotencia, (3) Room tiene índices únicos sobre clientId y remoteId.
 */
class RecordatorioRepositoryImpl(
    private val dao: RecordatorioDao,
    private val api: RecordatorioApi,
    private val preferencias: Flow<Preferencias>,
    private val scope: CoroutineScope,
    private val ahora: () -> Long = { System.currentTimeMillis() },
    private val nuevoClientId: () -> String = { UUID.randomUUID().toString() }
) : RecordatorioRepository {

    private val mutex = Mutex()
    private val _estado = MutableStateFlow<EstadoSincronizacion>(EstadoSincronizacion.Inactiva)
    override val estadoSincronizacion: StateFlow<EstadoSincronizacion> = _estado.asStateFlow()

    // ------------------------------------------------------------------
    // Lectura (siempre desde Room)
    // ------------------------------------------------------------------

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observarListado(): Flow<List<Recordatorio>> =
        preferencias.distinctUntilChanged().flatMapLatest { prefs ->
            // La preferencia "mostrar completados" decide qué consulta de Room se usa.
            val fuente =
                if (prefs.mostrarCompletados) dao.observarTodos() else dao.observarNoCompletados()
            fuente.map { lista -> ordenar(lista.map { it.aDominio() }, prefs.orden) }
        }

    override fun observarRecordatorio(id: Long): Flow<Recordatorio?> =
        dao.observarPorId(id).map { it?.aDominio() }

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
        val id = dao.insertar(recordatorio.aEntidadNueva(nuevoClientId(), ahora()))
        sincronizarEnSegundoPlano()
        return id
    }

    override suspend fun actualizar(recordatorio: Recordatorio) {
        val actual = dao.obtenerPorId(recordatorio.id) ?: return
        if (actual.operacion == CodigoOperacion.ELIMINAR) return
        // Si el registro nunca llegó al servicio sigue siendo una creación pendiente.
        val operacion =
            if (actual.operacion == CodigoOperacion.CREAR) CodigoOperacion.CREAR
            else CodigoOperacion.ACTUALIZAR
        dao.actualizar(
            recordatorio.aplicarSobre(actual).copy(
                syncEstado = CodigoSync.PENDIENTE,
                operacion = operacion,
                version = actual.version + 1
            )
        )
        sincronizarEnSegundoPlano()
    }

    override suspend fun eliminar(id: Long) {
        val actual = dao.obtenerPorId(id) ?: return
        if (actual.remoteId == null) {
            // Nunca llegó al servicio: alcanza con borrarlo localmente.
            dao.eliminar(id)
        } else {
            // Se oculta de la interfaz y queda pendiente de borrarse en el servicio.
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
        // Si ya hay una sincronización en curso, este disparo se descarta:
        // no se envía dos veces la misma operación.
        if (!mutex.tryLock()) return
        var resultado: EstadoSincronizacion = EstadoSincronizacion.Inactiva
        try {
            _estado.value = EstadoSincronizacion.EnCurso
            val falla = enviarPendientes() ?: descargar()
            resultado = if (falla == null) {
                EstadoSincronizacion.Correcta(ahora())
            } else {
                EstadoSincronizacion.Fallida(falla.mensaje, dao.obtenerPendientes().size)
            }
        } finally {
            _estado.value = resultado
            mutex.unlock()
        }
    }

    /** Envía cada operación pendiente. Devuelve una falla transitoria (y corta) o null. */
    private suspend fun enviarPendientes(): ResultadoRed.Falla? {
        for (item in dao.obtenerPendientes()) {
            val falla: ResultadoRed.Falla? = when (item.operacion) {
                CodigoOperacion.CREAR -> enviarCreacion(item)
                CodigoOperacion.ACTUALIZAR -> enviarActualizacion(item)
                CodigoOperacion.ELIMINAR -> enviarEliminacion(item)
                else -> null
            }
            if (falla != null) {
                if (falla.esTransitoria) return falla // sin red o servidor caído: se reintenta después
                dao.marcarError(item.id) // rechazo del servidor: queda marcado y se sigue con el resto
            }
        }
        return null
    }

    private suspend fun enviarCreacion(item: RecordatorioEntity): ResultadoRed.Falla? =
        // El clientId viaja en cada reintento: el servicio devuelve el mismo registro, no uno nuevo.
        when (val r = llamadaSegura { api.crear(item.aDto()) }) {
            is ResultadoRed.Exito -> confirmar(item, r.datos.id)
            is ResultadoRed.Falla -> r
        }

    private suspend fun enviarActualizacion(item: RecordatorioEntity): ResultadoRed.Falla? {
        val remoteId = item.remoteId ?: return enviarCreacion(item)
        return when (val r = llamadaSegura { api.actualizar(remoteId, item.aDto()) }) {
            is ResultadoRed.Exito -> confirmar(item, r.datos.id ?: remoteId)
            is ResultadoRed.SolicitudInvalida ->
                if (r.codigo == 404) enviarCreacion(item) else r // el servicio lo perdió: se vuelve a crear
            is ResultadoRed.Falla -> r
        }
    }

    private suspend fun enviarEliminacion(item: RecordatorioEntity): ResultadoRed.Falla? {
        val remoteId = item.remoteId
        if (remoteId == null) {
            dao.eliminar(item.id)
            return null
        }
        return when (val r = llamadaSeguraSinCuerpo { api.eliminar(remoteId) }) {
            is ResultadoRed.Exito -> { dao.eliminar(item.id); null }
            is ResultadoRed.SolicitudInvalida ->
                if (r.codigo == 404) { dao.eliminar(item.id); null } else r // ya no existía en el servicio
            is ResultadoRed.Falla -> r
        }
    }

    private suspend fun confirmar(item: RecordatorioEntity, remoteId: Long?): ResultadoRed.Falla? {
        if (remoteId == null) return ResultadoRed.Desconocido("la respuesta no incluye un identificador")
        val filas = dao.confirmarSincronizado(item.id, remoteId, item.version)
        // Si el registro se editó mientras se enviaba, no se pisa el cambio: queda pendiente de actualizar.
        if (filas == 0) dao.asignarRemoteId(item.id, remoteId)
        return null
    }

    /** Descarga el listado, transforma los DTO y actualiza Room sin duplicar registros. */
    private suspend fun descargar(): ResultadoRed.Falla? =
        when (val r = llamadaSegura { api.listar() }) {
            is ResultadoRed.Falla -> r
            is ResultadoRed.Exito -> {
                incorporar(r.datos.mapNotNull { it.aEntidadONull(ahora()) })
                null
            }
        }

    private suspend fun incorporar(desdeServidor: List<RecordatorioEntity>) {
        for (nuevo in desdeServidor) {
            val remoteId = nuevo.remoteId ?: continue
            val existente = dao.obtenerPorRemoteId(remoteId) ?: dao.obtenerPorClientId(nuevo.clientId)
            if (existente == null) {
                dao.insertar(nuevo)
            } else if (existente.operacion == CodigoOperacion.NINGUNA) {
                dao.actualizar(
                    nuevo.copy(id = existente.id, clientId = existente.clientId, version = existente.version)
                )
            }
            // Si hay cambios locales sin enviar, gana la versión local: no se pisa nada.
        }
        // Lo que estaba sincronizado y el servicio ya no tiene, se elimina localmente.
        val idsServidor = desdeServidor.mapNotNull { it.remoteId }.toSet()
        val huerfanos = dao.remoteIdsSincronizados().filter { it !in idsServidor }
        huerfanos.chunked(500).forEach { dao.eliminarSincronizadosPorRemoteIds(it) }
    }
}
