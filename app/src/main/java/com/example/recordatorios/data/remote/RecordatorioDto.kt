package com.example.recordatorios.data.remote

/**
 * Contrato del servicio remoto. Todos los campos son opcionales porque los datos
 * externos pueden venir incompletos: el mapeo descarta lo que no sea válido.
 * Los DTO nunca llegan a las funciones componibles.
 */
data class RecordatorioDto(
    val id: Long? = null,
    val clientId: String? = null,
    val titulo: String? = null,
    val descripcion: String? = null,
    val categoria: String? = null,
    val prioridad: String? = null,
    val estado: String? = null,
    val pospuestoHasta: String? = null,
    val fechaLimite: String? = null,
    val creadoEn: Long? = null
)
