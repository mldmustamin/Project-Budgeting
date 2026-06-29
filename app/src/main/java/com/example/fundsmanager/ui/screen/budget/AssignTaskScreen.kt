package com.example.fundsmanager.ui.screen.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignTaskScreen(
    viewModel: AssignTaskViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currencyFormat = remember { NumberFormat.getNumberInstance(Locale("id", "ID")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assign Task") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Kembali")
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }
                    Button(
                        onClick = { viewModel.saveDraft() },
                        modifier = Modifier.weight(1f),
                        enabled = uiState.taskNo.isNotBlank()
                                && uiState.vid.isNotBlank()
                                && !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(if (uiState.savedTask != null) "Update" else "Simpan")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Data Task", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.taskNo,
                onValueChange = viewModel::updateTaskNo,
                label = { Text("Task No") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))

            OutlinedTextField(
                value = uiState.vid,
                onValueChange = viewModel::updateVid,
                label = { Text("VID") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))

            OutlinedTextField(
                value = uiState.taskName,
                onValueChange = viewModel::updateTaskName,
                label = { Text("Nama Task") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))

            // Job Type Dropdown
            var jobExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = jobExpanded,
                onExpandedChange = { jobExpanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.jobType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Jenis Pekerjaan") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(jobExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = jobExpanded,
                    onDismissRequest = { jobExpanded = false }
                ) {
                    listOf("INSTALASI", "RELOKASI", "PMCM", "DISMANTLE", "SURVEY").forEach { job ->
                        DropdownMenuItem(
                            text = { Text(job) },
                            onClick = {
                                viewModel.updateJobType(job)
                                jobExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))

            // Location Dropdown
            var locExpanded by remember { mutableStateOf(false) }
            val locName = uiState.locations.find { it.id == uiState.locationId }?.remoteName
                ?: "Pilih Lokasi"
            ExposedDropdownMenuBox(
                expanded = locExpanded,
                onExpandedChange = { locExpanded = it }
            ) {
                OutlinedTextField(
                    value = locName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Lokasi") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(locExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = locExpanded,
                    onDismissRequest = { locExpanded = false }
                ) {
                    uiState.locations.forEach { loc ->
                        DropdownMenuItem(
                            text = { Text(loc.remoteName) },
                            onClick = {
                                viewModel.updateLocation(loc.id)
                                locExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))

            // Field Engineer Dropdown (placeholder for future)
            var feExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = feExpanded,
                onExpandedChange = { feExpanded = it }
            ) {
                OutlinedTextField(
                    value = if (uiState.fieldEngineerId != null) "Engineer #${uiState.fieldEngineerId}" else "Pilih Field Engineer",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Field Engineer") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(feExpanded) },
                    enabled = false, // Future feature
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = feExpanded,
                    onDismissRequest = { feExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Belum tersedia") },
                        onClick = { feExpanded = false }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::updateNotes,
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            uiState.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            uiState.savedTask?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Task disimpan: #${it.taskNo}",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}
