package com.example.recordatorios

import com.example.recordatorios.data.remote.RecordatorioDto
import com.example.recordatorios.data.remote.ResultadoRed
import com.example.recordatorios.data.remote.llamadaSegura
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ResultadoRedTest {

    private fun vacio() = "".toResponseBody("application/json".toMediaType())

    @Test
    fun codigo_200_es_exito() = runTest {
        val r = llamadaSegura { Response.success(RecordatorioDto(id = 1)) }
        assertTrue(r is ResultadoRed.Exito)
    }

    @Test
    fun codigo_400_es_solicitud_invalida_no_transitoria() = runTest {
        val r = llamadaSegura { Response.error<RecordatorioDto>(400, vacio()) }
        assertEquals(ResultadoRed.SolicitudInvalida(400), r)
        assertFalse((r as ResultadoRed.Falla).esTransitoria)
    }

    @Test
    fun codigo_500_es_error_de_servidor_transitorio() = runTest {
        val r = llamadaSegura { Response.error<RecordatorioDto>(500, vacio()) }
        assertEquals(ResultadoRed.ErrorServidor(500), r)
        assertTrue((r as ResultadoRed.Falla).esTransitoria)
    }

    @Test
    fun timeout_se_distingue_de_la_falta_de_conexion() = runTest {
        val timeout = llamadaSegura<RecordatorioDto> { throw SocketTimeoutException("lento") }
        val sinRed = llamadaSegura<RecordatorioDto> { throw UnknownHostException("sin red") }
        assertEquals(ResultadoRed.Timeout, timeout)
        assertEquals(ResultadoRed.SinConexion, sinRed)
    }
}
