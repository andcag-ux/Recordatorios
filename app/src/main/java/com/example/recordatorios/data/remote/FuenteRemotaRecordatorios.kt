package com.example.recordatorios.data.remote

/**
 * Fuente remota de recordatorios. El repositorio depende de esta interfaz y no
 * de Firebase: la implementación real es [FirestoreFuenteRemota] y las pruebas
 * usan una versión en memoria.
 *
 * Todas las operaciones reciben el uid de la cuenta: los datos remotos siempre
 * quedan asociados a una identidad.
 */
interface FuenteRemotaRecordatorios {

    suspend fun listar(uid: String): ResultadoRed<List<RecordatorioRemoto>>

    /** Crea o reemplaza el documento con ese id (operación idempotente). */
    suspend fun guardar(uid: String, recordatorio: RecordatorioRemoto): ResultadoRed<Unit>

    /** Elimina el documento. Si ya no existía, también es un éxito. */
    suspend fun eliminar(uid: String, id: String): ResultadoRed<Unit>
}
