package com.example.recordatorios.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Valores de [RecordatorioEntity.syncEstado]. */
object CodigoSync {
    const val SINCRONIZADO = "SINCRONIZADO"
    const val PENDIENTE = "PENDIENTE"
    const val ERROR = "ERROR"
}

/** Valores de [RecordatorioEntity.operacion]: qué falta enviar al servicio. */
object CodigoOperacion {
    const val NINGUNA = "NINGUNA"
    const val CREAR = "CREAR"
    const val ACTUALIZAR = "ACTUALIZAR"
    const val ELIMINAR = "ELIMINAR"
}

/**
 * Esquema de Room. Es independiente del modelo de dominio y del DTO.
 *
 * - [id]: identificador local y estable (lo usa la interfaz y la navegación).
 * - [clientId]: UUID generado en el dispositivo. Es la clave de idempotencia:
 *   si una creación se reenvía, el servicio reconoce el mismo registro.
 * - [remoteId]: identificador asignado por el servicio (null hasta que se confirma).
 * - [version]: se incrementa en cada cambio local; permite detectar ediciones
 *   que ocurren mientras se está sincronizando.
 *
 * Los índices únicos sobre clientId y remoteId impiden duplicados a nivel de base.
 */
@Entity(
    tableName = "recordatorios",
    indices = [
        Index(value = ["clientId"], unique = true),
        Index(value = ["remoteId"], unique = true)
    ]
)
data class RecordatorioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: String,
    val remoteId: Long? = null,
    val titulo: String,
    val descripcion: String? = null,
    val categoria: String,
    val prioridad: String,
    val estado: String,
    val pospuestoHasta: String? = null,
    val fechaLimite: String? = null,
    val creadoEn: Long,
    val syncEstado: String = CodigoSync.PENDIENTE,
    val operacion: String = CodigoOperacion.CREAR,
    val version: Int = 0
)
