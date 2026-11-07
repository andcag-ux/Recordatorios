package com.example.recordatorios.data.remote

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.io.IOException

/**
 * Resultado de una operación remota ya interpretado. La interpretación de las
 * excepciones de Firebase ocurre acá, en la capa de datos: las pantallas nunca
 * ven códigos ni clases del proveedor.
 */
sealed interface ResultadoRed<out T> {

    data class Exito<T>(val datos: T) : ResultadoRed<T>

    sealed interface Falla : ResultadoRed<Nothing> {
        val mensaje: String

        /** Falla transitoria (sin red, timeout, servicio caído): conviene reintentar más tarde. */
        val esTransitoria: Boolean
    }

    data object SinConexion : Falla {
        override val mensaje get() = "Sin conexión. Los cambios quedan guardados en el dispositivo."
        override val esTransitoria get() = true
    }

    data object Timeout : Falla {
        override val mensaje get() = "El servicio tardó demasiado en responder. Se reintentará."
        override val esTransitoria get() = true
    }

    data object ServicioNoDisponible : Falla {
        override val mensaje get() = "El servicio no está disponible en este momento. Se reintentará."
        override val esTransitoria get() = true
    }

    /** Las reglas de seguridad rechazaron la operación (dato de otra cuenta, por ejemplo). */
    data object AccesoDenegado : Falla {
        override val mensaje get() = "El servicio rechazó la operación: no tenés permiso sobre esos datos."
        override val esTransitoria get() = false
    }

    /** El servicio no reconoce la sesión: hay que volver a iniciar sesión. */
    data object SesionInvalida : Falla {
        override val mensaje get() = "La sesión ya no es válida. Iniciá sesión nuevamente."
        override val esTransitoria get() = false
    }

    data class Desconocido(val detalle: String) : Falla {
        override val mensaje get() = "Error inesperado del servicio: $detalle"
        override val esTransitoria get() = false
    }
}

/** Tiempo máximo de espera de una operación remota. */
const val TIMEOUT_REMOTO_MS = 15_000L

/**
 * Ejecuta una operación remota con límite de tiempo y traduce cualquier
 * excepción a [ResultadoRed]. Una cancelación real de la corrutina se propaga.
 */
suspend fun <T> llamadaRemota(
    timeoutMs: Long = TIMEOUT_REMOTO_MS,
    bloque: suspend () -> T
): ResultadoRed<T> =
    try {
        ResultadoRed.Exito(withTimeout(timeoutMs) { bloque() })
    } catch (e: TimeoutCancellationException) {
        ResultadoRed.Timeout
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        clasificarExcepcion(e)
    }

fun clasificarExcepcion(e: Exception): ResultadoRed.Falla = when (e) {
    is FirebaseFirestoreException -> clasificarCodigoFirestore(e.code.name)
    is FirebaseNetworkException -> ResultadoRed.SinConexion
    is IOException -> ResultadoRed.SinConexion
    else -> ResultadoRed.Desconocido(e.message ?: e::class.java.simpleName)
}

/** Traduce el nombre del código de Firestore (FirebaseFirestoreException.Code) a una falla propia. */
fun clasificarCodigoFirestore(codigo: String): ResultadoRed.Falla = when (codigo) {
    "PERMISSION_DENIED" -> ResultadoRed.AccesoDenegado
    "UNAUTHENTICATED" -> ResultadoRed.SesionInvalida
    "UNAVAILABLE" -> ResultadoRed.SinConexion
    "DEADLINE_EXCEEDED" -> ResultadoRed.Timeout
    "RESOURCE_EXHAUSTED", "ABORTED", "INTERNAL" -> ResultadoRed.ServicioNoDisponible
    else -> ResultadoRed.Desconocido(codigo)
}
