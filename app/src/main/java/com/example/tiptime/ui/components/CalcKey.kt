package com.example.tiptime.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/** Aspecto de una tecla; decide los colores. */
enum class KeyStyle {
    DIGIT,      // 0-9 y punto
    FUNCTION,   // sin, cos, tan, √, ln, (, ), %… (más discretas)
    OPERATOR,   // + − × ÷
    EQUALS,     // =
    CLEAR,      // AC y borrar
}

/**
 * Tecla de la calculadora: superficie redondeada con ripple, que se "hunde" un poco al pulsarla.
 *
 * @param fontSize tamaño del texto (lo calcula el teclado según el espacio disponible).
 * @param iconRes si no es `null` se dibuja este ícono en lugar de [label].
 * @param contentDescription descripción para lectores de pantalla (obligatoria con ícono).
 */
@Composable
fun CalcKey(
    label: String,
    style: KeyStyle,
    fontSize: TextUnit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = null,
    contentDescription: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val container: Color
    val content: Color
    var border: BorderStroke? = null
    when (style) {
        KeyStyle.DIGIT -> {
            container = scheme.surface.copy(alpha = 0.88f)
            content = scheme.onSurface
            border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.6f))
        }
        KeyStyle.FUNCTION -> {
            container = scheme.surfaceVariant.copy(alpha = 0.80f)
            content = scheme.onSurfaceVariant
            border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.45f))
        }
        KeyStyle.OPERATOR -> {
            container = scheme.secondaryContainer
            content = scheme.onSecondaryContainer
        }
        KeyStyle.EQUALS -> {
            container = scheme.primary
            content = scheme.onPrimary
        }
        KeyStyle.CLEAR -> {
            container = scheme.tertiaryContainer
            content = scheme.onTertiaryContainer
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "key-press-scale",
    )

    Surface(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        shape = RoundedCornerShape(22.dp),
        color = container,
        contentColor = content,
        border = border,
        interactionSource = interactionSource,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = contentDescription,
                    modifier = Modifier.size(fontSize.value.dp), // el ícono no crece con el tamaño de fuente del sistema
                )
            } else {
                Text(
                    text = label,
                    fontSize = fontSize,
                    fontWeight = when (style) {
                        KeyStyle.OPERATOR, KeyStyle.EQUALS, KeyStyle.CLEAR -> FontWeight.Medium
                        else -> FontWeight.Normal
                    },
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}
