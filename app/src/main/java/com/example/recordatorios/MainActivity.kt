package com.example.recordatorios

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.recordatorios.data.auth.EstadoAutenticacion
import com.example.recordatorios.data.prefs.TemaApp
import com.example.recordatorios.ui.auth.AuthRoute
import com.example.recordatorios.ui.navigation.AppNavHost
import com.example.recordatorios.ui.theme.RecordatoriosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appViewModel: AppViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { AppViewModel(AppContainer.preferencias, AppContainer.auth) }
                }
            )
            val preferencias by appViewModel.preferencias.collectAsStateWithLifecycle()
            val sesion by appViewModel.sesion.collectAsStateWithLifecycle()
            val actuales = preferencias
            if (actuales != null) {
                val oscuro = when (actuales.tema) {
                    TemaApp.Sistema -> isSystemInDarkTheme()
                    TemaApp.Claro -> false
                    TemaApp.Oscuro -> true
                }
                RecordatoriosTheme(darkTheme = oscuro) {
                    RaizApp(sesion)
                }
            }
        }
    }
}

/**
 * Decide qué mostrar según el estado de la sesión:
 * verificando -> carga, sin sesión -> acceso, sesión activa -> la app.
 */
@Composable
private fun RaizApp(sesion: EstadoAutenticacion) {
    when (sesion) {
        EstadoAutenticacion.Verificando -> PantallaVerificando()
        is EstadoAutenticacion.SinSesion -> AuthRoute(aviso = sesion.motivo)
        is EstadoAutenticacion.Activa ->
            // key(uid): al cambiar de cuenta se descarta toda la navegación y sus
            // ViewModels; la cuenta nueva no hereda ningún estado de la anterior.
            key(sesion.sesion.uid) { AppNavHost() }
    }
}

@Composable
private fun PantallaVerificando() {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(
                modifier = Modifier.semantics { contentDescription = "Verificando la sesión" }
            )
        }
    }
}
