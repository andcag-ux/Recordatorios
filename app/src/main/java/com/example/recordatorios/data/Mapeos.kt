package com.example.recordatorios.data

import com.example.recordatorios.Categoria
import com.example.recordatorios.Estado
import com.example.recordatorios.EstadoSync
import com.example.recordatorios.Prioridad
import com.example.recordatorios.Recordatorio
import com.example.recordatorios.data.local.CodigoOperacion
import com.example.recordatorios.data.local.CodigoSync
import com.example.recordatorios.data.local.RecordatorioEntity
import com.example.recordatorios.data.remote.RecordatorioRemoto

// ---------------------------------------------------------------
// Mapeos explícitos entre las tres capas:
//   Documento Firestore <-> Entity (esquema de Room) <-> Dominio (lo que ve la UI)
// Cada capa tiene su propio modelo; ninguna depende de la forma de la otra.
// ---------------------------------------------------------------

private const val BAJA = "BAJA"
private const val MEDIA = "MEDIA"
private const val ALTA = "ALTA"
private const val PENDIENTE = "PENDIENTE"
private const val COMPLETADO = "COMPLETADO"
private const val POSPUESTO = "POSPUESTO"

// ---- Códigos de texto <-> modelos de dominio ----

fun Prioridad.codigo(): String = when (this) {
    Prioridad.Baja -> BAJA
    Prioridad.Media -> MEDIA
    Prioridad.Alta -> ALTA
}

/** Un valor desconocido (por ejemplo, un dato corrupto) se interpreta como prioridad Media. */
fun prioridadDesdeCodigo(codigo: String?): Prioridad = when (codigo?.uppercase()) {
    BAJA -> Prioridad.Baja
    ALTA -> Prioridad.Alta
    else -> Prioridad.Media
}

fun Estado.codigo(): String = when (this) {
    Estado.Pendiente -> PENDIENTE
    Estado.Completado -> COMPLETADO
    is Estado.Pospuesto -> POSPUESTO
}

/** Un estado desconocido se interpreta como Pendiente. */
fun estadoDesdeCodigo(codigo: String?, pospuestoHasta: String?): Estado = when (codigo?.uppercase()) {
    COMPLETADO -> Estado.Completado
    POSPUESTO -> Estado.Pospuesto(pospuestoHasta ?: "fecha a definir")
    else -> Estado.Pendiente
}

private fun Estado.pospuestoHasta(): String? = (this as? Estado.Pospuesto)?.hasta

private fun syncDesdeCodigo(codigo: String): EstadoSync = when (codigo) {
    CodigoSync.SINCRONIZADO -> EstadoSync.Sincronizado
    CodigoSync.ERROR -> EstadoSync.Error
    else -> EstadoSync.Pendiente
}

// ---- Entity -> Dominio ----

fun RecordatorioEntity.aDominio(): Recordatorio = Recordatorio(
    id = id,
    titulo = titulo,
    categoria = Categoria(categoria),
    prioridad = prioridadDesdeCodigo(prioridad),
    estado = estadoDesdeCodigo(estado, pospuestoHasta),
    descripcion = descripcion,
    fechaLimite = fechaLimite,
    sync = syncDesdeCodigo(syncEstado)
)

// ---- Dominio -> Entity ----

/** Registro nuevo creado en el dispositivo por la cuenta [uid]: queda pendiente de enviarse (CREAR). */
fun Recordatorio.aEntidadNueva(uid: String, clientId: String, creadoEn: Long): RecordatorioEntity = RecordatorioEntity(
    uid = uid,
    clientId = clientId,
    titulo = titulo,
    descripcion = descripcion,
    categoria = categoria.nombre,
    prioridad = prioridad.codigo(),
    estado = estado.codigo(),
    pospuestoHasta = estado.pospuestoHasta(),
    fechaLimite = fechaLimite,
    creadoEn = creadoEn,
    syncEstado = CodigoSync.PENDIENTE,
    operacion = CodigoOperacion.CREAR
)

/** Copia los campos editables del dominio sobre una entidad existente (conserva ids y metadatos). */
fun Recordatorio.aplicarSobre(entidad: RecordatorioEntity): RecordatorioEntity = entidad.copy(
    titulo = titulo,
    descripcion = descripcion,
    categoria = categoria.nombre,
    prioridad = prioridad.codigo(),
    estado = estado.codigo(),
    pospuestoHasta = estado.pospuestoHasta(),
    fechaLimite = fechaLimite
)

// ---- Entity -> Documento de Firestore ----

/** El id del documento es el clientId: el mismo identificador en Room y en Firestore. */
fun RecordatorioEntity.aRemoto(): RecordatorioRemoto = RecordatorioRemoto(
    id = clientId,
    titulo = titulo,
    descripcion = descripcion,
    categoria = categoria,
    prioridad = prioridad,
    estado = estado,
    pospuestoHasta = pospuestoHasta,
    fechaLimite = fechaLimite,
    creadoEn = creadoEn,
    actualizadoEn = actualizadoEn
)

// ---- Documento de Firestore -> Entity ----

/**
 * Transforma un documento recibido antes de incorporarlo a Room, asociado a la
 * cuenta [uid]. Devuelve null si el documento es inválido (sin título): se descarta.
 */
fun RecordatorioRemoto.aEntidadONull(uid: String, ahora: Long): RecordatorioEntity? {
    if (id.isBlank()) return null
    val tituloLimpio = titulo?.trim()
    if (tituloLimpio.isNullOrEmpty()) return null
    val creado = creadoEn ?: ahora
    return RecordatorioEntity(
        uid = uid,
        clientId = id,
        enServidor = true,
        titulo = tituloLimpio,
        descripcion = descripcion?.takeIf { it.isNotBlank() },
        categoria = categoria?.takeIf { it.isNotBlank() } ?: "General",
        prioridad = prioridadDesdeCodigo(prioridad).codigo(),
        estado = estadoDesdeCodigo(estado, pospuestoHasta).codigo(),
        pospuestoHasta = pospuestoHasta,
        fechaLimite = fechaLimite,
        creadoEn = creado,
        actualizadoEn = actualizadoEn ?: creado,
        syncEstado = CodigoSync.SINCRONIZADO,
        operacion = CodigoOperacion.NINGUNA
    )
}
