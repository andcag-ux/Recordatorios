package com.example.recordatorios.ui.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recordatorios.Estado
import com.example.recordatorios.Recordatorio
import com.example.recordatorios.data.RecordatorioRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Recibe únicamente el identificador del recordatorio, no el objeto completo. */
class DetalleViewModel(
    private val id: Long,
    private val repositorio: RecordatorioRepository
) : ViewModel() {

    private val intentos = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DetalleUiState> = intentos
        .flatMapLatest {
            repositorio.observarRecordatorio(id)
                .map<Recordatorio?, DetalleUiState> { recordatorio ->
                    if (recordatorio == null) DetalleUiState.NoEncontrado
                    else DetalleUiState.Encontrado(recordatorio)
                }
                .catch { e -> emit(DetalleUiState.Error(e.message ?: "No se pudo leer la base local.")) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleUiState.Cargando)

    private val _eliminado = MutableStateFlow(false)

    /** Pasa a true cuando el recordatorio se eliminó: la pantalla vuelve al listado. */
    val eliminado: StateFlow<Boolean> = _eliminado.asStateFlow()

    fun cargar() {
        intentos.update { it + 1 }
    }

    fun alternarCompletado() {
        val actual = (uiState.value as? DetalleUiState.Encontrado)?.recordatorio ?: return
        val nuevoEstado =
            if (actual.estado == Estado.Completado) Estado.Pendiente else Estado.Completado
        viewModelScope.launch { repositorio.actualizar(actual.copy(estado = nuevoEstado)) }
    }

    fun eliminar() {
        viewModelScope.launch {
            repositorio.eliminar(id)
            _eliminado.value = true
        }
    }
}
