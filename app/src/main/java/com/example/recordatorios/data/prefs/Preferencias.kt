package com.example.recordatorios.data.prefs

/** Tema visual elegido por la persona. */
enum class TemaApp(val codigo: String, val etiqueta: String) {
    Sistema("sistema", "Del sistema"),
    Claro("claro", "Claro"),
    Oscuro("oscuro", "Oscuro");

    companion object {
        /** Un código desconocido o ausente vuelve al valor predeterminado. */
        fun desdeCodigo(codigo: String?): TemaApp =
            entries.firstOrNull { it.codigo == codigo } ?: Sistema
    }
}

/** Criterio de ordenamiento del listado. */
enum class OrdenListado(val codigo: String, val etiqueta: String) {
    Recientes("recientes", "Recientes"),
    Prioridad("prioridad", "Prioridad"),
    Titulo("titulo", "Título");

    companion object {
        fun desdeCodigo(codigo: String?): OrdenListado =
            entries.firstOrNull { it.codigo == codigo } ?: Recientes
    }
}

/** Modelo propio de preferencias. Los valores por defecto se usan si no hay datos guardados. */
data class Preferencias(
    val tema: TemaApp = TemaApp.Sistema,
    val mostrarCompletados: Boolean = true,
    val orden: OrdenListado = OrdenListado.Recientes
)

/**
 * Convierte los valores crudos de DataStore al modelo propio. Cualquier valor
 * ausente o inválido se reemplaza por el predeterminado (no falla la lectura).
 */
fun preferenciasDesde(
    tema: String?,
    mostrarCompletados: Boolean?,
    orden: String?
): Preferencias = Preferencias(
    tema = TemaApp.desdeCodigo(tema),
    mostrarCompletados = mostrarCompletados ?: true,
    orden = OrdenListado.desdeCodigo(orden)
)
