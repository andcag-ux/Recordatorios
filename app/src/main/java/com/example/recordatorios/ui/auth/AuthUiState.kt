package com.example.recordatorios.ui.auth

/** Las dos variantes del formulario de acceso. */
enum class ModoAcceso { IniciarSesion, Registro }

/** Estado del formulario de acceso. [enCurso] representa la autenticación en curso. */
data class AuthUiState(
    val modo: ModoAcceso = ModoAcceso.IniciarSesion,
    val email: String = "",
    val contrasena: String = "",
    val confirmacion: String = "",
    val errorEmail: String? = null,
    val errorContrasena: String? = null,
    val errorConfirmacion: String? = null,
    val enCurso: Boolean = false,
    val errorGeneral: String? = null
)
