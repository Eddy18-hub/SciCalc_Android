package com.example.tiptime.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class ViewportTest {

    private val width = 1000f
    private val height = 600f

    @Test
    fun centerOfCanvasIsViewportCenter() {
        val v = Viewport(centerX = 3.0, centerY = -2.0, unitsAcross = 20.0)
        assertEquals(500.0, v.worldToScreenX(3.0, width, height), 1e-9)
        assertEquals(300.0, v.worldToScreenY(-2.0, width, height), 1e-9)
        assertEquals(3.0, v.screenToWorldX(500f, width, height), 1e-9)
        assertEquals(-2.0, v.screenToWorldY(300f, width, height), 1e-9)
    }

    @Test
    fun scaleIsDerivedFromTheShorterSide() {
        val v = Viewport(unitsAcross = 20.0)
        assertEquals(30.0, v.pixelsPerUnit(width, height), 1e-9)   // 600 px / 20 unidades
        assertEquals(30.0, v.pixelsPerUnit(height, width), 1e-9)   // rotado: igual
    }

    @Test
    fun yAxisPointsUp() {
        val v = Viewport()
        assertEquals(true, v.worldToScreenY(1.0, width, height) < v.worldToScreenY(0.0, width, height))
    }

    @Test
    fun worldAndScreenAreInverse() {
        val v = Viewport(centerX = 1.5, centerY = 4.0, unitsAcross = 7.0)
        val px = v.worldToScreenX(-2.25, width, height).toFloat()
        val py = v.worldToScreenY(9.5, width, height).toFloat()
        assertEquals(-2.25, v.screenToWorldX(px, width, height), 1e-4)
        assertEquals(9.5, v.screenToWorldY(py, width, height), 1e-4)
    }

    @Test
    fun zoomKeepsThePointUnderTheFingersFixed() {
        val v = Viewport(centerX = 1.0, centerY = 2.0, unitsAcross = 20.0)
        val cx = 700f
        val cy = 150f
        val before = v.screenToWorldX(cx, width, height) to v.screenToWorldY(cy, width, height)

        val zoomed = v.transformed(cx, cy, panX = 0f, panY = 0f, zoom = 2f, width = width, height = height)

        assertEquals(10.0, zoomed.unitsAcross, 1e-9)   // zoom 2× → la mitad de unidades visibles
        assertEquals(before.first, zoomed.screenToWorldX(cx, width, height), 1e-9)
        assertEquals(before.second, zoomed.screenToWorldY(cy, width, height), 1e-9)
    }

    @Test
    fun panMovesContentWithTheFingers() {
        val v = Viewport()
        val moved = v.transformed(400f, 300f, panX = 60f, panY = -30f, zoom = 1f, width = width, height = height)
        // Arrastrar 60 px a la derecha: lo que estaba en x=0 ahora se ve 60 px a la derecha.
        assertEquals(500.0 + 60.0, moved.worldToScreenX(0.0, width, height), 1e-9)
        assertEquals(300.0 - 30.0, moved.worldToScreenY(0.0, width, height), 1e-9)
    }

    @Test
    fun zoomAndPanTogetherAnchorTheGesture() {
        val v = Viewport(centerX = -3.0, centerY = 1.0, unitsAcross = 12.0)
        val cx = 250f
        val cy = 420f
        val anchorX = v.screenToWorldX(cx, width, height)
        val anchorY = v.screenToWorldY(cy, width, height)

        val result = v.transformed(cx, cy, panX = 25f, panY = 10f, zoom = 1.7f, width = width, height = height)

        assertEquals((cx + 25f).toDouble(), result.worldToScreenX(anchorX, width, height), 1e-6)
        assertEquals((cy + 10f).toDouble(), result.worldToScreenY(anchorY, width, height), 1e-6)
    }

    @Test
    fun zoomIsClamped() {
        var v = Viewport()
        repeat(100) { v = v.transformed(500f, 300f, 0f, 0f, 2f, width, height) }
        assertEquals(Viewport.MIN_UNITS_ACROSS, v.unitsAcross, 0.0)
        repeat(200) { v = v.transformed(500f, 300f, 0f, 0f, 0.5f, width, height) }
        assertEquals(Viewport.MAX_UNITS_ACROSS, v.unitsAcross, 0.0)
    }

    @Test
    fun invalidGesturesAreIgnored() {
        val v = Viewport()
        assertEquals(v, v.transformed(0f, 0f, 0f, 0f, 0f, width, height))
        assertEquals(v, v.transformed(0f, 0f, 0f, 0f, 1f, 0f, height))
    }

    @Test
    fun niceStepPicksOneTwoFive() {
        assertEquals(1.0, niceStep(0.7), 1e-12)
        assertEquals(2.0, niceStep(1.3), 1e-12)
        assertEquals(5.0, niceStep(3.0), 1e-12)
        assertEquals(10.0, niceStep(7.0), 1e-12)
        assertEquals(0.02, niceStep(0.015), 1e-12)
        assertEquals(500.0, niceStep(320.0), 1e-9)
        assertEquals(1.0, niceStep(0.0), 0.0)
        assertEquals(1.0, niceStep(Double.NaN), 0.0)
    }
}
