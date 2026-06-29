package com.example.fundsmanager.ui.screen.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
fun SupervisorInboxScreen(
    viewModel: SupervisorInboxViewModel = hiltViewModel(),
    onTaskClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currencyFormat = remember { NumberFormat.getNumberInstance(Locale("id", "ID")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Supervisor Inbox") }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.pendingTasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Tidak ada task pending")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(uiState.pendingTasks) { task ->
                    SupervisorTaskCard(task, onTaskClick, currencyFormat)
                }
            }
        }
    }
}

@Composable
fun SupervisorTaskCard(
    task: BudgetTask,
    onClick: (String) -> Unit,
    currencyFormat: NumberFormat
) {
    Card(
        onClick = { onClick(task.uuid) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("#${task.taskNo}", fontWeight = FontWeight.Bold)
                Text(
                    StageBadge(task.stage),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(task.taskName ?: task.remoteName ?: "Task ${task.taskNo}")
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("VID: ${task.vid}", style = MaterialTheme.typography.labelSmall)
                    Text(task.jobType, style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Rp ${currencyFormat.format(task.totalEstimated)}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
