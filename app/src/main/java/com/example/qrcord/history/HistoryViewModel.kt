package com.example.qrcord.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrcord.data.QRHistory
import com.example.qrcord.data.QRHistoryDatabase
import com.example.qrcord.data.QRType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 履歴画面のUI状態
 */
data class HistoryUiState(
    val isLoading: Boolean = true,
    val selectedHistory: QRHistory? = null,
    val showDeleteConfirmDialog: Boolean = false,
    val showDeleteAllConfirmDialog: Boolean = false,
    val errorMessage: String? = null
)

/**
 * 履歴画面のViewModel
 */
class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val database = QRHistoryDatabase.getInstance(application)
    private val dao = database.qrHistoryDao()

    /**
     * 全履歴をFlowで監視
     */
    val historyList: StateFlow<List<QRHistory>> = dao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // 初期ロード完了をマーク
            historyList.collect {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    /**
     * 履歴アイテムを選択
     */
    fun selectHistory(history: QRHistory) {
        _uiState.value = _uiState.value.copy(selectedHistory = history)
    }

    /**
     * 選択をクリア
     */
    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedHistory = null)
    }

    /**
     * 削除確認ダイアログを表示
     */
    fun showDeleteConfirmDialog(history: QRHistory) {
        _uiState.value = _uiState.value.copy(
            selectedHistory = history,
            showDeleteConfirmDialog = true
        )
    }

    /**
     * 削除確認ダイアログを非表示
     */
    fun hideDeleteConfirmDialog() {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirmDialog = false
        )
    }

    /**
     * 全削除確認ダイアログを表示
     */
    fun showDeleteAllConfirmDialog() {
        _uiState.value = _uiState.value.copy(showDeleteAllConfirmDialog = true)
    }

    /**
     * 全削除確認ダイアログを非表示
     */
    fun hideDeleteAllConfirmDialog() {
        _uiState.value = _uiState.value.copy(showDeleteAllConfirmDialog = false)
    }

    /**
     * 履歴を削除
     */
    fun deleteHistory(id: Long) {
        viewModelScope.launch {
            try {
                dao.delete(id)
                _uiState.value = _uiState.value.copy(
                    showDeleteConfirmDialog = false,
                    selectedHistory = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "削除に失敗しました: ${e.message}"
                )
            }
        }
    }

    /**
     * 全履歴を削除
     */
    fun deleteAllHistory() {
        viewModelScope.launch {
            try {
                dao.deleteAll()
                _uiState.value = _uiState.value.copy(
                    showDeleteAllConfirmDialog = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "全削除に失敗しました: ${e.message}"
                )
            }
        }
    }

    /**
     * エラーメッセージをクリア
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * 新しい履歴を追加
     */
    fun addHistory(content: String, type: QRType? = null) {
        viewModelScope.launch {
            try {
                val qrType = type ?: QRHistory.detectType(content)
                val history = QRHistory(
                    content = content,
                    type = qrType
                )
                dao.insert(history)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "履歴の保存に失敗しました: ${e.message}"
                )
            }
        }
    }

    /**
     * 検索
     */
    fun search(query: String) = dao.search(query)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * タイプで絞り込み
     */
    fun filterByType(type: QRType) = dao.getByType(type)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
