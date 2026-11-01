package com.example.recordatorios.ui.detalle

import com.example.recordatorios.Recordatorio

sealed interface DetalleUiState {
    data object Cargando : DetalleUiState
    data class Encontrado(val recordatorio: Recordatorio) : DetalleUiState
    data object NoEncontrado : DetalleUiState
    data class Error(val mensaje: String) : DetalleUiState
}
