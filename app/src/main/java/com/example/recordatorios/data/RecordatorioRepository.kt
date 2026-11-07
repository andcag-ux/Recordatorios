package com.example.recordatorios.data

import com.example.recordatorios.Recordatorio
import com.example.recordatorios.data.remote.ResultadoRed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Estado de la sincronización con Firestore, expuesto para la interfaz sin detalles del proveedor. */
sealed interface EstadoSincronizacion {
    data object Inactiva : EstadoSincronizacion
    data object EnCurso : EstadoSincronizacion
    data class Correcta(val momento: Long) : EstadoSincronizacion

    /** Falla recuperable: los datos locales se conservan y se puede reintentar. */
    data class Fallida(val mensaje: String, val pendientes: Int) : EstadoSincronizacion
}

/**
 * Contrato local-first. La interfaz solo ve modelos de dominio y flujos que salen
 * de Room, siempre filtrados por la cuenta activa. Nunca depende de Firebase.
 */
interface RecordatorioRepository {

    /** Listado local de la cuenta activa, filtrado y ordenado según las preferencias (DataStore). */
    fun observarListado(): Flow<List<Recordatorio>>

    fun observarRecordatorio(id: Long): Flow<Recordatorio?>

    val estadoSincronizacion: StateFlow<EstadoSincronizacion>

    /** Guarda primero en Room (queda pendiente) y devuelve el id local. */
    suspend fun crear(recordatorio: Recordatorio): Long

    suspend fun actualizar(recordatorio: Recordatorio)

    suspend fun eliminar(id: Long)

    /** Envía las operaciones pendientes y descarga los datos de la cuenta. Si ya hay una en curso, no hace nada. */
    suspend fun sincronizar()

    fun sincronizarEnSegundoPlano()

    /** Cantidad de cambios de la cuenta activa que todavía no llegaron a Firestore. */
    suspend fun pendientesDeSincronizar(): Int

    /** Limpieza local al terminar la sesión de [uid] (ver RecordatorioDao.eliminarSincronizadosDeUsuario). */
    suspend fun alCerrarSesion(uid: String)

    /**
     * Solo para la prueba de seguridad (variante debug): intenta leer los
     * recordatorios de OTRA cuenta. Las reglas de Firestore deben rechazarlo.
     */
    suspend fun intentarLeerCuentaAjena(uidAjeno: String): ResultadoRed<Int>

    /** Igual que la anterior, pero intentando escribir un documento en la otra cuenta. */
    suspend fun intentarEscribirEnCuentaAjena(uidAjeno: String): ResultadoRed<Unit>
}
