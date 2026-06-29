package com.example.fundsmanager.ui.screen.settings

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.fundsmanager.util.CrashReporter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrashLogScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var crashFiles by remember { mutableStateOf(CrashReporter.getCrashFiles()) }
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crash Reports") },
                navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, "Kembali") } },
                actions = {
                    if (crashFiles.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, "Hapus Semua")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (crashFiles.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("Tidak ada crash report", style = MaterialTheme.typography.titleMedium)
                }
            }
        } else {
            LazyColumn(Modifier.padding(padding)) {
                item {
                    Card(
                        Modifier.fillMaxWidth().padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            "${crashFiles.size} crash report tersimpan. Tekan untuk lihat detail atau bagikan.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                items(crashFiles) { file ->
                    CrashFileCard(file, context)
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Hapus Semua Crash Report?") },
            confirmButton = {
                TextButton(onClick = {
                    CrashReporter.clearCrashes()
                    crashFiles = emptyList()
                    showClearDialog = false
                }) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Batal") } }
        )
    }
}

@Composable
fun CrashFileCard(file: File, context: android.content.Context) {
    var expanded by remember { mutableStateOf(false) }
    val content = remember { file.readText() }
    val firstLine = content.lines().firstOrNull { it.isNotBlank() } ?: "Unknown crash"
    val timestamp = file.name.removePrefix("crash_").removeSuffix(".txt").toLongOrNull() ?: 0L
    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

    Card(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(dateStr, fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = { shareCrash(context, file) }) {
                        Icon(Icons.Default.Share, "Bagikan", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { CrashReporter.deleteCrash(file) }) {
                        Icon(Icons.Default.Delete, "Hapus", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Text(firstLine.take(100), style = MaterialTheme.typography.bodySmall, maxLines = 2)

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
                    Text(
                        content.take(3000),
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

private fun shareCrash(context: android.content.Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "FundManager Crash Report")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan Crash Report"))
    } catch (_: Exception) {
        // Direct share via text
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, file.readText())
            putExtra(Intent.EXTRA_SUBJECT, "FundManager Crash Report")
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan Crash Report"))
    }
}
