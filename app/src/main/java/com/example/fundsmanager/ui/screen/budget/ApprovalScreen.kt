package com.example.fundsmanager.ui.screen.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fundsmanager.domain.model.BudgetTask
import com.example.fundsmanager.domain.model.ExpenseItemModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApprovalScreen(
    viewModel: ApprovalViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currencyFormat = remember { NumberFormat.getNumberInstance(Locale("id", "ID")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Approval") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Kembali") }
                }
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
        } else if (uiState.tasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Tidak ada task menunggu approval")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(uiState.tasks) { task ->
                    ApprovalTaskCard(
                        task = task,
                        currencyFormat = currencyFormat,
                        userName = { id -> uiState.userNames[id] ?: "User#$id" },
                        onClick = { viewModel.selectTask(task) }
                    )
                }
            }
        }

        // Approval Dialog
        uiState.selectedTask?.let { task ->
            ApprovalDialog(
                task = task,
                items = uiState.selectedItems,
                approvedAmounts = uiState.approvedAmounts,
                currencyFormat = currencyFormat,
                isSubmitting = uiState.isSubmitting,
                onAmountChange = { uuid, amount -> viewModel.updateApprovedAmount(uuid, amount) },
                onApprove = { viewModel.approveTask(task.id) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }
    }
}

@Composable
fun ApprovalTaskCard(
    task: BudgetTask,
    currencyFormat: NumberFormat,
    userName: (Long) -> String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("#${task.taskNo}", fontWeight = FontWeight.Bold)
                Text(StageBadge(task.stage), color = MaterialTheme.colorScheme.secondary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("VID: ${task.vid}", style = MaterialTheme.typography.bodySmall)
                Text(task.jobType, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "Diajukan: ${userName(task.submittedBy)}",
                style = MaterialTheme.typography.bodySmall
            )
            task.forwardedBy?.let { fwd ->
                Text(
                    "Diteruskan: ${userName(fwd)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Total Estimasi: Rp ${currencyFormat.format(task.totalEstimated)}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ApprovalDialog(
    task: BudgetTask,
    items: List<ExpenseItemModel>,
    approvedAmounts: Map<String, String>,
    currencyFormat: NumberFormat,
    isSubmitting: Boolean,
    onAmountChange: (String, String) -> Unit,
    onApprove: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Approval: #${task.taskNo}")
        },
        text = {
            Column {
                Text("VID: ${task.vid}", style = MaterialTheme.typography.bodySmall)
                Text("Jenis: ${task.jobType}", style = MaterialTheme.typography.bodySmall)
                Text(
                    "Total Estimasi: Rp ${currencyFormat.format(task.totalEstimated)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Item Budget:", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                if (items.isEmpty()) {
                    Text("Memuat item...", style = MaterialTheme.typography.bodySmall)
                } else {
                    items.forEachIndexed { index, item ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    "Item #${index + 1} — ${item.tanggal}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                item.note?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall)
                                }
                                Text(
                                    "Estimasi: Rp ${currencyFormat.format(item.estimatedAmount)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = approvedAmounts[item.uuid] ?: "",
                                    onValueChange = { onAmountChange(item.uuid, it) },
                                    label = { Text("Approved (Rp)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onApprove,
                enabled = !isSubmitting && items.isNotEmpty()
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Approve")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
