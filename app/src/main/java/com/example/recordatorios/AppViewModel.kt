package com.example.recordatorios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recordatorios.data.auth.AuthRepository
import com.example.recordatorios.data.auth.EstadoAutenticacion
import com.example.recordatorios.data.prefs.Preferencias
import com.example.recordatorios.data.prefs.PreferenciasRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Estado a nivel de aplicación: el tema (DataStore) y la sesión (Firebase Auth,
 * a través de [AuthRepository]). Las preferencias empiezan en null hasta que
 * DataStore responde, para no mostrar un tema equivocado por un instante.
 */
class AppViewModel(
    preferenciasRepository: PreferenciasRepository,
    auth: AuthRepository
) : ViewModel() {
    val preferencias: StateFlow<Preferencias?> = preferenciasRepository.preferencias
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val sesion: StateFlow<EstadoAutenticacion> = auth.estado
}
