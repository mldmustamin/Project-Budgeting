package com.example.fundsmanager.util.logging

interface AppLogger {
    fun debug(category: AppLogCategory, screen: String? = null, action: String? = null, message: String, details: String? = null)
    fun info(category: AppLogCategory, screen: String? = null, action: String? = null, message: String, details: String? = null)
    fun warning(category: AppLogCategory, screen: String? = null, action: String? = null, message: String, details: String? = null)
    fun error(category: AppLogCategory, screen: String? = null, action: String? = null, message: String, throwable: Throwable? = null, details: String? = null)
    fun crash(screen: String? = null, action: String? = null, message: String, throwable: Throwable)
}
