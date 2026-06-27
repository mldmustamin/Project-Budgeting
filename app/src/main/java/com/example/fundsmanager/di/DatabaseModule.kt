package com.example.fundsmanager.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.fundsmanager.data.local.AppDatabase
import com.example.fundsmanager.data.local.DatabaseMigrations
import com.example.fundsmanager.data.local.SyncStatus
import com.example.fundsmanager.data.local.dao.*
import com.example.fundsmanager.util.UuidGenerator
import com.example.fundsmanager.util.logging.AppLogCategory
import com.example.fundsmanager.util.logging.AppLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        appLogger: AppLogger
    ): AppDatabase {
        appLogger.info(
            category = AppLogCategory.DATABASE,
            screen = "DatabaseModule",
            action = "database_builder",
            message = "App database builder initialized",
            details = "name=funds_manager_db"
        )
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "funds_manager_db"
        ).addMigrations(*DatabaseMigrations.ALL)
            .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                appLogger.info(
                    category = AppLogCategory.DATABASE,
                    screen = "DatabaseModule",
                    action = "database_on_create",
                    message = "Database created; seeding defaults"
                )
                seedDatabase(db)
            }
        }).build()
    }

    private fun seedDatabase(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()
        val localUserUuid = UuidGenerator.newUuid()

        db.execSQL(
            "INSERT OR IGNORE INTO users (id, name, email, uuid, syncStatus, createdAt, updatedAt, deletedAt) VALUES (?, ?, NULL, ?, ?, ?, ?, NULL)",
            arrayOf<Any>(1, "Local User", localUserUuid, SyncStatus.PENDING, now, now)
        )

        val defaultAccounts = listOf(
            "Cash in Hand", "Transfer Kantor", "Rekening Pribadi", "Reimburse", "Lain-lain"
        )
        defaultAccounts.forEach { name ->
            db.execSQL(
                "INSERT OR IGNORE INTO accounts (name, createdAt, updatedAt) VALUES (?, ?, ?)",
                arrayOf<Any>(name, now, now)
            )
        }

        val defaultCategories = listOf(
            "Transfer Dana",
            "Pengeluaran Pekerjaan",
            "Pengeluaran Pribadi",
            "Makan & Minum",
            "Transportasi",
            "Penginapan",
            "Alat Tulis Kantor",
            "Lain-lain"
        )
        defaultCategories.forEach { name ->
            db.execSQL(
                "INSERT OR IGNORE INTO categories (name, createdAt, updatedAt) VALUES (?, ?, ?)",
                arrayOf<Any>(name, now, now)
            )
        }
    }

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun provideProjectDao(db: AppDatabase): ProjectDao = db.projectDao()

    @Provides
    fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideAttachmentDao(db: AppDatabase): AttachmentDao = db.attachmentDao()

    @Provides
    fun provideAuditLogDao(db: AppDatabase): AuditLogDao = db.auditLogDao()
}
