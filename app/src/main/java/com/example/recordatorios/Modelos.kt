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

/** Estado de sincronización de un registro con el servicio remoto (Actividad 3). */
enum class EstadoSync(val etiqueta: String) {
    Sincronizado("Sincronizado"),
    Pendiente("Pendiente de sincronizar"),
    Error("Error de sincronización, se reintentará")
}

/** Recordatorio. descripcion y fechaLimite son opcionales (String?). */
data class Recordatorio(
    val id: Long,
    val titulo: String,
    val categoria: Categoria,
    val prioridad: Prioridad,
    val estado: Estado = Estado.Pendiente,
    val descripcion: String? = null,
    val fechaLimite: String? = null,
    val sync: EstadoSync = EstadoSync.Sincronizado
)

// ---------------------------------------------------------------
// Reglas de validación (Actividad 2): funciones puras, reutilizadas por el
// ViewModel del formulario y por las pruebas unitarias.
// Cada una devuelve null si el dato es válido o el mensaje de error.
// ---------------------------------------------------------------

const val MIN_TITULO = 4
const val MAX_DESCRIPCION = 200

fun validarTitulo(titulo: String): String? =
    if (titulo.trim().length < MIN_TITULO) {
        "El título debe tener al menos $MIN_TITULO caracteres (sin contar espacios iniciales y finales)"
    } else null

fun validarDescripcion(descripcion: String): String? =
    if (descripcion.length > MAX_DESCRIPCION) {
        "La descripción no puede superar los $MAX_DESCRIPCION caracteres (tiene ${descripcion.length})"
    } else null

fun validarCategoria(
    categoria: Categoria?,
    opciones: Collection<Categoria> = categoriasDisponibles
): String? = when {
    categoria == null -> "Seleccioná una categoría"
    categoria !in opciones -> "La categoría seleccionada no es válida"
    else -> null
}

fun validarPrioridad(prioridad: Prioridad?): String? =
    if (prioridad == null) "Seleccioná una prioridad" else null

sealed class ResultadoValidacion {
    object Valido : ResultadoValidacion()
    data class Invalido(val errores: List<String>) : ResultadoValidacion()
}

fun validarRecordatorio(
    titulo: String,
    descripcion: String?,
    categoria: Categoria?,
    prioridad: Prioridad?
): ResultadoValidacion {
    val errores = listOfNotNull(
        validarTitulo(titulo),
        validarDescripcion(descripcion.orEmpty()),
        validarCategoria(categoria),
        validarPrioridad(prioridad)
    )
    return if (errores.isEmpty()) ResultadoValidacion.Valido
    else ResultadoValidacion.Invalido(errores)
}
