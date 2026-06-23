package com.example.fundsmanager.ui.screen.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.fundsmanager.domain.model.isIncome
import com.example.fundsmanager.domain.model.toUiLabel
import com.example.fundsmanager.ui.component.EmptyState
import com.example.fundsmanager.ui.component.PrimaryButton
import com.example.fundsmanager.ui.component.TypeBadge
import com.example.fundsmanager.ui.component.formatRupiah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardHomeScreen(
    onDashboardClick: () -> Unit,
    onProjectMenuClick: () -> Unit,
    onTransactionMenuClick: () -> Unit,
    onSettingClick: () -> Unit,
    onProjectClick: (Long) -> Unit,
    onOpenProjectList: () -> Unit,
    onOpenTransactionList: () -> Unit,
    onManageCategoriesClick: () -> Unit,
    viewModel: DashboardHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onDashboardClick) {
                        Icon(Icons.Default.BusinessCenter, contentDescription = "Menu")
                    }
                },
                title = {
                    Text(
                        "Dashboard",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                actions = {
                    IconButton(onClick = viewModel::refreshDashboard) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh dashboard")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            HomeBottomBar(
                selected = HomeMenu.Dashboard,
                onDashboardClick = onDashboardClick,
                onProjectClick = onProjectMenuClick,
                onTransactionClick = onTransactionMenuClick,
                onSettingClick = onSettingClick
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            uiState.error != null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    title = "Dashboard belum siap",
                    body = uiState.error,
                    action = {
                        PrimaryButton(text = "Coba Lagi", onClick = viewModel::refreshDashboard)
                    }
                )
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    HeroSummaryCard(
                        activeProjectCount = uiState.activeProjectCount,
                        archivedProjectCount = uiState.archivedProjectCount,
                        activeDifference = uiState.activeDifference,
                        overallDifference = uiState.overallDifference,
                        ongoingBalance = uiState.ongoingBalance,
                        totalIncome = uiState.totalIncome,
                        totalExpense = uiState.totalExpense
                    )
                }

                item {
                    QuickActionRow(
                        onProjectListClick = onOpenProjectList,
                        onTransactionListClick = onOpenTransactionList,
                        onCategoryClick = onManageCategoriesClick
                    )
                }

                if (uiState.projectItems.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Project Aktif", subtitle = "Pantau posisi tiap project dari sini")
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(uiState.projectItems, key = { it.project.id }) { item ->
                                ActiveProjectCard(
                                    item = item,
                                    onClick = { onProjectClick(item.project.id) }
                                )
                            }
                        }
                    }
                }

                if (uiState.longestRunningProject != null || uiState.biggestExpenseProject != null) {
                    item {
                        SectionHeader(title = "Perhatian", subtitle = "Sorotan otomatis untuk keputusan cepat")
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            uiState.longestRunningProject?.let {
                                AttentionCard(
                                    title = "Project paling lama berjalan",
                                    itemTitle = it.title,
                                    subtitle = it.subtitle,
                                    amount = it.amount,
                                    icon = Icons.Default.BusinessCenter,
                                    onClick = { onProjectClick(it.projectId) }
                                )
                            }
                            uiState.biggestExpenseProject?.let {
                                AttentionCard(
                                    title = "Expense terbesar",
                                    itemTitle = it.title,
                                    subtitle = it.subtitle,
                                    amount = it.amount,
                                    icon = Icons.Default.ArrowDownward,
                                    onClick = { onProjectClick(it.projectId) }
                                )
                            }
                        }
                    }
                }

                item {
                    SectionHeader(title = "Transaksi Terbaru", subtitle = "Urutan terbaru dari seluruh project")
                }

                if (uiState.recentTransactions.isEmpty()) {
                    item {
                        EmptyState(
                            title = "Belum ada transaksi",
                            body = "Tambahkan transaksi pertama untuk mulai memantau arus dana."
                        )
                    }
                } else {
                    items(uiState.recentTransactions, key = { it.transaction.id }) { item ->
                        RecentTransactionCard(
                            item = item,
                            onClick = { onProjectClick(item.transaction.projectId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroSummaryCard(
    activeProjectCount: Int,
    archivedProjectCount: Int,
    activeDifference: Long,
    overallDifference: Long,
    ongoingBalance: Long,
    totalIncome: Long,
    totalExpense: Long
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FBF8)),
        border = BorderStroke(1.dp, Color(0xFFDCEFE0))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("RINGKASAN KEUANGAN", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                    Text("Control Center", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "$activeProjectCount project aktif • $archivedProjectCount arsip • ${formatRupiah(overallDifference)} selisih total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF198754), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryMetricCard(
                        title = "Ongoing Project",
                        value = activeProjectCount.toString(),
                        subtitle = "Project Aktif",
                        icon = Icons.Default.BusinessCenter,
                        tint = Color(0xFF198754),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryMetricCard(
                        title = "Saldo Project Aktif",
                        value = formatRupiah(ongoingBalance),
                        subtitle = "Total saldo akumulatif",
                        icon = Icons.Default.AccountBalanceWallet,
                        tint = Color(0xFF198754),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryMetricCard(
                        title = "Total Dana Masuk",
                        value = formatRupiah(totalIncome),
                        subtitle = "Dari semua project",
                        icon = Icons.Default.ArrowUpward,
                        tint = Color(0xFF198754),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryMetricCard(
                        title = "Total Pengeluaran",
                        value = formatRupiah(totalExpense),
                        subtitle = "Keseluruhan",
                        icon = Icons.Default.ArrowDownward,
                        tint = Color(0xFFE11D48),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryMetricCard(
                        title = "Selisih Project Aktif",
                        value = formatRupiah(activeDifference),
                        subtitle = "Dana tersisa project aktif",
                        icon = Icons.Default.AccountBalanceWallet,
                        tint = Color(0xFF198754),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryMetricCard(
                        title = "Selisih Keseluruhan",
                        value = formatRupiah(overallDifference),
                        subtitle = "Dana semua project",
                        icon = Icons.Default.CalendarMonth,
                        tint = Color(0xFF198754),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionRow(
    onProjectListClick: () -> Unit,
    onTransactionListClick: () -> Unit,
    onCategoryClick: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        QuickActionCard(
            title = "Project",
            subtitle = "Buat Project Baru",
            icon = Icons.Default.BusinessCenter,
            onClick = onProjectListClick,
            modifier = Modifier.weight(1f).height(112.dp)
        )
        QuickActionCard(
            title = "Transaksi",
            subtitle = "Catat Transaksi",
            icon = Icons.Default.AccountBalanceWallet,
            onClick = onTransactionListClick,
            modifier = Modifier.weight(1f).height(112.dp)
        )
        QuickActionCard(
            title = "Kategori",
            subtitle = "Kelola Kategori",
            icon = Icons.AutoMirrored.Filled.Label,
            onClick = onCategoryClick,
            modifier = Modifier.weight(1f).height(112.dp)
        )
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ActiveProjectCard(
    item: DashboardProjectItem,
    onClick: () -> Unit
) {
    val project = item.project
    val summary = item.summary
    val netColor = if (summary.netPosition >= 0) Color(0xFF18A13A) else MaterialTheme.colorScheme.error
    Card(
        modifier = Modifier
            .size(width = 220.dp, height = 160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFE8EEF7))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(project.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                Text("${item.ageDays} hari berjalan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${formatRupiah(summary.totalFundIn)} masuk", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(formatRupiah(summary.netPosition), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = netColor)
                Text("Posisi Bersih", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AttentionCard(
    title: String,
    itemTitle: String,
    subtitle: String,
    amount: Long,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFE8EEF7))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFEAF0FA), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(itemTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(formatRupiah(amount), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.error)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF64748B))
            }
        }
    }
}

@Composable
private fun RecentTransactionCard(
    item: DashboardRecentTransactionItem,
    onClick: () -> Unit
) {
    val transaction = item.transaction
    val amountColor = if (transaction.type.isIncome()) Color(0xFF18A13A) else MaterialTheme.colorScheme.error
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFE8EEF7))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(transaction.description, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                Text(formatRupiah(transaction.realAmount), color = amountColor, fontWeight = FontWeight.ExtraBold)
            }
            Text("${item.projectName} • ${transaction.date}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TypeBadge(transaction.type.toUiLabel())
            }
        }
    }
}

@Composable
private fun SummaryMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE4ECE5))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(tint.copy(alpha = 0.12f), RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF198754)),
        border = BorderStroke(1.dp, Color(0xFF198754))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.88f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
