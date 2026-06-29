package com.example.fundsmanager.ui.screen.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fundsmanager.domain.model.BudgetTask
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTasksScreen(
    viewModel: MyTasksViewModel = hiltViewModel(),
    onCreateClick: () -> Unit = {},
    onTaskClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currencyFormat = remember { NumberFormat.getNumberInstance(Locale("id", "ID")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Tasks") },
                actions = {
                    IconButton(onClick = onCreateClick) {
                        Icon(Icons.Default.Add, "Buat Estimasi Baru")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.draftTasks.isEmpty() && uiState.pendingTasks.isEmpty()
            && uiState.activeTasks.isEmpty() && uiState.completedTasks.isEmpty()
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Belum ada task")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                if (uiState.draftTasks.isNotEmpty()) {
                    item { SectionHeader("Draft") }
                    items(uiState.draftTasks) { task -> TaskCard(task, onTaskClick, currencyFormat) }
                }
                if (uiState.pendingTasks.isNotEmpty()) {
                    item { SectionHeader("Menunggu Review") }
                    items(uiState.pendingTasks) { task -> TaskCard(task, onTaskClick, currencyFormat) }
                }
                if (uiState.activeTasks.isNotEmpty()) {
                    item { SectionHeader("Siap Dikerjakan") }
                    items(uiState.activeTasks) { task -> TaskCard(task, onTaskClick, currencyFormat) }
                }
                if (uiState.completedTasks.isNotEmpty()) {
                    item { SectionHeader("Selesai") }
                    items(uiState.completedTasks) { task -> TaskCard(task, onTaskClick, currencyFormat, compact = true) }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        title, style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun TaskCard(task: BudgetTask, onClick: (String) -> Unit, currencyFormat: NumberFormat, compact: Boolean = false) {
    Card(
        onClick = { onClick(task.uuid) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("#${task.taskNo}", fontWeight = FontWeight.Bold)
                Text(StageBadge(task.stage), color = MaterialTheme.colorScheme.secondary)
            }
            if (!compact) {
                Text(task.taskName ?: task.remoteName ?: "Task ${task.taskNo}")
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(task.jobType, style = MaterialTheme.typography.labelSmall)
                    Text("Rp ${currencyFormat.format(task.totalApproved)}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

fun StageBadge(stage: String): String = when (stage) {
    "DRAFT" -> "Draft"
    "ESTIMASI" -> "Menunggu Kordinator"
    "FORWARDED" -> "Menunggu Manager"
    "APPROVED" -> "Approved"
    "REALISASI" -> "Menunggu Verifikasi"
    "VERIFIED" -> "Terverifikasi"
    "RECONCILED" -> "Selesai"
    "REJECTED" -> "Ditolak"
    else -> stage
}
