package com.example.recordatorios.ui.detalle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.recordatorios.AppContainer
import com.example.recordatorios.Recordatorio
import com.example.recordatorios.ui.components.EtiquetaCategoria
import com.example.recordatorios.ui.components.EstadoCargando
import com.example.recordatorios.ui.components.EstadoError
import com.example.recordatorios.ui.components.MensajeEstado

/** Recibe solo el identificador; el ViewModel obtiene el registro desde el repositorio. */
@Composable
fun DetalleRoute(
    id: Long,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetalleViewModel = viewModel(
        factory = viewModelFactory {
            initializer { DetalleViewModel(id, AppContainer.repositorio) }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DetalleScreen(
        uiState = uiState,
        onVolver = onVolver,
        onReintentar = viewModel::cargar,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleScreen(
    uiState: DetalleUiState,
    onVolver: () -> Unit,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Detalle") },
                navigationIcon = { TextButton(onClick = onVolver) { Text("Volver") } }
            )
        }
    ) { innerPadding ->
        when (uiState) {
            DetalleUiState.Cargando ->
                EstadoCargando(modifier = Modifier.padding(innerPadding))

            is DetalleUiState.Encontrado ->
                ContenidoDetalle(uiState.recordatorio, Modifier.padding(innerPadding))

            DetalleUiState.NoEncontrado ->
                MensajeEstado(
                    titulo = "Recordatorio no encontrado",
                    mensaje = "El registro solicitado ya no existe o fue eliminado.",
                    modifier = Modifier.padding(innerPadding),
                    textoAccion = "Volver al listado",
                    onAccion = onVolver
                )

            is DetalleUiState.Error ->
                EstadoError(
                    mensaje = uiState.mensaje,
                    onReintentar = onReintentar,
                    modifier = Modifier.padding(innerPadding)
                )
        }
    }
}

@Composable
private fun ContenidoDetalle(
    recordatorio: Recordatorio,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(recordatorio.titulo, style = MaterialTheme.typography.headlineSmall)
                EtiquetaCategoria(recordatorio.categoria)
                FilaDato("Prioridad", recordatorio.prioridad.etiqueta)
                FilaDato("Estado", recordatorio.estado.etiqueta)
                FilaDato("Descripción", recordatorio.descripcion ?: "Sin descripción")
                FilaDato("Fecha límite", recordatorio.fechaLimite ?: "Sin fecha límite")
                FilaDato("Identificador", "#${recordatorio.id}")
            }
        }
    }
}

@Composable
private fun FilaDato(
    etiqueta: String,
    valor: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier
        .fillMaxWidth()
        .semantics(mergeDescendants = true) {}
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = valor, style = MaterialTheme.typography.bodyLarge)
    }
}
