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
 * Esquema de Room (versión 2, Actividad 4). Es independiente del modelo de
 * dominio y del documento de Firestore.
 *
 * - [id]: identificador local (lo usan la interfaz y la navegación).
 * - [uid]: cuenta dueña del registro. Todas las consultas filtran por la cuenta
 *   activa: otra persona que inicie sesión en el mismo dispositivo no lo ve.
 * - [clientId]: UUID generado en el dispositivo. Es también el id del documento
 *   en Firestore, así un reintento modifica el mismo documento y no lo duplica.
 * - [enServidor]: true cuando el documento ya existe en Firestore.
 * - [version]: aumenta en cada cambio local; detecta ediciones durante un envío.
 */
@Entity(
    tableName = "recordatorios",
    indices = [
        Index(value = ["clientId"], unique = true),
        Index(value = ["uid"])
    ]
)
data class RecordatorioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: String,
    val clientId: String,
    val enServidor: Boolean = false,
    val titulo: String,
    val descripcion: String? = null,
    val categoria: String,
    val prioridad: String,
    val estado: String,
    val pospuestoHasta: String? = null,
    val fechaLimite: String? = null,
    val creadoEn: Long,
    val actualizadoEn: Long = creadoEn,
    val syncEstado: String = CodigoSync.PENDIENTE,
    val operacion: String = CodigoOperacion.CREAR,
    val version: Int = 0
)
