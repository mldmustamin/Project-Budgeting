package com.example.fundsmanager.ui.screen.budget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
fun BudgetEstimateFormScreen(
    viewModel: BudgetEstimateViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currencyFormat = remember { NumberFormat.getNumberInstance(Locale("id", "ID")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estimasi Budget") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Kembali") }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = viewModel::saveDraft, modifier = Modifier.weight(1f)) {
                        Text(if (uiState.savedTask != null) "Update" else "Simpan")
                    }
                    Button(
                        onClick = { viewModel.saveDraft() },
                        modifier = Modifier.weight(1f),
                        enabled = uiState.taskNo.isNotBlank() && uiState.vid.isNotBlank()
                    ) { Text("Submit") }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            Text("Data Task", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(uiState.taskNo, viewModel::updateTaskNo, label = { Text("Task No") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(uiState.vid, viewModel::updateVid, label = { Text("VID") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))

            // Job Type
            var jobExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(jobExpanded, { jobExpanded = it }) {
                OutlinedTextField(
                    uiState.jobType, {}, readOnly = true, label = { Text("Jenis Pekerjaan") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(jobExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(jobExpanded, { jobExpanded = false }) {
                    listOf("INSTALASI", "RELOKASI", "PMCM", "DISMANTLE", "SURVEY").forEach { job ->
                        DropdownMenuItem(text = { Text(job) }, onClick = { viewModel.updateJobType(job); jobExpanded = false })
                    }
                }
            }
            Spacer(Modifier.height(6.dp))

            // Location
            var locExpanded by remember { mutableStateOf(false) }
            val locName = uiState.locations.find { it.id == uiState.locationId }?.remoteName ?: "Pilih Lokasi"
            ExposedDropdownMenuBox(locExpanded, { locExpanded = it }) {
                OutlinedTextField(
                    locName, {}, readOnly = true, label = { Text("Lokasi") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(locExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(locExpanded, { locExpanded = false }) {
                    uiState.locations.forEach { loc ->
                        DropdownMenuItem(text = { Text(loc.remoteName) }, onClick = { viewModel.updateLocation(loc.id); locExpanded = false })
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Items
            Text("Item Budget (${uiState.items.size})", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            uiState.items.forEachIndexed { index, item ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Item #${index + 1}", style = MaterialTheme.typography.titleSmall)

                        // Template dropdown
                        var tplExpanded by remember { mutableStateOf(false) }
                        val tplName = uiState.templates.find { it.id == item.templateId }?.categoryName ?: "Pilih Kategori"
                        ExposedDropdownMenuBox(tplExpanded, { tplExpanded = it }) {
                            OutlinedTextField(tplName, {}, readOnly = true, label = { Text("Kategori") },
                                modifier = Modifier.fillMaxWidth().menuAnchor())
                            ExposedDropdownMenu(tplExpanded, { tplExpanded = false }) {
                                uiState.templates.forEach { tpl ->
                                    DropdownMenuItem(text = {
                                        Text("${tpl.categoryName} (${tpl.paguType})")
                                    }, onClick = {
                                        viewModel.updateItem(index, "templateId", tpl.id.toString())
                                        tplExpanded = false
                                    })
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))

                        // Pagu info
                        item.templateId?.let { tid ->
                            uiState.templates.find { it.id == tid }?.let { tpl ->
                                if (tpl.paguAmount != null) {
                                    Text("Pagu: Rp ${currencyFormat.format(tpl.paguAmount)}", style = MaterialTheme.typography.labelSmall)
                                }
                                if (tpl.requiresBill) {
                                    Text("⚠ Wajib bukti fisik", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        OutlinedTextField(
                            item.estimatedAmount, { viewModel.updateItem(index, "estimatedAmount", it) },
                            label = { Text("Estimasi (Rp)") }, modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            item.tanggal, { viewModel.updateItem(index, "tanggal", it) },
                            label = { Text("Tanggal (yyyy-MM-dd)") }, modifier = Modifier.fillMaxWidth()
                        )

                        if (uiState.items.size > 1) {
                            TextButton(onClick = { viewModel.removeItem(index) }) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                Text("Hapus", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            OutlinedButton(onClick = viewModel::addItem, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null); Text("Tambah Item")
            }

            // Total
            val total = uiState.items.sumOf { it.estimatedAmount.toLongOrNull() ?: 0 }
            Text("Total: Rp ${currencyFormat.format(total)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(80.dp))
        }
    }
}
