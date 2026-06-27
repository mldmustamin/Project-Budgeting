package com.example.fundsmanager.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration6To7Test {

    private val testDb = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate6To7_backfillsUuidAndTransactionReferences() {
        var db = helper.createDatabase(testDb, 6).apply {
            execSQL(
                "INSERT INTO users (id, name, email, createdAt, updatedAt) VALUES (1, 'Test User', NULL, 100, 100)"
            )
            execSQL(
                "INSERT INTO projects (id, userId, name, description, isArchived, startAt, createdAt, updatedAt) VALUES (1, 1, 'P1', NULL, 0, 0, 100, 100)"
            )
            execSQL(
                """
                INSERT INTO transactions (
                    id, userId, projectId, type, date, description,
                    reportedAmount, realAmount, createdAt, updatedAt
                ) VALUES (1, 1, 1, 'FUND_IN', '2026-01-01', 'Seed', 1000, 1000, 100, 100)
                """.trimIndent()
            )
            close()
        }

        db = helper.runMigrationsAndValidate(testDb, 7, false, DatabaseMigrations.MIGRATION_6_7)

        db.query("SELECT uuid FROM users WHERE id = 1").use { cursor ->
            assert(cursor.moveToFirst())
            assertFalse(cursor.getString(0).isNullOrBlank())
        }

        db.query("SELECT uuid, projectUuid, userUuid, syncStatus, approvalStatus FROM transactions WHERE id = 1").use { cursor ->
            assert(cursor.moveToFirst())
            assertNotNull(cursor.getString(0))
            assertNotNull(cursor.getString(1))
            assertNotNull(cursor.getString(2))
            assert(cursor.getString(3) == SyncStatus.PENDING)
            assert(cursor.getString(4) == ApprovalStatus.DRAFT)
        }

        db.close()
    }
}
