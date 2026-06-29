package com.example.fundsmanager.ui.screen.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fundsmanager.domain.model.BudgetTask
import com.example.fundsmanager.domain.model.ExpenseItemModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationScreen(
    viewModel: VerificationViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currencyFormat = remember { NumberFormat.getNumberInstance(Locale("id", "ID")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verifikasi") },
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
                Text("Tidak ada task menunggu verifikasi")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(uiState.tasks) { task ->
                    val isExpanded = uiState.expandedTaskUuid == task.uuid
                    VerificationTaskCard(
                        task = task,
                        currencyFormat = currencyFormat,
                        isExpanded = isExpanded,
                        items = if (isExpanded) uiState.expandedItems else emptyList(),
                        billVerifiedMap = if (isExpanded) uiState.billVerifiedMap else emptyMap(),
                        isSubmitting = uiState.isSubmitting && isExpanded,
                        onToggleExpand = { viewModel.toggleExpanded(task) },
                        onToggleBillVerified = { viewModel.toggleBillVerified(it) },
                        onVerify = { viewModel.verifyTask(task.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun VerificationTaskCard(
    task: BudgetTask,
    currencyFormat: NumberFormat,
    isExpanded: Boolean,
    items: List<ExpenseItemModel>,
    billVerifiedMap: Map<String, Boolean>,
    isSubmitting: Boolean,
    onToggleExpand: () -> Unit,
    onToggleBillVerified: (String) -> Unit,
    onVerify: () -> Unit
) {
    val selisih = task.totalRealization - task.totalApproved

    Card(
        onClick = onToggleExpand,
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
            Spacer(modifier = Modifier.height(4.dp))

            // Summary row: Approved | Realisasi | Selisih
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Approved", style = MaterialTheme.typography.labelSmall)
                    Text(
                        "Rp ${currencyFormat.format(task.totalApproved)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Realisasi", style = MaterialTheme.typography.labelSmall)
                    Text(
                        "Rp ${currencyFormat.format(task.totalRealization)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Selisih", style = MaterialTheme.typography.labelSmall)
                    Text(
                        "Rp ${currencyFormat.format(selisih)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selisih < 0) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Expanded: per-item comparison
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Perbandingan Item:", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                if (items.isEmpty()) {
                    Text("Memuat item...", style = MaterialTheme.typography.bodySmall)
                } else {
                    items.forEachIndexed { index, item ->
                        val itemSelisih = (item.realizationAmount ?: 0) - (item.approvedAmount ?: 0)
                        val isVerified = billVerifiedMap[item.uuid] ?: false

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Item #${index + 1} — ${item.tanggal}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    // Checkbox for bill_verified
                                    IconButton(onClick = { onToggleBillVerified(item.uuid) }) {
                                        Icon(
                                            imageVector = if (isVerified) Icons.Default.CheckCircle
                                            else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = if (isVerified) "Terverifikasi" else "Belum",
                                            tint = if (isVerified) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                item.note?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall)
                                }

                                // Approved vs Realization row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Approved: Rp ${currencyFormat.format(item.approvedAmount ?: 0)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "Realisasi: Rp ${currencyFormat.format(item.realizationAmount ?: 0)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Text(
                                    "Selisih: Rp ${currencyFormat.format(itemSelisih)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (itemSelisih < 0) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary
                                )

                                // Bill verification status
                                if (item.requiresBill) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        if (isVerified) "✓ Bukti terverifikasi"
                                        else "⚠ Wajib verifikasi bukti",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isVerified) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Verify button
                    Button(
                        onClick = onVerify,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSubmitting
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Verifikasi")
                    }
                }
            }
        }
    }
}
