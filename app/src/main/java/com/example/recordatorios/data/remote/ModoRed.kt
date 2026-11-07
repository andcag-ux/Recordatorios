package com.example.recordatorios.data.remote

import kotlinx.coroutines.flow.StateFlow

/** Situaciones de red que se pueden simular para probar la sincronización. */
enum class ModoRed(val etiqueta: String) {
    Normal("Red normal"),
    SinConexion("Sin conexión"),
    Timeout("Timeout"),
    ErrorServidor("Error del servidor (500)"),
    SolicitudInvalida("Solicitud inválida (400)")
}

/** Permite cambiar el comportamiento del servicio simulado desde la interfaz. */
interface ControlRedSimulada {
    val modo: StateFlow<ModoRed>
    fun establecerModo(modo: ModoRed)
}
