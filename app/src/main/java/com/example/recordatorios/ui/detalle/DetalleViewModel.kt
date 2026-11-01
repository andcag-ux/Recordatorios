package com.example.recordatorios.ui.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recordatorios.data.RecordatorioRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Recibe únicamente el identificador del recordatorio, no el objeto completo. */
class DetalleViewModel(
    private val id: Long,
    private val repositorio: RecordatorioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetalleUiState>(DetalleUiState.Cargando)
    val uiState: StateFlow<DetalleUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _uiState.update { DetalleUiState.Cargando }
            val nuevoEstado: DetalleUiState = try {
                val recordatorio = repositorio.obtenerPorId(id)
                if (recordatorio == null) DetalleUiState.NoEncontrado
                else DetalleUiState.Encontrado(recordatorio)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DetalleUiState.Error(e.message ?: "Ocurrió un error inesperado.")
            }
            _uiState.update { nuevoEstado }
        }
    }
}
