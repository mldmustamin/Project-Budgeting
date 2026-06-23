package com.example.fundsmanager.domain.service

import com.example.fundsmanager.domain.model.*
import com.example.fundsmanager.domain.repository.FundsRepository
import com.example.fundsmanager.domain.usecase.CalculateProjectSummaryUseCase
import javax.inject.Inject

class ExportCsvUseCase @Inject constructor(
    private val repository: FundsRepository,
    private val calculateProjectSummaryUseCase: CalculateProjectSummaryUseCase
) {
    suspend fun execute(projectId: Long): String {
        val summary = calculateProjectSummaryUseCase(projectId) ?: return ""
        val transactions = repository.getTransactionsByProject(projectId)
            .filter { it.deletedAt == null }
        
        val sb = StringBuilder()
        // Header
        sb.append("Tanggal,Keterangan,Jenis,Nominal Dilaporkan,Nominal Real,Selisih,Catatan\n")
        
        // Rows
        transactions.forEach { tx ->
            val saving = if (tx.type == TransactionType.OFFICE_EXPENSE) {
                tx.reportedAmount - tx.realAmount
            } else 0L
            
            sb.append("${tx.date},")
            sb.append("${tx.description.escapeCsv()},")
            sb.append("${tx.type.toUiLabel()},")
            sb.append("${tx.reportedAmount},")
            sb.append("${tx.realAmount},")
            sb.append("$saving,")
            sb.append("${(tx.note ?: "").escapeCsv()}\n")
        }
        
        // Summary Footer
        sb.append("\nRINGKASAN\n")
        sb.append("Total Dana Masuk,${summary.totalFundIn}\n")
        sb.append("Dilaporkan ke Kantor,${summary.totalOfficeReported}\n")
        sb.append("Pengeluaran Real,${summary.totalOfficeReal}\n")
        sb.append("Expense Pribadi,${summary.totalPersonalExpense}\n")
        sb.append("Selisih / Hemat,${summary.saving}\n")
        sb.append("Sisa Real,${summary.remainingReal}\n")
        sb.append("Posisi Bersih,${summary.netPosition}\n")
        
        return sb.toString()
    }

    private fun String.escapeCsv(): String {
        return if (this.contains(",") || this.contains("\"") || this.contains("\n")) {
            "\"${this.replace("\"", "\"\"")}\""
        } else {
            this
        }
    }
}
