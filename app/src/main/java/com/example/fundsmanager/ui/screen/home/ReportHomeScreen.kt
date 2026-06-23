package com.example.fundsmanager.ui.screen.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fundsmanager.domain.model.ProjectSummary
import com.example.fundsmanager.ui.component.EmptyState
import com.example.fundsmanager.ui.component.formatRupiah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportHomeScreen(
    onProjectMenuClick: () -> Unit,
    onTransactionMenuClick: () -> Unit,
    onProjectClick: (Long) -> Unit,
    viewModel: ReportHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Laporan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold) },
                actions = { IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, contentDescription = "Menu") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            HomeBottomBar(
                selected = HomeMenu.Reports,
                onProjectClick = onProjectMenuClick,
                onTransactionClick = onTransactionMenuClick,
                onReportClick = {}
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            uiState.summaries.isEmpty() -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "Belum ada laporan",
                    body = "Buat project dan transaksi untuk melihat laporan."
                )
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                uiState.total?.let { total -> item { TotalReportCard(total) } }
                item { Text("Ringkasan Project", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold) }
                items(uiState.summaries, key = { it.projectId }) { summary ->
                    ProjectReportCard(summary = summary, onClick = { onProjectClick(summary.projectId) })
                }
            }
        }
    }
}

@Composable
private fun TotalReportCard(summary: ProjectSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF3FF)),
        border = BorderStroke(1.dp, Color(0xFFDCEAFF))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text("Total Semua Project", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            ReportRow("Dana Masuk", summary.totalFundIn, MaterialTheme.colorScheme.primary)
            ReportRow("Posisi Bersih", summary.netPosition, Color(0xFF18A13A), emphasized = true)
            HorizontalDivider(color = Color(0xFFC9DDF7))
            ReportRow("Total Keluar Real", summary.totalCashOut, MaterialTheme.colorScheme.error)
            ReportRow("Selisih / Hemat", summary.saving, if (summary.saving >= 0) Color(0xFF18A13A) else MaterialTheme.colorScheme.error)
            ReportRow("Sisa Real", summary.remainingReal, MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ProjectReportCard(summary: ProjectSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE8EEF7))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(summary.projectName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
            ReportRow("Dana Masuk", summary.totalFundIn, MaterialTheme.colorScheme.primary)
            ReportRow("Expense Kantor", summary.totalOfficeReal, MaterialTheme.colorScheme.error)
            ReportRow("Expense Pribadi", summary.totalPersonalExpense, MaterialTheme.colorScheme.error)
            ReportRow("Posisi Bersih", summary.netPosition, if (summary.netPosition >= 0) Color(0xFF18A13A) else MaterialTheme.colorScheme.error, emphasized = true)
        }
    }
}

@Composable
private fun ReportRow(label: String, amount: Long, color: Color, emphasized: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            formatRupiah(amount),
            style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
    }
}
