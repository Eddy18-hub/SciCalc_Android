package com.example.tiptime.engine.tools

import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BaseConverterTest {
    @Test fun decimalToOthers() {
        assertEquals("1010", BaseConverter.convert("10", NumberBase.DECIMAL, NumberBase.BINARY))
        assertEquals("FF", BaseConverter.convert("255", NumberBase.DECIMAL, NumberBase.HEXADECIMAL))
        assertEquals("17", BaseConverter.convert("15", NumberBase.DECIMAL, NumberBase.OCTAL))
    }
    @Test fun hexToDecimalAcceptsLowercase() {
        assertEquals("255", BaseConverter.convert("ff", NumberBase.HEXADECIMAL, NumberBase.DECIMAL))
        assertEquals("48879", BaseConverter.convert("BeEf", NumberBase.HEXADECIMAL, NumberBase.DECIMAL))
    }
    @Test fun negativeNumbers() {
        assertEquals("-101", BaseConverter.convert("-5", NumberBase.DECIMAL, NumberBase.BINARY))
        assertEquals("-5", BaseConverter.convert("-101", NumberBase.BINARY, NumberBase.DECIMAL))
    }
    @Test fun invalidInput() {
        assertEquals(null, BaseConverter.convert("102", NumberBase.BINARY, NumberBase.DECIMAL))
        assertEquals(null, BaseConverter.convert("", NumberBase.DECIMAL, NumberBase.BINARY))
        assertEquals(null, BaseConverter.convert("-", NumberBase.DECIMAL, NumberBase.BINARY))
        assertEquals(null, BaseConverter.convert("1G", NumberBase.HEXADECIMAL, NumberBase.DECIMAL))
        assertEquals(null, BaseConverter.convert("1".repeat(65), NumberBase.BINARY, NumberBase.DECIMAL))
    }
    @Test fun hugeValuesDoNotOverflow() {
        val all = BaseConverter.convertAll("FFFFFFFFFFFFFFFFFFFF", NumberBase.HEXADECIMAL)!!
        assertEquals(BigInteger("1208925819614629174706175"), BigInteger(all.getValue(NumberBase.DECIMAL)))
    }
    @Test fun zero() = assertEquals("0", BaseConverter.convert("0", NumberBase.BINARY, NumberBase.HEXADECIMAL))
}

class UnitConverterTest {
    private fun unit(c: UnitCategory, s: String) = c.units.first { it.symbol == s }
    private fun conv(v: Double, c: UnitCategory, from: String, to: String) =
        UnitConverter.convert(v, unit(c, from), unit(c, to))

    @Test fun length() {
        assertEquals(1000.0, conv(1.0, UnitCategory.LENGTH, "km", "m"), 1e-9)
        assertEquals(2.54, conv(1.0, UnitCategory.LENGTH, "in", "cm"), 1e-9)
        assertEquals(5280.0, conv(1.0, UnitCategory.LENGTH, "mi", "ft"), 1e-6)
    }
    @Test fun temperature() {
        assertEquals(212.0, conv(100.0, UnitCategory.TEMPERATURE, "°C", "°F"), 1e-9)
        assertEquals(0.0, conv(32.0, UnitCategory.TEMPERATURE, "°F", "°C"), 1e-9)
        assertEquals(273.15, conv(0.0, UnitCategory.TEMPERATURE, "°C", "K"), 1e-9)
        assertEquals(-40.0, conv(-40.0, UnitCategory.TEMPERATURE, "°C", "°F"), 1e-9)
    }
    @Test fun otherCategories() {
        assertEquals(1024.0, conv(1.0, UnitCategory.DATA, "KB", "B"), 0.0)
        assertEquals(60.0, conv(1.0, UnitCategory.TIME, "h", "min"), 1e-9)
        assertEquals(36.0, conv(10.0, UnitCategory.SPEED, "m/s", "km/h"), 1e-9)
        assertEquals(453.59237, conv(1.0, UnitCategory.MASS, "lb", "g"), 1e-9)
        assertEquals(3.785411784, conv(1.0, UnitCategory.VOLUME, "gal", "L"), 1e-9)
        assertEquals(10000.0, conv(1.0, UnitCategory.AREA, "ha", "m²"), 1e-9)
    }
    @Test fun roundTripIsIdentity() {
        for (cat in UnitCategory.entries) for (a in cat.units) for (b in cat.units) {
            val back = UnitConverter.convert(UnitConverter.convert(123.456, a, b), b, a)
            assertEquals("${cat.name} ${a.symbol}->${b.symbol}", 123.456, back, 1e-6)
        }
    }
}
