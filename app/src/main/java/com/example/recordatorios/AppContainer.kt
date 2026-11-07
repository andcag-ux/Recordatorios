package com.example.recordatorios

import android.content.Context
import com.example.recordatorios.data.MonitorConectividad
import com.example.recordatorios.data.RecordatorioRepository
import com.example.recordatorios.data.RecordatorioRepositoryImpl
import com.example.recordatorios.data.local.AppDatabase
import com.example.recordatorios.data.prefs.PreferenciasRepository
import com.example.recordatorios.data.remote.ApiFactory
import com.example.recordatorios.data.remote.ControlRedSimulada
import com.example.recordatorios.data.remote.ServidorSimulado
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

/**
 * Inyección de dependencias manual. Los ViewModels reciben interfaces; solo este
 * objeto conoce las implementaciones concretas (Room, DataStore, Retrofit).
 */
object AppContainer {

    lateinit var repositorio: RecordatorioRepository
        private set

    lateinit var preferencias: PreferenciasRepository
        private set

    /** Solo existe cuando se usa el servidor simulado (ver local.properties.example). */
    var controlRed: ControlRedSimulada? = null
        private set

    fun inicializar(context: Context) {
        if (::repositorio.isInitialized) return
        val app = context.applicationContext

        preferencias = PreferenciasRepository(app)

        val servidor: ServidorSimulado? =
            if (BuildConfig.USAR_SERVIDOR_SIMULADO) {
                ServidorSimulado(File(app.filesDir, "servidor_simulado.json"))
            } else null
        controlRed = servidor

        val api = ApiFactory.crear(BuildConfig.API_BASE_URL, servidor)
        val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        repositorio = RecordatorioRepositoryImpl(
            dao = AppDatabase.obtener(app).recordatorioDao(),
            api = api,
            preferencias = preferencias.preferencias,
            scope = alcance
        )

        // Al recuperarse la conexión se intenta sincronizar (el repositorio evita solapamientos).
        MonitorConectividad(app) { repositorio.sincronizarEnSegundoPlano() }.iniciar()
    }
}
