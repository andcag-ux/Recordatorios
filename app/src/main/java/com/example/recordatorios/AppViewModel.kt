package com.example.recordatorios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recordatorios.data.prefs.Preferencias
import com.example.recordatorios.data.prefs.PreferenciasRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Expone las preferencias a nivel de aplicación (el tema). Empieza en null hasta
 * que DataStore responde, para no mostrar un tema equivocado por un instante.
 */
class AppViewModel(preferenciasRepository: PreferenciasRepository) : ViewModel() {
    val preferencias: StateFlow<Preferencias?> = preferenciasRepository.preferencias
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
