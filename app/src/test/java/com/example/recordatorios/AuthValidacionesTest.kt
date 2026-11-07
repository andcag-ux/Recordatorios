package com.example.recordatorios

import com.example.recordatorios.data.auth.mensajeErrorAuth
import com.example.recordatorios.data.auth.validarConfirmacion
import com.example.recordatorios.data.auth.validarContrasena
import com.example.recordatorios.data.auth.validarEmail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidacionesTest {

    @Test
    fun email_valido_e_invalido() {
        assertNull(validarEmail(" ana@ejemplo.com "))
        assertNotNull(validarEmail(""))
        assertNotNull(validarEmail("ana@"))
        assertNotNull(validarEmail("ana ejemplo.com"))
    }

    @Test
    fun contrasena_minimo_seis_caracteres() {
        assertNotNull(validarContrasena("12345"))
        assertNull(validarContrasena("123456"))
    }

    @Test
    fun confirmacion_debe_coincidir() {
        assertNotNull(validarConfirmacion("secreto1", "secreto2"))
        assertNull(validarConfirmacion("secreto1", "secreto1"))
    }

    @Test
    fun credenciales_incorrectas_no_revelan_si_el_correo_existe() {
        // Mismo mensaje para usuario inexistente y contraseña equivocada.
        assertEquals(mensajeErrorAuth("ERROR_USER_NOT_FOUND"), mensajeErrorAuth("ERROR_WRONG_PASSWORD"))
        assertEquals(mensajeErrorAuth("ERROR_INVALID_CREDENTIAL"), mensajeErrorAuth("ERROR_WRONG_PASSWORD"))
    }
}
