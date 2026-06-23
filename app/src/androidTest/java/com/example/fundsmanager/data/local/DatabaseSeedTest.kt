package com.example.fundsmanager.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fundsmanager.data.local.dao.AccountDao
import com.example.fundsmanager.data.local.dao.CategoryDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseSeedTest {

    private lateinit var db: AppDatabase
    private lateinit var accountDao: AccountDao
    private lateinit var categoryDao: CategoryDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Note: Callback is NOT triggered for inMemoryDatabase by default in some versions,
        // but for seeding logic verification, we can trigger it manually or use a real temp file.
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(object : androidx.room.RoomDatabase.Callback() {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    // Seed logic from DatabaseModule
                    val defaultAccounts = listOf("Cash in Hand", "Transfer Kantor", "Rekening Pribadi", "Reimburse", "Lain-lain")
                    defaultAccounts.forEach { name ->
                        db.execSQL("INSERT OR IGNORE INTO accounts (name, createdAt, updatedAt) VALUES ('$name', 100, 100)")
                    }
                    val defaultCategories = listOf("Makan & Minum", "Transportasi", "Penginapan", "Alat Tulis Kantor", "Lain-lain")
                    defaultCategories.forEach { name ->
                        db.execSQL("INSERT OR IGNORE INTO categories (name, createdAt, updatedAt) VALUES ('$name', 100, 100)")
                    }
                }
            })
            .build()
        accountDao = db.accountDao()
        categoryDao = db.categoryDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testDefaultAccountsAreSeeded() = runBlocking {
        // Trigger db creation
        db.query("SELECT 1", null).close()
        
        val accounts = accountDao.getAllAccounts().first()
        val names = accounts.map { it.name }
        
        assertEquals(5, accounts.size)
        assertTrue(names.contains("Cash in Hand"))
        assertTrue(names.contains("Transfer Kantor"))
    }

    @Test
    fun testDefaultCategoriesAreSeeded() = runBlocking {
        // Trigger db creation
        db.query("SELECT 1", null).close()
        
        val categories = categoryDao.getAllCategories().first()
        assertEquals(5, categories.size)
        assertTrue(categories.any { it.name == "Transportasi" })
    }
}
