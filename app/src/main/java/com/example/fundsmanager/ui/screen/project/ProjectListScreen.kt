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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.material3.rememberDatePickerState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    onDashboardClick: () -> Unit,
    onProjectClick: (Long) -> Unit,
    onTransactionsClick: () -> Unit,
    onSettingClick: () -> Unit,
    viewModel: ProjectListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<ProjectListItem?>(null) }
    var scheduleTarget by remember { mutableStateOf<ProjectListItem?>(null) }
    var archiveTarget by remember { mutableStateOf<ProjectListItem?>(null) }
    var deleteTarget by remember { mutableStateOf<ProjectListItem?>(null) }

    if (showAddDialog) {
        AddProjectDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, startDate, completedDate ->
                viewModel.addProject(name, startDate, completedDate)
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

    scheduleTarget?.let { item ->
        EditProjectScheduleDialog(
            initialStartDate = formatProjectDate(item.project.startAt.takeIf { it > 0L } ?: item.project.createdAt),
            initialCompletedDate = item.project.completedAt?.let(::formatProjectDate).orEmpty(),
            onDismiss = { scheduleTarget = null },
            onConfirm = { startDate, completedDate ->
                viewModel.updateProjectSchedule(item.project.id, startDate, completedDate.ifBlank { null })
                scheduleTarget = null
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

    deleteTarget?.let { item ->
        DeleteProjectDialog(
            projectName = item.project.name,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                viewModel.deleteProject(item.project.id)
                deleteTarget = null
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Funds Manager", fontWeight = FontWeight.ExtraBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            HomeBottomBar(
                selected = HomeMenu.Projects,
                onDashboardClick = onDashboardClick,
                onProjectClick = {},
                onTransactionClick = onTransactionsClick,
                onSettingClick = onSettingClick
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
                            onEditScheduleClick = { scheduleTarget = item },
                            onArchiveClick = { archiveTarget = item },
                            onRestoreClick = { viewModel.setProjectArchived(item.project.id, false) },
                            onDeleteClick = { deleteTarget = item }
                        )
                    }
                    item {
                        ArchiveToggleButton(
                            showArchived = uiState.showArchived,
                            onClick = { viewModel.onShowArchivedChange(!uiState.showArchived) }
                        )
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
    onEditScheduleClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onDeleteClick: () -> Unit
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
                Text(
                    "Mulai ${formatProjectDate(project.startAt.takeIf { it > 0 } ?: project.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                project.completedAt?.let { completedAt ->
                    Text(
                        "Selesai ${formatProjectDate(completedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(formatRupiah(netPosition), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = amountColor)
                Text("Posisi Bersih", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus project", tint = MaterialTheme.colorScheme.error)
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
                        DropdownMenuItem(
                            text = { Text("Edit Tanggal") },
                            onClick = {
                                menuExpanded = false
                                onEditScheduleClick()
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
}

private fun projectAccent(id: Long): Color = when ((id % 3).toInt()) {
    0 -> Color(0xFF2D5BBA)
    1 -> Color(0xFFF97316)
    else -> Color(0xFFDC2626)
}

private fun formatProjectDate(epochMillis: Long): String {
    return runCatching {
        Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
    }.getOrDefault("-")
}

@Composable
fun AddProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var completedDate by remember { mutableStateOf("") }
    var showStartPicker by remember { mutableStateOf(false) }
    var showCompletedPicker by remember { mutableStateOf(false) }
    val nameFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        nameFocusRequester.requestFocus()
    }

    if (showStartPicker) {
        ProjectDatePickerDialog(
            currentDate = startDate,
            onDismiss = { showStartPicker = false },
            onDateSelected = {
                startDate = it
                showStartPicker = false
            }
        )
    }

    if (showCompletedPicker) {
        ProjectDatePickerDialog(
            currentDate = completedDate.ifBlank { startDate },
            onDismiss = { showCompletedPicker = false },
            onDateSelected = {
                completedDate = it
                showCompletedPicker = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Project") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Project") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(nameFocusRequester),
                    singleLine = true
                )
                ProjectDialogDateField(
                    value = startDate,
                    label = "Project Start",
                    placeholder = "Pilih tanggal mulai",
                    onClick = { showStartPicker = true }
                )
                ProjectDialogDateField(
                    value = completedDate,
                    label = "Project Selesai (opsional)",
                    placeholder = "Biarkan kosong jika masih berjalan",
                    onClick = { showCompletedPicker = true },
                    onClear = { completedDate = "" }
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name, startDate, completedDate.ifBlank { null }) }) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectDatePickerDialog(
    currentDate: String,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val zone = ZoneId.systemDefault()
    val initialMillis = remember(currentDate) {
        runCatching {
            LocalDate.parse(currentDate, DateTimeFormatter.ISO_LOCAL_DATE)
                .atStartOfDay(zone)
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val selected = Instant.ofEpochMilli(millis)
                            .atZone(zone)
                            .toLocalDate()
                            .format(DateTimeFormatter.ISO_LOCAL_DATE)
                        onDateSelected(selected)
                    } else {
                        onDismiss()
                    }
                }
            ) { Text("Pilih") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun EditProjectScheduleDialog(
    initialStartDate: String,
    initialCompletedDate: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var startDate by remember(initialStartDate) { mutableStateOf(initialStartDate) }
    var completedDate by remember(initialCompletedDate) { mutableStateOf(initialCompletedDate) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showCompletedPicker by remember { mutableStateOf(false) }

    if (showStartPicker) {
        ProjectDatePickerDialog(
            currentDate = startDate,
            onDismiss = { showStartPicker = false },
            onDateSelected = {
                startDate = it
                showStartPicker = false
            }
        )
    }

    if (showCompletedPicker) {
        ProjectDatePickerDialog(
            currentDate = completedDate.ifBlank { startDate },
            onDismiss = { showCompletedPicker = false },
            onDateSelected = {
                completedDate = it
                showCompletedPicker = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Tanggal Project") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProjectDialogDateField(
                    value = startDate,
                    label = "Project Start",
                    placeholder = "Pilih tanggal mulai",
                    onClick = { showStartPicker = true }
                )
                ProjectDialogDateField(
                    value = completedDate,
                    label = "Project Selesai (opsional)",
                    placeholder = "Biarkan kosong jika masih berjalan",
                    onClick = { showCompletedPicker = true },
                    onClear = { completedDate = "" }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(startDate, completedDate) }) {
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
private fun ProjectDialogDateField(
    value: String,
    label: String,
    placeholder: String,
    onClick: () -> Unit,
    onClear: (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value.ifBlank { placeholder },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (onClear != null && value.isNotBlank()) {
                        IconButton(onClick = onClear) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Kosongkan tanggal")
                        }
                    }
                    Icon(Icons.Default.CalendarToday, contentDescription = "Pilih tanggal")
                }
            }
        }
    }
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

@Composable
private fun DeleteProjectDialog(
    projectName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hapus project?") },
        text = {
            Text("Project \"$projectName\" akan disembunyikan dari daftar dan tidak ikut perhitungan.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Hapus")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
