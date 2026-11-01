package com.example.recordatorios.data

import com.example.recordatorios.Recordatorio
import kotlinx.coroutines.delay

/**
 * Implementación en memoria. Simula latencia con delay() (no bloquea el hilo)
 * y permite forzar estados vacíos o errores mediante [SimulacionRepositorio].
 */
class InMemoryRecordatorioRepository(
    private val iniciales: List<Recordatorio>,
    private val demoraCargaMs: Long = 900,
    private val demoraGuardadoMs: Long = 1500
) : RecordatorioRepository, SimulacionRepositorio {

    private val datos = iniciales.toMutableList()
    private var siguienteId: Long = (iniciales.maxOfOrNull { it.id } ?: 0L) + 1
    private var fallarProximaCarga = false
    private var fallarProximoGuardado = false

    override suspend fun obtenerRecordatorios(): List<Recordatorio> {
        delay(demoraCargaMs)
        if (fallarProximaCarga) {
            fallarProximaCarga = false
            throw ErrorDeRepositorio("No se pudieron cargar los recordatorios.")
        }
        return datos.toList()
    }

    override suspend fun obtenerPorId(id: Long): Recordatorio? {
        delay(400)
        return datos.firstOrNull { it.id == id }
    }

    override suspend fun guardar(recordatorio: Recordatorio): Recordatorio {
        delay(demoraGuardadoMs)
        if (fallarProximoGuardado) {
            fallarProximoGuardado = false
            throw ErrorDeRepositorio("No se pudo guardar el recordatorio.")
        }
        val guardado = recordatorio.copy(id = siguienteId++)
        datos.add(0, guardado) // el más reciente se muestra primero
        return guardado
    }

    override fun establecerModo(modo: ModoSimulacion) {
        when (modo) {
            ModoSimulacion.DatosNormales -> {
                datos.clear()
                datos.addAll(iniciales)
                fallarProximaCarga = false
                fallarProximoGuardado = false
            }
            ModoSimulacion.ListaVacia -> datos.clear()
            ModoSimulacion.ErrorDeCarga -> fallarProximaCarga = true
            ModoSimulacion.ErrorDeGuardado -> fallarProximoGuardado = true
        }
    }
}
