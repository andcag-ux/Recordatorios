package com.example.recordatorios

import com.example.recordatorios.data.aDominio
import com.example.recordatorios.data.aEntidadNueva
import com.example.recordatorios.data.aEntidadONull
import com.example.recordatorios.data.aRemoto
import com.example.recordatorios.data.local.CodigoOperacion
import com.example.recordatorios.data.local.CodigoSync
import com.example.recordatorios.data.remote.RecordatorioRemoto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapeosTest {

    private val recordatorio = Recordatorio(
        id = 0,
        titulo = "Preparar informe",
        categoria = categoriaTrabajo,
        prioridad = Prioridad.Alta,
        estado = Estado.Pospuesto("05/11"),
        descripcion = "Con gráficos"
    )

    @Test
    fun dominio_entidad_dominio_conserva_los_datos() {
        val entidad = recordatorio.aEntidadNueva(uid = "ana", clientId = "abc", creadoEn = 10L)
        assertEquals("ana", entidad.uid)
        assertEquals(CodigoSync.PENDIENTE, entidad.syncEstado)
        assertEquals(CodigoOperacion.CREAR, entidad.operacion)

        val vuelta = entidad.aDominio()
        assertEquals(recordatorio.titulo, vuelta.titulo)
        assertEquals(recordatorio.prioridad, vuelta.prioridad)
        assertEquals(recordatorio.estado, vuelta.estado)
        assertEquals(EstadoSync.Pendiente, vuelta.sync)
    }

    @Test
    fun entidad_a_documento_usa_el_clientId_como_id() {
        val entidad = recordatorio.aEntidadNueva("ana", "uuid-1", 10L)
        val remoto = entidad.aRemoto()
        assertEquals("uuid-1", remoto.id)
        assertTrue("id" !in remoto.aMapa()) // el id es la clave del documento, no un campo
    }

    @Test
    fun documento_valido_se_transforma_en_entidad_sincronizada_de_la_cuenta() {
        val remoto = RecordatorioRemoto(
            id = "c-7", titulo = " Pagar luz ", categoria = "Personal",
            prioridad = "BAJA", estado = "COMPLETADO", creadoEn = 5L
        )
        val entidad = remoto.aEntidadONull(uid = "ana", ahora = 99L)
        assertNotNull(entidad)
        assertEquals("ana", entidad!!.uid)
        assertEquals("c-7", entidad.clientId)
        assertEquals("Pagar luz", entidad.titulo)
        assertTrue(entidad.enServidor)
        assertEquals(CodigoSync.SINCRONIZADO, entidad.syncEstado)
        assertEquals(CodigoOperacion.NINGUNA, entidad.operacion)
    }

    @Test
    fun documento_sin_titulo_se_descarta() {
        assertNull(RecordatorioRemoto(id = "1", titulo = "   ").aEntidadONull("ana", 0L))
        assertNull(RecordatorioRemoto(id = "1", titulo = null).aEntidadONull("ana", 0L))
    }

    @Test
    fun valores_desconocidos_usan_predeterminados() {
        val remoto = RecordatorioRemoto(id = "1", titulo = "Algo largo", prioridad = "URGENTE", estado = "ROTO")
        val dominio = remoto.aEntidadONull("ana", 0L)!!.aDominio()
        assertEquals(Prioridad.Media, dominio.prioridad)
        assertEquals(Estado.Pendiente, dominio.estado)
    }

    @Test
    fun mapa_de_firestore_con_tipos_inesperados_no_falla() {
        val remoto = RecordatorioRemoto.desdeMapa("d1", mapOf("titulo" to 123, "creadoEn" to 50L))
        assertNull(remoto.titulo)
        assertEquals(50L, remoto.creadoEn)
    }
}
