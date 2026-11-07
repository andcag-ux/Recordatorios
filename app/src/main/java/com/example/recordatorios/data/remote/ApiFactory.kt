package com.example.recordatorios.data.remote

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Construye Retrofit + OkHttp. La URL base es configurable (ver local.properties.example). */
object ApiFactory {

    fun crear(baseUrl: String, interceptorSimulado: Interceptor?): RecordatorioApi {
        val cliente = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .apply { if (interceptorSimulado != null) addInterceptor(interceptorSimulado) }
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RecordatorioApi::class.java)
    }
}
