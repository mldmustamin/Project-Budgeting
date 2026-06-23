package com.example.fundsmanager.ui.navigation

sealed class Screen(val route: String) {
    object ProjectList : Screen("project_list")
    object GlobalTransactionList : Screen("transaction_home")
    object Dashboard : Screen("dashboard")
    object CategoryManager : Screen("category_manager")
    object ProjectDashboard : Screen("project_dashboard/{projectId}") {
        fun createRoute(projectId: Long) = "project_dashboard/$projectId"
    }
    object TransactionList : Screen("transaction_list/{projectId}") {
        fun createRoute(projectId: Long) = "transaction_list/$projectId"
    }
    object TransactionForm : Screen("transaction_form/{projectId}?transactionId={transactionId}") {
        fun createRoute(projectId: Long, transactionId: Long? = null) = 
            "transaction_form/$projectId" + (transactionId?.let { "?transactionId=$it" } ?: "")
    }
    object Settings : Screen("settings")
}
