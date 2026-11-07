package com.example.recordatorios

import com.example.recordatorios.data.remote.ResultadoRed
import com.example.recordatorios.data.remote.clasificarCodigoFirestore
import com.example.recordatorios.data.remote.llamadaRemota
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ResultadoRedTest {

    @Test
    fun operacion_correcta_es_exito() = runTest {
        assertEquals(ResultadoRed.Exito(3), llamadaRemota { 3 })
    }

    @Test
    fun permiso_denegado_no_es_transitorio() {
        val r = clasificarCodigoFirestore("PERMISSION_DENIED")
        assertEquals(ResultadoRed.AccesoDenegado, r)
        assertFalse(r.esTransitoria)
    }

    @Test
    fun sin_autenticar_es_sesion_invalida() {
        assertEquals(ResultadoRed.SesionInvalida, clasificarCodigoFirestore("UNAUTHENTICATED"))
    }

    @Test
    fun servicio_no_disponible_es_transitorio() {
        assertTrue(clasificarCodigoFirestore("UNAVAILABLE").esTransitoria)
        assertTrue(clasificarCodigoFirestore("INTERNAL").esTransitoria)
    }

    @Test
    fun timeout_y_falta_de_red_se_distinguen() = runTest {
        val lenta = llamadaRemota(timeoutMs = 100) { delay(1_000); 1 }
        val sinRed = llamadaRemota<Int> { throw IOException("sin red") }
        assertEquals(ResultadoRed.Timeout, lenta)
        assertEquals(ResultadoRed.SinConexion, sinRed)
    }
}
