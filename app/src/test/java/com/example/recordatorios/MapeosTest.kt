package com.example.recordatorios

import com.example.recordatorios.data.aDominio
import com.example.recordatorios.data.aDto
import com.example.recordatorios.data.aEntidadNueva
import com.example.recordatorios.data.aEntidadONull
import com.example.recordatorios.data.local.CodigoOperacion
import com.example.recordatorios.data.local.CodigoSync
import com.example.recordatorios.data.remote.RecordatorioDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
        val entidad = recordatorio.aEntidadNueva(clientId = "abc", creadoEn = 10L)
        assertEquals(CodigoSync.PENDIENTE, entidad.syncEstado)
        assertEquals(CodigoOperacion.CREAR, entidad.operacion)

        val vuelta = entidad.aDominio()
        assertEquals(recordatorio.titulo, vuelta.titulo)
        assertEquals(recordatorio.prioridad, vuelta.prioridad)
        assertEquals(recordatorio.estado, vuelta.estado)
        assertEquals(EstadoSync.Pendiente, vuelta.sync)
    }

    @Test
    fun dto_valido_se_transforma_en_entidad_sincronizada() {
        val dto = RecordatorioDto(id = 7, clientId = "c-7", titulo = " Pagar luz ", categoria = "Personal",
            prioridad = "BAJA", estado = "COMPLETADO", creadoEn = 5L)
        val entidad = dto.aEntidadONull(ahora = 99L)
        assertNotNull(entidad)
        assertEquals(7L, entidad!!.remoteId)
        assertEquals("Pagar luz", entidad.titulo)
        assertEquals(CodigoSync.SINCRONIZADO, entidad.syncEstado)
        assertEquals(CodigoOperacion.NINGUNA, entidad.operacion)
    }

    @Test
    fun dto_sin_id_o_sin_titulo_se_descarta() {
        assertNull(RecordatorioDto(id = null, titulo = "Algo largo").aEntidadONull(0L))
        assertNull(RecordatorioDto(id = 1, titulo = "   ").aEntidadONull(0L))
    }

    @Test
    fun valores_desconocidos_del_servicio_usan_predeterminados() {
        val dto = RecordatorioDto(id = 1, titulo = "Algo largo", prioridad = "URGENTE", estado = "ROTO")
        val dominio = dto.aEntidadONull(0L)!!.aDominio()
        assertEquals(Prioridad.Media, dominio.prioridad)
        assertEquals(Estado.Pendiente, dominio.estado)
    }

    @Test
    fun entidad_a_dto_envia_clientId_para_idempotencia() {
        val entidad = recordatorio.aEntidadNueva(clientId = "uuid-1", creadoEn = 10L)
        assertEquals("uuid-1", entidad.aDto().clientId)
        assertNull(entidad.aDto().id) // todavía no tiene id remoto
    }
}
