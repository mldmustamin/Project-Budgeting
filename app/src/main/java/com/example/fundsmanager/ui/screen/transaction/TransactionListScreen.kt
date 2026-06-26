package com.example.fundsmanager.ui.screen.transaction

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.fundsmanager.domain.model.TransactionType
import com.example.fundsmanager.domain.model.isIncome
import com.example.fundsmanager.domain.model.toUiLabel
import com.example.fundsmanager.ui.component.DeleteConfirmationDialog
import com.example.fundsmanager.ui.component.EmptyState
import com.example.fundsmanager.ui.component.PrimaryButton
import com.example.fundsmanager.ui.component.ReceiptBadge
import com.example.fundsmanager.ui.component.TypeBadge
import com.example.fundsmanager.ui.component.formatRupiah
import com.example.fundsmanager.ui.screen.home.HomeBottomBar
import com.example.fundsmanager.ui.screen.home.HomeMenu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    onBackClick: () -> Unit,
    onDashboardClick: () -> Unit,
    onProjectClick: (Long) -> Unit,
    onTransactionMenuClick: () -> Unit,
    onSettingClick: () -> Unit,
    onAddTransactionClick: (Long) -> Unit,
    onEditTransaction: (Long, Long) -> Unit,
    viewModel: TransactionListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDeleteId by remember { mutableStateOf<Long?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadItems()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    pendingDeleteId?.let { transactionId ->
        DeleteConfirmationDialog(
            onDismiss = { pendingDeleteId = null },
            onConfirm = {
                viewModel.softDeleteTransaction(transactionId)
                pendingDeleteId = null
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Transaksi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
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
                onProjectClick = {
                    if (uiState.projectId > 0L) {
                        onProjectClick(uiState.projectId)
                    }
                },
                onTransactionClick = onTransactionMenuClick,
                onSettingClick = onSettingClick
            )
        },
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp))
                    .clickable { onAddTransactionClick(uiState.projectId) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Transaksi", tint = MaterialTheme.colorScheme.onPrimary)
            }
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
                placeholder = { Text("Cari keterangan, catatan, atau tanggal") },
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
                    CompactChip(
                        selected = uiState.selectedType == null,
                        label = "Semua",
                        onClick = { viewModel.onTypeSelected(null) }
                    )
                }
                items(TransactionType.entries) { type ->
                    CompactChip(
                        selected = uiState.selectedType == type,
                        label = type.toUiLabel(),
                        onClick = { viewModel.onTypeSelected(type) }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Switch(
                    checked = uiState.onlyMissingReceipt,
                    onCheckedChange = viewModel::onOnlyMissingReceiptChange,
                    modifier = Modifier.size(width = 42.dp, height = 28.dp)
                )
                Text("Belum ada bukti", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            when {
                uiState.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                uiState.items.isEmpty() -> EmptyTransactionState(onAddClick = { onAddTransactionClick(uiState.projectId) })
                else -> LazyColumn(
                    state = rememberLazyListState(),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.items, key = { it.transaction.id }) { item ->
                        TransactionCard(
                            item = item,
                            onClick = { onEditTransaction(item.transaction.projectId, item.transaction.id) },
                            onDeleteClick = { pendingDeleteId = item.transaction.id }
                        )
                    }
                }
            }
        }
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
private fun EmptyTransactionState(onAddClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(
            title = "Belum ada transaksi",
            body = "Yuk, catat transaksi pertama kamu pada project ini.",
            action = {
                PrimaryButton(
                    text = "+  Tambah Transaksi",
                    onClick = onAddClick,
                    modifier = Modifier.fillMaxWidth(0.86f)
                )
            }
        )
    }
}

@Composable
private fun TransactionCard(
    item: TransactionListItem,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val transaction = item.transaction
    val amountColor = if (transaction.type.isIncome()) Color(0xFF18A13A) else MaterialTheme.colorScheme.error
    val metadata = listOfNotNull(transaction.type.toUiLabel(), item.accountName.ifBlank { null }).joinToString("  •  ")

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
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(transaction.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Text(transaction.description, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
                    Text(formatRupiah(transaction.realAmount), fontWeight = FontWeight.ExtraBold, color = amountColor, style = MaterialTheme.typography.labelMedium)
                }
                Text(metadata, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TypeBadge(transaction.type.toUiLabel())
                    item.categoryName?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                ReceiptBadge(item.hasReceipt)
            }
            IconButton(onClick = onDeleteClick, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = "Hapus", tint = Color(0xFF475569))
            }
        }
    }
}
