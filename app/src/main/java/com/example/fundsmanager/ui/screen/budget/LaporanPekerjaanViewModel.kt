package com.example.fundsmanager.ui.screen.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LaporanUiState(
    // Tim
    val namaTeknisi: String = "",
    val noHpTeknisi: String = "",
    val koordinator: String = "",
    val tglBerangkat: String = "",
    val tglTiba: String = "",
    val tglMulai: String = "",
    val tglSelesai: String = "",

    // Customer
    val namaCustomer: String = "",
    val alamatCustomer: String = "",
    val picLokasi: String = "",
    val ipLan: String = "",

    // Parameter
    val hubSatelite: String = "",
    val sqfAwal: String = "",
    val sqfPointing: String = "",
    val targetEsno: String = "",
    val signalTelkomsel: String = "",
    val signalIndosat: String = "",

    // Sarpen
    val kondisiAc: String = "",
    val sumberElektrikal: String = "",

    // Tindakan
    val tindakanTeknisi: String = "",
    val tindakanFlm: String = "",

    // Catatan
    val penyebabGangguan: String = "",
    val catatan: String = "",

    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LaporanPekerjaanViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(LaporanUiState())
    val uiState: StateFlow<LaporanUiState> = _uiState.asStateFlow()

    fun updateField(field: String, value: String) {
        _uiState.update { state ->
            when (field) {
                "namaTeknisi" -> state.copy(namaTeknisi = value)
                "noHpTeknisi" -> state.copy(noHpTeknisi = value)
                "koordinator" -> state.copy(koordinator = value)
                "tglBerangkat" -> state.copy(tglBerangkat = value)
                "tglTiba" -> state.copy(tglTiba = value)
                "tglMulai" -> state.copy(tglMulai = value)
                "tglSelesai" -> state.copy(tglSelesai = value)
                "namaCustomer" -> state.copy(namaCustomer = value)
                "alamatCustomer" -> state.copy(alamatCustomer = value)
                "picLokasi" -> state.copy(picLokasi = value)
                "ipLan" -> state.copy(ipLan = value)
                "hubSatelite" -> state.copy(hubSatelite = value)
                "sqfPointing" -> state.copy(sqfPointing = value)
                "targetEsno" -> state.copy(targetEsno = value)
                "signalTelkomsel" -> state.copy(signalTelkomsel = value)
                "signalIndosat" -> state.copy(signalIndosat = value)
                "kondisiAc" -> state.copy(kondisiAc = value)
                "sumberElektrikal" -> state.copy(sumberElektrikal = value)
                "tindakanTeknisi" -> state.copy(tindakanTeknisi = value)
                "tindakanFlm" -> state.copy(tindakanFlm = value)
                "penyebabGangguan" -> state.copy(penyebabGangguan = value)
                "catatan" -> state.copy(catatan = value)
                else -> state
            }
        }
    }

    fun save() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                _uiState.update { it.copy(isSaving = false, saved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }
}
