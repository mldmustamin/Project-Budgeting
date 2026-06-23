package com.example.fundsmanager.domain.usecase

import com.example.fundsmanager.domain.model.Transaction
import com.example.fundsmanager.domain.model.TransactionType
import com.example.fundsmanager.domain.repository.FundsRepository
import java.time.LocalDate
import java.time.format.DateTimeParseException
import javax.inject.Inject

class ValidateTransactionUseCase @Inject constructor(
    private val repository: FundsRepository
) {
    suspend operator fun invoke(transaction: Transaction): TransactionValidationResult {
        validateFields(transaction)?.let { return TransactionValidationResult.Invalid(it) }

        val duplicate = repository.getTransactionsByProject(transaction.projectId)
            .asSequence()
            .filter { it.deletedAt == null }
            .filter { it.id != transaction.id }
            .firstOrNull { existing -> existing.isDuplicateOf(transaction) }

        return if (duplicate != null) {
            TransactionValidationResult.DuplicateWarning(
                message = "Possible duplicate transaction on ${transaction.date}: ${transaction.description}"
            )
        } else {
            TransactionValidationResult.Valid
        }
    }

    private fun validateFields(transaction: Transaction): String? {
        if (transaction.date.isBlank()) return "Tanggal wajib diisi"
        try {
            LocalDate.parse(transaction.date)
        } catch (_: DateTimeParseException) {
            return "Format tanggal harus yyyy-MM-dd"
        }

        if (transaction.reportedAmount <= 0) return "Nominal wajib lebih dari 0"
        if (transaction.realAmount <= 0) return "Nominal real wajib lebih dari 0"
        if (transaction.accountId == null) return "Akun wajib dipilih"
        if (transaction.type != TransactionType.FUND_IN && transaction.description.isBlank()) {
            return "Keterangan wajib diisi untuk expense"
        }

        return null
    }

    private fun Transaction.isDuplicateOf(other: Transaction): Boolean {
        return projectId == other.projectId &&
            type == other.type &&
            date == other.date &&
            reportedAmount == other.reportedAmount &&
            realAmount == other.realAmount &&
            categoryId == other.categoryId &&
            description.normalizedDescription() == other.description.normalizedDescription()
    }

    private fun String.normalizedDescription(): String {
        return trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
    }
}

sealed interface TransactionValidationResult {
    object Valid : TransactionValidationResult
    data class Invalid(val message: String) : TransactionValidationResult
    data class DuplicateWarning(val message: String) : TransactionValidationResult
}
