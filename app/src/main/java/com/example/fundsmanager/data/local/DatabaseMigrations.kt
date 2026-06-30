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

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // task_expenses
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS task_expenses (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    uuid TEXT NOT NULL,
                    projectId INTEGER NOT NULL,
                    locationId INTEGER,
                    taskNo TEXT NOT NULL,
                    vid TEXT NOT NULL,
                    taskName TEXT,
                    remoteName TEXT,
                    jobType TEXT NOT NULL,
                    stage TEXT NOT NULL DEFAULT 'DRAFT',
                    submittedBy INTEGER NOT NULL,
                    forwardedBy INTEGER,
                    approvedBy INTEGER,
                    verifiedBy INTEGER,
                    reconciledBy INTEGER,
                    totalEstimated INTEGER NOT NULL DEFAULT 0,
                    totalRevised INTEGER NOT NULL DEFAULT 0,
                    totalApproved INTEGER NOT NULL DEFAULT 0,
                    totalRealization INTEGER NOT NULL DEFAULT 0,
                    rejectionReason TEXT,
                    notes TEXT,
                    completedAt INTEGER,
                    deadlineAt INTEGER,
                    syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                    lastSyncedAt INTEGER,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    deletedAt INTEGER
                )
            """.trimIndent())
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_task_expenses_uuid ON task_expenses(uuid)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_task_expenses_submittedBy ON task_expenses(submittedBy)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_task_expenses_stage ON task_expenses(stage)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_task_expenses_syncStatus ON task_expenses(syncStatus)")

            // expense_items
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS expense_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    uuid TEXT NOT NULL,
                    taskExpenseId INTEGER NOT NULL,
                    templateId INTEGER,
                    tanggal TEXT NOT NULL,
                    note TEXT,
                    estimatedAmount INTEGER NOT NULL DEFAULT 0,
                    revisedAmount INTEGER,
                    approvedAmount INTEGER,
                    realizationAmount INTEGER,
                    buktiPath TEXT,
                    requiresBill INTEGER NOT NULL DEFAULT 0,
                    billVerified INTEGER NOT NULL DEFAULT 0,
                    itemStatus TEXT NOT NULL DEFAULT 'DRAFT',
                    rejectionReason TEXT,
                    sortOrder INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY (taskExpenseId) REFERENCES task_expenses(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_expense_items_uuid ON expense_items(uuid)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_expense_items_taskExpenseId ON expense_items(taskExpenseId)")

            // budget_templates
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS budget_templates (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    uuid TEXT NOT NULL,
                    categoryName TEXT NOT NULL,
                    categoryGroup TEXT NOT NULL,
                    paguType TEXT NOT NULL,
                    paguAmount INTEGER,
                    paguNote TEXT,
                    requiresBill INTEGER NOT NULL DEFAULT 0,
                    billNote TEXT,
                    displayOrder INTEGER NOT NULL DEFAULT 0,
                    isActive INTEGER NOT NULL DEFAULT 1
                )
            """.trimIndent())

            // master_locations
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS master_locations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    uuid TEXT NOT NULL,
                    projectId INTEGER NOT NULL,
                    remoteName TEXT NOT NULL,
                    address TEXT NOT NULL DEFAULT '',
                    city TEXT,
                    province TEXT,
                    latitude REAL,
                    longitude REAL
                )
            """.trimIndent())
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_master_locations_uuid ON master_locations(uuid)")
        }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // task_expenses (created in MIGRATION_8_9, but re-created for safety)
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS task_expenses (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    uuid TEXT NOT NULL,
                    projectId INTEGER NOT NULL,
                    locationId INTEGER,
                    taskNo TEXT NOT NULL,
                    vid TEXT NOT NULL,
                    taskName TEXT,
                    remoteName TEXT,
                    jobType TEXT NOT NULL,
                    stage TEXT NOT NULL DEFAULT 'DRAFT',
                    submittedBy INTEGER NOT NULL,
                    forwardedBy INTEGER,
                    approvedBy INTEGER,
                    verifiedBy INTEGER,
                    reconciledBy INTEGER,
                    totalEstimated INTEGER NOT NULL DEFAULT 0,
                    totalRevised INTEGER NOT NULL DEFAULT 0,
                    totalApproved INTEGER NOT NULL DEFAULT 0,
                    totalRealization INTEGER NOT NULL DEFAULT 0,
                    rejectionReason TEXT,
                    notes TEXT,
                    completedAt INTEGER,
                    deadlineAt INTEGER,
                    syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                    lastSyncedAt INTEGER,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    deletedAt INTEGER
                )
            """.trimIndent())
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_task_expenses_uuid ON task_expenses(uuid)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS expense_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    uuid TEXT NOT NULL,
                    taskExpenseId INTEGER NOT NULL,
                    templateId INTEGER,
                    tanggal TEXT NOT NULL,
                    note TEXT,
                    estimatedAmount INTEGER NOT NULL DEFAULT 0,
                    revisedAmount INTEGER,
                    approvedAmount INTEGER,
                    realizationAmount INTEGER,
                    buktiPath TEXT,
                    requiresBill INTEGER NOT NULL DEFAULT 0,
                    billVerified INTEGER NOT NULL DEFAULT 0,
                    itemStatus TEXT NOT NULL DEFAULT 'DRAFT',
                    sortOrder INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY (taskExpenseId) REFERENCES task_expenses(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_expense_items_uuid ON expense_items(uuid)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS budget_templates (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    uuid TEXT NOT NULL,
                    categoryName TEXT NOT NULL,
                    categoryGroup TEXT NOT NULL,
                    paguType TEXT NOT NULL,
                    paguAmount INTEGER,
                    requiresBill INTEGER NOT NULL DEFAULT 0,
                    displayOrder INTEGER NOT NULL DEFAULT 0,
                    isActive INTEGER NOT NULL DEFAULT 1
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS master_locations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    uuid TEXT NOT NULL,
                    projectId INTEGER NOT NULL,
                    remoteName TEXT NOT NULL,
                    address TEXT NOT NULL DEFAULT '',
                    city TEXT,
                    province TEXT,
                    latitude REAL,
                    longitude REAL
                )
            """.trimIndent())
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_master_locations_uuid ON master_locations(uuid)")
        }
    }

    val ALL = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9,
        MIGRATION_9_10
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