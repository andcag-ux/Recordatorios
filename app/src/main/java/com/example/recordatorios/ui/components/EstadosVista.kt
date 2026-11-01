package com.example.recordatorios.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Base reutilizable para comunicar carga, vacío o error, con una acción opcional. */
@Composable
fun MensajeEstado(
    titulo: String,
    mensaje: String,
    modifier: Modifier = Modifier,
    textoAccion: String? = null,
    onAccion: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(titulo, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(mensaje, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        if (textoAccion != null && onAccion != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAccion) { Text(textoAccion) }
        }
    }
}

@Composable
fun EstadoCargando(
    modifier: Modifier = Modifier,
    mensaje: String = "Cargando…"
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text(mensaje, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun EstadoVacio(
    onCrear: () -> Unit,
    modifier: Modifier = Modifier
) {
    MensajeEstado(
        titulo = "Todavía no hay recordatorios",
        mensaje = "Creá tu primer recordatorio para empezar a organizar tus tareas.",
        modifier = modifier,
        textoAccion = "Crear recordatorio",
        onAccion = onCrear
    )
}

@Composable
fun EstadoError(
    mensaje: String,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier
) {
    MensajeEstado(
        titulo = "Algo salió mal",
        mensaje = mensaje,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        textoAccion = "Reintentar",
        onAccion = onReintentar
    )
}
