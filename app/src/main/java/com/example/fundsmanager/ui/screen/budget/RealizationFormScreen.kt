package com.example.fundsmanager.ui.screen.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealizationFormScreen(
    taskUuid: String,
    viewModel: RealizationViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    LaunchedEffect(taskUuid) { viewModel.loadTask(taskUuid) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val fmt = remember { NumberFormat.getNumberInstance(Locale("id", "ID")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Realisasi Budget") },
                navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, "Kembali") } }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = viewModel::saveRealization,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    enabled = !uiState.isSaving && !uiState.saved
                ) {
                    Text(if (uiState.saved) "Tersimpan" else "Simpan Realisasi")
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding)) { CircularProgressIndicator() }
        } else {
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
                uiState.task?.let { task ->
                    Text("#${task.taskNo} — ${task.jobType}", style = MaterialTheme.typography.titleMedium)
                    Text("Budget Approved: Rp ${fmt.format(task.totalApproved)}", color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                }

                uiState.items.forEachIndexed { index, item ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(item.note ?: "Item #${index + 1}", style = MaterialTheme.typography.titleSmall)
                            Text("Approved: Rp ${fmt.format(item.approvedAmount ?: 0)}", style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(
                                value = item.realizationAmount?.toString() ?: "",
                                onValueChange = { viewModel.updateItemAmount(index, it.toLongOrNull() ?: 0) },
                                label = { Text("Realisasi (Rp)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(
                                value = item.note ?: "",
                                onValueChange = { viewModel.updateItemNote(index, it) },
                                label = { Text("Catatan") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (item.requiresBill) {
                                Text("⚠ Wajib upload bukti", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // Total
                val totalReal = uiState.items.sumOf { it.realizationAmount ?: 0 }
                val totalAppr = uiState.task?.totalApproved ?: 0
                Spacer(Modifier.height(12.dp))
                Text("Total Realisasi: Rp ${fmt.format(totalReal)}", style = MaterialTheme.typography.titleSmall)
                Text("Selisih: Rp ${fmt.format(totalAppr - totalReal)}")

                uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}
