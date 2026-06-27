package com.example.fundsmanager.data.local

object SyncStatus {
    const val PENDING = "PENDING"
    const val SYNCED = "SYNCED"
    const val REJECTED = "REJECTED"
    const val CONFLICT = "CONFLICT"
}

object ApprovalStatus {
    const val DRAFT = "DRAFT"
    const val PENDING = "PENDING"
    const val APPROVED = "APPROVED"
    const val REJECTED = "REJECTED"
    const val NEED_REVISION = "NEED_REVISION"
}

object FinanceStatus {
    const val ACTIVE = "ACTIVE"
    const val CORRECTED = "CORRECTED"
    const val VOIDED = "VOIDED"
}
