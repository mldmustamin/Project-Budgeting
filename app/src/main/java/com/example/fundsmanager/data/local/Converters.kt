package com.example.fundsmanager.data.local

import androidx.room.TypeConverter
import com.example.fundsmanager.data.local.entity.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String {
        return value.name
    }

    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        return try {
            TransactionType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            TransactionType.OFFICE_EXPENSE // Default or throw
        }
    }
}
