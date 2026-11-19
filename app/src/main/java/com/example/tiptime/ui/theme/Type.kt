package com.example.tiptime.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Tipografía: la fuente del sistema, con cifras de ancho fijo ("tnum") en los estilos que
 * muestran números para que los dígitos no "bailen" al cambiar.
 */
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
)

/** Estilo base de la pantalla (resultado y expresión): ligero y con cifras tabulares. */
val DisplayTextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Light,
    fontFeatureSettings = "tnum",
    letterSpacing = (-0.5).sp,
)
