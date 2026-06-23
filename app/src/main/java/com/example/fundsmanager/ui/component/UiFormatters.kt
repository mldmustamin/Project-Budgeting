package com.example.fundsmanager.ui.component

import java.text.NumberFormat
import java.util.Locale

fun formatMoney(amount: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID"))
    formatter.maximumFractionDigits = 0
    return formatter.format(amount)
}

fun formatRupiah(amount: Long): String {
    return "Rp ${formatMoney(amount)}"
}

fun digitsOnly(value: String): String {
    return value.filter { it.isDigit() }
}
