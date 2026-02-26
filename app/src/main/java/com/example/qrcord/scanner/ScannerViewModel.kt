package com.example.qrcord.scanner

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.qrcord.data.QRHistory
import com.example.qrcord.data.QRHistoryDatabase
import com.example.qrcord.domain.SafeUrlChecker
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ScannerUiState(
    val scannedResult: String? = null,
    val errorMessage: String? = null,
    val isScanning: Boolean = true,
    val isAnalyzingImage: Boolean = false
)

class ScannerViewModel(
    private val applicationContext: Context
) : ViewModel() {

    private val database = QRHistoryDatabase.getInstance(applicationContext)
    private val dao = database.qrHistoryDao()

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private var lastScannedValue: String? = null
    private var lastScannedTime: Long = 0

    fun onQRCodeScanned(barcodeValue: String, saveHistory: Boolean = true) {
        // Debounce duplicate scans within 2 seconds
        val currentTime = System.currentTimeMillis()
        if (barcodeValue == lastScannedValue && currentTime - lastScannedTime < SCAN_DEBOUNCE_MS) {
            return
        }

        lastScannedValue = barcodeValue
        lastScannedTime = currentTime

        Log.d(TAG, "QR Code scanned: $barcodeValue")

        _uiState.update { it.copy(scannedResult = barcodeValue, isScanning = false) }

        // Save to history if enabled
        if (saveHistory) {
            saveToHistory(barcodeValue)
        }
    }

    fun onCameraError(error: CameraError) {
        Log.e(TAG, "Camera error: ${error.message}", error.cause)

        val userMessage = when (error) {
            is CameraError.InitializationFailed ->
                "カメラの起動に失敗しました。再試行するか、アプリを再起動してください。"
            is CameraError.AnalysisFailed ->
                "QRコードのスキャン中にエラーが発生しました。再試行中..."
            is CameraError.Unknown ->
                "予期しないカメラエラーが発生しました: ${error.message}"
        }

        _uiState.update { it.copy(errorMessage = userMessage) }
    }

    fun clearScannedResult() {
        _uiState.update { it.copy(scannedResult = null, isScanning = true) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun copyToClipboard(context: Context, text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("QR Code", text)
            clipboard.setPrimaryClip(clip)
            Log.d(TAG, "Copied to clipboard: $text")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy to clipboard", e)
            _uiState.update { it.copy(errorMessage = "クリップボードへのコピーに失敗しました") }
        }
    }

    fun openUrl(context: Context, url: String) {
        // Safety check
        if (!SafeUrlChecker.isSafeUrl(url)) {
            val reason = SafeUrlChecker.getDangerReason(url)
            Toast.makeText(context, reason ?: "安全でないURLです", Toast.LENGTH_LONG).show()
            Log.w(TAG, "Blocked unsafe URL: $url")
            return
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
            Log.d(TAG, "Opening URL: $url")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open URL", e)
            // Don't swallow the exception - show error to user
            Toast.makeText(context, "URLを開けませんでした: ${e.message}", Toast.LENGTH_SHORT).show()
            _uiState.update { it.copy(errorMessage = "URLを開けませんでした: ${e.message}") }
        }
    }

    private fun saveToHistory(barcodeValue: String) {
        viewModelScope.launch {
            try {
                val qrType = QRHistory.detectType(barcodeValue)
                val history = QRHistory(
                    content = barcodeValue,
                    type = qrType
                )
                dao.insert(history)
                Log.d(TAG, "Saved to history: $barcodeValue")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save to history", e)
                // Don't show error to user for history save failure - it's not critical
            }
        }
    }

    fun analyzeImageUri(context: Context, uri: Uri, saveHistory: Boolean = true) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzingImage = true, errorMessage = null) }

            try {
                val image = InputImage.fromFilePath(context, uri)
                val scanner = BarcodeScanning.getClient()
                val barcodes = scanner.process(image).await()

                if (barcodes.isNotEmpty()) {
                    val barcode = barcodes.first()
                    val value = barcode.rawValue
                    if (value != null) {
                        Log.d(TAG, "QR Code found in image: $value")
                        _uiState.update {
                            it.copy(
                                scannedResult = value,
                                isScanning = false,
                                isAnalyzingImage = false
                            )
                        }
                        if (saveHistory) {
                            saveToHistory(value)
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                errorMessage = "QRコードの内容を読み取れませんでした",
                                isAnalyzingImage = false
                            )
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            errorMessage = "画像からQRコードが見つかりませんでした",
                            isAnalyzingImage = false
                        )
                    }
                }

                scanner.close()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to analyze image", e)
                _uiState.update {
                    it.copy(
                        errorMessage = "画像の解析に失敗しました: ${e.message}",
                        isAnalyzingImage = false
                    )
                }
            }
        }
    }

    companion object {
        private const val TAG = "ScannerViewModel"
        private const val SCAN_DEBOUNCE_MS = 2000L
    }
}

class ScannerViewModelFactory(
    private val applicationContext: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScannerViewModel::class.java)) {
            return ScannerViewModel(applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

sealed class CameraError(val message: String, val cause: Throwable? = null) {
    class InitializationFailed(cause: Throwable? = null) :
        CameraError("Camera initialization failed", cause)
    class AnalysisFailed(cause: Throwable? = null) :
        CameraError("Image analysis failed", cause)
    class Unknown(message: String, cause: Throwable? = null) :
        CameraError(message, cause)
}
