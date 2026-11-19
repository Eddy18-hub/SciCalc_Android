package com.example.tiptime.engine.tools

/**
 * Una unidad de medida. [toBase] / [fromBase] convierten desde/hacia la unidad base de su categoría
 * (así también se pueden modelar escalas con desplazamiento, como la temperatura).
 */
class MeasureUnit(
    val symbol: String,
    val toBase: (Double) -> Double,
    val fromBase: (Double) -> Double,
)

/** Categorías de unidades. El nombre mostrado se toma de los recursos de texto (localizados). */
enum class UnitCategory(val units: List<MeasureUnit>) {

    LENGTH(  // base: metro
        listOf(
            linear("mm", 0.001), linear("cm", 0.01), linear("m", 1.0), linear("km", 1000.0),
            linear("in", 0.0254), linear("ft", 0.3048), linear("yd", 0.9144), linear("mi", 1609.344),
        )
    ),
    MASS(  // base: kilogramo
        listOf(
            linear("mg", 1e-6), linear("g", 0.001), linear("kg", 1.0), linear("t", 1000.0),
            linear("oz", 0.028349523125), linear("lb", 0.45359237),
        )
    ),
    TEMPERATURE(  // base: kelvin
        listOf(
            MeasureUnit("°C", { it + 273.15 }, { it - 273.15 }),
            MeasureUnit("°F", { (it - 32.0) * 5.0 / 9.0 + 273.15 }, { (it - 273.15) * 9.0 / 5.0 + 32.0 }),
            MeasureUnit("K", { it }, { it }),
        )
    ),
    TIME(  // base: segundo
        listOf(
            linear("ms", 0.001), linear("s", 1.0), linear("min", 60.0), linear("h", 3600.0),
            linear("d", 86400.0), linear("wk", 604800.0),
        )
    ),
    SPEED(  // base: metro por segundo
        listOf(linear("m/s", 1.0), linear("km/h", 1.0 / 3.6), linear("mph", 0.44704), linear("kn", 0.514444444)),
    ),
    AREA(  // base: metro cuadrado
        listOf(
            linear("cm²", 1e-4), linear("m²", 1.0), linear("ha", 10000.0), linear("km²", 1e6),
            linear("ft²", 0.09290304), linear("ac", 4046.8564224),
        )
    ),
    VOLUME(  // base: litro
        listOf(
            linear("mL", 0.001), linear("L", 1.0), linear("m³", 1000.0),
            linear("gal", 3.785411784), linear("fl oz", 0.0295735295625),
        )
    ),
    DATA(  // base: byte
        listOf(
            linear("B", 1.0), linear("KB", 1024.0), linear("MB", 1024.0 * 1024.0),
            linear("GB", 1024.0 * 1024.0 * 1024.0), linear("TB", 1024.0 * 1024.0 * 1024.0 * 1024.0),
        )
    );
}

private fun linear(symbol: String, factor: Double) =
    MeasureUnit(symbol, toBase = { it * factor }, fromBase = { it / factor })

object UnitConverter {

    /** Convierte [value] de [from] a [to] (ambas de la misma categoría). */
    fun convert(value: Double, from: MeasureUnit, to: MeasureUnit): Double =
        to.fromBase(from.toBase(value))
}
