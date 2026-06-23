package com.example.fundsmanager.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.fundsmanager.data.local.AppDatabase
import com.example.fundsmanager.data.local.dao.*
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
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
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

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS audit_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId INTEGER NOT NULL,
                    entityType TEXT NOT NULL,
                    entityId INTEGER NOT NULL,
                    action TEXT NOT NULL,
                    oldValueJson TEXT,
                    newValueJson TEXT,
                    createdAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_audit_logs_entityType_entityId ON audit_logs(entityType, entityId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_audit_logs_userId ON audit_logs(userId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_audit_logs_createdAt ON audit_logs(createdAt)")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "INSERT OR IGNORE INTO users (id, name, email, createdAt, updatedAt, deletedAt) VALUES (1, 'Local User', NULL, 0, 0, NULL)"
            )
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Reserved version step. Version 4 previously held an experimental local queue
            // during development; runtime UI builds should not add sync infrastructure.
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Keep user data intact. Extra experimental tables from v4, if present, are ignored.
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE projects ADD COLUMN startAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE projects ADD COLUMN completedAt INTEGER")
        }
    }

    private fun seedDatabase(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()

        db.execSQL(
            "INSERT OR IGNORE INTO users (id, name, email, createdAt, updatedAt, deletedAt) VALUES (?, ?, NULL, ?, ?, NULL)",
            arrayOf<Any>(1, "Local User", now, now)
        )
        
        // Seed Accounts
        val defaultAccounts = listOf(
            "Cash in Hand", "Transfer Kantor", "Rekening Pribadi", "Reimburse", "Lain-lain"
        )
        defaultAccounts.forEach { name ->
            db.execSQL(
                "INSERT OR IGNORE INTO accounts (name, createdAt, updatedAt) VALUES (?, ?, ?)",
                arrayOf<Any>(name, now, now)
            )
        }

        // Seed Categories
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
