package com.example.recordatorios.ui.listado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recordatorios.data.ModoSimulacion
import com.example.recordatorios.data.RecordatorioRepository
import com.example.recordatorios.data.SimulacionRepositorio
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ListadoViewModel(
    private val repositorio: RecordatorioRepository,
    private val simulacion: SimulacionRepositorio
) : ViewModel() {

    // Estado modificable: privado. La interfaz solo recibe la versión de solo lectura.
    private val _uiState = MutableStateFlow<ListadoUiState>(ListadoUiState.Cargando)
    val uiState: StateFlow<ListadoUiState> = _uiState.asStateFlow()

    private var trabajoCarga: Job? = null

    init {
        cargar()
    }

    /** Carga inicial y reintento: muestra el estado de carga. */
    fun cargar() {
        trabajoCarga?.cancel()
        trabajoCarga = viewModelScope.launch {
            _uiState.update { ListadoUiState.Cargando }
            val nuevoEstado = obtenerEstado()
            _uiState.update { nuevoEstado }
        }
    }

    /**
     * Se llama al volver a la pantalla (por ejemplo, después de crear un
     * recordatorio). Actualiza sin mostrar "Cargando" y conserva los datos
     * actuales si la operación falla.
     */
    fun refrescar() {
        val actual = _uiState.value
        if (actual is ListadoUiState.Cargando || actual is ListadoUiState.Error) return
        trabajoCarga?.cancel()
        trabajoCarga = viewModelScope.launch {
            val nuevoEstado = obtenerEstado()
            if (nuevoEstado !is ListadoUiState.Error) {
                _uiState.update { nuevoEstado }
            }
        }
    }

    fun simular(modo: ModoSimulacion) {
        simulacion.establecerModo(modo)
        // El error de guardado se manifiesta en el próximo guardado, no en el listado.
        if (modo != ModoSimulacion.ErrorDeGuardado) cargar()
    }

    private suspend fun obtenerEstado(): ListadoUiState = try {
        val lista = repositorio.obtenerRecordatorios()
        if (lista.isEmpty()) ListadoUiState.Vacio else ListadoUiState.ConDatos(lista)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ListadoUiState.Error(e.message ?: "Ocurrió un error inesperado.")
    }
}
