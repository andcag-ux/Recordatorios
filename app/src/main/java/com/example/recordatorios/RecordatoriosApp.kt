package com.example.recordatorios

import android.app.Application

/** Crea las dependencias una sola vez, al iniciar el proceso. */
class RecordatoriosApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.inicializar(this)
    }
}
