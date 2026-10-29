package br.com.fiap.letssign

import br.com.fiap.letssign.core.model.SignatureField
import br.com.fiap.letssign.feature.documents.domain.FieldGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldGeometryTest {

    private fun field(x: Float, y: Float, w: Float = 0.3f, h: Float = 0.08f) =
        SignatureField("f1", "s1", 0, x, y, w, h)

    @Test
    fun `campo arrastado para fora da direita fica encostado na borda`() {
        val clamped = FieldGeometry.clamp(field(x = 0.95f, y = 0.5f))
        assertEquals(0.7f, clamped.xRatio, 0.0001f)
        assertTrue(FieldGeometry.isInsidePage(clamped))
    }

    @Test
    fun `campo arrastado para cima e para a esquerda volta para zero`() {
        val clamped = FieldGeometry.clamp(field(x = -0.2f, y = -0.4f))
        assertEquals(0f, clamped.xRatio, 0.0001f)
        assertEquals(0f, clamped.yRatio, 0.0001f)
    }

    @Test
    fun `campo abaixo da pagina desconta a altura`() {
        val clamped = FieldGeometry.clamp(field(x = 0.1f, y = 0.99f))
        assertEquals(0.92f, clamped.yRatio, 0.0001f)
    }

    @Test
    fun `tamanho minimo e maximo sao respeitados`() {
        val tiny = FieldGeometry.clamp(field(x = 0.1f, y = 0.1f, w = 0.01f, h = 0.001f))
        assertEquals(FieldGeometry.MIN_WIDTH, tiny.widthRatio, 0.0001f)
        assertEquals(FieldGeometry.MIN_HEIGHT, tiny.heightRatio, 0.0001f)
        val huge = FieldGeometry.clamp(field(x = 0.5f, y = 0.5f, w = 2f, h = 2f))
        assertEquals(1f, huge.widthRatio, 0.0001f)
        assertEquals(0f, huge.xRatio, 0.0001f)
    }

    @Test
    fun `isInsidePage detecta campo fora dos limites`() {
        assertFalse(FieldGeometry.isInsidePage(field(x = 0.8f, y = 0.1f)))
        assertTrue(FieldGeometry.isInsidePage(field(x = 0.7f, y = 0.92f)))
    }

    @Test
    fun `campos novos ficam dentro da pagina mesmo em cascata`() {
        repeat(12) { n ->
            val (x, y) = FieldGeometry.newFieldPosition(n)
            assertTrue(FieldGeometry.isInsidePage(field(x, y)))
        }
    }
}
