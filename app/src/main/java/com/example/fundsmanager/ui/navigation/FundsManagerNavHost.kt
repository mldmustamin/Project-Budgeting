package com.example.fundsmanager.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.fundsmanager.data.local.SessionManager
import com.example.fundsmanager.ui.screen.auth.LoginScreen
import com.example.fundsmanager.ui.screen.auth.LoginViewModel
import com.example.fundsmanager.ui.screen.auth.PasswordChangeScreen
import com.example.fundsmanager.ui.screen.dashboard.DashboardScreen
import com.example.fundsmanager.ui.screen.home.DashboardHomeScreen
import com.example.fundsmanager.ui.screen.home.GlobalTransactionScreen
import com.example.fundsmanager.ui.screen.project.ProjectListScreen
import com.example.fundsmanager.ui.screen.settings.AccountManagementScreen
import com.example.fundsmanager.ui.screen.settings.CategoryManagementScreen
import com.example.fundsmanager.ui.screen.settings.SettingsScreen
import com.example.fundsmanager.ui.screen.transaction.TransactionListScreen
import com.example.fundsmanager.ui.screen.transaction.TransactionFormScreen
import com.example.fundsmanager.ui.screen.budget.MyTasksScreen
import com.example.fundsmanager.ui.screen.budget.BudgetEstimateFormScreen
import com.example.fundsmanager.ui.screen.budget.ApprovalScreen
import com.example.fundsmanager.ui.screen.budget.VerificationScreen
import com.example.fundsmanager.ui.screen.budget.SupervisorInboxScreen
import com.example.fundsmanager.ui.screen.budget.AssignTaskScreen
import com.example.fundsmanager.ui.screen.budget.RealizationFormScreen
import com.example.fundsmanager.ui.screen.budget.LaporanPekerjaanScreen
import com.example.fundsmanager.util.logging.AppLogCategory
import com.example.fundsmanager.util.logging.AppLogger
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FundsManagerNavHost(
    navController: NavHostController,
    appLogger: AppLogger,
    modifier: Modifier = Modifier
) {
    val backStackEntry = navController.currentBackStackEntryAsState().value
    LaunchedEffect(backStackEntry?.destination?.route, backStackEntry?.arguments) {
        val route = backStackEntry?.destination?.route ?: return@LaunchedEffect
        appLogger.info(
            category = AppLogCategory.NAVIGATION,
            screen = route,
            action = "screen_opened",
            message = "Screen opened",
            details = "route=$route args=${backStackEntry.arguments?.keySet()?.joinToString() ?: "-"}"
        )
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onRequirePasswordChange = {
                    navController.navigate(Screen.PasswordChange.route) {
                        popUpTo(Screen.Login.route) { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.PasswordChange.route) {
            PasswordChangeScreen(
                onPasswordChanged = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardHomeScreen(
                onDashboardClick = {},
                onProjectMenuClick = {
                    navController.navigate(Screen.ProjectList.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onTransactionMenuClick = {
                    navController.navigate(Screen.GlobalTransactionList.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onSettingClick = {
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onProjectClick = { projectId ->
                    navController.navigate(Screen.ProjectDashboard.createRoute(projectId))
                },
                onOpenProjectList = {
                    navController.navigate(Screen.ProjectList.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onOpenTransactionList = {
                    navController.navigate(Screen.GlobalTransactionList.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onManageCategoriesClick = {
                    navController.navigate(Screen.CategoryManager.route)
                }
            )
        }

        composable(Screen.ProjectList.route) {
            ProjectListScreen(
                onDashboardClick = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onProjectClick = { projectId: Long ->
                    navController.navigate(Screen.ProjectDashboard.createRoute(projectId))
                },
                onTransactionsClick = {
                    navController.navigate(Screen.GlobalTransactionList.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onSettingClick = {
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.GlobalTransactionList.route) {
            GlobalTransactionScreen(
                onDashboardClick = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onProjectMenuClick = {
                    navController.navigate(Screen.ProjectList.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onSettingClick = {
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onAddTransactionClick = { projectId ->
                    navController.navigate(Screen.TransactionForm.createRoute(projectId))
                },
                onEditTransaction = { projectId, transactionId ->
                    navController.navigate(Screen.TransactionForm.createRoute(projectId, transactionId))
                }
            )
        }

        composable(Screen.Settings.route) {
            val context = LocalContext.current
            val sessionManager = remember { SessionManager(context.applicationContext) }
            val scope = rememberCoroutineScope()
            SettingsScreen(
                onDashboardClick = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onProjectClick = {
                    navController.navigate(Screen.ProjectList.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onTransactionClick = {
                    navController.navigate(Screen.GlobalTransactionList.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onManageCategoriesClick = {
                    navController.navigate(Screen.CategoryManager.route)
                },
                onManageAccountsClick = {
                    navController.navigate(Screen.AccountManager.route)
                },
                onLogoutClick = {
                    scope.launch {
                        sessionManager.clearSession()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.AccountManager.route) {
            AccountManagementScreen(
                onBackClick = { navController.popBackStack() },
                onDashboardClick = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onProjectClick = {
                    navController.navigate(Screen.ProjectList.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onTransactionClick = {
                    navController.navigate(Screen.GlobalTransactionList.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.CategoryManager.route) {
            CategoryManagementScreen(
                onBackClick = { navController.popBackStack() },
                onDashboardClick = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onProjectClick = {
                    navController.navigate(Screen.ProjectList.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onTransactionClick = {
                    navController.navigate(Screen.GlobalTransactionList.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }
        
        composable(
            route = Screen.ProjectDashboard.route,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) {
            DashboardScreen(
                onBackClick = { navController.popBackStack() },
                onAddTransactionClick = { projectId ->
                    navController.navigate(Screen.TransactionForm.createRoute(projectId))
                },
                onOpenTransactionList = { projectId ->
                    navController.navigate(Screen.TransactionList.createRoute(projectId))
                }
            )
        }

        composable(
            route = Screen.TransactionList.route,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) {
            TransactionListScreen(
                onBackClick = { navController.popBackStack() },
                onDashboardClick = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onProjectClick = { projectId ->
                    navController.navigate(Screen.ProjectDashboard.createRoute(projectId)) {
                        launchSingleTop = true
                    }
                },
                onTransactionMenuClick = {
                    navController.navigate(Screen.GlobalTransactionList.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onSettingClick = {
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onAddTransactionClick = { projectId ->
                    navController.navigate(Screen.TransactionForm.createRoute(projectId))
                },
                onEditTransaction = { projectId, transactionId ->
                    navController.navigate(Screen.TransactionForm.createRoute(projectId, transactionId))
                }
            )
        }

        composable(
            route = Screen.TransactionForm.route,
            arguments = listOf(
                navArgument("projectId") { type = NavType.LongType },
                navArgument("transactionId") { 
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) {
            TransactionFormScreen(
                onBackClick = { navController.popBackStack() },
                onDashboardClick = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onProjectClick = { projectId ->
                    navController.navigate(Screen.ProjectDashboard.createRoute(projectId)) {
                        launchSingleTop = true
                    }
                },
                onTransactionMenuClick = {
                    navController.navigate(Screen.GlobalTransactionList.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onSettingClick = {
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        // === Budget Request Screens ===

        composable(Screen.MyTasks.route) {
            MyTasksScreen(
                onCreateClick = {
                    navController.navigate(Screen.BudgetEstimateForm.route)
                },
                onTaskClick = { uuid ->
                    // TODO: navigate to task detail/form
                }
            )
        }

        composable(Screen.BudgetEstimateForm.route) {
            BudgetEstimateFormScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("realization/{taskUuid}") {
            RealizationFormScreen(
                taskUuid = it.arguments?.getString("taskUuid") ?: "",
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.LaporanPekerjaan.route) {
            LaporanPekerjaanScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SupervisorInbox.route) {
            SupervisorInboxScreen(
                onTaskClick = { uuid ->
                    // TODO: navigate to task detail
                }
            )
        }

        composable(Screen.AssignTask.route) {
            AssignTaskScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Approval.route) {
            ApprovalScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Verification.route) {
            VerificationScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}