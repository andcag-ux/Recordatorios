package com.example.recordatorios.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.error as errorSemantico
import androidx.compose.ui.unit.dp

/**
 * Campo de texto controlado: el valor y los cambios viven fuera del componente.
 * Un error se comunica con isError, un mensaje textual y semántica de accesibilidad.
 */
@Composable
fun CampoTexto(
    valor: String,
    onValorChange: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    mensajeError: String? = null,
    ayuda: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValorChange,
        label = { Text(etiqueta) },
        isError = mensajeError != null,
        supportingText = {
            val texto = mensajeError ?: ayuda
            if (texto != null) Text(texto)
        },
        singleLine = singleLine,
        minLines = minLines,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .semantics { if (mensajeError != null) errorSemantico(mensajeError) }
    )
}

/**
 * Selector controlado de una opción entre varias (chips). La opción elegida se
 * marca también con "✓", de modo que el color no es el único indicador.
 */
@Composable
fun <T> SelectorOpciones(
    titulo: String,
    opciones: List<T>,
    seleccionada: T?,
    textoOpcion: (T) -> String,
    onSeleccionar: (T) -> Unit,
    modifier: Modifier = Modifier,
    mensajeError: String? = null,
    ayuda: String? = null,
    enabled: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth().selectableGroup()) {
        Text(titulo, style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier
                .padding(top = 4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            opciones.forEach { opcion ->
                val elegida = opcion == seleccionada
                FilterChip(
                    selected = elegida,
                    onClick = { onSeleccionar(opcion) },
                    enabled = enabled,
                    label = { Text(if (elegida) "✓ ${textoOpcion(opcion)}" else textoOpcion(opcion)) }
                )
            }
        }
        val texto = mensajeError ?: ayuda
        if (texto != null) {
            Text(
                text = texto,
                style = MaterialTheme.typography.bodySmall,
                color = if (mensajeError != null) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .semantics { if (mensajeError != null) errorSemantico(mensajeError) }
            )
        }
    }
}
