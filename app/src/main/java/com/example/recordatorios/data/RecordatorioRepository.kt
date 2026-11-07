package com.example.recordatorios.data

import com.example.recordatorios.Recordatorio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Estado de la sincronización con el servicio remoto, expuesto para la interfaz. */
sealed interface EstadoSincronizacion {
    data object Inactiva : EstadoSincronizacion
    data object EnCurso : EstadoSincronizacion
    data class Correcta(val momento: Long) : EstadoSincronizacion

    /** Falla recuperable: los datos locales se conservan y se puede reintentar. */
    data class Fallida(val mensaje: String, val pendientes: Int) : EstadoSincronizacion
}

/**
 * Contrato local-first. La interfaz solo ve modelos de dominio y flujos que salen
 * de Room; nunca depende de la conectividad ni de los DTO de Retrofit.
 */
interface RecordatorioRepository {

    /** Listado local, filtrado y ordenado según las preferencias (DataStore). */
    fun observarListado(): Flow<List<Recordatorio>>

    fun observarRecordatorio(id: Long): Flow<Recordatorio?>

    val estadoSincronizacion: StateFlow<EstadoSincronizacion>

    /** Guarda primero en Room (queda pendiente) y devuelve el id local. */
    suspend fun crear(recordatorio: Recordatorio): Long

    suspend fun actualizar(recordatorio: Recordatorio)

    suspend fun eliminar(id: Long)

    /** Envía las operaciones pendientes y descarga los datos del servicio. Si ya hay una en curso, no hace nada. */
    suspend fun sincronizar()

    fun sincronizarEnSegundoPlano()
}
