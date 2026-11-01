package com.example.recordatorios.ui.listado

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.recordatorios.AppContainer
import com.example.recordatorios.Recordatorio
import com.example.recordatorios.data.ModoSimulacion
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
            initializer { ListadoViewModel(AppContainer.repositorio, AppContainer.simulacion) }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Al volver (por ejemplo, tras guardar) se actualiza la lista sin mostrar "Cargando".
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refrescar() }

    ListadoScreen(
        uiState = uiState,
        onCrearRecordatorio = onCrearRecordatorio,
        onRecordatorioClick = onRecordatorioClick,
        onReintentar = viewModel::cargar,
        onSimular = viewModel::simular,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListadoScreen(
    uiState: ListadoUiState,
    onCrearRecordatorio: () -> Unit,
    onRecordatorioClick: (Long) -> Unit,
    onReintentar: () -> Unit,
    onSimular: (ModoSimulacion) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Mis recordatorios") },
                actions = { MenuSimulacion(onSimular) }
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
        // when exhaustivo: no puede mostrarse contenido, carga y error a la vez.
        when (uiState) {
            ListadoUiState.Cargando ->
                EstadoCargando(modifier = Modifier.padding(innerPadding))

            ListadoUiState.Vacio ->
                EstadoVacio(onCrear = onCrearRecordatorio, modifier = Modifier.padding(innerPadding))

            is ListadoUiState.ConDatos ->
                ListaRecordatorios(
                    recordatorios = uiState.recordatorios,
                    onRecordatorioClick = onRecordatorioClick,
                    modifier = Modifier.padding(innerPadding)
                )

            is ListadoUiState.Error ->
                EstadoError(
                    mensaje = uiState.mensaje,
                    onReintentar = onReintentar,
                    modifier = Modifier.padding(innerPadding)
                )
        }
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

/** Mecanismo de simulación: permite comprobar cada estado sin base de datos ni red. */
@Composable
private fun MenuSimulacion(onSimular: (ModoSimulacion) -> Unit) {
    var expandido by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expandido = true }) { Text("Simular") }
        DropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            ModoSimulacion.entries.forEach { modo ->
                DropdownMenuItem(
                    text = { Text(modo.etiqueta) },
                    onClick = {
                        expandido = false
                        onSimular(modo)
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ListadoConDatosPreview() {
    RecordatoriosTheme {
        ListadoScreen(
            uiState = ListadoUiState.ConDatos(recordatoriosSimulados),
            onCrearRecordatorio = {},
            onRecordatorioClick = {},
            onReintentar = {},
            onSimular = {}
        )
    }
}
