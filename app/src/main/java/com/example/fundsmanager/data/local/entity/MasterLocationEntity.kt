package com.example.fundsmanager.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "master_locations",
    indices = [Index(value = ["uuid"], unique = true)]
)
data class MasterLocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val projectId: Long,
    val remoteName: String,
    val address: String,
    val provinsi: String? = null,
    val kotaKab: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)
