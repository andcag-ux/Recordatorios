package com.example.recordatorios.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Acceso a la tabla de recordatorios. Las consultas que usa la interfaz devuelven
 * Flow: Room vuelve a emitir cada vez que cambia la tabla. Los registros marcados
 * con operación ELIMINAR (pendientes de borrarse en el servicio) no se muestran.
 */
@Dao
interface RecordatorioDao {

    // ---- Consultas observables (las usa la interfaz a través del repositorio) ----

    @Query("SELECT * FROM recordatorios WHERE operacion != 'ELIMINAR' ORDER BY creadoEn DESC")
    fun observarTodos(): Flow<List<RecordatorioEntity>>

    @Query(
        "SELECT * FROM recordatorios WHERE operacion != 'ELIMINAR' AND estado != 'COMPLETADO' " +
            "ORDER BY creadoEn DESC"
    )
    fun observarNoCompletados(): Flow<List<RecordatorioEntity>>

    @Query(
        "SELECT * FROM recordatorios WHERE operacion != 'ELIMINAR' AND estado = :estado " +
            "ORDER BY creadoEn DESC"
    )
    fun observarPorEstado(estado: String): Flow<List<RecordatorioEntity>>

    @Query(
        "SELECT * FROM recordatorios WHERE operacion != 'ELIMINAR' AND categoria = :categoria " +
            "ORDER BY creadoEn DESC"
    )
    fun observarPorCategoria(categoria: String): Flow<List<RecordatorioEntity>>

    @Query("SELECT * FROM recordatorios WHERE id = :id AND operacion != 'ELIMINAR'")
    fun observarPorId(id: Long): Flow<RecordatorioEntity?>

    // ---- Consultas puntuales (las usa el repositorio / la sincronización) ----

    @Query("SELECT * FROM recordatorios WHERE id = :id")
    suspend fun obtenerPorId(id: Long): RecordatorioEntity?

    @Query("SELECT * FROM recordatorios WHERE remoteId = :remoteId LIMIT 1")
    suspend fun obtenerPorRemoteId(remoteId: Long): RecordatorioEntity?

    @Query("SELECT * FROM recordatorios WHERE clientId = :clientId LIMIT 1")
    suspend fun obtenerPorClientId(clientId: String): RecordatorioEntity?

    @Query("SELECT * FROM recordatorios WHERE operacion != 'NINGUNA' ORDER BY creadoEn ASC")
    suspend fun obtenerPendientes(): List<RecordatorioEntity>

    @Query("SELECT remoteId FROM recordatorios WHERE operacion = 'NINGUNA' AND remoteId IS NOT NULL")
    suspend fun remoteIdsSincronizados(): List<Long>

    // ---- Escrituras ----

    @Insert
    suspend fun insertar(entidad: RecordatorioEntity): Long

    @Update
    suspend fun actualizar(entidad: RecordatorioEntity): Int

    @Query("DELETE FROM recordatorios WHERE id = :id")
    suspend fun eliminar(id: Long): Int

    @Query("DELETE FROM recordatorios WHERE operacion = 'NINGUNA' AND remoteId IN (:remoteIds)")
    suspend fun eliminarSincronizadosPorRemoteIds(remoteIds: List<Long>): Int

    /**
     * Confirma el envío solo si el registro no cambió mientras se enviaba
     * (misma versión). Devuelve la cantidad de filas modificadas.
     */
    @Query(
        "UPDATE recordatorios SET remoteId = :remoteId, syncEstado = 'SINCRONIZADO', " +
            "operacion = 'NINGUNA' WHERE id = :id AND version = :version"
    )
    suspend fun confirmarSincronizado(id: Long, remoteId: Long, version: Int): Int

    /** El registro cambió durante el envío: conserva el id remoto y queda pendiente de actualizar. */
    @Query(
        "UPDATE recordatorios SET remoteId = :remoteId, " +
            "operacion = CASE WHEN operacion = 'CREAR' THEN 'ACTUALIZAR' ELSE operacion END " +
            "WHERE id = :id"
    )
    suspend fun asignarRemoteId(id: Long, remoteId: Long): Int

    @Query("UPDATE recordatorios SET syncEstado = 'ERROR' WHERE id = :id")
    suspend fun marcarError(id: Long): Int
}
