package com.example.qrcord.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val autoOpenUrl: Boolean = false,
    val saveHistory: Boolean = true,
    val vibrateOnScan: Boolean = true,
    val continuousScan: Boolean = false,
    val privacyNoticeShown: Boolean = false
)

class SettingsViewModel(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsDataStore.autoOpenUrlFlow,
        settingsDataStore.saveHistoryFlow,
        settingsDataStore.vibrateOnScanFlow,
        settingsDataStore.continuousScanFlow,
        settingsDataStore.privacyNoticeShownFlow
    ) { autoOpenUrl, saveHistory, vibrateOnScan, continuousScan, privacyNoticeShown ->
        SettingsUiState(
            autoOpenUrl = autoOpenUrl,
            saveHistory = saveHistory,
            vibrateOnScan = vibrateOnScan,
            continuousScan = continuousScan,
            privacyNoticeShown = privacyNoticeShown
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun updateAutoOpenUrl(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateAutoOpenUrl(enabled)
        }
    }

    fun updateSaveHistory(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateSaveHistory(enabled)
        }
    }

    fun updateVibrateOnScan(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateVibrateOnScan(enabled)
        }
    }

    fun updateContinuousScan(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateContinuousScan(enabled)
        }
    }

    fun updatePrivacyNoticeShown(shown: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updatePrivacyNoticeShown(shown)
        }
    }

    class Factory(
        private val settingsDataStore: SettingsDataStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                return SettingsViewModel(settingsDataStore) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
