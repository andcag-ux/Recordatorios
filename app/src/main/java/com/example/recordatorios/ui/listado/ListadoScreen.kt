package com.example.recordatorios.ui.listado

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.recordatorios.AppContainer
import com.example.recordatorios.BuildConfig
import com.example.recordatorios.Recordatorio
import com.example.recordatorios.data.EstadoSincronizacion
import com.example.recordatorios.data.prefs.OrdenListado
import com.example.recordatorios.data.prefs.Preferencias
import com.example.recordatorios.data.prefs.TemaApp
import com.example.recordatorios.recordatoriosSimulados
import com.example.recordatorios.ui.components.EstadoCargando
import com.example.recordatorios.ui.components.EstadoError
import com.example.recordatorios.ui.components.EstadoVacio
import com.example.recordatorios.ui.components.TarjetaRecordatorio
import com.example.recordatorios.ui.theme.RecordatoriosTheme

/** Conecta el ViewModel con la pantalla. Observa el estado respetando el ciclo de vida. */
@Composable
fun ListadoRoute(
    onCrearRecordatorio: () -> Unit,
    onRecordatorioClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ListadoViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ListadoViewModel(
                    AppContainer.repositorio,
                    AppContainer.preferencias,
                    AppContainer.auth
                )
            }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sincronizacion by viewModel.sincronizacion.collectAsStateWithLifecycle()
    val preferencias by viewModel.preferencias.collectAsStateWithLifecycle()
    val cuenta by viewModel.cuenta.collectAsStateWithLifecycle()
    val confirmarCierre by viewModel.confirmarCierre.collectAsStateWithLifecycle()
    val pruebaAcceso by viewModel.pruebaAcceso.collectAsStateWithLifecycle()

    ListadoScreen(
        uiState = uiState,
        sincronizacion = sincronizacion,
        preferencias = preferencias,
        emailCuenta = cuenta?.email,
        onCerrarSesion = viewModel::solicitarCierreSesion,
        // La prueba de seguridad solo existe en la variante debug.
        onPruebaSeguridad = if (BuildConfig.DEBUG) viewModel::abrirPruebaAcceso else null,
        onCrearRecordatorio = onCrearRecordatorio,
        onRecordatorioClick = onRecordatorioClick,
        onReintentar = viewModel::reintentar,
        onSincronizar = viewModel::sincronizar,
        onCambiarTema = viewModel::cambiarTema,
        onCambiarMostrarCompletados = viewModel::cambiarMostrarCompletados,
        onCambiarOrden = viewModel::cambiarOrden,
        modifier = modifier
    )

    confirmarCierre?.let { pendientes ->
        CerrarSesionDialog(
            pendientes = pendientes,
            onConfirmar = viewModel::confirmarCierreSesion,
            onCancelar = viewModel::cancelarCierreSesion
        )
    }
    if (pruebaAcceso.visible) {
        PruebaAccesoDialog(
            estado = pruebaAcceso,
            uidPropio = cuenta?.uid.orEmpty(),
            onUid = viewModel::cambiarUidAjeno,
            onProbar = viewModel::probarAccesoAjeno,
            onCerrar = viewModel::cerrarPruebaAcceso
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListadoScreen(
    uiState: ListadoUiState,
    sincronizacion: EstadoSincronizacion,
    preferencias: Preferencias,
    emailCuenta: String?,
    onCerrarSesion: () -> Unit,
    onPruebaSeguridad: (() -> Unit)?,
    onCrearRecordatorio: () -> Unit,
    onRecordatorioClick: (Long) -> Unit,
    onReintentar: () -> Unit,
    onSincronizar: () -> Unit,
    onCambiarTema: (TemaApp) -> Unit,
    onCambiarMostrarCompletados: (Boolean) -> Unit,
    onCambiarOrden: (OrdenListado) -> Unit,
    modifier: Modifier = Modifier
) {
    var mostrandoAjustes by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Mis recordatorios") },
                actions = {
                    MenuOpciones(
                        emailCuenta = emailCuenta,
                        onSincronizar = onSincronizar,
                        onAjustes = { mostrandoAjustes = true },
                        onCerrarSesion = onCerrarSesion,
                        onPruebaSeguridad = onPruebaSeguridad
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCrearRecordatorio,
                modifier = Modifier.semantics { contentDescription = "Crear recordatorio" }
            ) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            BannerSincronizacion(sincronizacion = sincronizacion, onReintentar = onSincronizar)

            Box(modifier = Modifier.weight(1f)) {
                // when exhaustivo: no puede mostrarse contenido, carga y error a la vez.
                when (uiState) {
                    ListadoUiState.Cargando -> EstadoCargando()

                    ListadoUiState.Vacio -> EstadoVacio(onCrear = onCrearRecordatorio)

                    is ListadoUiState.ConDatos ->
                        ListaRecordatorios(
                            recordatorios = uiState.recordatorios,
                            onRecordatorioClick = onRecordatorioClick
                        )

                    is ListadoUiState.Error ->
                        EstadoError(mensaje = uiState.mensaje, onReintentar = onReintentar)
                }
            }
        }
    }

    if (mostrandoAjustes) {
        AjustesDialog(
            preferencias = preferencias,
            onCambiarTema = onCambiarTema,
            onCambiarMostrarCompletados = onCambiarMostrarCompletados,
            onCambiarOrden = onCambiarOrden,
            onCerrar = { mostrandoAjustes = false }
        )
    }
}

@Composable
private fun ListaRecordatorios(
    recordatorios: List<Recordatorio>,
    onRecordatorioClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Las claves mantienen la posición sobre el elemento visible: si se agrega un
    // recordatorio arriba, la lista volvería a quedar desplazada. Al cambiar el
    // primer elemento se vuelve al inicio para mostrar el recordatorio nuevo.
    val estadoLista = rememberLazyListState()
    LaunchedEffect(recordatorios.firstOrNull()?.id) {
        estadoLista.animateScrollToItem(0)
    }

    LazyColumn(
        state = estadoLista,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(recordatorios, key = { it.id }) { recordatorio ->
            TarjetaRecordatorio(recordatorio = recordatorio, onClick = onRecordatorioClick)
        }
    }
}

/** Franja que informa el estado de la sincronización. Es un anuncio accesible (liveRegion). */
@Composable
private fun BannerSincronizacion(
    sincronizacion: EstadoSincronizacion,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val texto: String
    val esError: Boolean
    when (sincronizacion) {
        EstadoSincronizacion.Inactiva -> return
        EstadoSincronizacion.EnCurso -> { texto = "Sincronizando con el servidor…"; esError = false }
        is EstadoSincronizacion.Correcta -> { texto = "Sincronizado correctamente"; esError = false }
        is EstadoSincronizacion.Fallida -> {
            val pendientes = if (sincronizacion.pendientes > 0) {
                " Pendientes de enviar: ${sincronizacion.pendientes}."
            } else ""
            texto = sincronizacion.mensaje + pendientes
            esError = true
        }
    }

    Surface(
        color = if (esError) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = texto,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            if (esError) TextButton(onClick = onReintentar) { Text("Reintentar") }
        }
    }
}

/** Menú superior: cuenta, sincronizar, ajustes, cerrar sesión y (debug) la prueba de seguridad. */
@Composable
private fun MenuOpciones(
    emailCuenta: String?,
    onSincronizar: () -> Unit,
    onAjustes: () -> Unit,
    onCerrarSesion: () -> Unit,
    onPruebaSeguridad: (() -> Unit)?
) {
    var expandido by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expandido = true }) { Text("Opciones") }
        DropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            if (emailCuenta != null) {
                DropdownMenuItem(
                    text = { Text(emailCuenta, style = MaterialTheme.typography.labelMedium) },
                    enabled = false,
                    onClick = {}
                )
                HorizontalDivider()
            }
            DropdownMenuItem(
                text = { Text("Sincronizar ahora") },
                onClick = { expandido = false; onSincronizar() }
            )
            DropdownMenuItem(
                text = { Text("Ajustes") },
                onClick = { expandido = false; onAjustes() }
            )
            if (onPruebaSeguridad != null) {
                DropdownMenuItem(
                    text = { Text("Prueba de seguridad (debug)") },
                    onClick = { expandido = false; onPruebaSeguridad() }
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Cerrar sesión") },
                onClick = { expandido = false; onCerrarSesion() }
            )
        }
    }
}

/** Confirma el cierre de sesión e informa qué pasa con los datos locales. */
@Composable
private fun CerrarSesionDialog(
    pendientes: Int,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Cerrar sesión") },
        text = {
            Text(
                if (pendientes == 0) {
                    "Se borrará la copia local de tus recordatorios. Siguen guardados en tu cuenta " +
                        "y se descargan al volver a iniciar sesión."
                } else {
                    "Tenés $pendientes cambio(s) sin sincronizar. Se conservarán en este dispositivo, " +
                        "ocultos para otras cuentas, y se enviarán cuando vuelvas a iniciar sesión " +
                        "con esta cuenta. El resto de la copia local se borra."
                }
            )
        },
        confirmButton = { TextButton(onClick = onConfirmar) { Text("Cerrar sesión") } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

/**
 * Prueba de autorización (solo debug): intenta leer y escribir en la ruta de
 * OTRA cuenta. La interfaz normal nunca arma esas rutas; acá se fuerza el
 * intento para comprobar que las reglas de Firestore lo rechazan.
 */
@Composable
private fun PruebaAccesoDialog(
    estado: PruebaAccesoUi,
    uidPropio: String,
    onUid: (String) -> Unit,
    onProbar: () -> Unit,
    onCerrar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Prueba de seguridad") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tu uid: $uidPropio", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = estado.uidAjeno,
                    onValueChange = onUid,
                    label = { Text("uid de otra cuenta") },
                    singleLine = true,
                    enabled = !estado.enCurso
                )
                if (estado.enCurso) Text("Intentando acceder…")
                estado.resultado?.let {
                    Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onProbar, enabled = !estado.enCurso) { Text("Intentar acceso") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } }
    )
}

/** Preferencias guardadas en DataStore: tema, completados y orden. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AjustesDialog(
    preferencias: Preferencias,
    onCambiarTema: (TemaApp) -> Unit,
    onCambiarMostrarCompletados: (Boolean) -> Unit,
    onCambiarOrden: (OrdenListado) -> Unit,
    onCerrar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Ajustes") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Tema", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TemaApp.entries.forEach { tema ->
                        val elegido = tema == preferencias.tema
                        FilterChip(
                            selected = elegido,
                            onClick = { onCambiarTema(tema) },
                            label = { Text(if (elegido) "✓ ${tema.etiqueta}" else tema.etiqueta) }
                        )
                    }
                }

                Text("Orden del listado", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OrdenListado.entries.forEach { orden ->
                        val elegido = orden == preferencias.orden
                        FilterChip(
                            selected = elegido,
                            onClick = { onCambiarOrden(orden) },
                            label = { Text(if (elegido) "✓ ${orden.etiqueta}" else orden.etiqueta) }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Mostrar completados",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = preferencias.mostrarCompletados,
                        onCheckedChange = onCambiarMostrarCompletados
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } }
    )
}

@Preview(showBackground = true)
@Composable
private fun ListadoConDatosPreview() {
    RecordatoriosTheme {
        ListadoScreen(
            uiState = ListadoUiState.ConDatos(recordatoriosSimulados),
            sincronizacion = EstadoSincronizacion.Correcta(0L),
            preferencias = Preferencias(),
            emailCuenta = "ana@ejemplo.com",
            onCerrarSesion = {},
            onPruebaSeguridad = null,
            onCrearRecordatorio = {},
            onRecordatorioClick = {},
            onReintentar = {},
            onSincronizar = {},
            onCambiarTema = {},
            onCambiarMostrarCompletados = {},
            onCambiarOrden = {}
        )
    }
}
