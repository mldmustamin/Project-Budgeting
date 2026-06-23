package com.example.fundsmanager.domain.model

fun TransactionType.toUiLabel(): String {
    return when (this) {
        TransactionType.FUND_IN -> "Dana Masuk"
        TransactionType.OFFICE_EXPENSE -> "Expense Kantor"
        TransactionType.PERSONAL_EXPENSE -> "Expense Pribadi"
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
    return TransactionType.OFFICE_EXPENSE
}
