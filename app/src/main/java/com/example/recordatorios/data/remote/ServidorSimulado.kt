package com.example.recordatorios.data.remote

import com.example.recordatorios.data.toDto
import com.example.recordatorios.recordatoriosSimulados
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.io.File
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Servicio REST simulado dentro de la propia app, como interceptor de OkHttp.
 * Retrofit y OkHttp trabajan igual que con un servidor real (códigos HTTP,
 * JSON, excepciones de red), pero no hace falta internet ni credenciales.
 *
 * - Guarda sus datos en un archivo para que sobrevivan al cierre de la app.
 * - POST es idempotente: si recibe un clientId que ya existe, devuelve ese
 *   mismo registro en lugar de crear otro.
 * - Con [establecerModo] se simulan: sin conexión, timeout, error 500 y error 400.
 */
class ServidorSimulado(
    private val archivo: File,
    private val latenciaMs: Long = 700,
    private val ahora: () -> Long = { System.currentTimeMillis() }
) : Interceptor, ControlRedSimulada {

    private val gson = Gson()
    private val lock = Any()
    private val json = "application/json".toMediaType()

    private val _modo = MutableStateFlow(ModoRed.Normal)
    override val modo: StateFlow<ModoRed> = _modo.asStateFlow()

    private val datos: MutableList<RecordatorioDto> = cargar()
    private var siguienteId: Long = (datos.maxOfOrNull { it.id ?: 0L } ?: 0L) + 1

    override fun establecerModo(modo: ModoRed) {
        _modo.value = modo
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        Thread.sleep(latenciaMs) // latencia de red simulada; corre en el hilo de OkHttp, no en el principal

        return when (_modo.value) {
            ModoRed.SinConexion -> throw UnknownHostException("Sin conexión (simulada)")
            ModoRed.Timeout -> throw SocketTimeoutException("Timeout (simulado)")
            ModoRed.ErrorServidor -> respuesta(request, 500, "Internal Server Error", """{"error":"fallo interno"}""")
            ModoRed.SolicitudInvalida -> respuesta(request, 400, "Bad Request", """{"error":"solicitud inválida"}""")
            ModoRed.Normal -> synchronized(lock) { atender(request) }
        }
    }

    private fun atender(request: Request): Response {
        val segmentos = request.url.pathSegments.filter { it.isNotEmpty() }
        val posicion = segmentos.indexOf("recordatorios")
        if (posicion < 0) return respuesta(request, 404, "Not Found", """{"error":"ruta inexistente"}""")
        val id = segmentos.getOrNull(posicion + 1)?.toLongOrNull()

        return when (request.method) {
            "GET" -> respuesta(request, 200, "OK", gson.toJson(datos.sortedByDescending { it.creadoEn ?: 0L }))
            "POST" -> crear(request)
            "PUT" -> if (id == null) respuesta(request, 400, "Bad Request", "{}") else actualizar(request, id)
            "DELETE" -> if (id == null) respuesta(request, 400, "Bad Request", "{}") else eliminar(request, id)
            else -> respuesta(request, 405, "Method Not Allowed", "{}")
        }
    }

    private fun crear(request: Request): Response {
        val dto = leerCuerpo(request) ?: return respuesta(request, 400, "Bad Request", """{"error":"cuerpo inválido"}""")
        val clientId = dto.clientId
        if (clientId.isNullOrBlank()) return respuesta(request, 400, "Bad Request", """{"error":"falta clientId"}""")

        // Idempotencia: el mismo clientId nunca genera un segundo registro.
        val existente = datos.firstOrNull { it.clientId == clientId }
        if (existente != null) return respuesta(request, 200, "OK", gson.toJson(existente))

        val nuevo = dto.copy(id = siguienteId++, creadoEn = dto.creadoEn ?: ahora())
        datos.add(nuevo)
        guardar()
        return respuesta(request, 201, "Created", gson.toJson(nuevo))
    }

    private fun actualizar(request: Request, id: Long): Response {
        val dto = leerCuerpo(request) ?: return respuesta(request, 400, "Bad Request", """{"error":"cuerpo inválido"}""")
        val indice = datos.indexOfFirst { it.id == id }
        if (indice < 0) return respuesta(request, 404, "Not Found", """{"error":"no existe"}""")
        val actual = datos[indice]
        val nuevo = dto.copy(id = id, clientId = actual.clientId, creadoEn = actual.creadoEn)
        datos[indice] = nuevo
        guardar()
        return respuesta(request, 200, "OK", gson.toJson(nuevo))
    }

    private fun eliminar(request: Request, id: Long): Response {
        val eliminado = datos.removeAll { it.id == id }
        if (!eliminado) return respuesta(request, 404, "Not Found", """{"error":"no existe"}""")
        guardar()
        return respuesta(request, 204, "No Content", "")
    }

    private fun leerCuerpo(request: Request): RecordatorioDto? {
        val dto = try {
            val buffer = Buffer()
            request.body?.writeTo(buffer)
            gson.fromJson(buffer.readUtf8(), RecordatorioDto::class.java)
        } catch (e: JsonSyntaxException) {
            null
        }
        // Validación del servidor: título obligatorio de al menos 4 caracteres.
        return dto?.takeIf { (it.titulo?.trim()?.length ?: 0) >= 4 }
    }

    private fun respuesta(request: Request, codigo: Int, mensaje: String, cuerpo: String): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(codigo)
            .message(mensaje)
            .body(cuerpo.toResponseBody(json))
            .build()

    private fun guardar() {
        archivo.writeText(gson.toJson(datos))
    }

    private fun cargar(): MutableList<RecordatorioDto> {
        if (archivo.exists()) {
            try {
                val guardados = gson.fromJson(archivo.readText(), Array<RecordatorioDto>::class.java)
                if (guardados != null) return guardados.toMutableList()
            } catch (e: Exception) {
                // archivo dañado: se vuelve a sembrar con los datos iniciales
            }
        }
        // Primera ejecución: el "servidor" arranca con los recordatorios de ejemplo.
        val base = ahora()
        val iniciales = recordatoriosSimulados.mapIndexed { posicion, recordatorio ->
            recordatorio.toDto(
                clientId = "semilla-${recordatorio.id}",
                creadoEn = base - posicion * 1_000L
            )
        }.toMutableList()
        archivo.writeText(gson.toJson(iniciales))
        return iniciales
    }
}
