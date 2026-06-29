package com.example.fundsmanager.ui.screen.budget

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
import com.example.fundsmanager.domain.model.BudgetTemplate
import com.example.fundsmanager.domain.model.MasterLocation
import com.example.fundsmanager.ui.component.AppDropdownField
import com.example.fundsmanager.ui.component.MoneyInputField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetEstimateFormScreen(
    viewModel: BudgetEstimateViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estimasi Budget") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Kembali")
                    }
                },
                actions = {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    }
                }
            )
        },
        bottomBar = {
            BottomBar(viewModel, uiState)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Task Info Section
            Text("Data Task", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.taskNo,
                onValueChange = viewModel::updateTaskNo,
                label = { Text("Task No") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.vid,
                onValueChange = viewModel::updateVid,
                label = { Text("VID") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Job Type Dropdown
            AppDropdownField(
                label = "Jenis Pekerjaan",
                value = uiState.jobType,
                options = listOf("INSTALASI", "RELOKASI", "PMCM", "DISMANTLE", "SURVEY"),
                onValueChange = viewModel::updateJobType
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Location Dropdown
            AppDropdownField(
                label = "Lokasi",
                value = uiState.locations.find { it.id == uiState.locationId }?.remoteName ?: "Pilih Lokasi",
                options = uiState.locations.map { it.remoteName },
                onValueChange = { name ->
                    val loc = uiState.locations.find { it.remoteName == name }
                    viewModel.updateLocation(loc?.id)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Pagu warnings
            if (uiState.paguWarnings.isNotEmpty()) {
                uiState.paguWarnings.forEach { warning ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(warning, modifier = Modifier.padding(8.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // Items Section
            Text("Item Budget", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            uiState.items.forEachIndexed { index, item ->
                ItemCard(
                    index = index,
                    item = item,
                    templates = uiState.templates,
                    onFieldChange = { field, value -> viewModel.updateItem(index, field, value) },
                    onRemove = { viewModel.removeItem(index) },
                    canRemove = uiState.items.size > 1
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Add Item Button
            OutlinedButton(
                onClick = viewModel::addItem,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tambah Item")
            }

            // Total
            Spacer(modifier = Modifier.height(16.dp))
            val total = uiState.items.sumOf { it.estimatedAmount.toLongOrNull() ?: 0 }
            Text(
                "Total Estimasi: Rp ${"%,d".format(total)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // Error
            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(error, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun ItemCard(
    index: Int,
    item: ExpenseItemDraft,
    templates: List<BudgetTemplate>,
    onFieldChange: (String, String) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Item #${index + 1}", style = MaterialTheme.typography.titleSmall)
                if (canRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Delete, "Hapus", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Template dropdown
            AppDropdownField(
                label = "Kategori",
                value = templates.find { it.id == item.templateId }?.categoryName ?: "Pilih Kategori",
                options = templates.map { "${it.categoryName} (${it.paguType})" },
                onValueChange = { selected ->
                    val tpl = templates.find { "${it.categoryName} (${it.paguType})" == selected }
                    onFieldChange("templateId", tpl?.id?.toString() ?: "")
                }
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Show pagu info if template selected
            item.templateId?.let { tid ->
                templates.find { it.id == tid }?.let { tpl ->
                    tpl.paguAmount?.let { pagu ->
                        Text("Pagu: Rp ${"%,d".format(pagu)} (${tpl.paguType})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                    if (tpl.requiresBill) {
                        Text("⚠ Wajib bukti fisik",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Amount
            MoneyInputField(
                value = item.estimatedAmount,
                onValueChange = { onFieldChange("estimatedAmount", it) },
                label = "Estimasi (Rp)",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = item.tanggal,
                onValueChange = { onFieldChange("tanggal", it) },
                label = { Text("Tanggal (yyyy-MM-dd)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun BottomBar(viewModel: BudgetEstimateViewModel, uiState: BudgetEstimateUiState) {
    Surface(
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = viewModel::saveDraft,
                enabled = !uiState.isSaving && uiState.isDirty,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (uiState.savedTask != null) "Update" else "Simpan Draft")
            }
            Button(
                onClick = {
                    viewModel.saveDraft()
                    // TODO: navigate to submit confirmation
                },
                enabled = !uiState.isSaving && uiState.taskNo.isNotBlank() && uiState.vid.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Submit")
            }
        }
    }
}
