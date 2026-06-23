package com.example.fundsmanager.ui.component

fun receiptLabel(hasReceipt: Boolean): String {
    return if (hasReceipt) "Ada bukti" else "Belum ada bukti"
}
