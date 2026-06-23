package com.example.fundsmanager.ui.screen.home

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.fundsmanager.domain.model.TransactionType
import com.example.fundsmanager.domain.model.isIncome
import com.example.fundsmanager.domain.model.toUiLabel
import com.example.fundsmanager.ui.component.EmptyState
import com.example.fundsmanager.ui.component.PrimaryButton
import com.example.fundsmanager.ui.component.ProofSourceDialog
import com.example.fundsmanager.ui.component.DeleteConfirmationDialog
import com.example.fundsmanager.ui.component.ReceiptBadge
import com.example.fundsmanager.ui.component.TypeBadge
import com.example.fundsmanager.ui.component.formatRupiah
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalTransactionScreen(
    onDashboardClick: () -> Unit,
    onProjectMenuClick: () -> Unit,
    onSettingClick: () -> Unit,
    onAddTransactionClick: (Long) -> Unit,
    onEditTransaction: (Long, Long) -> Unit,
    viewModel: GlobalTransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var pendingProofTransactionId by remember { mutableStateOf<Long?>(null) }
    var pendingDeleteTransactionId by remember { mutableStateOf<Long?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val transactionId = pendingProofTransactionId
        if (uri != null && transactionId != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "bukti-transaksi"
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                viewModel.saveProof(transactionId, ByteArrayInputStream(bytes), fileName)
            }
        }
        pendingProofTransactionId = null
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        val transactionId = pendingProofTransactionId
        if (bitmap != null && transactionId != null) {
            val output = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
            viewModel.saveProof(
                transactionId = transactionId,
                inputStream = ByteArrayInputStream(output.toByteArray()),
                fileName = "bukti-${System.currentTimeMillis()}.jpg"
            )
        }
        pendingProofTransactionId = null
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    pendingDeleteTransactionId?.let { transactionId ->
        DeleteConfirmationDialog(
            onDismiss = { pendingDeleteTransactionId = null },
            onConfirm = {
                viewModel.deleteTransaction(transactionId)
                pendingDeleteTransactionId = null
            }
        )
    }

    pendingProofTransactionId?.let {
        ProofSourceDialog(
            onDismiss = { pendingProofTransactionId = null },
            onSelectFile = { fileLauncher.launch("*/*") },
            onOpenCamera = { cameraLauncher.launch(null) }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Transaksi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold) },
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Default.FilterList, contentDescription = "Filter") }
                    IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, contentDescription = "Menu") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            HomeBottomBar(
                selected = HomeMenu.Transactions,
                onDashboardClick = onDashboardClick,
                onProjectClick = onProjectMenuClick,
                onTransactionClick = {},
                onSettingClick = onSettingClick
            )
        },
        floatingActionButton = {
            val firstProjectId = uiState.projects.firstOrNull()?.id
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        if (firstProjectId == null) Color(0xFF94A3B8) else MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(18.dp)
                    )
                    .clickable(enabled = firstProjectId != null) { firstProjectId?.let(onAddTransactionClick) },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Add, contentDescription = "Tambah Transaksi", tint = Color.White) }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                placeholder = { Text("Cari keterangan, project, atau tanggal") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 20.dp)
            ) {
                item {
                    CompactChip(uiState.selectedType == null, "Semua") { viewModel.onTypeSelected(null) }
                }
                items(TransactionType.entries) { type ->
                    CompactChip(uiState.selectedType == type, type.toUiLabel()) { viewModel.onTypeSelected(type) }
                }
            }

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                uiState.items.isEmpty() -> GlobalTransactionEmpty(
                    hasProject = uiState.projects.isNotEmpty(),
                    onAddClick = { uiState.projects.firstOrNull()?.id?.let(onAddTransactionClick) }
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.items, key = { it.transaction.id }) { item ->
                        GlobalTransactionCard(
                            item = item,
                            onClick = { onEditTransaction(item.transaction.projectId, item.transaction.id) },
                            onProofClick = { pendingProofTransactionId = item.transaction.id },
                            onDeleteClick = { pendingDeleteTransactionId = item.transaction.id }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlobalTransactionEmpty(hasProject: Boolean, onAddClick: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(
            title = "Belum ada transaksi",
            body = if (hasProject) "Catat transaksi pertama dari menu ini." else "Buat project terlebih dahulu sebelum mencatat transaksi.",
            action = if (hasProject) {
                { PrimaryButton(text = "+  Tambah Transaksi", onClick = onAddClick, modifier = Modifier.fillMaxWidth(0.86f)) }
            } else null
        )
    }
}

@Composable
private fun CompactChip(selected: Boolean, label: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
        shape = RoundedCornerShape(7.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun GlobalTransactionCard(
    item: GlobalTransactionItem,
    onClick: () -> Unit,
    onProofClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val transaction = item.transaction
    val amountColor = if (transaction.type.isIncome()) Color(0xFF18A13A) else MaterialTheme.colorScheme.error
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE8EEF7))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(transaction.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatRupiah(transaction.realAmount), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = amountColor)
            }
            Text(transaction.description, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
            Text("${item.projectName}  •  ${item.accountName.ifBlank { "Akun belum dipilih" }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TypeBadge(transaction.type.toUiLabel())
                Box(modifier = Modifier.clickable(onClick = onProofClick)) {
                    ReceiptBadge(item.hasReceipt)
                }
                if (!item.hasReceipt) {
                    Text(
                        "Laporkan bukti",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.clickable(onClick = onProofClick)
                    )
                }
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus transaksi", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

enum class HomeMenu { Dashboard, Projects, Transactions, Setting }

@Composable
fun HomeBottomBar(
    selected: HomeMenu,
    onDashboardClick: () -> Unit,
    onProjectClick: () -> Unit,
    onTransactionClick: () -> Unit,
    onSettingClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HomeBottomItem(Icons.Default.BusinessCenter, "Dashboard", selected == HomeMenu.Dashboard, onDashboardClick)
        HomeBottomItem(Icons.Default.Folder, "Project", selected == HomeMenu.Projects, onProjectClick)
        HomeBottomItem(Icons.AutoMirrored.Filled.ReceiptLong, "Transaksi", selected == HomeMenu.Transactions, onTransactionClick)
        HomeBottomItem(Icons.Default.Settings, "Setting", selected == HomeMenu.Setting, onSettingClick)
    }
}

@Composable
private fun HomeBottomItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF64748B)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 2.dp)
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp), tint = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}
