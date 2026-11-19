package com.example.tiptime.ui

import androidx.annotation.StringRes
import com.example.tiptime.R
import com.example.tiptime.engine.CalcError

/** Texto (recurso de cadena) que se muestra al usuario para cada tipo de error de cálculo. */
@StringRes
fun CalcError.messageRes(): Int = when (this) {
    CalcError.SYNTAX -> R.string.error_syntax
    CalcError.DIVISION_BY_ZERO -> R.string.error_division_by_zero
    CalcError.DOMAIN -> R.string.error_domain
    CalcError.OVERFLOW -> R.string.error_overflow
}
