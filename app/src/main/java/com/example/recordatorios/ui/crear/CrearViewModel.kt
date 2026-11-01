package com.example.recordatorios.ui.crear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recordatorios.Estado
import com.example.recordatorios.Recordatorio
import com.example.recordatorios.data.RecordatorioRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CrearViewModel(
    private val repositorio: RecordatorioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CrearUiState())
    val uiState: StateFlow<CrearUiState> = _uiState.asStateFlow()

    fun onEvento(evento: CrearEvento) {
        when (evento) {
            is CrearEvento.TituloCambiado -> _uiState.update {
                it.copy(
                    titulo = evento.valor,
                    camposTocados = it.camposTocados + CampoFormulario.Titulo,
                    errorGuardado = null
                )
            }
            is CrearEvento.DescripcionCambiada -> _uiState.update {
                it.copy(
                    descripcion = evento.valor,
                    camposTocados = it.camposTocados + CampoFormulario.Descripcion,
                    errorGuardado = null
                )
            }
            is CrearEvento.CategoriaSeleccionada -> _uiState.update {
                it.copy(categoria = evento.categoria, errorGuardado = null)
            }
            is CrearEvento.PrioridadSeleccionada -> _uiState.update {
                it.copy(prioridad = evento.prioridad, errorGuardado = null)
            }
            CrearEvento.GuardarPresionado -> guardar()
        }
    }

    private fun guardar() {
        val actual = _uiState.value
        if (!actual.puedeGuardar) return // inválido o envío en curso
        val categoria = actual.categoria ?: return
        val prioridad = actual.prioridad ?: return

        _uiState.update { it.copy(enviando = true, errorGuardado = null) }

        viewModelScope.launch {
            try {
                repositorio.guardar(
                    Recordatorio(
                        id = 0L, // el repositorio asigna el identificador
                        titulo = actual.titulo.trim(),
                        categoria = categoria,
                        prioridad = prioridad,
                        estado = Estado.Pendiente,
                        descripcion = actual.descripcion.trim().ifBlank { null }
                    )
                )
                _uiState.update { it.copy(enviando = false, guardadoExitoso = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        enviando = false,
                        errorGuardado = (e.message ?: "No se pudo guardar.") + " Intentá nuevamente."
                    )
                }
            }
        }
    }
}
