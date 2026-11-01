package com.example.recordatorios.ui.crear

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.recordatorios.AppContainer
import com.example.recordatorios.MAX_DESCRIPCION
import com.example.recordatorios.categoriasDisponibles
import com.example.recordatorios.prioridadesDisponibles
import com.example.recordatorios.ui.components.CampoTexto
import com.example.recordatorios.ui.components.SelectorOpciones

/**
 * Conecta el ViewModel con la pantalla. [onGuardar] se invoca cuando el guardado
 * finalizó con éxito; [onCancelar] cuando la persona decide salir sin guardar.
 */
@Composable
fun CrearRoute(
    onGuardar: () -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CrearViewModel = viewModel(
        factory = viewModelFactory {
            initializer { CrearViewModel(AppContainer.repositorio) }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.guardadoExitoso) {
        if (uiState.guardadoExitoso) onGuardar()
    }

    CrearScreen(
        uiState = uiState,
        onEvento = viewModel::onEvento,
        onGuardar = { viewModel.onEvento(CrearEvento.GuardarPresionado) },
        onCancelar = onCancelar,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearScreen(
    uiState: CrearUiState,
    onEvento: (CrearEvento) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Nuevo recordatorio") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CampoTexto(
                valor = uiState.titulo,
                onValorChange = { onEvento(CrearEvento.TituloCambiado(it)) },
                etiqueta = "Título *",
                mensajeError = uiState.errorTituloVisible,
                ayuda = "Mínimo 4 caracteres",
                enabled = !uiState.enviando
            )

            CampoTexto(
                valor = uiState.descripcion,
                onValorChange = { onEvento(CrearEvento.DescripcionCambiada(it)) },
                etiqueta = "Descripción (opcional)",
                mensajeError = uiState.errorDescripcionVisible,
                ayuda = "${uiState.descripcion.length}/$MAX_DESCRIPCION",
                singleLine = false,
                minLines = 3,
                enabled = !uiState.enviando
            )

            SelectorOpciones(
                titulo = "Categoría *",
                opciones = categoriasDisponibles,
                seleccionada = uiState.categoria,
                textoOpcion = { it.nombre },
                onSeleccionar = { onEvento(CrearEvento.CategoriaSeleccionada(it)) },
                ayuda = if (uiState.categoria == null) "Obligatoria" else null,
                enabled = !uiState.enviando
            )

            SelectorOpciones(
                titulo = "Prioridad *",
                opciones = prioridadesDisponibles,
                seleccionada = uiState.prioridad,
                textoOpcion = { it.etiqueta },
                onSeleccionar = { onEvento(CrearEvento.PrioridadSeleccionada(it)) },
                ayuda = if (uiState.prioridad == null) "Obligatoria" else null,
                enabled = !uiState.enviando
            )

            if (uiState.errorGuardado != null) {
                Text(
                    text = uiState.errorGuardado,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }
                )
            }

            if (!uiState.esValido && !uiState.enviando) {
                Text(
                    text = "Completá los campos obligatorios (*) para habilitar el guardado.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancelar,
                    enabled = !uiState.enviando,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = onGuardar,
                    enabled = uiState.puedeGuardar,
                    modifier = Modifier.weight(1f)
                ) {
                    if (uiState.enviando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Guardando…")
                    } else {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}
