package com.example.recordatorios.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos de la aplicación. Se crea una única instancia por proceso.
 *
 * Versión 2 (Actividad 4): se agregan uid y enServidor, y el id remoto pasa a ser
 * el clientId. La migración es destructiva a propósito: la copia local se vuelve
 * a descargar de Firestore al iniciar sesión (los datos de la versión 1 eran de
 * un servidor simulado).
 */
@Database(entities = [RecordatorioEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recordatorioDao(): RecordatorioDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        fun obtener(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "recordatorios.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build().also { instancia = it }
            }
    }
}
