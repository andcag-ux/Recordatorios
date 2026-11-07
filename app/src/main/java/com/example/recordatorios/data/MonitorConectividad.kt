package com.example.recordatorios.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities

/**
 * Avisa cuando el dispositivo recupera la conexión. El aviso solo dispara
 * una sincronización: el repositorio se encarga de que no se solapen.
 */
class MonitorConectividad(
    context: Context,
    private val alHaberConexion: () -> Unit
) {
    private val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java)

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            alHaberConexion()
        }
    }

    /** Consulta puntual: true si la red activa tiene acceso a internet. */
    fun hayConexion(): Boolean = try {
        val red = manager?.activeNetwork
        val capacidades = manager?.getNetworkCapabilities(red)
        capacidades?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    } catch (e: SecurityException) {
        true // si no se puede consultar, se intenta igual y la llamada remota informará el error
    }

    fun iniciar() {
        try {
            manager?.registerDefaultNetworkCallback(callback)
        } catch (e: SecurityException) {
            // Sin permiso de red: la sincronización sigue disponible de forma manual.
        }
    }
}
