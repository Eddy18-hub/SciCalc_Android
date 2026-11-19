package com.example.tiptime.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tiptime.R
import com.example.tiptime.ui.KeyAction
import com.example.tiptime.ui.MemoryOp
import kotlin.math.min

private val KeyGap = 8.dp

/** Descripción de una tecla; [weight] > 1 la hace más ancha (el "0"). */
private class KeySpec(
    val label: String,
    val action: KeyAction,
    val style: KeyStyle,
    val weight: Float = 1f,
    @DrawableRes val iconRes: Int? = null,
    val contentDescription: String? = null,
    /** Si no es `null` se ejecuta en lugar de enviar [action] (p. ej. la tecla "2nd"). */
    val onClick: (() -> Unit)? = null,
)

private fun operatorKey(symbol: Char) = KeySpec(symbol.toString(), KeyAction.Operator(symbol), KeyStyle.OPERATOR)
private fun digitKey(d: Char, weight: Float = 1f) = KeySpec(d.toString(), KeyAction.Digit(d), KeyStyle.DIGIT, weight)
private fun functionKey(label: String, token: String = "$label(") =
    KeySpec(label, KeyAction.MathFunction(token), KeyStyle.FUNCTION)

/**
 * Bloque científico (4 filas × 5 columnas):
 *
 *     sen   cos   tan   π     ^
 *     (     )     √     ln    log
 *     2nd   x!    abs   e     Ans
 *     MC    MR    M+    M−
 *
 * La tecla **2nd** cambia sen/cos/tan por sus inversas (sen⁻¹, cos⁻¹, tan⁻¹), ln/log por senh/cosh y
 * abs por tanh. El rótulo de sen/sin depende del idioma; el motor entiende ambos.
 */
@Composable
fun ScientificKeys(onKey: (KeyAction) -> Unit, modifier: Modifier = Modifier) {
    val sin = stringResource(R.string.key_sin)
    val cos = stringResource(R.string.key_cos)
    val tan = stringResource(R.string.key_tan)
    val secondLabel = stringResource(R.string.key_second)
    var second by remember { mutableStateOf(false) }

    val rows = listOf(
        listOf(
            if (second) functionKey("${sin}⁻¹", "asin(") else functionKey(sin),
            if (second) functionKey("${cos}⁻¹", "acos(") else functionKey(cos),
            if (second) functionKey("${tan}⁻¹", "atan(") else functionKey(tan),
            KeySpec("π", KeyAction.Constant("π"), KeyStyle.FUNCTION),
            KeySpec("^", KeyAction.Operator('^'), KeyStyle.FUNCTION),
        ),
        listOf(
            KeySpec("(", KeyAction.OpenParenthesis, KeyStyle.FUNCTION),
            KeySpec(")", KeyAction.CloseParenthesis, KeyStyle.FUNCTION),
            functionKey("√"),
            if (second) functionKey("${sin}h", "sinh(") else functionKey("ln"),
            if (second) functionKey("${cos}h", "cosh(") else functionKey("log"),
        ),
        listOf(
            KeySpec(
                label = secondLabel,
                action = KeyAction.Clear, // no se usa: la tecla tiene su propio clic
                style = if (second) KeyStyle.OPERATOR else KeyStyle.FUNCTION,
                onClick = { second = !second },
            ),
            KeySpec("x!", KeyAction.Factorial, KeyStyle.FUNCTION),
            if (second) functionKey("${tan}h", "tanh(") else functionKey("abs"),
            KeySpec("e", KeyAction.Constant("e"), KeyStyle.FUNCTION),
            KeySpec("Ans", KeyAction.Constant("Ans"), KeyStyle.FUNCTION),
        ),
        listOf(
            KeySpec("MC", KeyAction.Memory(MemoryOp.CLEAR), KeyStyle.FUNCTION),
            KeySpec("MR", KeyAction.Memory(MemoryOp.RECALL), KeyStyle.FUNCTION),
            KeySpec("M+", KeyAction.Memory(MemoryOp.ADD), KeyStyle.FUNCTION),
            KeySpec("M−", KeyAction.Memory(MemoryOp.SUBTRACT), KeyStyle.FUNCTION),
        ),
    )
    KeyGrid(rows = rows, columns = 5, textScale = 0.36f, onKey = onKey, modifier = modifier)
}

/** Teclado numérico clásico: AC ⌫ % ÷ / 7 8 9 × / 4 5 6 − / 1 2 3 + / 0 . = */
@Composable
fun MainKeys(onKey: (KeyAction) -> Unit, modifier: Modifier = Modifier) {
    val rows = listOf(
        listOf(
            KeySpec(stringResource(R.string.key_clear), KeyAction.Clear, KeyStyle.CLEAR),
            KeySpec(
                label = "",
                action = KeyAction.Backspace,
                style = KeyStyle.CLEAR,
                iconRes = R.drawable.ic_backspace,
                contentDescription = stringResource(R.string.cd_backspace),
            ),
            KeySpec("%", KeyAction.Percent, KeyStyle.FUNCTION),
            operatorKey('÷'),
        ),
        listOf(digitKey('7'), digitKey('8'), digitKey('9'), operatorKey('×')),
        listOf(digitKey('4'), digitKey('5'), digitKey('6'), operatorKey('−')),
        listOf(digitKey('1'), digitKey('2'), digitKey('3'), operatorKey('+')),
        listOf(
            digitKey('0', weight = 2f),
            KeySpec(".", KeyAction.Decimal, KeyStyle.DIGIT),
            KeySpec("=", KeyAction.Equals, KeyStyle.EQUALS, contentDescription = stringResource(R.string.cd_equals)),
        ),
    )
    KeyGrid(rows = rows, columns = 4, textScale = 0.40f, onKey = onKey, modifier = modifier)
}

/**
 * Rejilla de teclas que ocupa todo el espacio que se le da. El tamaño del texto se deriva del
 * lado menor de una tecla, de modo que escala con la pantalla (teléfono, tablet, horizontal).
 */
@Composable
private fun KeyGrid(
    rows: List<List<KeySpec>>,
    columns: Int,
    textScale: Float,
    onKey: (KeyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val rowHeight = (maxHeight - KeyGap * (rows.size - 1)) / rows.size
        val columnWidth = (maxWidth - KeyGap * (columns - 1)) / columns
        val fontSize = (min(rowHeight.value, columnWidth.value) * textScale).coerceIn(14f, 34f).sp

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(KeyGap),
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(KeyGap),
                ) {
                    row.forEach { key ->
                        CalcKey(
                            label = key.label,
                            style = key.style,
                            fontSize = fontSize,
                            onClick = key.onClick ?: { onKey(key.action) },
                            modifier = Modifier
                                .weight(key.weight)
                                .fillMaxHeight(),
                            iconRes = key.iconRes,
                            contentDescription = key.contentDescription,
                        )
                    }
                }
            }
        }
    }
}
