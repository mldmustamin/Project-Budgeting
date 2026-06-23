package com.example.fundsmanager.ui.screen.transaction

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fundsmanager.domain.model.Account
import com.example.fundsmanager.domain.model.Category
import com.example.fundsmanager.domain.model.TransactionType
import com.example.fundsmanager.domain.model.isIncome
import com.example.fundsmanager.domain.model.requiresRealAmountInput
import com.example.fundsmanager.domain.model.toUiLabel
import com.example.fundsmanager.ui.component.AppDropdownField
import com.example.fundsmanager.ui.component.MoneyInputField
import com.example.fundsmanager.ui.component.PrimaryButton
import com.example.fundsmanager.ui.component.ProofSourceDialog
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    onBackClick: () -> Unit,
    viewModel: TransactionFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showProofDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val attachmentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = it.lastPathSegment ?: "bukti-transaksi"
            context.contentResolver.openInputStream(it)?.use { stream ->
                val bytes = stream.readBytes()
                viewModel.onAttachmentSelected(ByteArrayInputStream(bytes), fileName)
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            val output = ByteArrayOutputStream()
            it.compress(Bitmap.CompressFormat.JPEG, 92, output)
            viewModel.onAttachmentSelected(
                inputStream = ByteArrayInputStream(output.toByteArray()),
                fileName = "bukti-${System.currentTimeMillis()}.jpg"
            )
        }
    }

    if (showProofDialog) {
        ProofSourceDialog(
            onDismiss = { showProofDialog = false },
            onSelectFile = {
                showProofDialog = false
                attachmentLauncher.launch("*/*")
            },
            onOpenCamera = {
                showProofDialog = false
                cameraLauncher.launch(null)
            }
        )
    }

    if (showDatePicker) {
        TransactionDatePickerDialog(
            currentDate = uiState.date,
            onDismiss = { showDatePicker = false },
            onDateSelected = {
                viewModel.onDateChange(it)
                showDatePicker = false
            }
        )
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onBackClick()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isEditMode) "Edit Transaksi" else "Form Transaksi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { showProofDialog = true }) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Lampirkan bukti")
                    }
                    IconButton(onClick = { viewModel.saveTransaction(1L) }) {
                        Icon(Icons.Default.Check, contentDescription = "Simpan")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                TransactionTypeDropdown(
                    selectedType = uiState.type,
                    onTypeSelected = viewModel::onTypeChange
                )

                DatePickerField(
                    label = "Tanggal",
                    value = uiState.date,
                    onClick = { showDatePicker = true }
                )

                LabeledTextField(
                    label = "Keterangan",
                    value = uiState.description,
                    onValueChange = viewModel::onDescriptionChange,
                    placeholder = "Contoh: Transfer masuk"
                )

                if (uiState.type.requiresRealAmountInput()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MoneyInputField(
                            value = uiState.reportedAmount,
                            onValueChange = viewModel::onReportedAmountChange,
                            label = "Nominal Dilaporkan",
                            modifier = Modifier.weight(1f)
                        )
                        MoneyInputField(
                            value = uiState.realAmount,
                            onValueChange = viewModel::onRealAmountChange,
                            label = "Nominal Real",
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    MoneyInputField(
                        value = uiState.reportedAmount,
                        onValueChange = viewModel::onReportedAmountChange,
                        label = if (uiState.type.isIncome()) "Nominal Dana Masuk" else "Nominal Expense",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                AccountDropdown(
                    accounts = uiState.accounts,
                    selectedAccountId = uiState.selectedAccountId,
                    onAccountSelected = viewModel::onAccountChange
                )

                CategoryDropdown(
                    categories = uiState.categories,
                    selectedCategoryId = uiState.selectedCategoryId,
                    onCategorySelected = viewModel::onCategoryChange
                )

                LabeledTextField(
                    label = "Catatan (Opsional)",
                    value = uiState.note,
                    onValueChange = viewModel::onNoteChange,
                    placeholder = "Catatan tambahan (opsional)"
                )

                ProofInputCard(
                    attachmentNames = uiState.attachments.map { it.fileName ?: "Bukti tanpa nama" } + uiState.pendingAttachmentNames,
                    onClick = { showProofDialog = true }
                )

                uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }

                uiState.duplicateWarning?.let { warning ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)), shape = RoundedCornerShape(14.dp)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(warning, fontWeight = FontWeight.Bold)
                            Text("Tekan Simpan sekali lagi untuk tetap menyimpan.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                PrimaryButton(
                    text = if (uiState.isEditMode) "Simpan Perubahan" else "Simpan",
                    onClick = { viewModel.saveTransaction(1L) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ProofInputCard(
    attachmentNames: List<String>,
    onClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Bukti Transaksi",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        if (attachmentNames.isEmpty()) "Tambahkan bukti" else "${attachmentNames.size} bukti terlampir",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        if (attachmentNames.isEmpty()) "Pilih file atau ambil foto" else attachmentNames.joinToString(", "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder?.let { { Text(it) } },
            trailingIcon = trailingIcon,
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDatePickerDialog(
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
private fun DatePickerField(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    .padding(horizontal = 16.dp, vertical = 17.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value.ifBlank { "yyyy-MM-dd" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Icon(Icons.Default.CalendarToday, contentDescription = "Pilih tanggal", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun TransactionTypeDropdown(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit
) {
    AppDropdownField(
        label = "Jenis Transaksi",
        selectedLabel = selectedType.toUiLabel(),
        options = TransactionType.entries,
        optionLabel = { it.toUiLabel() },
        onSelected = onTypeSelected,
        leadingIcon = { TypeIcon(selectedType) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun TypeIcon(type: TransactionType) {
    val tint = when {
        type.isIncome() -> Color(0xFF18A13A)
        type.requiresRealAmountInput() -> MaterialTheme.colorScheme.error
        else -> Color(0xFF7C3AED)
    }
    Box(modifier = Modifier.size(28.dp).background(tint.copy(alpha = 0.13f), RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
        Icon(Icons.Default.Payments, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun AccountDropdown(
    accounts: List<Account>,
    selectedAccountId: Long?,
    onAccountSelected: (Long) -> Unit
) {
    val selectedAccount = accounts.find { it.id == selectedAccountId }
    AppDropdownField(
        label = "Akun",
        selectedLabel = selectedAccount?.name.orEmpty(),
        options = accounts,
        optionLabel = { it.name },
        onSelected = { onAccountSelected(it.id) },
        leadingIcon = { SmallBlueIcon(Icons.Default.AccountBalanceWallet) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CategoryDropdown(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit
) {
    val noCategory = Category(id = -1, name = "Tanpa Kategori", description = null)
    val options = listOf(noCategory) + categories
    val selectedCategory = categories.find { it.id == selectedCategoryId }
    AppDropdownField(
        label = "Kategori (Opsional)",
        selectedLabel = selectedCategory?.name ?: noCategory.name,
        options = options,
        optionLabel = { it.name },
        onSelected = { onCategorySelected(it.id.takeIf { id -> id > 0 }) },
        leadingIcon = { SmallBlueIcon(Icons.Default.BusinessCenter) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SmallBlueIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(modifier = Modifier.size(28.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
    }
}
