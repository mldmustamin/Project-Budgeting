package com.example.fundsmanager.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.fundsmanager.util.UuidGenerator

object DatabaseMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
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

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "INSERT OR IGNORE INTO users (id, name, email, createdAt, updatedAt, deletedAt) VALUES (1, 'Local User', NULL, 0, 0, NULL)"
            )
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Reserved version step.
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Keep user data intact.
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE projects ADD COLUMN startAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE projects ADD COLUMN completedAt INTEGER")
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            addUserSyncColumns(db)
            addProjectSyncColumns(db)
            addTransactionSyncColumns(db)
            addAttachmentSyncColumns(db)

            backfillUuids(db, "users")
            backfillUuids(db, "projects")
            backfillUuids(db, "transactions")
            backfillUuids(db, "attachments")

            db.execSQL(
                """
                UPDATE transactions SET projectUuid = (
                    SELECT uuid FROM projects WHERE projects.id = transactions.projectId
                ) WHERE projectUuid IS NULL
                """.trimIndent()
            )
            db.execSQL(
                """
                UPDATE transactions SET userUuid = (
                    SELECT uuid FROM users WHERE users.id = transactions.userId
                ) WHERE userUuid IS NULL
                """.trimIndent()
            )

            createSyncIndexes(db)
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS sync_outbox (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    uuid TEXT NOT NULL,
                    localUserId INTEGER NOT NULL,
                    serverUserId TEXT,
                    userUuid TEXT,
                    deviceId TEXT,
                    sessionId TEXT,
                    entityType TEXT NOT NULL,
                    entityUuid TEXT NOT NULL,
                    operation TEXT NOT NULL,
                    payloadJson TEXT NOT NULL,
                    idempotencyKey TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'PENDING',
                    retryCount INTEGER NOT NULL DEFAULT 0,
                    lastError TEXT,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sync_outbox_idempotencyKey ON sync_outbox(idempotencyKey)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_outbox_status ON sync_outbox(status)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_outbox_localUserId_deviceId_sessionId ON sync_outbox(localUserId, deviceId, sessionId)")
        }
    }

    val ALL = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8
    )

    internal fun backfillUuids(db: SupportSQLiteDatabase, table: String) {
        db.query("SELECT id FROM $table WHERE uuid IS NULL OR uuid = ''").use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                db.execSQL("UPDATE $table SET uuid = ? WHERE id = ?", arrayOf<Any>(UuidGenerator.newUuid(), id))
            }
        }
    }

    private fun addUserSyncColumns(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE users ADD COLUMN uuid TEXT")
        db.execSQL("ALTER TABLE users ADD COLUMN serverId TEXT")
        db.execSQL("ALTER TABLE users ADD COLUMN serverUserId TEXT")
        db.execSQL("ALTER TABLE users ADD COLUMN syncStatus TEXT NOT NULL DEFAULT '${SyncStatus.PENDING}'")
        db.execSQL("ALTER TABLE users ADD COLUMN lastSyncedAt INTEGER")
    }

    private fun addProjectSyncColumns(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE projects ADD COLUMN uuid TEXT")
        db.execSQL("ALTER TABLE projects ADD COLUMN serverId TEXT")
        db.execSQL("ALTER TABLE projects ADD COLUMN deviceId TEXT")
        db.execSQL("ALTER TABLE projects ADD COLUMN syncStatus TEXT NOT NULL DEFAULT '${SyncStatus.PENDING}'")
        db.execSQL("ALTER TABLE projects ADD COLUMN lastSyncedAt INTEGER")
    }

    private fun addTransactionSyncColumns(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN uuid TEXT")
        db.execSQL("ALTER TABLE transactions ADD COLUMN serverId TEXT")
        db.execSQL("ALTER TABLE transactions ADD COLUMN deviceId TEXT")
        db.execSQL("ALTER TABLE transactions ADD COLUMN syncStatus TEXT NOT NULL DEFAULT '${SyncStatus.PENDING}'")
        db.execSQL("ALTER TABLE transactions ADD COLUMN approvalStatus TEXT NOT NULL DEFAULT '${ApprovalStatus.DRAFT}'")
        db.execSQL("ALTER TABLE transactions ADD COLUMN financeStatus TEXT NOT NULL DEFAULT '${FinanceStatus.ACTIVE}'")
        db.execSQL("ALTER TABLE transactions ADD COLUMN lastSyncedAt INTEGER")
        db.execSQL("ALTER TABLE transactions ADD COLUMN sessionId TEXT")
        db.execSQL("ALTER TABLE transactions ADD COLUMN serverUserId TEXT")
        db.execSQL("ALTER TABLE transactions ADD COLUMN userUuid TEXT")
        db.execSQL("ALTER TABLE transactions ADD COLUMN projectUuid TEXT")
    }

    private fun addAttachmentSyncColumns(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE attachments ADD COLUMN uuid TEXT")
        db.execSQL("ALTER TABLE attachments ADD COLUMN serverId TEXT")
        db.execSQL("ALTER TABLE attachments ADD COLUMN deviceId TEXT")
        db.execSQL("ALTER TABLE attachments ADD COLUMN syncStatus TEXT NOT NULL DEFAULT '${SyncStatus.PENDING}'")
        db.execSQL("ALTER TABLE attachments ADD COLUMN lastSyncedAt INTEGER")
    }

    private fun createSyncIndexes(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_users_uuid ON users(uuid)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_projects_uuid ON projects(uuid)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_transactions_uuid ON transactions(uuid)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_attachments_uuid ON attachments(uuid)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_syncStatus ON transactions(syncStatus)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_serverId ON transactions(serverId)")
    }
}