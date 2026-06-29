package com.example.fundsmanager.ui.screen.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaporanPekerjaanScreen(
    viewModel: LaporanPekerjaanViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan Pekerjaan") },
                navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, "Kembali") } }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = viewModel::save,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    enabled = !uiState.isSaving && !uiState.saved
                ) { Text(if (uiState.saved) "Tersimpan" else "Simpan Laporan") }
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            SectionTitle("1. Tim")
            FormField("Nama Teknisi", uiState.namaTeknisi) { viewModel.updateField("namaTeknisi", it) }
            FormField("No HP", uiState.noHpTeknisi) { viewModel.updateField("noHpTeknisi", it) }
            FormField("Koordinator", uiState.koordinator) { viewModel.updateField("koordinator", it) }
            FormField("Tgl Berangkat", uiState.tglBerangkat) { viewModel.updateField("tglBerangkat", it) }
            FormField("Tgl Tiba", uiState.tglTiba) { viewModel.updateField("tglTiba", it) }
            FormField("Tgl Mulai", uiState.tglMulai) { viewModel.updateField("tglMulai", it) }
            FormField("Tgl Selesai", uiState.tglSelesai) { viewModel.updateField("tglSelesai", it) }

            SectionTitle("2. Customer")
            FormField("Nama Customer", uiState.namaCustomer) { viewModel.updateField("namaCustomer", it) }
            FormField("Alamat", uiState.alamatCustomer) { viewModel.updateField("alamatCustomer", it) }
            FormField("PIC Lokasi", uiState.picLokasi) { viewModel.updateField("picLokasi", it) }
            FormField("IP LAN", uiState.ipLan) { viewModel.updateField("ipLan", it) }

            SectionTitle("3. Parameter Sinyal")
            FormField("Hub/Satelite", uiState.hubSatelite) { viewModel.updateField("hubSatelite", it) }
            FormField("SQF Pointing", uiState.sqfPointing) { viewModel.updateField("sqfPointing", it) }
            FormField("Target ESNO", uiState.targetEsno) { viewModel.updateField("targetEsno", it) }
            FormField("Signal Telkomsel", uiState.signalTelkomsel) { viewModel.updateField("signalTelkomsel", it) }
            FormField("Signal Indosat", uiState.signalIndosat) { viewModel.updateField("signalIndosat", it) }

            SectionTitle("4. Sarpen")
            FormField("Kondisi AC", uiState.kondisiAc) { viewModel.updateField("kondisiAc", it) }
            FormField("Sumber Elektrikal", uiState.sumberElektrikal) { viewModel.updateField("sumberElektrikal", it) }

            SectionTitle("5. Tindakan")
            FormField("Tindakan Teknisi", uiState.tindakanTeknisi, multiline = true) { viewModel.updateField("tindakanTeknisi", it) }
            FormField("Tindakan FLM", uiState.tindakanFlm, multiline = true) { viewModel.updateField("tindakanFlm", it) }

            SectionTitle("6. Catatan")
            FormField("Penyebab Gangguan", uiState.penyebabGangguan, multiline = true) { viewModel.updateField("penyebabGangguan", it) }
            FormField("Catatan", uiState.catatan, multiline = true) { viewModel.updateField("catatan", it) }

            uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
}

@Composable
fun FormField(label: String, value: String, multiline: Boolean = false, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        maxLines = if (multiline) 4 else 1,
        minLines = if (multiline) 2 else 1
    )
}
