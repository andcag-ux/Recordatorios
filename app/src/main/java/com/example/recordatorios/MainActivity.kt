package com.example.recordatorios

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.recordatorios.ui.theme.RecordatoriosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecordatoriosTheme {
                PantallaRecordatorios()
            }
        }
    }
}

// MAP de filtros: nombre del chip -> condición que debe cumplir el recordatorio.
private val filtros: Map<String, (Recordatorio) -> Boolean> = mapOf(
    "Todos" to { true },
    "Pendientes" to { it.estado == Estado.Pendiente },
    "Completados" to { it.estado == Estado.Completado },
    "Prioridad alta" to { it.prioridad == Prioridad.Alta }
)

// ---------------------------------------------------------------
// Consigna 4: pantalla inicial con Jetpack Compose y Material 3
// ---------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaRecordatorios() {
    var filtroActual by remember { mutableStateOf("Todos") }
    val visibles = recordatoriosSimulados.filter(filtros.getValue(filtroActual))

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text("Mis recordatorios") })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Text(
                text = "${categoriasUnicas.size} categorías · ${visibles.size} recordatorios",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filtros.keys.forEach { nombre ->
                    FilterChip(
                        selected = filtroActual == nombre,
                        onClick = { filtroActual = nombre },
                        label = { Text(nombre) }
                    )
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(visibles, key = { it.id }) { recordatorio ->
                    TarjetaRecordatorio(recordatorio)
                }
            }
        }
    }
}

@Composable
fun TarjetaRecordatorio(r: Recordatorio) {
    // Color de la categoría leído desde el Map; si no existe, gris.
    val color = Color(coloresCategoria[r.categoria] ?: 0xFF9E9E9E)

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = r.titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = r.categoria.nombre,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .background(color, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                )
                Text(
                    text = "Prioridad: ${r.prioridad.etiqueta}",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Text(
                text = "Estado: ${r.estado.etiqueta}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            // Datos opcionales: solo se muestran si existen (null safety).
            r.descripcion?.let {
                Text(it, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp))
            }
            r.fechaLimite?.let {
                Text("Vence: $it", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PantallaPreview() {
    RecordatoriosTheme { PantallaRecordatorios() }
}
