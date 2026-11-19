package com.example.tiptime.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.tiptime.R
import com.example.tiptime.engine.AngleMode
import com.example.tiptime.engine.EvalResult
import com.example.tiptime.engine.ExpressionEngine
import com.example.tiptime.engine.NumberFormatter
import com.example.tiptime.engine.tools.BaseConverter
import com.example.tiptime.engine.tools.NumberBase
import com.example.tiptime.engine.tools.UnitCategory
import com.example.tiptime.engine.tools.UnitConverter

/**
 * Herramientas: conversor de bases y conversor de unidades.
 * Todo se calcula al escribir; no hay botón "calcular". Los campos numéricos aceptan expresiones
 * ("1/3", "2π") porque se evalúan con el mismo motor de la calculadora.
 */
@Composable
fun ToolsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var tab by rememberSaveable { mutableStateOf(0) }
    val titles = listOf(R.string.tools_tab_bases, R.string.tools_tab_units)

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.cd_back),
                    )
                }
                Text(
                    text = stringResource(R.string.tools_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.0f)) {
                titles.forEachIndexed { index, title ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(stringResource(title)) })
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    when (tab) {
                        0 -> BasesTool()
                        else -> UnitsTool()
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Utilidades
// ─────────────────────────────────────────────────────────────────────────────

/** Evalúa un campo numérico (admite expresiones); `null` si está vacío o no es válido. */
private fun parseNumber(text: String): Double? {
    if (text.isBlank()) return null
    return (ExpressionEngine.evaluate(text.replace(',', '.'), AngleMode.RADIANS) as? EvalResult.Success)?.value
}

private fun fmt(value: Double) = NumberFormatter.format(value)

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    ascii: Boolean = false,
    isError: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (ascii) KeyboardType.Ascii else KeyboardType.Text,
            imeAction = ImeAction.Done,
        ),
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun ResultLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(min = 64.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = FontFamily.Monospace,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bases numéricas
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BasesTool() {
    var base by rememberSaveable { mutableStateOf(NumberBase.DECIMAL.ordinal) }
    var text by rememberSaveable { mutableStateOf("") }
    val from = NumberBase.entries[base]
    val converted = if (text.isBlank()) null else BaseConverter.convertAll(text, from)

    SectionLabel(stringResource(R.string.tools_from))
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NumberBase.entries.forEach { option ->
            FilterChip(
                selected = option == from,
                onClick = { base = option.ordinal },
                label = { Text(option.label) },
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    NumberField(
        label = stringResource(R.string.tools_value),
        value = text,
        onValueChange = { text = it.take(BaseConverter.MAX_DIGITS + 1) },
        modifier = Modifier.fillMaxWidth(),
        ascii = true,
        isError = text.isNotBlank() && converted == null,
    )
    if (text.isNotBlank() && converted == null) {
        Text(
            text = stringResource(R.string.tools_invalid_input),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    if (converted != null) {
        NumberBase.entries.forEach { option -> ResultLine(option.label, converted.getValue(option)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Unidades
// ─────────────────────────────────────────────────────────────────────────────

@StringRes
private fun UnitCategory.labelRes(): Int = when (this) {
    UnitCategory.LENGTH -> R.string.tools_category_length
    UnitCategory.MASS -> R.string.tools_category_mass
    UnitCategory.TEMPERATURE -> R.string.tools_category_temperature
    UnitCategory.TIME -> R.string.tools_category_time
    UnitCategory.SPEED -> R.string.tools_category_speed
    UnitCategory.AREA -> R.string.tools_category_area
    UnitCategory.VOLUME -> R.string.tools_category_volume
    UnitCategory.DATA -> R.string.tools_category_data
}

@Composable
private fun UnitsTool() {
    var categoryIndex by rememberSaveable { mutableStateOf(0) }
    var fromIndex by rememberSaveable { mutableStateOf(0) }
    var toIndex by rememberSaveable { mutableStateOf(1) }
    var text by rememberSaveable { mutableStateOf("1") }

    val category = UnitCategory.entries[categoryIndex]
    // Al cambiar de categoría los índices podrían quedar fuera de rango.
    val from = category.units[fromIndex.coerceIn(0, category.units.lastIndex)]
    val to = category.units[toIndex.coerceIn(0, category.units.lastIndex)]
    val value = parseNumber(text)

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        UnitCategory.entries.forEach { option ->
            FilterChip(
                selected = option == category,
                onClick = {
                    categoryIndex = option.ordinal
                    fromIndex = 0
                    toIndex = 1.coerceAtMost(option.units.lastIndex)
                },
                label = { Text(stringResource(option.labelRes())) },
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    NumberField(
        label = stringResource(R.string.tools_value),
        value = text,
        onValueChange = { text = it },
        modifier = Modifier.fillMaxWidth(),
        isError = text.isNotBlank() && value == null,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        UnitPicker(stringResource(R.string.tools_from), from.symbol, category, Modifier.weight(1f)) { fromIndex = it }
        UnitPicker(stringResource(R.string.tools_to), to.symbol, category, Modifier.weight(1f)) { toIndex = it }
    }
    Spacer(modifier = Modifier.height(16.dp))
    if (value != null) {
        val result = UnitConverter.convert(value, from, to)
        Text(
            text = "${fmt(value)} ${from.symbol} = ${fmt(result)} ${to.symbol}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun UnitPicker(
    label: String,
    selectedSymbol: String,
    category: UnitCategory,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$label: $selectedSymbol")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            category.units.forEachIndexed { index, unit ->
                DropdownMenuItem(
                    text = { Text(unit.symbol) },
                    onClick = {
                        expanded = false
                        onSelect(index)
                    },
                )
            }
        }
    }
}
