package com.example.recordatorios.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.recordatorios.Categoria
import com.example.recordatorios.coloresCategoria

/** Etiqueta con el color de la categoría (leído del Map). El nombre va siempre como texto. */
@Composable
fun EtiquetaCategoria(
    categoria: Categoria,
    modifier: Modifier = Modifier
) {
    val color = Color(coloresCategoria[categoria] ?: 0xFF9E9E9E)
    Text(
        text = categoria.nombre,
        color = Color.White,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier
            .background(color, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 2.dp)
    )
}
