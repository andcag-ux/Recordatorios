package com.example.recordatorios

import com.example.recordatorios.data.EstadoSincronizacion
import com.example.recordatorios.data.RecordatorioRepositoryImpl
import com.example.recordatorios.data.prefs.Preferencias
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SincronizacionTest {

    private val dao = FakeDao()
    private val api = FakeApi()

    private fun TestScope.repositorio(): RecordatorioRepositoryImpl {
        var n = 0
        return RecordatorioRepositoryImpl(
            dao = dao,
            api = api,
            preferencias = flowOf(Preferencias()),
            scope = this, // no backgroundScope: advanceUntilIdle() no ejecuta sus tareas
            ahora = { 1_000L + n },
            nuevoClientId = { "cliente-${++n}" }
        )
    }

    private fun nuevo(titulo: String) =
        Recordatorio(0, titulo, categoriaTrabajo, Prioridad.Media)

    @Test
    fun sin_conexion_el_registro_se_guarda_en_Room_y_queda_pendiente() = runTest {
        api.sinConexion = true
        val repo = repositorio()

        repo.crear(nuevo("Registro sin red"))
        advanceUntilIdle()

        val lista = repo.observarListado().first()
        assertEquals(1, lista.size)
        assertEquals(EstadoSync.Pendiente, lista.first().sync)
        assertTrue(repo.estadoSincronizacion.value is EstadoSincronizacion.Fallida)
    }

    @Test
    fun al_recuperar_la_red_se_envia_y_se_confirma_sin_crear_otro_registro() = runTest {
        api.sinConexion = true
        val repo = repositorio()
        repo.crear(nuevo("Registro sin red"))
        advanceUntilIdle()

        api.sinConexion = false
        repo.sincronizar()

        assertEquals(1, dao.todas.size)
        assertEquals(EstadoSync.Sincronizado, repo.observarListado().first().first().sync)
        assertEquals(1, api.servidor.size)
        assertTrue(repo.estadoSincronizacion.value is EstadoSincronizacion.Correcta)
    }

    @Test
    fun dos_sincronizaciones_simultaneas_envian_la_operacion_una_sola_vez() = runTest {
        val repo = repositorio()
        api.sinConexion = true
        repo.crear(nuevo("Registro unico"))
        advanceUntilIdle()

        api.sinConexion = false
        api.demoraMs = 500
        val llamadasAntes = api.llamadasCrear
        launch { repo.sincronizar() }
        launch { repo.sincronizar() } // el Mutex descarta este disparo
        advanceUntilIdle()

        assertEquals("estado=${repo.estadoSincronizacion.value} filas=${dao.todas} servidor=${api.servidor}", 1, api.llamadasCrear - llamadasAntes)
        assertEquals(1, dao.todas.size)
        assertEquals(1, api.servidor.size)
    }

    @Test
    fun el_reintento_usa_el_mismo_clientId_y_no_duplica() = runTest {
        val repo = repositorio()
        api.sinConexion = true
        repo.crear(nuevo("Reintento"))
        advanceUntilIdle()
        api.sinConexion = false

        repo.sincronizar()
        repo.sincronizar()
        repo.sincronizar()

        assertEquals(1, dao.todas.size)
        assertEquals(1, api.servidor.size)
        assertEquals(1, api.clientIdsRecibidos.toSet().size)
    }

    @Test
    fun la_descarga_incorpora_datos_del_servicio_sin_duplicar() = runTest {
        api.servidor += com.example.recordatorios.data.remote.RecordatorioDto(
            id = 1, clientId = "s-1", titulo = "Desde el servidor", categoria = "Trabajo",
            prioridad = "ALTA", estado = "PENDIENTE", creadoEn = 50L
        )
        val repo = repositorio()

        repo.sincronizar()
        repo.sincronizar()

        assertEquals(1, dao.todas.size)
        assertEquals("Desde el servidor", repo.observarListado().first().first().titulo)
    }

    @Test
    fun eliminar_un_registro_sincronizado_lo_borra_tambien_del_servicio() = runTest {
        val repo = repositorio()
        repo.crear(nuevo("Para borrar"))
        advanceUntilIdle()
        val id = dao.todas.first().id
        assertEquals("estado=${repo.estadoSincronizacion.value} filas=${dao.todas} servidor=${api.servidor}", 1, api.servidor.size)

        repo.eliminar(id)
        advanceUntilIdle()

        assertEquals(0, dao.todas.size)
        assertEquals(0, api.servidor.size)
    }

    @Test
    fun editar_un_registro_pendiente_de_creacion_sigue_siendo_una_creacion() = runTest {
        api.sinConexion = true
        val repo = repositorio()
        val id = repo.crear(nuevo("Titulo original"))
        advanceUntilIdle()

        repo.actualizar(nuevo("Titulo editado").copy(id = id))
        advanceUntilIdle()
        api.sinConexion = false
        repo.sincronizar()

        assertEquals(1, api.servidor.size)
        assertEquals("Titulo editado", api.servidor.first().titulo)
        assertEquals(1, dao.todas.size)
    }
}
