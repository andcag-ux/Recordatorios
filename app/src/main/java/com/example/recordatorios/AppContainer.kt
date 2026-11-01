package com.example.recordatorios

import com.example.recordatorios.data.InMemoryRecordatorioRepository
import com.example.recordatorios.data.RecordatorioRepository
import com.example.recordatorios.data.SimulacionRepositorio

/**
 * Inyección de dependencias manual (suficiente para esta etapa). Los
 * ViewModels reciben interfaces; solo este objeto conoce la implementación.
 */
object AppContainer {
    private val repositorioEnMemoria = InMemoryRecordatorioRepository(recordatoriosSimulados)

    val repositorio: RecordatorioRepository = repositorioEnMemoria
    val simulacion: SimulacionRepositorio = repositorioEnMemoria
}
