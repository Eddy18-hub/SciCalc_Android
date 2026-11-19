package com.example.tiptime.engine

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculusTest {

    @Test fun derivativeOfPolynomial() {
        assertEquals(6.0, Calculus.derivative({ it * it }, 3.0), 1e-7)
        // d/dx (x³ − 3x) = 3x² − 3  →  en x = 2 vale 9
        assertEquals(9.0, Calculus.derivative({ it * it * it - 3 * it }, 2.0), 1e-6)
    }

    @Test fun derivativeOfSine() {
        assertEquals(1.0, Calculus.derivative({ sin(it) }, 0.0), 1e-8)
        assertEquals(-1.0, Calculus.derivative({ sin(it) }, PI), 1e-8)
    }

    @Test fun derivativeFarFromOrigin() {
        assertEquals(2000.0, Calculus.derivative({ it * it }, 1000.0), 1e-3)
    }

    @Test fun derivativeOutsideDomainIsNaN() {
        assertTrue(Calculus.derivative({ Math.sqrt(it).let { v -> if (it < 0) Double.NaN else v } }, -1.0).isNaN())
    }

    @Test fun integrals() {
        assertEquals(2.0, Calculus.integral({ sin(it) }, 0.0, PI), 1e-9)
        assertEquals(9.0, Calculus.integral({ it * it }, 0.0, 3.0), 1e-9)
        assertEquals(-9.0, Calculus.integral({ it * it }, 3.0, 0.0), 1e-9) // límites invertidos
        assertEquals(0.0, Calculus.integral({ it }, 2.0, 2.0), 0.0)
        assertEquals(Math.E - 1.0, Calculus.integral({ Math.exp(it) }, 0.0, 1.0), 1e-9)
    }

    @Test fun integralOfSharpFunction() {
        // |x| en [−1, 1] tiene un pico en 0; el método adaptativo debe dar 1
        assertEquals(1.0, Calculus.integral({ abs(it) }, -1.0, 1.0), 1e-9)
    }

    @Test fun integralOverUndefinedRegionIsNaN() {
        assertTrue(Calculus.integral({ if (it < 0) Double.NaN else it }, -1.0, 1.0).isNaN())
    }

    @Test fun rootsOfParabola() {
        val r = Calculus.roots({ it * it - 4.0 }, -10.0, 10.0)
        assertEquals(2, r.size)
        assertEquals(-2.0, r[0].x, 1e-9); assertEquals(2.0, r[1].x, 1e-9)
    }

    @Test fun doubleRootIsFound() {
        val r = Calculus.roots({ it * it }, -5.0, 5.0)
        assertEquals(1, r.size)
        assertEquals(0.0, r[0].x, 1e-6)
    }

    @Test fun rootsOfSine() {
        val r = Calculus.roots({ sin(it) }, 0.5, 10.0)   // π, 2π, 3π
        assertEquals(3, r.size)
        assertEquals(PI, r[0].x, 1e-9); assertEquals(3 * PI, r[2].x, 1e-9)
    }

    @Test fun rootExactlyOnGridPointIsNotDuplicated() {
        val r = Calculus.roots({ it }, -1.0, 1.0)
        assertEquals(1, r.size)
        assertEquals(0.0, r[0].x, 1e-12)
    }

    @Test fun asymptoteIsNotARoot() {
        // 1/x cambia de signo en 0 pero no cruza el eje
        val r = Calculus.roots({ if (it == 0.0) Double.NaN else 1.0 / it }, -1.0, 1.0)
        assertTrue(r.isEmpty())
    }

    @Test fun noRoots() = assertTrue(Calculus.roots({ it * it + 1.0 }, -10.0, 10.0).isEmpty())

    @Test fun extremaOfCubic() {
        val e = Calculus.extrema({ it * it * it - 3.0 * it }, -3.0, 3.0)
        assertEquals(2, e.size)
        val max = e.first { it.kind == PointKind.MAXIMUM }
        val min = e.first { it.kind == PointKind.MINIMUM }
        assertEquals(-1.0, max.x, 1e-6); assertEquals(2.0, max.y, 1e-9)
        assertEquals(1.0, min.x, 1e-6); assertEquals(-2.0, min.y, 1e-9)
    }

    @Test fun extremaOfSine() {
        val e = Calculus.extrema({ sin(it) }, 0.0, 2 * PI)
        assertEquals(2, e.size)
        assertEquals(PI / 2, e.first { it.kind == PointKind.MAXIMUM }.x, 1e-6)
    }

    @Test fun constantHasNoExtrema() = assertTrue(Calculus.extrema({ 3.0 }, -5.0, 5.0).isEmpty())

    @Test fun intersections() {
        val p = Calculus.intersections({ it }, { it * it }, -2.0, 3.0)
        assertEquals(2, p.size)
        assertEquals(0.0, p[0].x, 1e-9); assertEquals(1.0, p[1].x, 1e-9)
        assertEquals(1.0, p[1].y, 1e-9)
        assertTrue(p.all { it.kind == PointKind.INTERSECTION })
    }

    @Test fun parallelLinesNeverIntersect() =
        assertTrue(Calculus.intersections({ it }, { it + 1.0 }, -10.0, 10.0).isEmpty())

    @Test fun invalidRangeGivesNothing() {
        assertTrue(Calculus.roots({ it }, 1.0, 1.0).isEmpty())
        assertTrue(Calculus.roots({ it }, 2.0, 1.0).isEmpty())
    }
}
