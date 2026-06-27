package com.example.fundsmanager.util

import java.util.UUID

object UuidGenerator {
    fun newUuid(): String = UUID.randomUUID().toString()
}
