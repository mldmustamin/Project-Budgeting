package com.example.fundsmanager.domain.model

fun TransactionType.toUiLabel(): String {
    return when (this) {
        TransactionType.FUND_IN -> "Transfer Dana"
        TransactionType.OFFICE_EXPENSE -> "Pengeluaran Pekerjaan"
        TransactionType.PERSONAL_EXPENSE -> "Pengeluaran Pribadi"
    }
}

fun TransactionType.requiresRealAmountInput(): Boolean {
    return this == TransactionType.OFFICE_EXPENSE
}

fun TransactionType.isIncome(): Boolean {
    return this == TransactionType.FUND_IN
}

fun TransactionType.isExpense(): Boolean {
    return this != TransactionType.FUND_IN
}

fun defaultTransactionType(): TransactionType {
    return TransactionType.FUND_IN
}
