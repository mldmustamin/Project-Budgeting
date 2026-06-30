package com.example.fundsmanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.fundsmanager.data.local.dao.*
import com.example.fundsmanager.data.local.entity.*

@Database(
    entities = [
        UserEntity::class,
        ProjectEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        AttachmentEntity::class,
        AuditLogEntity::class,
        SyncOutboxEntity::class,
        TaskExpenseEntity::class,
        ExpenseItemEntity::class,
        BudgetTemplateEntity::class,
        MasterLocationEntity::class
    ],
    version = 10,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun projectDao(): ProjectDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun syncOutboxDao(): SyncOutboxDao
    abstract fun taskExpenseDao(): TaskExpenseDao
    abstract fun expenseItemDao(): ExpenseItemDao
    abstract fun budgetTemplateDao(): BudgetTemplateDao
    abstract fun masterLocationDao(): MasterLocationDao
}
