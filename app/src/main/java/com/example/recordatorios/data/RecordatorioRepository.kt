package com.example.recordatorios.data

import com.example.recordatorios.Recordatorio

/**
 * Contrato de acceso a los recordatorios. Los ViewModels dependen de esta
 * interfaz y no de una lista concreta: en los próximos módulos se podrá
 * reemplazar la implementación en memoria por Room o un servicio remoto
 * sin modificar pantallas ni componentes.
 */
interface RecordatorioRepository {
    suspend fun obtenerRecordatorios(): List<Recordatorio>
    suspend fun obtenerPorId(id: Long): Recordatorio?

    /** Guarda un recordatorio nuevo (id = 0) y devuelve el registro con su id asignado. */
    suspend fun guardar(recordatorio: Recordatorio): Recordatorio
}

/** Modos para comprobar todos los estados de la interfaz sin base de datos ni red. */
enum class ModoSimulacion(val etiqueta: String) {
    DatosNormales("Datos normales"),
    ListaVacia("Lista vacía"),
    ErrorDeCarga("Error de carga"),
    ErrorDeGuardado("Error al guardar (próximo guardado)")
}

interface SimulacionRepositorio {
    fun establecerModo(modo: ModoSimulacion)
}

/** Error simulado de la fuente de datos. */
class ErrorDeRepositorio(mensaje: String) : Exception(mensaje)
