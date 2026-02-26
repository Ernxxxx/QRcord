package com.example.qrcord.scanner

import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val TAG = "CameraPreview"

sealed class CameraState {
    data object Loading : CameraState()
    data object Ready : CameraState()
    data class Error(val message: String, val canRetry: Boolean = true) : CameraState()
}

@Composable
fun CameraPreview(
    onQRCodeScanned: (String) -> Unit,
    onError: (CameraError) -> Unit,
    isTorchEnabled: Boolean = false,
    useFrontCamera: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var cameraState by remember { mutableStateOf<CameraState>(CameraState.Loading) }
    var retryTrigger by remember { mutableStateOf(0) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember { createBarcodeScanner() }

    // Control torch when isTorchEnabled changes
    androidx.compose.runtime.LaunchedEffect(isTorchEnabled, camera) {
        camera?.cameraControl?.enableTorch(isTorchEnabled)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            barcodeScanner.close()
        }
    }

    Box(modifier = modifier) {
        when (val state = cameraState) {
            is CameraState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Starting camera...")
                    }
                }
            }

            is CameraState.Error -> {
                CameraErrorUI(
                    message = state.message,
                    canRetry = state.canRetry,
                    onRetry = { retryTrigger++ }
                )
            }

            is CameraState.Ready -> {
                // Camera preview is shown via AndroidView below
            }
        }

        // Always render the AndroidView, but only bind camera when needed
        // key() forces recomposition when camera changes
        key(useFrontCamera) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { previewView ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder()
                            .build()
                            .also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                        val imageAnalyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                    processImageProxy(
                                        barcodeScanner = barcodeScanner,
                                        imageProxy = imageProxy,
                                        onBarcodeDetected = onQRCodeScanned,
                                        onError = { error ->
                                            onError(CameraError.AnalysisFailed(error))
                                        }
                                    )
                                }
                            }

                        val lensFacing = if (useFrontCamera) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(lensFacing)
                            .build()

                        // Unbind all use cases before rebinding
                        cameraProvider.unbindAll()

                        // Bind use cases to camera
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalyzer
                        )

                        // Apply initial torch state
                        camera?.cameraControl?.enableTorch(isTorchEnabled)

                        cameraState = CameraState.Ready
                        Log.d(TAG, "Camera bound successfully")

                    } catch (e: Exception) {
                        Log.e(TAG, "Camera binding failed", e)
                        cameraState = CameraState.Error(
                            message = "Failed to start camera: ${e.localizedMessage ?: "Unknown error"}",
                            canRetry = true
                        )
                        onError(CameraError.InitializationFailed(e))
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        )
        }
    }
}

@Composable
private fun CameraErrorUI(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Camera Error",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )

            if (canRetry) {
                Spacer(modifier = Modifier.height(24.dp))

                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
    }
}

private fun createBarcodeScanner(): BarcodeScanner {
    val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            // 2D codes
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_AZTEC,
            Barcode.FORMAT_DATA_MATRIX,
            Barcode.FORMAT_PDF417,
            // 1D product barcodes
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            // 1D industrial barcodes
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_CODE_93,
            Barcode.FORMAT_CODABAR,
            Barcode.FORMAT_ITF
        )
        .build()

    return BarcodeScanning.getClient(options)
}

// Scan frame ratio (must match ScanFrameOverlay in ScannerScreen.kt)
private const val SCAN_FRAME_RATIO = 0.7f

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    barcodeScanner: BarcodeScanner,
    imageProxy: ImageProxy,
    onBarcodeDetected: (String) -> Unit,
    onError: (Exception) -> Unit
) {
    val mediaImage = imageProxy.image

    if (mediaImage == null) {
        imageProxy.close()
        return
    }

    val inputImage = InputImage.fromMediaImage(
        mediaImage,
        imageProxy.imageInfo.rotationDegrees
    )

    // Calculate scan frame bounds in image coordinates
    val imageWidth = imageProxy.width
    val imageHeight = imageProxy.height
    val frameSize = (minOf(imageWidth, imageHeight) * SCAN_FRAME_RATIO).toInt()
    val frameLeft = (imageWidth - frameSize) / 2
    val frameTop = (imageHeight - frameSize) / 2
    val frameRight = frameLeft + frameSize
    val frameBottom = frameTop + frameSize

    barcodeScanner.process(inputImage)
        .addOnSuccessListener { barcodes ->
            // Only detect barcodes whose center is within the scan frame
            barcodes.firstOrNull { barcode ->
                barcode.boundingBox?.let { box ->
                    val centerX = box.centerX()
                    val centerY = box.centerY()
                    centerX in frameLeft..frameRight &&
                    centerY in frameTop..frameBottom
                } ?: false
            }?.rawValue?.let { value ->
                onBarcodeDetected(value)
            }
        }
        .addOnFailureListener { exception ->
            Log.e(TAG, "Barcode scanning failed", exception)
            onError(exception)
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}
