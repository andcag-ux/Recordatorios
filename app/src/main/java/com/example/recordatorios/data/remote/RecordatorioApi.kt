package com.example.recordatorios.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** Interfaz del servicio. Todas las operaciones son suspendidas y devuelven Response para leer el código HTTP. */
interface RecordatorioApi {

    @GET("recordatorios")
    suspend fun listar(): Response<List<RecordatorioDto>>

    @POST("recordatorios")
    suspend fun crear(@Body dto: RecordatorioDto): Response<RecordatorioDto>

    @PUT("recordatorios/{id}")
    suspend fun actualizar(@Path("id") id: Long, @Body dto: RecordatorioDto): Response<RecordatorioDto>

    @DELETE("recordatorios/{id}")
    suspend fun eliminar(@Path("id") id: Long): Response<Unit>
}
