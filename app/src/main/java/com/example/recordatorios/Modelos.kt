package com.example.recordatorios

// ---------------------------------------------------------------
// Consigna 2: modelo de datos con data classes inmutables y sealed classes
// ---------------------------------------------------------------

/** Categoría de un recordatorio. Es inmutable (solo val). */
data class Categoria(val nombre: String)

/** Prioridad: jerarquía sellada, el compilador conoce todas las opciones. */
sealed class Prioridad(val etiqueta: String, val peso: Int) {
    object Baja : Prioridad("Baja", 1)
    object Media : Prioridad("Media", 2)
    object Alta : Prioridad("Alta", 3)
}

/** Estado: algunas variantes llevan datos propios (Pospuesto) y otras no. */
sealed class Estado {
    object Pendiente : Estado()
    object Completado : Estado()
    data class Pospuesto(val hasta: String) : Estado()

    /** Texto que se muestra en pantalla. when exhaustivo: sin rama else. */
    val etiqueta: String
        get() = when (this) {
            Pendiente -> "Pendiente"
            Completado -> "Completado"
            is Pospuesto -> "Pospuesto hasta $hasta"
        }
}

/** Recordatorio. descripcion y fechaLimite son opcionales (String?). */
data class Recordatorio(
    val id: Long,
    val titulo: String,
    val categoria: Categoria,
    val prioridad: Prioridad,
    val estado: Estado = Estado.Pendiente,
    val descripcion: String? = null,
    val fechaLimite: String? = null
)

// ---------------------------------------------------------------
// Reglas de validación (evitan estados inválidos)
// ---------------------------------------------------------------

sealed class ResultadoValidacion {
    object Valido : ResultadoValidacion()
    data class Invalido(val errores: List<String>) : ResultadoValidacion()
}

fun validarRecordatorio(
    titulo: String,
    descripcion: String?,
    minTitulo: Int = 3,
    maxTitulo: Int = 60
): ResultadoValidacion {
    val errores = mutableListOf<String>()
    val tituloLimpio = titulo.trim()

    if (tituloLimpio.length < minTitulo) {
        errores.add("El título debe tener al menos $minTitulo caracteres")
    }
    if (tituloLimpio.length > maxTitulo) {
        errores.add("El título no puede superar $maxTitulo caracteres")
    }
    // descripcion es opcional: si existe, no puede ser solo espacios
    if (descripcion != null && descripcion.isBlank()) {
        errores.add("La descripción, si se indica, no puede estar vacía")
    }
    return if (errores.isEmpty()) ResultadoValidacion.Valido
    else ResultadoValidacion.Invalido(errores)
}

