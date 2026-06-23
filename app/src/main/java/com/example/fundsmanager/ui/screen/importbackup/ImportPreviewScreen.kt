package com.example.fundsmanager.ui.screen.importbackup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fundsmanager.domain.model.ImportItemStatus
import com.example.fundsmanager.domain.model.ImportPreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPreviewScreen(
    onBackClick: () -> Unit,
    viewModel: ImportPreviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
            if (content != null) {
                viewModel.onFileSelected(content)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import tracker-duit") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val state = uiState) {
                is ImportPreviewUiState.Idle -> {
                    Button(
                        onClick = { filePicker.launch("application/json") },
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Text("Select JSON File")
                    }
                }
                is ImportPreviewUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ImportPreviewUiState.PreviewLoaded -> {
                    PreviewContent(
                        preview = state.preview,
                        onConfirm = { viewModel.confirmImport() }
                    )
                }
                is ImportPreviewUiState.Success -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Import Successful!")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBackClick) { Text("Go Back") }
                    }
                }
                is ImportPreviewUiState.Error -> {
                    Text(state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
fun PreviewContent(
    preview: ImportPreview,
    onConfirm: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Import Summary", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                StatRow("Projects Found", preview.projectCount)
                StatRow("Fund In", preview.fundInCount)
                StatRow("Office Expenses", preview.officeExpenseCount)
                StatRow("Personal Expenses", preview.personalExpenseCount)
                Divider()
                StatRow("Valid to Import", preview.validCount)
                StatRow("Duplicates (Skipped)", preview.duplicateCount)
                StatRow("Invalid (Skipped)", preview.invalidCount)
            }
        }

        if (preview.errors.isNotEmpty()) {
            Text("Critical Errors:", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
            preview.errors.forEach { err ->
                Text("- $err", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            enabled = preview.validCount > 0
        ) {
            Text("Confirm Import (${preview.validCount} items)")
        }
    }
}

@Composable
fun StatRow(label: String, value: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value.toString(), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
    }
}
