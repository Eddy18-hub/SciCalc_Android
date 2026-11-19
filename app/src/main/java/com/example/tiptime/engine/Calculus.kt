package com.example.tiptime.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/** Tipo de punto notable de una curva. */
enum class PointKind { ROOT, MINIMUM, MAXIMUM, INTERSECTION }

/** Punto notable (x, y) de una gráfica. */
data class CurvePoint(val x: Double, val y: Double, val kind: PointKind)

/**
 * Cálculo numérico sobre funciones de una variable (Kotlin puro).
 *
 * Una función es `(Double) -> Double` que devuelve NaN donde no está definida (así se evalúan
 * las expresiones compiladas al graficar). Los resultados son aproximaciones numéricas.
 */
object Calculus {

    private const val SAMPLES = 2000

    /**
     * Derivada en [x] por diferencias centrales con extrapolación de Richardson (error ~ h⁴).
     * Devuelve NaN si la función no está definida cerca de [x].
     */
    fun derivative(f: (Double) -> Double, x: Double): Double {
        val h = 1e-3 * max(1.0, abs(x))
        fun central(step: Double) = (f(x + step) - f(x - step)) / (2.0 * step)
        val d1 = central(h)
        val d2 = central(h / 2.0)
        return (4.0 * d2 - d1) / 3.0
    }

    /**
     * Integral definida de [a] a [b] con el método de Simpson adaptativo.
     * Devuelve NaN si la función no está definida en algún punto del intervalo.
     */
    fun integral(f: (Double) -> Double, a: Double, b: Double): Double {
        if (a == b) return 0.0
        if (a > b) return -integral(f, b, a)
        val fa = f(a)
        val fb = f(b)
        val m = (a + b) / 2.0
        val fm = f(m)
        if (fa.isNaN() || fb.isNaN() || fm.isNaN()) return Double.NaN
        val whole = (b - a) / 6.0 * (fa + 4.0 * fm + fb)
        return adaptiveSimpson(f, a, b, fa, fm, fb, whole, 1e-10, 24)
    }

    private fun adaptiveSimpson(
        f: (Double) -> Double, a: Double, b: Double,
        fa: Double, fm: Double, fb: Double, whole: Double, tol: Double, depth: Int,
    ): Double {
        val m = (a + b) / 2.0
        val lm = (a + m) / 2.0
        val rm = (m + b) / 2.0
        val flm = f(lm)
        val frm = f(rm)
        if (flm.isNaN() || frm.isNaN()) return Double.NaN
        val left = (m - a) / 6.0 * (fa + 4.0 * flm + fm)
        val right = (b - m) / 6.0 * (fm + 4.0 * frm + fb)
        val delta = left + right - whole
        if (depth <= 0 || abs(delta) <= 15.0 * tol) return left + right + delta / 15.0
        val l = adaptiveSimpson(f, a, m, fa, flm, fm, left, tol / 2.0, depth - 1)
        val r = adaptiveSimpson(f, m, b, fm, frm, fb, right, tol / 2.0, depth - 1)
        return l + r
    }

    /** Raíces de [f] en [[lo], [hi]], ordenadas de menor a mayor (incluye raíces dobles, como x² en 0). */
    fun roots(f: (Double) -> Double, lo: Double, hi: Double): List<CurvePoint> =
        findRoots(f, lo, hi).map { CurvePoint(it, 0.0, PointKind.ROOT) }

    /** Mínimos y máximos locales de [f] en [[lo], [hi]]. */
    fun extrema(f: (Double) -> Double, lo: Double, hi: Double): List<CurvePoint> {
        val xs = grid(lo, hi)
        val ys = DoubleArray(xs.size) { f(xs[it]) }
        val out = ArrayList<CurvePoint>()
        for (i in 1 until xs.size - 1) {
            val a = ys[i - 1]; val b = ys[i]; val c = ys[i + 1]
            if (a.isNaN() || b.isNaN() || c.isNaN()) continue
            val isMax = b > a && b >= c
            val isMin = b < a && b <= c
            if (!isMax && !isMin) continue
            val x = golden(f, xs[i - 1], xs[i + 1], maximize = isMax)
            val y = f(x)
            if (y.isNaN()) continue
            // Descarta "extremos" que en realidad son ruido numérico en un tramo casi plano.
            if (abs(y - a) < 1e-12 && abs(y - c) < 1e-12) continue
            out += CurvePoint(x, y, if (isMax) PointKind.MAXIMUM else PointKind.MINIMUM)
        }
        return out.distinctByNear()
    }

    /** Puntos donde [f] y [g] se cruzan en [[lo], [hi]]. */
    fun intersections(f: (Double) -> Double, g: (Double) -> Double, lo: Double, hi: Double): List<CurvePoint> =
        findRoots({ f(it) - g(it) }, lo, hi).mapNotNull { x ->
            val y = f(x)
            if (y.isNaN()) null else CurvePoint(x, y, PointKind.INTERSECTION)
        }

    // ── Implementación ───────────────────────────────────────────────────────

    private fun grid(lo: Double, hi: Double) = DoubleArray(SAMPLES + 1) { lo + (hi - lo) * it / SAMPLES }

    private fun findRoots(f: (Double) -> Double, lo: Double, hi: Double): List<Double> {
        if (!(hi > lo)) return emptyList()
        val xs = grid(lo, hi)
        val ys = DoubleArray(xs.size) { f(xs[it]) }
        val found = ArrayList<Double>()

        for (i in 0 until xs.size - 1) {
            val a = ys[i]; val b = ys[i + 1]
            if (a.isNaN() || b.isNaN()) continue
            when {
                a == 0.0 -> found += xs[i]
                a * b < 0.0 -> {
                    val x = bisect(f, xs[i], xs[i + 1])
                    // Un salto (como tan o 1/x en 0) cambia de signo sin cruzar el eje: se descarta.
                    if (abs(f(x)) < 1e-6 * max(1.0, max(abs(a), abs(b)))) found += x
                }
            }
        }
        if (ys.last() == 0.0) found += xs.last()

        // Raíces dobles (la curva toca el eje sin cruzarlo): mínimo local de |f| casi nulo.
        for (i in 1 until xs.size - 1) {
            val a = abs(ys[i - 1]); val b = abs(ys[i]); val c = abs(ys[i + 1])
            if (a.isNaN() || b.isNaN() || c.isNaN() || ys[i - 1] * ys[i] <= 0.0 || ys[i] * ys[i + 1] <= 0.0) continue
            if (b <= a && b <= c && b > 0.0) {
                val x = golden({ abs(f(it)) }, xs[i - 1], xs[i + 1], maximize = false)
                if (abs(f(x)) < 1e-9) found += x
            }
        }
        return found.sorted().distinctNear()
    }

    private fun bisect(f: (Double) -> Double, lo0: Double, hi0: Double): Double {
        var lo = lo0; var hi = hi0
        var flo = f(lo)
        repeat(100) {
            val mid = (lo + hi) / 2.0
            val fm = f(mid)
            if (fm == 0.0) return mid
            if (flo * fm < 0.0) hi = mid else { lo = mid; flo = fm }
        }
        return (lo + hi) / 2.0
    }

    /** Búsqueda de sección áurea del máximo/mínimo en [[lo], [hi]]. */
    private fun golden(f: (Double) -> Double, lo0: Double, hi0: Double, maximize: Boolean): Double {
        val gr = (sqrt(5.0) - 1.0) / 2.0
        var lo = lo0; var hi = hi0
        val sign = if (maximize) -1.0 else 1.0
        var c = hi - gr * (hi - lo)
        var d = lo + gr * (hi - lo)
        repeat(100) {
            if (sign * f(c) < sign * f(d)) hi = d else lo = c
            c = hi - gr * (hi - lo)
            d = lo + gr * (hi - lo)
        }
        return (lo + hi) / 2.0
    }

    /** Quita duplicados casi iguales (el muestreo puede encontrar la misma raíz dos veces). */
    private fun List<Double>.distinctNear(): List<Double> {
        val out = ArrayList<Double>()
        for (x in this) if (out.isEmpty() || abs(x - out.last()) > 1e-7 * max(1.0, abs(x))) out += x
        return out
    }

    private fun List<CurvePoint>.distinctByNear(): List<CurvePoint> {
        val out = ArrayList<CurvePoint>()
        for (p in this) if (out.none { abs(it.x - p.x) < 1e-6 * max(1.0, abs(p.x)) && it.kind == p.kind }) out += p
        return out
    }
}
