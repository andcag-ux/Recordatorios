package com.example.recordatorios

import com.example.recordatorios.data.prefs.OrdenListado
import com.example.recordatorios.data.prefs.Preferencias
import com.example.recordatorios.data.prefs.TemaApp
import com.example.recordatorios.data.prefs.preferenciasDesde
import org.junit.Assert.assertEquals
import org.junit.Test

class PreferenciasTest {

    @Test
    fun sin_datos_guardados_se_usan_los_valores_predeterminados() {
        assertEquals(Preferencias(), preferenciasDesde(null, null, null))
    }

    @Test
    fun valores_guardados_se_conservan() {
        val prefs = preferenciasDesde("oscuro", false, "prioridad")
        assertEquals(TemaApp.Oscuro, prefs.tema)
        assertEquals(false, prefs.mostrarCompletados)
        assertEquals(OrdenListado.Prioridad, prefs.orden)
    }

    @Test
    fun opciones_desconocidas_vuelven_al_valor_predeterminado() {
        val prefs = preferenciasDesde("fucsia", true, "al_azar")
        assertEquals(TemaApp.Sistema, prefs.tema)
        assertEquals(OrdenListado.Recientes, prefs.orden)
    }
}
