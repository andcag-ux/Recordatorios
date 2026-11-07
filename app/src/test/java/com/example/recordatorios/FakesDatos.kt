package com.example.recordatorios

import com.example.recordatorios.data.local.RecordatorioDao
import com.example.recordatorios.data.local.RecordatorioEntity
import com.example.recordatorios.data.remote.RecordatorioApi
import com.example.recordatorios.data.remote.RecordatorioDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.io.IOException

/** DAO en memoria para probar el repositorio sin Android ni Room. */
class FakeDao : RecordatorioDao {
    private val filas = MutableStateFlow<List<RecordatorioEntity>>(emptyList())
    private var siguienteId = 1L

    val todas: List<RecordatorioEntity> get() = filas.value

    private fun visibles() = filas.map { l -> l.filter { it.operacion != "ELIMINAR" }.sortedByDescending { it.creadoEn } }

    override fun observarTodos(): Flow<List<RecordatorioEntity>> = visibles()
    override fun observarNoCompletados(): Flow<List<RecordatorioEntity>> =
        visibles().map { l -> l.filter { it.estado != "COMPLETADO" } }
    override fun observarPorEstado(estado: String): Flow<List<RecordatorioEntity>> =
        visibles().map { l -> l.filter { it.estado == estado } }
    override fun observarPorCategoria(categoria: String): Flow<List<RecordatorioEntity>> =
        visibles().map { l -> l.filter { it.categoria == categoria } }
    override fun observarPorId(id: Long): Flow<RecordatorioEntity?> =
        visibles().map { l -> l.firstOrNull { it.id == id } }

    override suspend fun obtenerPorId(id: Long) = filas.value.firstOrNull { it.id == id }
    override suspend fun obtenerPorRemoteId(remoteId: Long) = filas.value.firstOrNull { it.remoteId == remoteId }
    override suspend fun obtenerPorClientId(clientId: String) = filas.value.firstOrNull { it.clientId == clientId }
    override suspend fun obtenerPendientes() =
        filas.value.filter { it.operacion != "NINGUNA" }.sortedBy { it.creadoEn }
    override suspend fun remoteIdsSincronizados() =
        filas.value.filter { it.operacion == "NINGUNA" && it.remoteId != null }.map { it.remoteId!! }

    override suspend fun insertar(entidad: RecordatorioEntity): Long {
        // Replica los índices únicos de Room: un clientId o remoteId repetido es un error.
        check(filas.value.none { it.clientId == entidad.clientId }) { "clientId duplicado" }
        check(entidad.remoteId == null || filas.value.none { it.remoteId == entidad.remoteId }) { "remoteId duplicado" }
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

    override suspend fun eliminarSincronizadosPorRemoteIds(remoteIds: List<Long>): Int {
        filas.value = filas.value.filterNot { it.operacion == "NINGUNA" && it.remoteId in remoteIds }
        return 1
    }

    override suspend fun confirmarSincronizado(id: Long, remoteId: Long, version: Int): Int {
        var cambios = 0
        filas.value = filas.value.map {
            if (it.id == id && it.version == version) {
                cambios++
                it.copy(remoteId = remoteId, syncEstado = "SINCRONIZADO", operacion = "NINGUNA")
            } else it
        }
        return cambios
    }

    override suspend fun asignarRemoteId(id: Long, remoteId: Long): Int {
        filas.value = filas.value.map {
            if (it.id == id) it.copy(
                remoteId = remoteId,
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

/** Servicio falso: guarda registros por clientId (idempotente) y permite simular fallas. */
class FakeApi : RecordatorioApi {
    val servidor = mutableListOf<RecordatorioDto>()
    var sinConexion = false
    var demoraMs = 0L
    var llamadasCrear = 0
    val clientIdsRecibidos = mutableListOf<String?>()
    private var siguienteId = 100L

    private fun <T> vacioError(codigo: Int): Response<T> =
        Response.error(codigo, "".toResponseBody("application/json".toMediaType()))

    private suspend fun simularRed() {
        if (demoraMs > 0) delay(demoraMs)
        if (sinConexion) throw IOException("sin conexión")
    }

    override suspend fun listar(): Response<List<RecordatorioDto>> {
        simularRed()
        return Response.success(servidor.toList())
    }

    override suspend fun crear(dto: RecordatorioDto): Response<RecordatorioDto> {
        simularRed()
        llamadasCrear++
        clientIdsRecibidos += dto.clientId
        val existente = servidor.firstOrNull { it.clientId == dto.clientId }
        if (existente != null) return Response.success(existente)
        val nuevo = dto.copy(id = siguienteId++)
        servidor += nuevo
        return Response.success(nuevo)
    }

    override suspend fun actualizar(id: Long, dto: RecordatorioDto): Response<RecordatorioDto> {
        simularRed()
        val i = servidor.indexOfFirst { it.id == id }
        if (i < 0) return vacioError(404)
        servidor[i] = dto.copy(id = id)
        return Response.success(servidor[i])
    }

    override suspend fun eliminar(id: Long): Response<Unit> {
        simularRed()
        return if (servidor.removeAll { it.id == id }) Response.success(Unit) else vacioError(404)
    }
}
