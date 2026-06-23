package com.example.fundsmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.fundsmanager.ui.navigation.FundsManagerNavHost
import com.example.fundsmanager.ui.theme.FundsManagerTheme
import com.example.fundsmanager.util.logging.AppLogCategory
import com.example.fundsmanager.util.logging.FileAppLogger
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appLogger = FileAppLogger(applicationContext)
        appLogger.info(
            category = AppLogCategory.APP,
            screen = "MainActivity",
            action = "on_create",
            message = "MainActivity created"
        )
        enableEdgeToEdge()
        setContent {
            FundsManagerTheme {
                val navController = rememberNavController()
                FundsManagerNavHost(
                    navController = navController,
                    appLogger = appLogger,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
