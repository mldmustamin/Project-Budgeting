package com.example.fundsmanager.ui.screen.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    onBack: () -> Unit = {},
    viewModel: SummaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currencyFormat = remember { NumberFormat.getNumberInstance(Locale("id", "ID")) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Dashboard Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
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
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.error != null) {
                val error = uiState.error!!
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Surplus / Defisit Badge
                    item {
                        SurplusDefisitBadge(
                            surplusDefisit = uiState.surplusDefisit,
                            currencyFormat = currencyFormat
                        )
                    }

                    // 4 Stat Cards
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                title = "Dana Masuk",
                                amount = uiState.totalDanaMasuk,
                                currencyFormat = currencyFormat,
                                color = Color(0xFF18A13A),
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Kas Keluar",
                                amount = uiState.totalKasKeluar,
                                currencyFormat = currencyFormat,
                                color = Color(0xFFD32F2F),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                title = "Posisi Bersih",
                                amount = uiState.posisiBersih,
                                currencyFormat = currencyFormat,
                                color = if (uiState.posisiBersih >= 0) Color(0xFF18A13A) else Color(0xFFD32F2F),
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Pending",
                                amount = uiState.pendingAmount,
                                currencyFormat = currencyFormat,
                                color = Color(0xFFFFA000),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Office Real + Personal Totals
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF3FF)),
                            border = BorderStroke(1.dp, Color(0xFFDCEAFF))
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(11.dp)
                            ) {
                                Text(
                                    "Rincian Pengeluaran",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                HorizontalDivider(color = Color(0xFFC9DDF7))
                                DetailRow(
                                    label = "Office Real",
                                    amount = uiState.officeRealTotal,
                                    currencyFormat = currencyFormat
                                )
                                DetailRow(
                                    label = "Personal",
                                    amount = uiState.personalTotal,
                                    currencyFormat = currencyFormat
                                )
                                HorizontalDivider(color = Color(0xFFC9DDF7))
                                DetailRow(
                                    label = "Total Kas Keluar",
                                    amount = uiState.totalKasKeluar,
                                    currencyFormat = currencyFormat,
                                    emphasized = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SurplusDefisitBadge(
    surplusDefisit: Long,
    currencyFormat: NumberFormat
) {
    val isSurplus = surplusDefisit >= 0
    val badgeColor = if (isSurplus) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val textColor = if (isSurplus) Color(0xFF2E7D32) else Color(0xFFC62828)
    val label = if (isSurplus) "SURPLUS" else "DEFISIT"
    val icon = if (isSurplus) "▲" else "▼"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = badgeColor),
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    "Posisi keuangan saat ini",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "$icon Rp ${currencyFormat.format(kotlin.math.abs(surplusDefisit))}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = textColor
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    amount: Long,
    currencyFormat: NumberFormat,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE8EEF7))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Rp ${currencyFormat.format(amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    amount: Long,
    currencyFormat: NumberFormat,
    emphasized: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = if (emphasized) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodySmall,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Rp ${currencyFormat.format(amount)}",
            style = if (emphasized) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodySmall,
            fontWeight = if (emphasized) FontWeight.ExtraBold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
