package com.example.recordatorios.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Acceso a la tabla de recordatorios. Todas las consultas reciben el uid de la
 * cuenta activa: un registro de otra cuenta nunca llega a la interfaz.
 * Los registros con operación ELIMINAR (pendientes de borrarse en el servicio)
 * no se muestran.
 */
@Dao
interface RecordatorioDao {

    // ---- Consultas observables (las usa la interfaz a través del repositorio) ----

    @Query(
        "SELECT * FROM recordatorios WHERE uid = :uid AND operacion != 'ELIMINAR' " +
            "ORDER BY creadoEn DESC"
    )
    fun observarTodos(uid: String): Flow<List<RecordatorioEntity>>

    @Query(
        "SELECT * FROM recordatorios WHERE uid = :uid AND operacion != 'ELIMINAR' " +
            "AND estado != 'COMPLETADO' ORDER BY creadoEn DESC"
    )
    fun observarNoCompletados(uid: String): Flow<List<RecordatorioEntity>>

    @Query("SELECT * FROM recordatorios WHERE id = :id AND uid = :uid AND operacion != 'ELIMINAR'")
    fun observarPorId(id: Long, uid: String): Flow<RecordatorioEntity?>

    // ---- Consultas puntuales (las usa el repositorio / la sincronización) ----

    @Query("SELECT * FROM recordatorios WHERE id = :id AND uid = :uid")
    suspend fun obtenerPorId(id: Long, uid: String): RecordatorioEntity?

    @Query("SELECT * FROM recordatorios WHERE uid = :uid AND clientId = :clientId LIMIT 1")
    suspend fun obtenerPorClientId(uid: String, clientId: String): RecordatorioEntity?

    @Query("SELECT * FROM recordatorios WHERE uid = :uid AND operacion != 'NINGUNA' ORDER BY creadoEn ASC")
    suspend fun obtenerPendientes(uid: String): List<RecordatorioEntity>

    @Query("SELECT COUNT(*) FROM recordatorios WHERE uid = :uid AND operacion != 'NINGUNA'")
    suspend fun contarPendientes(uid: String): Int

    @Query("SELECT clientId FROM recordatorios WHERE uid = :uid AND operacion = 'NINGUNA' AND enServidor = 1")
    suspend fun clientIdsSincronizados(uid: String): List<String>

    // ---- Escrituras ----

    @Insert
    suspend fun insertar(entidad: RecordatorioEntity): Long

    @Update
    suspend fun actualizar(entidad: RecordatorioEntity): Int

    @Query("DELETE FROM recordatorios WHERE id = :id")
    suspend fun eliminar(id: Long): Int

    @Query(
        "DELETE FROM recordatorios WHERE uid = :uid AND operacion = 'NINGUNA' " +
            "AND clientId IN (:clientIds)"
    )
    suspend fun eliminarSincronizadosPorClientIds(uid: String, clientIds: List<String>): Int

    /**
     * Al cerrar sesión: borra la copia local de lo que ya está en Firestore.
     * Se conserva solo lo pendiente (para no perder cambios hechos sin conexión);
     * sigue asociado a su uid y ninguna otra cuenta lo ve.
     */
    @Query("DELETE FROM recordatorios WHERE uid = :uid AND operacion = 'NINGUNA'")
    suspend fun eliminarSincronizadosDeUsuario(uid: String): Int

    /**
     * Confirma el envío solo si el registro no cambió mientras se enviaba
     * (misma versión). Devuelve la cantidad de filas modificadas.
     */
    @Query(
        "UPDATE recordatorios SET enServidor = 1, syncEstado = 'SINCRONIZADO', " +
            "operacion = 'NINGUNA' WHERE id = :id AND version = :version"
    )
    suspend fun confirmarSincronizado(id: Long, version: Int): Int

    /** El registro cambió durante el envío: ya existe en Firestore, pero queda pendiente de actualizar. */
    @Query(
        "UPDATE recordatorios SET enServidor = 1, " +
            "operacion = CASE WHEN operacion = 'CREAR' THEN 'ACTUALIZAR' ELSE operacion END " +
            "WHERE id = :id"
    )
    suspend fun marcarEnServidor(id: Long): Int

    @Query("UPDATE recordatorios SET syncEstado = 'ERROR' WHERE id = :id")
    suspend fun marcarError(id: Long): Int
}
