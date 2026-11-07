package com.example.recordatorios.data.auth

import kotlinx.coroutines.flow.StateFlow

/** Identidad autenticada. No incluye contraseñas ni tokens: esos los administra Firebase. */
data class SesionUsuario(val uid: String, val email: String?)

/** Estado de la autenticación que observa la interfaz. */
sealed interface EstadoAutenticacion {
    /** Al abrir la app: todavía no se sabe si hay una sesión guardada. */
    data object Verificando : EstadoAutenticacion

    /** No hay sesión. [motivo] explica un cierre que la persona no pidió (sesión vencida, cuenta deshabilitada). */
    data class SinSesion(val motivo: String? = null) : EstadoAutenticacion

    data class Activa(val sesion: SesionUsuario) : EstadoAutenticacion
}

/** Resultado de registrar o iniciar sesión, sin clases del proveedor. */
sealed interface ResultadoAuth {
    data object Exito : ResultadoAuth
    data class Error(val mensaje: String) : ResultadoAuth
}

/**
 * Contrato de autenticación. La interfaz y los ViewModels dependen de esta
 * interfaz; solo [FirebaseAuthRepository] conoce Firebase Authentication.
 */
interface AuthRepository {
    val estado: StateFlow<EstadoAutenticacion>

    suspend fun registrar(email: String, contrasena: String): ResultadoAuth

    suspend fun iniciarSesion(email: String, contrasena: String): ResultadoAuth

    /** Cierre pedido por la persona. */
    fun cerrarSesion()

    /** Consulta al servicio si la sesión guardada sigue siendo válida; si no, la cierra. */
    suspend fun verificarSesion()
}

/** Mensajes en castellano para los códigos de error de Firebase Authentication. */
fun mensajeErrorAuth(codigo: String?): String = when (codigo) {
    "ERROR_INVALID_EMAIL" -> "El correo no tiene un formato válido."
    "ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND" ->
        "Correo o contraseña incorrectos."
    "ERROR_EMAIL_ALREADY_IN_USE" -> "Ya existe una cuenta con ese correo."
    "ERROR_WEAK_PASSWORD" -> "La contraseña es demasiado débil (mínimo 6 caracteres)."
    "ERROR_USER_DISABLED" -> "La cuenta está deshabilitada."
    "ERROR_TOO_MANY_REQUESTS" -> "Demasiados intentos. Esperá unos minutos y volvé a probar."
    "ERROR_USER_TOKEN_EXPIRED", "ERROR_INVALID_USER_TOKEN" ->
        "La sesión venció. Iniciá sesión nuevamente."
    "SIN_CONEXION" -> "No hay conexión. Para iniciar sesión o registrarte necesitás internet."
    else -> "No se pudo completar la operación. Intentá nuevamente."
}

/** Validaciones del formulario de acceso (funciones puras, se prueban sin Android). */
const val MIN_CONTRASENA = 6

fun validarEmail(email: String): String? {
    val limpio = email.trim()
    return when {
        limpio.isEmpty() -> "Ingresá tu correo"
        !Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(limpio) -> "El correo no tiene un formato válido"
        else -> null
    }
}

fun validarContrasena(contrasena: String): String? =
    if (contrasena.length < MIN_CONTRASENA) "La contraseña debe tener al menos $MIN_CONTRASENA caracteres" else null

fun validarConfirmacion(contrasena: String, confirmacion: String): String? =
    if (contrasena != confirmacion) "Las contraseñas no coinciden" else null
