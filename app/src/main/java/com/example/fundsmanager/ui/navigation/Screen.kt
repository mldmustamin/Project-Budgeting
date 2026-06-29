package com.example.fundsmanager.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object PasswordChange : Screen("password_change")
    object ProjectList : Screen("project_list")
    object GlobalTransactionList : Screen("transaction_home")
    object Dashboard : Screen("dashboard")
    object CategoryManager : Screen("category_manager")
    object AccountManager : Screen("account_manager")
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

    // Budget Request Screens
    object MyTasks : Screen("my_tasks")
    object BudgetEstimateForm : Screen("budget_estimate_form")
    object SupervisorInbox : Screen("supervisor_inbox")
    object AssignTask : Screen("assign_task")
    object Approval : Screen("approval")
    object Verification : Screen("verification")
    object LaporanPekerjaan : Screen("laporan_pekerjaan")
}
