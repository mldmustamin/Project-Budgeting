package com.example.fundsmanager.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.fundsmanager.ui.screen.dashboard.DashboardScreen
import com.example.fundsmanager.ui.screen.home.GlobalTransactionScreen
import com.example.fundsmanager.ui.screen.home.ReportHomeScreen
import com.example.fundsmanager.ui.screen.project.ProjectListScreen
import com.example.fundsmanager.ui.screen.transaction.TransactionFormScreen
import com.example.fundsmanager.ui.screen.transaction.TransactionListScreen
import com.example.fundsmanager.ui.screen.importbackup.ImportPreviewScreen
import com.example.fundsmanager.util.logging.AppLogCategory
import com.example.fundsmanager.util.logging.AppLogger

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
        startDestination = Screen.ProjectList.route,
        modifier = modifier
    ) {
        composable(Screen.ProjectList.route) {
            ProjectListScreen(
                onProjectClick = { projectId: Long ->
                    navController.navigate(Screen.ProjectDashboard.createRoute(projectId))
                },
                onTransactionsClick = {
                    navController.navigate(Screen.GlobalTransactionList.route) {
                        launchSingleTop = true
                    }
                },
                onReportsClick = {
                    navController.navigate(Screen.ReportHome.route) {
                        launchSingleTop = true
                    }
                },
                onImportClick = {
                    navController.navigate(Screen.ImportPreview.route)
                }
            )
        }

        composable(Screen.GlobalTransactionList.route) {
            GlobalTransactionScreen(
                onProjectMenuClick = {
                    navController.navigate(Screen.ProjectList.route) {
                        popUpTo(Screen.ProjectList.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onReportMenuClick = {
                    navController.navigate(Screen.ReportHome.route) {
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

        composable(Screen.ReportHome.route) {
            ReportHomeScreen(
                onProjectMenuClick = {
                    navController.navigate(Screen.ProjectList.route) {
                        popUpTo(Screen.ProjectList.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onTransactionMenuClick = {
                    navController.navigate(Screen.GlobalTransactionList.route) {
                        launchSingleTop = true
                    }
                },
                onProjectClick = { projectId ->
                    navController.navigate(Screen.ProjectDashboard.createRoute(projectId))
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
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.ImportPreview.route) {
            ImportPreviewScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
