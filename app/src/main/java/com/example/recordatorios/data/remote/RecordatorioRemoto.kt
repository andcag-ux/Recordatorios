package com.example.recordatorios.data.remote

/**
 * Modelo del documento en Cloud Firestore:
 *   users/{uid}/reminders/{id}
 *
 * [id] es el clientId generado en el dispositivo: el mismo identificador en Room
 * y en Firestore. Por eso un reintento escribe sobre el mismo documento y no
 * crea una copia.
 *
 * Los campos se leen y escriben a mano (sin reflexión), así la versión release
 * optimizada con R8 no depende de reglas extra para este modelo.
 */
data class RecordatorioRemoto(
    val id: String,
    val titulo: String?,
    val descripcion: String? = null,
    val categoria: String? = null,
    val prioridad: String? = null,
    val estado: String? = null,
    val pospuestoHasta: String? = null,
    val fechaLimite: String? = null,
    val creadoEn: Long? = null,
    val actualizadoEn: Long? = null
) {
    /** Campos que se guardan en el documento (el id es la clave del documento, no un campo). */
    fun aMapa(): Map<String, Any?> = mapOf(
        "titulo" to titulo,
        "descripcion" to descripcion,
        "categoria" to categoria,
        "prioridad" to prioridad,
        "estado" to estado,
        "pospuestoHasta" to pospuestoHasta,
        "fechaLimite" to fechaLimite,
        "creadoEn" to creadoEn,
        "actualizadoEn" to actualizadoEn
    )

    companion object {
        /** Lee un documento. Los valores con tipo inesperado se toman como ausentes. */
        fun desdeMapa(id: String, datos: Map<String, Any?>): RecordatorioRemoto = RecordatorioRemoto(
            id = id,
            titulo = datos["titulo"] as? String,
            descripcion = datos["descripcion"] as? String,
            categoria = datos["categoria"] as? String,
            prioridad = datos["prioridad"] as? String,
            estado = datos["estado"] as? String,
            pospuestoHasta = datos["pospuestoHasta"] as? String,
            fechaLimite = datos["fechaLimite"] as? String,
            creadoEn = (datos["creadoEn"] as? Number)?.toLong(),
            actualizadoEn = (datos["actualizadoEn"] as? Number)?.toLong()
        )
    }
}
