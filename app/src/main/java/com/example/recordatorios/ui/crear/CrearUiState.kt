package com.example.recordatorios.ui.crear

import com.example.recordatorios.Categoria
import com.example.recordatorios.Prioridad
import com.example.recordatorios.validarCategoria
import com.example.recordatorios.validarDescripcion
import com.example.recordatorios.validarPrioridad
import com.example.recordatorios.validarTitulo

enum class CampoFormulario { Titulo, Descripcion }

/** Estado inmutable del formulario. Cada cambio produce una nueva instancia. */
data class CrearUiState(
    val titulo: String = "",
    val descripcion: String = "",
    val categoria: Categoria? = null,
    val prioridad: Prioridad? = null,
    val camposTocados: Set<CampoFormulario> = emptySet(),
    val enviando: Boolean = false,
    val guardadoExitoso: Boolean = false,
    val errorGuardado: String? = null
) {
    // Mensajes de validación derivados de los valores actuales.
    val errorTitulo: String? get() = validarTitulo(titulo)
    val errorDescripcion: String? get() = validarDescripcion(descripcion)
    val errorCategoria: String? get() = validarCategoria(categoria)
    val errorPrioridad: String? get() = validarPrioridad(prioridad)

    // Solo se muestran los errores de campos de texto que la persona ya editó.
    val errorTituloVisible: String? get() =
        if (CampoFormulario.Titulo in camposTocados) errorTitulo else null
    val errorDescripcionVisible: String? get() =
        if (CampoFormulario.Descripcion in camposTocados) errorDescripcion else null

    val esValido: Boolean get() =
        errorTitulo == null && errorDescripcion == null &&
            errorCategoria == null && errorPrioridad == null

    /** El guardado se deshabilita con formulario inválido o con un envío en curso. */
    val puedeGuardar: Boolean get() = esValido && !enviando
}

/** Eventos que la interfaz envía al ViewModel. */
sealed interface CrearEvento {
    data class TituloCambiado(val valor: String) : CrearEvento
    data class DescripcionCambiada(val valor: String) : CrearEvento
    data class CategoriaSeleccionada(val categoria: Categoria) : CrearEvento
    data class PrioridadSeleccionada(val prioridad: Prioridad) : CrearEvento
    data object GuardarPresionado : CrearEvento
}
