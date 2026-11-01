package com.example.recordatorios.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.recordatorios.Recordatorio
import com.example.recordatorios.recordatoriosSimulados
import com.example.recordatorios.ui.theme.RecordatoriosTheme

/**
 * Tarjeta reutilizable de un recordatorio. No conoce el NavController:
 * solo informa la selección mediante [onClick].
 */
@Composable
fun TarjetaRecordatorio(
    recordatorio: Recordatorio,
    onClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = { onClick(recordatorio.id) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = recordatorio.titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EtiquetaCategoria(recordatorio.categoria)
                Text(
                    text = "Prioridad: ${recordatorio.prioridad.etiqueta}",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Text(
                text = "Estado: ${recordatorio.estado.etiqueta}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TarjetaRecordatorioPreview() {
    RecordatoriosTheme {
        TarjetaRecordatorio(recordatorio = recordatoriosSimulados.first(), onClick = {})
    }
}
