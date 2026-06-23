package com.example.fundsmanager.ui.screen.project

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fundsmanager.ui.screen.home.HomeBottomBar
import com.example.fundsmanager.ui.screen.home.HomeMenu
import com.example.fundsmanager.ui.component.EmptyState
import com.example.fundsmanager.ui.component.PrimaryButton
import com.example.fundsmanager.ui.component.formatRupiah

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun ProjectListScreen(
    onProjectClick: (Long) -> Unit,
    onTransactionsClick: () -> Unit,
    onReportsClick: () -> Unit,
    onImportClick: () -> Unit,
    viewModel: ProjectListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<ProjectListItem?>(null) }
    var archiveTarget by remember { mutableStateOf<ProjectListItem?>(null) }

    if (showAddDialog) {
        AddProjectDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                viewModel.addProject(name)
                showAddDialog = false
            }
        )
    }

    renameTarget?.let { item ->
        RenameProjectDialog(
            initialName = item.project.name,
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                viewModel.renameProject(item.project.id, name)
                renameTarget = null
            }
        )
    }

    archiveTarget?.let { item ->
        ArchiveProjectDialog(
            onDismiss = { archiveTarget = null },
            onConfirm = {
                viewModel.setProjectArchived(item.project.id, true)
                archiveTarget = null
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Funds Manager", fontWeight = FontWeight.ExtraBold) },
                actions = {
                    IconButton(onClick = onImportClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Pengaturan")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            HomeBottomBar(
                selected = HomeMenu.Projects,
                onProjectClick = {},
                onTransactionClick = onTransactionsClick,
                onReportClick = onReportsClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (uiState.showArchived) "Project Arsip" else "Project Aktif",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                    Text("Project", fontWeight = FontWeight.Bold)
                }
            }

            uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            if (uiState.projects.isEmpty()) {
                if (uiState.showArchived) {
                    EmptyState(
                        title = "Belum ada project arsip",
                        body = "Project yang diarsipkan akan muncul di sini."
                    )
                } else {
                    EmptyProjectState(onCreateClick = { showAddDialog = true })
                }
                ArchiveToggleButton(
                    showArchived = uiState.showArchived,
                    onClick = { viewModel.onShowArchivedChange(!uiState.showArchived) }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 18.dp)
                ) {
                    items(uiState.projects, key = { it.project.id }) { item ->
                        ProjectCard(
                            item = item,
                            onClick = { onProjectClick(item.project.id) },
                            onRenameClick = { renameTarget = item },
                            onArchiveClick = { archiveTarget = item },
                            onRestoreClick = { viewModel.setProjectArchived(item.project.id, false) }
                        )
                    }
                    item {
                        ArchiveToggleButton(
                            showArchived = uiState.showArchived,
                            onClick = { viewModel.onShowArchivedChange(!uiState.showArchived) }
                        )
                    }
                    item {
                        Text("Import & Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 18.dp))
                    }
                    item {
                        ImportBackupCard(onClick = onImportClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveToggleButton(
    showArchived: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(11.dp),
        border = BorderStroke(1.dp, Color(0xFF9DB7E8)),
        contentPadding = PaddingValues(vertical = 11.dp)
    ) {
        Icon(Icons.Default.BusinessCenter, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.size(8.dp))
        Text(if (showArchived) "Lihat Project Aktif" else "Lihat Project Arsip", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyProjectState(onCreateClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(
            title = "Belum ada project",
            body = "Kelola dana proyek kamu dari satu tempat.",
            action = {
                PrimaryButton(
                    text = "Buat Project",
                    onClick = onCreateClick,
                    modifier = Modifier.fillMaxWidth(0.62f)
                )
            }
        )
    }
}

@Composable
private fun ProjectCard(
    item: ProjectListItem,
    onClick: () -> Unit,
    onRenameClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onRestoreClick: () -> Unit
) {
    val project = item.project
    val netPosition = item.summary?.netPosition ?: 0L
    val totalFundIn = item.summary?.totalFundIn ?: 0L
    val amountColor = if (netPosition < 0) MaterialTheme.colorScheme.error else Color(0xFF18A13A)
    val iconTint = projectAccent(project.id)
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE8EEF7))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(iconTint.copy(alpha = 0.13f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.BusinessCenter, contentDescription = null, tint = iconTint)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(project.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                Text("${item.transactionCount} transaksi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Dana Masuk ${formatRupiah(totalFundIn)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(formatRupiah(netPosition), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = amountColor)
                Text("Posisi Bersih", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Menu project", tint = Color(0xFF64748B))
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Ganti Nama") },
                        onClick = {
                            menuExpanded = false
                            onRenameClick()
                        }
                    )
                    if (project.isArchived) {
                        DropdownMenuItem(
                            text = { Text("Pulihkan") },
                            onClick = {
                                menuExpanded = false
                                onRestoreClick()
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Arsipkan") },
                            onClick = {
                                menuExpanded = false
                                onArchiveClick()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportBackupCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE8EEF7))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ImportExport, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(10.dp))
            Text("Import dari tracker-duit", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF64748B))
        }
    }
}

private fun projectAccent(id: Long): Color = when ((id % 3).toInt()) {
    0 -> Color(0xFF2D5BBA)
    1 -> Color(0xFFF97316)
    else -> Color(0xFFDC2626)
}

@Composable
fun AddProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Project") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Project") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name) }) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun RenameProjectDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ganti Nama Project") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Project") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name) }) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun ArchiveProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Arsipkan project?") },
        text = { Text("Project akan dipindahkan ke Project Arsip. Data transaksi tetap aman.") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Arsipkan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
