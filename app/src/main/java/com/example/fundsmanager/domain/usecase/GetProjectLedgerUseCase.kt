package com.example.fundsmanager.domain.usecase

import com.example.fundsmanager.domain.model.Transaction
import com.example.fundsmanager.domain.repository.FundsRepository
import javax.inject.Inject

class GetProjectLedgerUseCase @Inject constructor(
    private val repository: FundsRepository
) {
    suspend operator fun invoke(projectId: Long): List<Transaction> {
        return repository.getTransactionsByProject(projectId)
            .filter { it.deletedAt == null }
    }
}
