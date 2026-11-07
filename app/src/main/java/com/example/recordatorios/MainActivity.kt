package com.example.recordatorios

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.recordatorios.data.prefs.TemaApp
import com.example.recordatorios.ui.navigation.AppNavHost
import com.example.recordatorios.ui.theme.RecordatoriosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appViewModel: AppViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { AppViewModel(AppContainer.preferencias) }
                }
            )
            val preferencias by appViewModel.preferencias.collectAsStateWithLifecycle()
            val actuales = preferencias
            if (actuales != null) {
                val oscuro = when (actuales.tema) {
                    TemaApp.Sistema -> isSystemInDarkTheme()
                    TemaApp.Claro -> false
                    TemaApp.Oscuro -> true
                }
                RecordatoriosTheme(darkTheme = oscuro) {
                    AppNavHost()
                }
            }
        }
    }
}
