package com.example.recordatorios

// ---------------------------------------------------------------
// Consigna 3: List, Set y Map con datos simulados
// ---------------------------------------------------------------

val categoriaTrabajo = Categoria("Trabajo")
val categoriaPersonal = Categoria("Personal")
val categoriaEstudio = Categoria("Estudio")
val categoriaSalud = Categoria("Salud")

// MAP: asocia cada categoría con un color (ARGB). Se mantiene como Long
// para que los datos no dependan de Compose.
val coloresCategoria: Map<Categoria, Long> = mapOf(
    categoriaTrabajo to 0xFF1E88E5,
    categoriaPersonal to 0xFF8E24AA,
    categoriaEstudio to 0xFF43A047,
    categoriaSalud to 0xFFE53935
)

// LIST: conserva el orden y admite repetidos. Colección de solo lectura.
val recordatoriosSimulados: List<Recordatorio> = listOf(
    Recordatorio(1, "Entregar informe mensual", categoriaTrabajo, Prioridad.Alta,
        descripcion = "Enviar el informe al área de control", fechaLimite = "30/10"),
    Recordatorio(2, "Comprar insumos de oficina", categoriaTrabajo, Prioridad.Baja),
    Recordatorio(3, "Estudiar funciones en Kotlin", categoriaEstudio, Prioridad.Alta,
        descripcion = "Repasar colecciones y data classes"),
    Recordatorio(4, "Pagar el alquiler", categoriaPersonal, Prioridad.Alta,
        estado = Estado.Completado, fechaLimite = "10/10"),
    Recordatorio(5, "Turno con el odontólogo", categoriaSalud, Prioridad.Media,
        estado = Estado.Pospuesto("05/11")),
    Recordatorio(6, "Llamar a mamá", categoriaPersonal, Prioridad.Media),
    Recordatorio(7, "Leer el Módulo 2", categoriaEstudio, Prioridad.Media,
        fechaLimite = "01/11"),
    Recordatorio(8, "Renovar la tarjeta de transporte", categoriaPersonal, Prioridad.Baja,
        estado = Estado.Completado)
)

// SET: elimina categorías repetidas (varios recordatorios comparten categoría).
val categoriasUnicas: Set<Categoria> = recordatoriosSimulados
    .map { it.categoria }
    .toSet()

// Filtros reutilizables: reciben una lista y devuelven otra, sin modificar la original.
fun filtrarPorEstado(lista: List<Recordatorio>, estado: Estado): List<Recordatorio> =
    lista.filter { it.estado == estado }

fun filtrarPorPrioridad(lista: List<Recordatorio>, prioridad: Prioridad): List<Recordatorio> =
    lista.filter { it.prioridad == prioridad }
