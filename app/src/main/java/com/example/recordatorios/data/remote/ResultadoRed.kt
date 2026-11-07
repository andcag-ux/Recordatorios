package com.example.recordatorios.data.remote

import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.InterruptedIOException

/**
 * Resultado de una llamada de red ya interpretado. Distingue éxito, solicitud
 * inválida (4xx), error del servidor (5xx), timeout, falta de conexión y otros.
 * Esta interpretación ocurre en la capa de datos: las pantallas no ven códigos HTTP.
 */
sealed interface ResultadoRed<out T> {

    data class Exito<T>(val datos: T) : ResultadoRed<T>

    sealed interface Falla : ResultadoRed<Nothing> {
        val mensaje: String

        /** Si es una falla transitoria (conexión, timeout, servidor): conviene reintentar luego. */
        val esTransitoria: Boolean
    }

    data class SolicitudInvalida(val codigo: Int) : Falla {
        override val mensaje get() = "El servidor rechazó la solicitud (código $codigo)."
        override val esTransitoria get() = false
    }

    data class ErrorServidor(val codigo: Int) : Falla {
        override val mensaje get() = "El servidor no está disponible (código $codigo). Se reintentará."
        override val esTransitoria get() = true
    }

    data object Timeout : Falla {
        override val mensaje get() = "El servidor tardó demasiado en responder. Se reintentará."
        override val esTransitoria get() = true
    }

    data object SinConexion : Falla {
        override val mensaje get() = "Sin conexión. Los cambios quedan guardados en el dispositivo."
        override val esTransitoria get() = true
    }

    data class Desconocido(val detalle: String) : Falla {
        override val mensaje get() = "Respuesta inesperada del servidor: $detalle"
        override val esTransitoria get() = false
    }
}

/** Ejecuta una llamada que devuelve cuerpo y traduce códigos HTTP y excepciones. */
suspend fun <T : Any> llamadaSegura(bloque: suspend () -> Response<T>): ResultadoRed<T> =
    try {
        val respuesta = bloque()
        if (respuesta.isSuccessful) {
            val cuerpo = respuesta.body()
            if (cuerpo != null) ResultadoRed.Exito(cuerpo)
            else ResultadoRed.Desconocido("respuesta sin contenido")
        } else {
            clasificarCodigo(respuesta.code())
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        clasificarExcepcion(e)
    }

/** Variante para operaciones sin cuerpo de respuesta (DELETE). */
suspend fun llamadaSeguraSinCuerpo(bloque: suspend () -> Response<Unit>): ResultadoRed<Unit> =
    try {
        val respuesta = bloque()
        if (respuesta.isSuccessful) ResultadoRed.Exito(Unit) else clasificarCodigo(respuesta.code())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        clasificarExcepcion(e)
    }

fun clasificarCodigo(codigo: Int): ResultadoRed.Falla = when (codigo) {
    in 400..499 -> ResultadoRed.SolicitudInvalida(codigo)
    in 500..599 -> ResultadoRed.ErrorServidor(codigo)
    else -> ResultadoRed.Desconocido("código HTTP $codigo")
}

fun clasificarExcepcion(e: Exception): ResultadoRed.Falla = when (e) {
    // SocketTimeoutException es una InterruptedIOException: ambas indican timeout.
    is InterruptedIOException -> ResultadoRed.Timeout
    // UnknownHostException, ConnectException y el resto de IOException: no se pudo llegar al servidor.
    is java.io.IOException -> ResultadoRed.SinConexion
    else -> ResultadoRed.Desconocido(e.message ?: e::class.java.simpleName)
}
