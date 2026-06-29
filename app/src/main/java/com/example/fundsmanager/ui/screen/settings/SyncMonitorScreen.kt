package com.example.fundsmanager.ui.screen.settings

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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncMonitorScreen(
    onBack: () -> Unit = {},
    viewModel: SyncMonitorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Sync Monitor",
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
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = uiState.error,
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
                    // Sync Status Card
                    item {
                        SyncStatusCard(
                            syncStatus = uiState.syncStatus,
                            lastSyncTime = uiState.lastSyncTime,
                            pendingCount = uiState.pendingCount,
                            rejectedCount = uiState.rejectedCount
                        )
                    }

                    // Sync Now Button
                    item {
                        Button(
                            onClick = { viewModel.triggerSync() },
                            enabled = !uiState.isSyncing,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (uiState.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.size(8.dp))
                                Text("Syncing...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(8.dp))
                                Text("Sync Now", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Sync Message
                    uiState.syncMessage?.let { message ->
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                border = BorderStroke(1.dp, Color(0xFFC8E6C9))
                            ) {
                                Text(
                                    text = message,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }

                    // Info Rows
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
                                    "Detail Sinkronisasi",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                HorizontalDivider(color = Color(0xFFC9DDF7))
                                InfoRow(
                                    label = "Status",
                                    value = statusLabel(uiState.syncStatus)
                                )
                                InfoRow(
                                    label = "Terakhir Sinkron",
                                    value = uiState.lastSyncTime?.let { formatTimestamp(it) } ?: "-"
                                )
                                InfoRow(
                                    label = "Pending",
                                    value = "${uiState.pendingCount} item"
                                )
                                InfoRow(
                                    label = "Ditolak",
                                    value = "${uiState.rejectedCount} item"
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
private fun SyncStatusCard(
    syncStatus: String,
    lastSyncTime: Long?,
    pendingCount: Int,
    rejectedCount: Int
) {
    val (icon, iconColor, bgColor) = when (syncStatus) {
        "UP_TO_DATE" -> Triple(Icons.Default.Cloud, Color(0xFF2E7D32), Color(0xFFE8F5E9))
        "HAS_PENDING" -> Triple(Icons.Default.CloudSync, Color(0xFFFFA000), Color(0xFFFFF8E1))
        "HAS_REJECTED" -> Triple(Icons.Default.CloudOff, Color(0xFFC62828), Color(0xFFFFEBEE))
        else -> Triple(Icons.Default.CloudSync, Color(0xFF757575), Color(0xFFF5F5F5))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, iconColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(iconColor.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    statusLabel(syncStatus),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1A1A1A)
                )
                Text(
                    lastSyncTime?.let { "Terakhir: ${formatTimestamp(it)}" } ?: "Belum pernah sinkron",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (pendingCount > 0 || rejectedCount > 0) {
                    Text(
                        "$pendingCount pending, $rejectedCount ditolak",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = iconColor
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun statusLabel(status: String): String = when (status) {
    "UP_TO_DATE" -> "Up to Date"
    "HAS_PENDING" -> "Ada Pending"
    "HAS_REJECTED" -> "Ada Ditolak"
    "IDLE" -> "Idle"
    else -> status
}

private fun formatTimestamp(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
        sdf.format(Date(timestamp))
    } catch (_: Exception) {
        "-"
    }
}
