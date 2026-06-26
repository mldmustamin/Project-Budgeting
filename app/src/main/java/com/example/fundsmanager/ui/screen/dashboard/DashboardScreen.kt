package com.example.fundsmanager.ui.screen.dashboard

import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.fundsmanager.domain.model.ProjectSummary
import com.example.fundsmanager.domain.model.Transaction
import com.example.fundsmanager.domain.model.isIncome
import com.example.fundsmanager.domain.model.toUiLabel
import com.example.fundsmanager.domain.service.ReportFile
import com.example.fundsmanager.ui.component.PrimaryButton
import com.example.fundsmanager.ui.component.TypeBadge
import com.example.fundsmanager.ui.component.formatRupiah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onBackClick: () -> Unit,
    onAddTransactionClick: (Long) -> Unit,
    onOpenTransactionList: (Long) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshDashboard()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState) {
        val state = uiState as? DashboardUiState.Success ?: return@LaunchedEffect
        state.exportMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearExportMessage()
        }
        state.exportError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearExportMessage()
        }
        state.pendingShareFile?.let { reportFile ->
            shareReportFile(context, reportFile, viewModel::logShareError)
            viewModel.onShareConsumed()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    val title = when (val state = uiState) {
                        is DashboardUiState.Success -> state.summary.projectName
                        else -> "Dashboard"
                    }
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    val successState = uiState as? DashboardUiState.Success
                    if (successState != null) {
                        val projectId = successState.summary.projectId
                        ExportMenu(
                            isExporting = successState.isExporting,
                            onExportPdf = viewModel::exportPdf,
                            onExportExcel = viewModel::exportExcel,
                            onSavePdf = viewModel::savePdf,
                            onSaveExcel = viewModel::saveExcel
                        )
                        IconButton(onClick = { onOpenTransactionList(projectId) }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Daftar transaksi")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is DashboardUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is DashboardUiState.Error -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    PrimaryButton(text = "Coba Lagi", onClick = { viewModel.refreshDashboard(force = true) })
                }
                is DashboardUiState.Success -> DashboardContent(
                    summary = state.summary,
                    transactions = state.recentItems,
                    onAddTransactionClick = onAddTransactionClick,
                    onOpenTransactionList = onOpenTransactionList
                )
            }
        }
    }
}

@Composable
private fun ExportMenu(
    isExporting: Boolean,
    onExportPdf: () -> Unit,
    onExportExcel: () -> Unit,
    onSavePdf: () -> Unit,
    onSaveExcel: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }, enabled = !isExporting) {
        if (isExporting) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Icon(Icons.Default.Share, contentDescription = "Export laporan")
        }
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("Bagikan PDF") },
            onClick = {
                expanded = false
                onExportPdf()
            }
        )
        DropdownMenuItem(
            text = { Text("Bagikan Excel") },
            onClick = {
                expanded = false
                onExportExcel()
            }
        )
        DropdownMenuItem(
            text = { Text("Simpan PDF") },
            onClick = {
                expanded = false
                onSavePdf()
            }
        )
        DropdownMenuItem(
            text = { Text("Simpan Excel") },
            onClick = {
                expanded = false
                onSaveExcel()
            }
        )
    }
}

private fun shareReportFile(context: android.content.Context, reportFile: ReportFile, onError: (Throwable, ReportFile) -> Unit) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            reportFile.file
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = reportFile.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(sendIntent, reportFile.chooserTitle))
    } catch (e: Exception) {
        onError(e, reportFile)
    }
}

@Composable
private fun DashboardContent(
    summary: ProjectSummary,
    transactions: List<Transaction>,
    onAddTransactionClick: (Long) -> Unit,
    onOpenTransactionList: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { SummaryWidget(summary) }
        item {
            PrimaryButton(
                text = "+  Tambah Transaksi",
                onClick = { onAddTransactionClick(summary.projectId) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Transaksi Terbaru", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Lihat Semua",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onOpenTransactionList(summary.projectId) }
                )
            }
        }
        items(transactions, key = { it.id }) { tx -> TransactionItem(tx) }
    }
}

@Composable
private fun SummaryWidget(summary: ProjectSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF3FF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Color(0xFFDCEAFF))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            SummaryRow("Total Dana Masuk", summary.totalFundIn, amountColor = MaterialTheme.colorScheme.primary, compact = true)
            SummaryRow("Posisi Bersih", summary.netPosition, emphasized = true, amountColor = Color(0xFF18A13A))
            SummaryRow("Dilaporkan ke Kantor", summary.totalOfficeReported, compact = true)
            SummaryRow("Pengeluaran Real", summary.totalOfficeReal, compact = true)
            SummaryRow("Expense Pribadi", summary.totalPersonalExpense, compact = true)
            SummaryRow("Total Keluar Real", summary.totalCashOut, compact = true)
            SummaryRow("Selisih / Hemat", summary.saving, amountColor = if (summary.saving >= 0) Color(0xFF18A13A) else MaterialTheme.colorScheme.error, compact = true)
            HorizontalDivider(color = Color(0xFFC9DDF7))
            SummaryRow("Sisa Berdasarkan Laporan", summary.remainingReported, amountColor = MaterialTheme.colorScheme.primary, compact = true)
            SummaryRow("Sisa Real", summary.remainingReal, amountColor = MaterialTheme.colorScheme.primary, compact = true)
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    amount: Long,
    emphasized: Boolean = false,
    amountColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    compact: Boolean = false
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = formatRupiah(amount),
            style = if (emphasized) MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = amountColor
        )
    }
}

@Composable
private fun TransactionItem(tx: Transaction) {
    val amountColor = if (tx.type.isIncome()) Color(0xFF18A13A) else MaterialTheme.colorScheme.error
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE8EEF7))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(if (tx.type.isIncome()) Color(0xFFE3FBEA) else Color(0xFFFFE8E8), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.BusinessCenter, contentDescription = null, tint = amountColor, modifier = Modifier.size(19.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(tx.description, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
                Text("${tx.date}  •  ${tx.type.toUiLabel()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TypeBadge(tx.type.toUiLabel())
            }
            Text(
                text = formatRupiah(tx.realAmount),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = amountColor
            )
        }
    }
}
