package com.example.recordatorios.ui.navigation

/** Rutas y argumentos de navegación de la aplicación. */
object Rutas {
    const val LISTADO = "listado"
    const val CREAR = "crear"

    const val ARG_ID = "id"
    const val DETALLE = "detalle/{$ARG_ID}"

    /** Construye la ruta concreta del detalle. Solo viaja el identificador. */
    fun detalle(id: Long): String = "detalle/$id"
}
