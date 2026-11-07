package com.example.recordatorios.ui.listado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recordatorios.Recordatorio
import com.example.recordatorios.data.EstadoSincronizacion
import com.example.recordatorios.data.RecordatorioRepository
import com.example.recordatorios.data.prefs.OrdenListado
import com.example.recordatorios.data.prefs.Preferencias
import com.example.recordatorios.data.prefs.PreferenciasRepository
import com.example.recordatorios.data.prefs.TemaApp
import com.example.recordatorios.data.remote.ControlRedSimulada
import com.example.recordatorios.data.remote.ModoRed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ListadoViewModel(
    private val repositorio: RecordatorioRepository,
    private val preferenciasRepositorio: PreferenciasRepository,
    private val controlRed: ControlRedSimulada?
) : ViewModel() {

    // Cada incremento vuelve a suscribirse al flujo de Room (usado por "Reintentar").
    private val intentos = MutableStateFlow(0)

    // La interfaz observa Room (a través del repositorio): cualquier cambio en la base se refleja solo.
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ListadoUiState> = intentos
        .flatMapLatest {
            repositorio.observarListado()
                .map<List<Recordatorio>, ListadoUiState> { lista ->
                    if (lista.isEmpty()) ListadoUiState.Vacio else ListadoUiState.ConDatos(lista)
                }
                .catch { e -> emit(ListadoUiState.Error(e.message ?: "No se pudo leer la base local.")) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListadoUiState.Cargando)

    val sincronizacion: StateFlow<EstadoSincronizacion> = repositorio.estadoSincronizacion

    val preferencias: StateFlow<Preferencias> = preferenciasRepositorio.preferencias
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Preferencias())

    /** null cuando no se usa el servidor simulado: la interfaz oculta esas opciones. */
    val modoRed: StateFlow<ModoRed?> = controlRed?.modo ?: MutableStateFlow<ModoRed?>(null)

    fun reintentar() {
        intentos.update { it + 1 }
    }

    fun sincronizar() = repositorio.sincronizarEnSegundoPlano()

    fun cambiarModoRed(modo: ModoRed) {
        controlRed?.establecerModo(modo)
        // Al volver la "red", se intenta enviar lo que quedó pendiente.
        if (modo == ModoRed.Normal) repositorio.sincronizarEnSegundoPlano()
    }

    fun cambiarTema(tema: TemaApp) {
        viewModelScope.launch { preferenciasRepositorio.guardarTema(tema) }
    }

    fun cambiarMostrarCompletados(mostrar: Boolean) {
        viewModelScope.launch { preferenciasRepositorio.guardarMostrarCompletados(mostrar) }
    }

    fun cambiarOrden(orden: OrdenListado) {
        viewModelScope.launch { preferenciasRepositorio.guardarOrden(orden) }
    }
}
