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
import com.example.recordatorios.data.auth.AuthRepository
import com.example.recordatorios.data.auth.EstadoAutenticacion
import com.example.recordatorios.data.auth.SesionUsuario
import com.example.recordatorios.data.remote.ResultadoRed
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

class ListadoViewModel(
    private val repositorio: RecordatorioRepository,
    private val preferenciasRepositorio: PreferenciasRepository,
    private val auth: AuthRepository
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

    /** Cuenta activa (para mostrar el correo en el menú). */
    val cuenta: StateFlow<SesionUsuario?> = auth.estado
        .map { (it as? EstadoAutenticacion.Activa)?.sesion }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** null = sin diálogo; un número = diálogo de cierre abierto con esa cantidad de cambios pendientes. */
    private val _confirmarCierre = MutableStateFlow<Int?>(null)
    val confirmarCierre: StateFlow<Int?> = _confirmarCierre.asStateFlow()

    /** Resultado de la prueba de acceso a otra cuenta (solo variante debug). */
    private val _pruebaAcceso = MutableStateFlow(PruebaAccesoUi())
    val pruebaAcceso: StateFlow<PruebaAccesoUi> = _pruebaAcceso.asStateFlow()

    fun reintentar() {
        intentos.update { it + 1 }
    }

    fun sincronizar() = repositorio.sincronizarEnSegundoPlano()

    // ---- Cierre de sesión ----

    /** Antes de cerrar se informa si hay cambios sin sincronizar. */
    fun solicitarCierreSesion() {
        viewModelScope.launch { _confirmarCierre.value = repositorio.pendientesDeSincronizar() }
    }

    fun cancelarCierreSesion() {
        _confirmarCierre.value = null
    }

    fun confirmarCierreSesion() {
        _confirmarCierre.value = null
        auth.cerrarSesion() // la limpieza local la coordina la capa de datos al detectar el cambio de cuenta
    }

    // ---- Prueba de seguridad (debug) ----

    fun abrirPruebaAcceso() = _pruebaAcceso.update { PruebaAccesoUi(visible = true) }

    fun cerrarPruebaAcceso() = _pruebaAcceso.update { PruebaAccesoUi() }

    fun cambiarUidAjeno(uid: String) = _pruebaAcceso.update { it.copy(uidAjeno = uid, resultado = null) }

    fun probarAccesoAjeno() {
        val uidAjeno = _pruebaAcceso.value.uidAjeno.trim()
        val propio = cuenta.value?.uid
        if (uidAjeno.isEmpty()) {
            _pruebaAcceso.update { it.copy(resultado = "Ingresá el uid de otra cuenta.") }
            return
        }
        if (uidAjeno == propio) {
            _pruebaAcceso.update { it.copy(resultado = "Ese es tu propio uid: ingresá el de OTRA cuenta.") }
            return
        }
        _pruebaAcceso.update { it.copy(enCurso = true, resultado = null) }
        viewModelScope.launch {
            val lectura = when (val r = repositorio.intentarLeerCuentaAjena(uidAjeno)) {
                is ResultadoRed.Exito -> "LECTURA PERMITIDA: se leyeron ${r.datos} documentos ajenos. Revisá las reglas."
                ResultadoRed.AccesoDenegado -> "Lectura RECHAZADA por Firestore (PERMISSION_DENIED)."
                is ResultadoRed.Falla -> "Lectura no completada: ${r.mensaje}"
            }
            val escritura = when (val r = repositorio.intentarEscribirEnCuentaAjena(uidAjeno)) {
                is ResultadoRed.Exito -> "ESCRITURA PERMITIDA en la otra cuenta. Revisá las reglas."
                ResultadoRed.AccesoDenegado -> "Escritura RECHAZADA por Firestore (PERMISSION_DENIED)."
                is ResultadoRed.Falla -> "Escritura no completada: ${r.mensaje}"
            }
            _pruebaAcceso.update { it.copy(enCurso = false, resultado = "$lectura\n$escritura") }
        }
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

/** Estado del diálogo de prueba de acceso a otra cuenta (variante debug). */
data class PruebaAccesoUi(
    val visible: Boolean = false,
    val uidAjeno: String = "",
    val enCurso: Boolean = false,
    val resultado: String? = null
)
