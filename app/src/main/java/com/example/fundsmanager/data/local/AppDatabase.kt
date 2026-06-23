package com.example.fundsmanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.fundsmanager.data.local.dao.AccountDao
import com.example.fundsmanager.data.local.dao.AuditLogDao
import com.example.fundsmanager.data.local.dao.AttachmentDao
import com.example.fundsmanager.data.local.dao.CategoryDao
import com.example.fundsmanager.data.local.dao.ProjectDao
import com.example.fundsmanager.data.local.dao.TransactionDao
import com.example.fundsmanager.data.local.dao.UserDao
import com.example.fundsmanager.data.local.entity.AccountEntity
import com.example.fundsmanager.data.local.entity.AuditLogEntity
import com.example.fundsmanager.data.local.entity.AttachmentEntity
import com.example.fundsmanager.data.local.entity.CategoryEntity
import com.example.fundsmanager.data.local.entity.ProjectEntity
import com.example.fundsmanager.data.local.entity.TransactionEntity
import com.example.fundsmanager.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        ProjectEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        AttachmentEntity::class,
        AuditLogEntity::class
    ],
    version = 6,
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
}
