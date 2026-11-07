package com.example.recordatorios.data.remote

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

/**
 * Implementación con Cloud Firestore. Es la única clase que conoce las rutas
 * y la API de Firestore.
 *
 * Estructura: users/{uid}/reminders/{reminderId}
 * Las reglas de seguridad publicadas solo permiten acceder a users/{uid} cuando
 * uid coincide con la cuenta autenticada (ver firestore.rules).
 */
class FirestoreFuenteRemota(private val db: FirebaseFirestore) : FuenteRemotaRecordatorios {

    private fun coleccion(uid: String): CollectionReference =
        db.collection("users").document(uid).collection("reminders")

    override suspend fun listar(uid: String): ResultadoRed<List<RecordatorioRemoto>> = llamadaRemota {
        // Source.SERVER: se lee del servicio, no de la caché de Firestore. Room ya es la caché local.
        coleccion(uid).get(Source.SERVER).await().documents.map { doc ->
            RecordatorioRemoto.desdeMapa(doc.id, doc.data.orEmpty())
        }
    }

    override suspend fun guardar(uid: String, recordatorio: RecordatorioRemoto): ResultadoRed<Unit> =
        llamadaRemota {
            coleccion(uid).document(recordatorio.id).set(recordatorio.aMapa()).await()
            Unit
        }

    override suspend fun eliminar(uid: String, id: String): ResultadoRed<Unit> = llamadaRemota {
        coleccion(uid).document(id).delete().await()
        Unit
    }
}
