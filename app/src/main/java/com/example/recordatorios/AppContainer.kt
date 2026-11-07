package com.example.recordatorios

import android.content.Context
import com.example.recordatorios.data.MonitorConectividad
import com.example.recordatorios.data.RecordatorioRepository
import com.example.recordatorios.data.RecordatorioRepositoryImpl
import com.example.recordatorios.data.auth.AuthRepository
import com.example.recordatorios.data.auth.EstadoAutenticacion
import com.example.recordatorios.data.auth.FirebaseAuthRepository
import com.example.recordatorios.data.local.AppDatabase
import com.example.recordatorios.data.prefs.PreferenciasRepository
import com.example.recordatorios.data.remote.FirestoreFuenteRemota
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Inyección de dependencias manual. Los ViewModels reciben interfaces; solo este
 * objeto conoce las implementaciones concretas (Room, DataStore, Firebase).
 */
object AppContainer {

    lateinit var repositorio: RecordatorioRepository
        private set

    lateinit var preferencias: PreferenciasRepository
        private set

    lateinit var auth: AuthRepository
        private set

    fun inicializar(context: Context) {
        if (::repositorio.isInitialized) return
        val app = context.applicationContext
        val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        preferencias = PreferenciasRepository(app)

        // Firestore sin caché persistente: la copia local de la app es Room.
        val firestore = FirebaseFirestore.getInstance().apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()
        }

        auth = FirebaseAuthRepository(FirebaseAuth.getInstance(), alcance)

        // uid de la cuenta activa, compartido por el repositorio para filtrar Room y armar las rutas de Firestore.
        val uidActivo = MutableStateFlow<String?>(null)
        val monitor = MonitorConectividad(app) { repositorio.sincronizarEnSegundoPlano() }

        repositorio = RecordatorioRepositoryImpl(
            dao = AppDatabase.obtener(app).recordatorioDao(),
            remota = FirestoreFuenteRemota(firestore),
            preferencias = preferencias.preferencias,
            uidActivo = uidActivo,
            scope = alcance,
            hayConexion = monitor::hayConexion
        )

        // Coordinación de sesión: cuando cambia la cuenta, se limpia lo de la anterior
        // y se descargan los datos de la nueva (por ejemplo, en un segundo dispositivo).
        alcance.launch {
            var anterior: String? = null
            auth.estado.collect { estado ->
                val uid = when (estado) {
                    is EstadoAutenticacion.Activa -> estado.sesion.uid
                    is EstadoAutenticacion.SinSesion -> null
                    EstadoAutenticacion.Verificando -> return@collect
                }
                if (uid == anterior) return@collect
                val saliente = anterior
                anterior = uid
                uidActivo.value = uid
                if (saliente != null) repositorio.alCerrarSesion(saliente)
                if (uid != null) repositorio.sincronizarEnSegundoPlano()
            }
        }

        // Al recuperarse la conexión se intenta sincronizar (el repositorio evita solapamientos).
        monitor.iniciar()
    }
}
