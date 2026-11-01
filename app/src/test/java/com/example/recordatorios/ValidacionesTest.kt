package com.example.recordatorios

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ValidacionesTest {

    @Test
    fun titulo_ignora_espacios_iniciales_y_finales() {
        assertNotNull(validarTitulo("  abc  "))      // 3 caracteres reales
        assertNull(validarTitulo("  abcd  "))        // 4 caracteres reales
    }

    @Test
    fun descripcion_admite_hasta_200_caracteres() {
        assertNull(validarDescripcion(""))
        assertNull(validarDescripcion("a".repeat(200)))
        assertNotNull(validarDescripcion("a".repeat(201)))
    }

    @Test
    fun categoria_debe_estar_entre_las_opciones() {
        assertNotNull(validarCategoria(null))
        assertNotNull(validarCategoria(Categoria("Inventada")))
        assertNull(validarCategoria(categoriaTrabajo))
    }

    @Test
    fun prioridad_es_obligatoria() {
        assertNotNull(validarPrioridad(null))
        assertNull(validarPrioridad(Prioridad.Alta))
    }

    @Test
    fun recordatorio_valido_e_invalido() {
        assertEquals(
            ResultadoValidacion.Valido,
            validarRecordatorio("Pagar luz", null, categoriaPersonal, Prioridad.Media)
        )
        val resultado = validarRecordatorio("ab", "a".repeat(201), null, null)
        assertEquals(4, (resultado as ResultadoValidacion.Invalido).errores.size)
    }
}
