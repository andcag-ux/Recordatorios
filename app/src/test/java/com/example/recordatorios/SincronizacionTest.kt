package com.example.recordatorios

import com.example.recordatorios.data.EstadoSincronizacion
import com.example.recordatorios.data.RecordatorioRepositoryImpl
import com.example.recordatorios.data.prefs.Preferencias
import com.example.recordatorios.data.remote.RecordatorioRemoto
import com.example.recordatorios.data.remote.ResultadoRed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
    private val nube = FakeFirestore()
    private val uidActivo = MutableStateFlow<String?>("ana")
    private var n = 0

    private fun TestScope.repositorio(daoUsado: FakeDao = dao): RecordatorioRepositoryImpl {
        nube.uidAutenticado = "ana"
        return RecordatorioRepositoryImpl(
            dao = daoUsado,
            remota = nube,
            preferencias = flowOf(Preferencias()),
            uidActivo = uidActivo,
            scope = this, // no backgroundScope: advanceUntilIdle() no ejecuta sus tareas
            ahora = { 1_000L + n },
            nuevoClientId = { "cliente-${++n}" }
        )
    }

    private fun nuevo(titulo: String) = Recordatorio(0, titulo, categoriaTrabajo, Prioridad.Media)

    @Test
    fun sin_conexion_el_registro_se_guarda_en_Room_y_queda_pendiente() = runTest {
        nube.sinConexion = true
        val repo = repositorio()

        repo.crear(nuevo("Registro sin red"))
        advanceUntilIdle()

        val lista = repo.observarListado().first()
        assertEquals(1, lista.size)
        assertEquals(EstadoSync.Pendiente, lista.first().sync)
        assertTrue(repo.estadoSincronizacion.value is EstadoSincronizacion.Fallida)
        assertTrue(nube.documentos("ana").isEmpty())
    }

    @Test
    fun al_recuperar_la_red_se_envia_y_se_confirma_sin_duplicar() = runTest {
        nube.sinConexion = true
        val repo = repositorio()
        repo.crear(nuevo("Registro sin red"))
        advanceUntilIdle()

        nube.sinConexion = false
        repo.sincronizar()

        assertEquals(1, dao.todas.size)
        assertEquals(EstadoSync.Sincronizado, repo.observarListado().first().first().sync)
        assertEquals(1, nube.documentos("ana").size)
        // El id del documento en Firestore es el clientId de Room.
        assertEquals(dao.todas.first().clientId, nube.documentos("ana").keys.first())
    }

    @Test
    fun reintentos_repetidos_escriben_siempre_el_mismo_documento() = runTest {
        val repo = repositorio()
        nube.sinConexion = true
        repo.crear(nuevo("Reintento"))
        advanceUntilIdle()
        nube.sinConexion = false

        repo.sincronizar()
        repo.sincronizar()
        repo.sincronizar()

        assertEquals(1, dao.todas.size)
        assertEquals(1, nube.documentos("ana").size)
    }

    @Test
    fun dos_sincronizaciones_simultaneas_envian_la_operacion_una_sola_vez() = runTest {
        val repo = repositorio()
        nube.sinConexion = true
        repo.crear(nuevo("Registro unico"))
        advanceUntilIdle()

        nube.sinConexion = false
        nube.demoraMs = 500
        val antes = nube.escrituras
        launch { repo.sincronizar() }
        launch { repo.sincronizar() } // el Mutex descarta este disparo
        advanceUntilIdle()

        assertEquals(1, nube.escrituras - antes)
        assertEquals(1, nube.documentos("ana").size)
    }

    @Test
    fun modificacion_sin_conexion_se_envia_al_volver_la_red() = runTest {
        val repo = repositorio()
        val id = repo.crear(nuevo("Titulo original"))
        advanceUntilIdle()
        assertEquals("Titulo original", nube.documentos("ana").values.first().titulo)

        nube.sinConexion = true
        repo.actualizar(nuevo("Titulo editado").copy(id = id))
        advanceUntilIdle()
        assertEquals("ACTUALIZAR", dao.todas.first().operacion)

        nube.sinConexion = false
        repo.sincronizar()
        assertEquals("Titulo editado", nube.documentos("ana").values.first().titulo)
        assertEquals("NINGUNA", dao.todas.first().operacion)
    }

    @Test
    fun otro_dispositivo_recupera_los_datos_de_la_cuenta() = runTest {
        // Dispositivo 1 crea y sincroniza.
        val repo1 = repositorio()
        repo1.crear(nuevo("Creado en el telefono 1"))
        advanceUntilIdle()

        // Dispositivo 2: Room vacío, misma cuenta.
        val dao2 = FakeDao()
        val repo2 = repositorio(dao2)
        repo2.sincronizar()

        assertEquals(1, dao2.todas.size)
        assertEquals("Creado en el telefono 1", repo2.observarListado().first().first().titulo)
        assertEquals(dao.todas.first().clientId, dao2.todas.first().clientId)
    }

    @Test
    fun eliminar_un_registro_sincronizado_lo_borra_tambien_de_firestore() = runTest {
        val repo = repositorio()
        repo.crear(nuevo("Para borrar"))
        advanceUntilIdle()
        assertEquals(1, nube.documentos("ana").size)

        repo.eliminar(dao.todas.first().id)
        advanceUntilIdle()

        assertEquals(0, dao.todas.size)
        assertEquals(0, nube.documentos("ana").size)
    }

    @Test
    fun una_cuenta_nueva_no_ve_los_datos_de_la_anterior() = runTest {
        val repo = repositorio()
        repo.crear(nuevo("Dato de Ana"))
        advanceUntilIdle()

        uidActivo.value = "beto"
        nube.uidAutenticado = "beto"

        assertTrue(repo.observarListado().first().isEmpty())
    }

    @Test
    fun al_cerrar_sesion_se_borra_lo_sincronizado_y_se_conserva_lo_pendiente() = runTest {
        val repo = repositorio()
        repo.crear(nuevo("Ya sincronizado"))
        advanceUntilIdle()
        nube.sinConexion = true
        repo.crear(nuevo("Sin enviar"))
        advanceUntilIdle()

        uidActivo.value = null
        repo.alCerrarSesion("ana")

        assertEquals(1, dao.todas.size)
        assertEquals("Sin enviar", dao.todas.first().titulo)
        assertEquals("ana", dao.todas.first().uid)
    }

    @Test
    fun las_reglas_rechazan_el_acceso_a_otra_cuenta() = runTest {
        nube.documentos("beto")["x"] = RecordatorioRemoto(id = "x", titulo = "Privado de Beto")
        val repo = repositorio()

        assertEquals(ResultadoRed.AccesoDenegado, repo.intentarLeerCuentaAjena("beto"))
        assertEquals(ResultadoRed.AccesoDenegado, repo.intentarEscribirEnCuentaAjena("beto"))
        assertEquals(1, nube.documentos("beto").size)
    }

    @Test
    fun la_descarga_no_pisa_cambios_locales_pendientes() = runTest {
        val repo = repositorio()
        val id = repo.crear(nuevo("Version local"))
        advanceUntilIdle()
        val clientId = dao.todas.first().clientId

        nube.sinConexion = true
        repo.actualizar(nuevo("Editado sin red").copy(id = id))
        advanceUntilIdle()
        // Otro dispositivo cambió el documento mientras tanto.
        nube.documentos("ana")[clientId] = nube.documentos("ana")[clientId]!!.copy(titulo = "Cambio remoto")

        nube.sinConexion = false
        repo.sincronizar()

        // Se envía primero lo pendiente: gana el cambio local más reciente.
        assertEquals("Editado sin red", dao.todas.first().titulo)
        assertEquals("Editado sin red", nube.documentos("ana")[clientId]!!.titulo)
    }
}
