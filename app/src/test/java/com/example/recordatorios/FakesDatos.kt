package com.example.recordatorios

import com.example.recordatorios.data.local.RecordatorioDao
import com.example.recordatorios.data.local.RecordatorioEntity
import com.example.recordatorios.data.remote.FuenteRemotaRecordatorios
import com.example.recordatorios.data.remote.RecordatorioRemoto
import com.example.recordatorios.data.remote.ResultadoRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** DAO en memoria para probar el repositorio sin Android ni Room. */
class FakeDao : RecordatorioDao {
    private val filas = MutableStateFlow<List<RecordatorioEntity>>(emptyList())
    private var siguienteId = 1L

    val todas: List<RecordatorioEntity> get() = filas.value

    private fun visibles(uid: String) = filas.map { l ->
        l.filter { it.uid == uid && it.operacion != "ELIMINAR" }.sortedByDescending { it.creadoEn }
    }

    override fun observarTodos(uid: String): Flow<List<RecordatorioEntity>> = visibles(uid)
    override fun observarNoCompletados(uid: String): Flow<List<RecordatorioEntity>> =
        visibles(uid).map { l -> l.filter { it.estado != "COMPLETADO" } }
    override fun observarPorId(id: Long, uid: String): Flow<RecordatorioEntity?> =
        visibles(uid).map { l -> l.firstOrNull { it.id == id } }

    override suspend fun obtenerPorId(id: Long, uid: String) =
        filas.value.firstOrNull { it.id == id && it.uid == uid }
    override suspend fun obtenerPorClientId(uid: String, clientId: String) =
        filas.value.firstOrNull { it.uid == uid && it.clientId == clientId }
    override suspend fun obtenerPendientes(uid: String) =
        filas.value.filter { it.uid == uid && it.operacion != "NINGUNA" }.sortedBy { it.creadoEn }
    override suspend fun contarPendientes(uid: String) =
        filas.value.count { it.uid == uid && it.operacion != "NINGUNA" }
    override suspend fun clientIdsSincronizados(uid: String) =
        filas.value.filter { it.uid == uid && it.operacion == "NINGUNA" && it.enServidor }.map { it.clientId }

    override suspend fun insertar(entidad: RecordatorioEntity): Long {
        // Replica el índice único de Room: un clientId repetido es un error.
        check(filas.value.none { it.clientId == entidad.clientId }) { "clientId duplicado" }
        val id = siguienteId++
        filas.value = filas.value + entidad.copy(id = id)
        return id
    }

    override suspend fun actualizar(entidad: RecordatorioEntity): Int {
        filas.value = filas.value.map { if (it.id == entidad.id) entidad else it }
        return 1
    }

    override suspend fun eliminar(id: Long): Int {
        filas.value = filas.value.filterNot { it.id == id }
        return 1
    }

    override suspend fun eliminarSincronizadosPorClientIds(uid: String, clientIds: List<String>): Int {
        filas.value = filas.value.filterNot { it.uid == uid && it.operacion == "NINGUNA" && it.clientId in clientIds }
        return 1
    }

    override suspend fun eliminarSincronizadosDeUsuario(uid: String): Int {
        filas.value = filas.value.filterNot { it.uid == uid && it.operacion == "NINGUNA" }
        return 1
    }

    override suspend fun confirmarSincronizado(id: Long, version: Int): Int {
        var cambios = 0
        filas.value = filas.value.map {
            if (it.id == id && it.version == version) {
                cambios++
                it.copy(enServidor = true, syncEstado = "SINCRONIZADO", operacion = "NINGUNA")
            } else it
        }
        return cambios
    }

    override suspend fun marcarEnServidor(id: Long): Int {
        filas.value = filas.value.map {
            if (it.id == id) it.copy(
                enServidor = true,
                operacion = if (it.operacion == "CREAR") "ACTUALIZAR" else it.operacion
            ) else it
        }
        return 1
    }

    override suspend fun marcarError(id: Long): Int {
        filas.value = filas.value.map { if (it.id == id) it.copy(syncEstado = "ERROR") else it }
        return 1
    }
}

/**
 * Firestore falso: documentos por uid y por id, con las mismas reglas que las
 * publicadas (solo la cuenta autenticada accede a su ruta).
 */
class FakeFirestore : FuenteRemotaRecordatorios {
    /** uid -> (id de documento -> documento) */
    val datos = mutableMapOf<String, MutableMap<String, RecordatorioRemoto>>()
    var sinConexion = false
    var demoraMs = 0L
    var escrituras = 0
    /** Cuenta autenticada frente al servicio (lo que las reglas ven como request.auth.uid). */
    var uidAutenticado: String? = null

    fun documentos(uid: String) = datos.getOrPut(uid) { mutableMapOf() }

    private suspend fun simular(uid: String): ResultadoRed.Falla? {
        if (demoraMs > 0) delay(demoraMs)
        if (sinConexion) return ResultadoRed.SinConexion
        if (uidAutenticado != uid) return ResultadoRed.AccesoDenegado
        return null
    }

    override suspend fun listar(uid: String): ResultadoRed<List<RecordatorioRemoto>> =
        simular(uid) ?: ResultadoRed.Exito(documentos(uid).values.toList())

    override suspend fun guardar(uid: String, recordatorio: RecordatorioRemoto): ResultadoRed<Unit> {
        simular(uid)?.let { return it }
        escrituras++
        documentos(uid)[recordatorio.id] = recordatorio // set(): crea o reemplaza el mismo documento
        return ResultadoRed.Exito(Unit)
    }

    override suspend fun eliminar(uid: String, id: String): ResultadoRed<Unit> {
        simular(uid)?.let { return it }
        documentos(uid).remove(id)
        return ResultadoRed.Exito(Unit)
    }
}
