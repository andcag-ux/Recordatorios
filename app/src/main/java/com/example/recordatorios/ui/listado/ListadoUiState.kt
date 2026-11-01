package com.example.recordatorios.ui.listado

import com.example.recordatorios.Recordatorio

/** Estados mutuamente excluyentes de la pantalla de listado. */
sealed interface ListadoUiState {
    data object Cargando : ListadoUiState
    data object Vacio : ListadoUiState
    data class ConDatos(val recordatorios: List<Recordatorio>) : ListadoUiState
    data class Error(val mensaje: String) : ListadoUiState
}
