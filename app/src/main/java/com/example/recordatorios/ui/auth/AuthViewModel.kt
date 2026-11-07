package com.example.recordatorios.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recordatorios.data.auth.AuthRepository
import com.example.recordatorios.data.auth.ResultadoAuth
import com.example.recordatorios.data.auth.validarConfirmacion
import com.example.recordatorios.data.auth.validarContrasena
import com.example.recordatorios.data.auth.validarEmail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Registro e inicio de sesión. La contraseña vive solo en este estado en memoria
 * mientras se completa el formulario: no se guarda en DataStore ni en Room.
 * Cuando Firebase confirma la sesión, la raíz de la app cambia de pantalla sola
 * (observa AuthRepository.estado), así que este ViewModel no navega.
 */
class AuthViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun cambiarEmail(valor: String) =
        _uiState.update { it.copy(email = valor, errorEmail = null, errorGeneral = null) }

    fun cambiarContrasena(valor: String) =
        _uiState.update { it.copy(contrasena = valor, errorContrasena = null, errorGeneral = null) }

    fun cambiarConfirmacion(valor: String) =
        _uiState.update { it.copy(confirmacion = valor, errorConfirmacion = null, errorGeneral = null) }

    fun cambiarModo(modo: ModoAcceso) =
        _uiState.update {
            it.copy(
                modo = modo, confirmacion = "", errorEmail = null, errorContrasena = null,
                errorConfirmacion = null, errorGeneral = null
            )
        }

    fun enviar() {
        val estado = _uiState.value
        if (estado.enCurso) return // evita un doble envío
        val errorEmail = validarEmail(estado.email)
        val errorContrasena = validarContrasena(estado.contrasena)
        val errorConfirmacion =
            if (estado.modo == ModoAcceso.Registro) validarConfirmacion(estado.contrasena, estado.confirmacion)
            else null
        if (errorEmail != null || errorContrasena != null || errorConfirmacion != null) {
            _uiState.update {
                it.copy(
                    errorEmail = errorEmail, errorContrasena = errorContrasena,
                    errorConfirmacion = errorConfirmacion
                )
            }
            return
        }

        _uiState.update { it.copy(enCurso = true, errorGeneral = null) }
        viewModelScope.launch {
            val resultado = when (estado.modo) {
                ModoAcceso.IniciarSesion -> auth.iniciarSesion(estado.email, estado.contrasena)
                ModoAcceso.Registro -> auth.registrar(estado.email, estado.contrasena)
            }
            _uiState.update {
                when (resultado) {
                    // Se borra la contraseña del estado apenas deja de hacer falta.
                    ResultadoAuth.Exito -> it.copy(enCurso = false, contrasena = "", confirmacion = "")
                    is ResultadoAuth.Error -> it.copy(enCurso = false, errorGeneral = resultado.mensaje)
                }
            }
        }
    }
}
