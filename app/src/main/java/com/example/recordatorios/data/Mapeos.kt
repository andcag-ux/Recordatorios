package com.example.recordatorios.data

import com.example.recordatorios.Categoria
import com.example.recordatorios.Estado
import com.example.recordatorios.EstadoSync
import com.example.recordatorios.Prioridad
import com.example.recordatorios.Recordatorio
import com.example.recordatorios.data.local.CodigoOperacion
import com.example.recordatorios.data.local.CodigoSync
import com.example.recordatorios.data.local.RecordatorioEntity
import com.example.recordatorios.data.remote.RecordatorioDto

// ---------------------------------------------------------------
// Mapeos explícitos entre las tres capas:
//   DTO (contrato del servicio) <-> Entity (esquema de Room) <-> Dominio (lo que ve la UI)
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

/** Registro nuevo creado en el dispositivo: queda pendiente de enviarse (operación CREAR). */
fun Recordatorio.aEntidadNueva(clientId: String, creadoEn: Long): RecordatorioEntity = RecordatorioEntity(
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

// ---- Entity -> DTO ----

fun RecordatorioEntity.aDto(): RecordatorioDto = RecordatorioDto(
    id = remoteId,
    clientId = clientId,
    titulo = titulo,
    descripcion = descripcion,
    categoria = categoria,
    prioridad = prioridad,
    estado = estado,
    pospuestoHasta = pospuestoHasta,
    fechaLimite = fechaLimite,
    creadoEn = creadoEn
)

// ---- DTO -> Entity ----

/**
 * Transforma un DTO recibido antes de incorporarlo a Room. Devuelve null si el
 * DTO es inválido (sin id o sin título): ese registro se descarta.
 */
fun RecordatorioDto.aEntidadONull(ahora: Long): RecordatorioEntity? {
    val idRemoto = id ?: return null
    val tituloLimpio = titulo?.trim()
    if (tituloLimpio.isNullOrEmpty()) return null
    return RecordatorioEntity(
        clientId = clientId?.takeIf { it.isNotBlank() } ?: "remoto-$idRemoto",
        remoteId = idRemoto,
        titulo = tituloLimpio,
        descripcion = descripcion?.takeIf { it.isNotBlank() },
        categoria = categoria?.takeIf { it.isNotBlank() } ?: "General",
        prioridad = prioridadDesdeCodigo(prioridad).codigo(),
        estado = estadoDesdeCodigo(estado, pospuestoHasta).codigo(),
        pospuestoHasta = pospuestoHasta,
        fechaLimite = fechaLimite,
        creadoEn = creadoEn ?: ahora,
        syncEstado = CodigoSync.SINCRONIZADO,
        operacion = CodigoOperacion.NINGUNA
    )
}

// ---- Dominio -> DTO (solo para sembrar el servidor simulado) ----

fun Recordatorio.toDto(clientId: String, creadoEn: Long): RecordatorioDto = RecordatorioDto(
    id = id,
    clientId = clientId,
    titulo = titulo,
    descripcion = descripcion,
    categoria = categoria.nombre,
    prioridad = prioridad.codigo(),
    estado = estado.codigo(),
    pospuestoHasta = estado.pospuestoHasta(),
    fechaLimite = fechaLimite,
    creadoEn = creadoEn
)
