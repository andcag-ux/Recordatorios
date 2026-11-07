package com.example.recordatorios.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Única instancia de DataStore: el delegado la crea una sola vez por proceso. */
private val Context.dataStoreAjustes: DataStore<Preferences> by preferencesDataStore(name = "ajustes")

/**
 * Preferencias simples (tema, orden, completados). Los recordatorios NO se
 * guardan acá: son registros estructurados y viven en Room.
 */
class PreferenciasRepository(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.applicationContext.dataStoreAjustes)

    private object Claves {
        val TEMA = stringPreferencesKey("tema")
        val MOSTRAR_COMPLETADOS = booleanPreferencesKey("mostrar_completados")
        val ORDEN = stringPreferencesKey("orden")
    }

    /** Flujo observable con el modelo propio. Un error de lectura recuperable usa los valores por defecto. */
    val preferencias: Flow<Preferencias> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { datos ->
            preferenciasDesde(
                tema = datos[Claves.TEMA],
                mostrarCompletados = datos[Claves.MOSTRAR_COMPLETADOS],
                orden = datos[Claves.ORDEN]
            )
        }

    // Funciones suspendidas: se llaman desde viewModelScope, nunca bloquean el hilo principal.
    suspend fun guardarTema(tema: TemaApp) {
        dataStore.edit { it[Claves.TEMA] = tema.codigo }
    }

    suspend fun guardarMostrarCompletados(mostrar: Boolean) {
        dataStore.edit { it[Claves.MOSTRAR_COMPLETADOS] = mostrar }
    }

    suspend fun guardarOrden(orden: OrdenListado) {
        dataStore.edit { it[Claves.ORDEN] = orden.codigo }
    }
}
