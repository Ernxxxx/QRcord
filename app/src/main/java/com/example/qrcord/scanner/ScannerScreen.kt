package com.example.qrcord.scanner

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qrcord.ads.AdManager
import com.example.qrcord.domain.SafeUrlChecker
import com.example.qrcord.settings.SettingsDataStore
import com.example.qrcord.ui.UrlConfirmDialog

@Composable
fun ScannerScreen(
    hasCameraPermission: Boolean,
    onRequestCameraPermission: () -> Unit,
    settingsDataStore: SettingsDataStore,
    isAdFree: Boolean = false,
    activity: android.app.Activity? = null,
    viewModel: ScannerViewModel = viewModel(
        factory = ScannerViewModelFactory(LocalContext.current.applicationContext)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Ad manager
    val adManager = remember { AdManager.getInstance(context) }

    // Settings state
    val autoOpenUrl by settingsDataStore.autoOpenUrlFlow.collectAsState(initial = false)
    val saveHistory by settingsDataStore.saveHistoryFlow.collectAsState(initial = true)
    val vibrateOnScan by settingsDataStore.vibrateOnScanFlow.collectAsState(initial = true)
    val continuousScan by settingsDataStore.continuousScanFlow.collectAsState(initial = false)
    val scanCount by settingsDataStore.scanCountFlow.collectAsState(initial = 0)
    val ratingShown by settingsDataStore.ratingShownFlow.collectAsState(initial = false)

    // Rating dialog state
    var showRatingDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // URL confirm dialog state
    var showUrlConfirmDialog by remember { mutableStateOf(false) }
    var pendingUrl by remember { mutableStateOf("") }

    // Torch state
    var isTorchEnabled by remember { mutableStateOf(false) }

    // Camera facing state
    var useFrontCamera by remember { mutableStateOf(false) }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            viewModel.analyzeImageUri(context, it, saveHistory)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearScannedResult()
        }
    }

    // Vibrate and auto open URL when scanned
    LaunchedEffect(uiState.scannedResult) {
        val result = uiState.scannedResult ?: return@LaunchedEffect

        // Vibrate on scan success
        if (vibrateOnScan) {
            triggerVibration(context)
        }

        // Increment scan count for rating prompt
        coroutineScope.launch {
            settingsDataStore.incrementScanCount()
        }

        // Show rating dialog after 10 scans (if not shown before)
        if (!ratingShown && scanCount >= 9) {
            showRatingDialog = true
        }

        // Continuous scan mode - auto clear after brief display
        if (continuousScan) {
            Toast.makeText(context, "スキャン完了: ${result.take(50)}...", Toast.LENGTH_SHORT).show()
            delay(500)
            viewModel.clearScannedResult()
            return@LaunchedEffect
        }

        if (autoOpenUrl && SafeUrlChecker.isUrl(result) && SafeUrlChecker.isSafeUrl(result)) {
            viewModel.openUrl(context, result)
            viewModel.clearScannedResult()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            CameraPreview(
                onQRCodeScanned = { barcode ->
                    viewModel.onQRCodeScanned(barcode, saveHistory)
                },
                onError = { error ->
                    viewModel.onCameraError(error)
                },
                isTorchEnabled = isTorchEnabled,
                useFrontCamera = useFrontCamera,
                modifier = Modifier.fillMaxSize()
            )

            // Modern scan frame overlay with dimmed background
            ModernScanFrameOverlay(
                modifier = Modifier.fillMaxSize()
            )

            // Top action buttons
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Camera switch button
                ActionButton(
                    icon = Icons.Default.Cameraswitch,
                    contentDescription = if (useFrontCamera) "外カメラに切替" else "内カメラに切替",
                    isActive = false,
                    onClick = {
                        useFrontCamera = !useFrontCamera
                        if (useFrontCamera) {
                            isTorchEnabled = false
                        }
                    }
                )

                // Torch button (only show for back camera)
                if (!useFrontCamera) {
                    ActionButton(
                        icon = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = if (isTorchEnabled) "ライトをオフ" else "ライトをオン",
                        isActive = isTorchEnabled,
                        onClick = { isTorchEnabled = !isTorchEnabled }
                    )
                }

                // Gallery button
                ActionButton(
                    icon = Icons.Default.PhotoLibrary,
                    contentDescription = "写真から読み取る",
                    isActive = false,
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }

            // Scan hint text
            if (uiState.scannedResult == null && !uiState.isAnalyzingImage) {
                Text(
                    text = "QRコードを枠内に合わせてください",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 280.dp)
                )
            }

            // Loading indicator for image analysis
            if (uiState.isAnalyzingImage) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "画像を解析中...",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Scanned result overlay
            uiState.scannedResult?.let { result ->
                ModernScannedResultCard(
                    result = result,
                    onDismiss = {
                        // Increment ad scan count and show interstitial if needed
                        if (!isAdFree && !continuousScan) {
                            adManager.onScanCompleted()
                            if (adManager.shouldShowInterstitial() && activity != null) {
                                adManager.showInterstitialAd(activity) {
                                    viewModel.clearScannedResult()
                                }
                            } else {
                                viewModel.clearScannedResult()
                            }
                        } else {
                            viewModel.clearScannedResult()
                        }
                    },
                    onCopy = {
                        viewModel.copyToClipboard(context, result)
                        Toast.makeText(context, "コピーしました", Toast.LENGTH_SHORT).show()
                    },
                    onOpen = {
                        if (SafeUrlChecker.isUrl(result)) {
                            if (autoOpenUrl && SafeUrlChecker.isSafeUrl(result)) {
                                viewModel.openUrl(context, result)
                            } else {
                                pendingUrl = result
                                showUrlConfirmDialog = true
                            }
                        }
                    },
                    onShare = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, result)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "共有"))
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }

            // Error snackbar
            uiState.errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("閉じる")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        } else {
            CameraPermissionDeniedUI(
                onRequestPermission = onRequestCameraPermission,
                onOpenSettings = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            )
        }
    }

    // URL Confirmation Dialog
    if (showUrlConfirmDialog) {
        UrlConfirmDialog(
            url = pendingUrl,
            onConfirm = {
                showUrlConfirmDialog = false
                viewModel.openUrl(context, pendingUrl)
                pendingUrl = ""
            },
            onDismiss = {
                showUrlConfirmDialog = false
                pendingUrl = ""
            }
        )
    }

    // Rating Dialog
    if (showRatingDialog) {
        RatingDialog(
            onRate = {
                showRatingDialog = false
                coroutineScope.launch {
                    settingsDataStore.updateRatingShown(true)
                }
                // Open Play Store
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                context.startActivity(intent)
            },
            onLater = {
                showRatingDialog = false
            },
            onNever = {
                showRatingDialog = false
                coroutineScope.launch {
                    settingsDataStore.updateRatingShown(true)
                }
            }
        )
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Black.copy(alpha = 0.4f)
        },
        animationSpec = tween(200),
        label = "backgroundColor"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            Color.White
        },
        animationSpec = tween(200),
        label = "iconColor"
    )

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = backgroundColor,
        modifier = Modifier.size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun CameraPermissionDeniedUI(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "カメラ権限が必要です",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "QRコードの読み取りにカメラを使用します。\nカメラ映像はサーバーには送信されません。",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onRequestPermission,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "カメラ権限を許可",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "アプリ設定を開く",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun ModernScannedResultCard(
    result: String,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUrl = remember(result) { SafeUrlChecker.isUrl(result) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "QRコードを検出",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "閉じる",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Text(
                    text = result,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onCopy,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("コピー", style = MaterialTheme.typography.labelMedium)
                }

                FilledTonalButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("共有", style = MaterialTheme.typography.labelMedium)
                }

                if (isUrl) {
                    Button(
                        onClick = onOpen,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("開く", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

// Must match SCAN_FRAME_RATIO in CameraPreview.kt
private const val SCAN_FRAME_RATIO = 0.7f

@Composable
private fun ModernScanFrameOverlay(
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val overlayColor = Color.Black.copy(alpha = 0.5f)

    Canvas(modifier = modifier) {
        val frameSize = size.minDimension * SCAN_FRAME_RATIO
        val left = (size.width - frameSize) / 2
        val top = (size.height - frameSize) / 2
        val right = left + frameSize
        val bottom = top + frameSize
        val cornerLength = frameSize * 0.15f
        val cornerRadius = 24f
        val strokeWidth = 6f

        // Draw semi-transparent overlay outside the scan frame (4 rectangles)
        // Top
        drawRect(
            color = overlayColor,
            topLeft = Offset(0f, 0f),
            size = Size(size.width, top)
        )
        // Bottom
        drawRect(
            color = overlayColor,
            topLeft = Offset(0f, bottom),
            size = Size(size.width, size.height - bottom)
        )
        // Left
        drawRect(
            color = overlayColor,
            topLeft = Offset(0f, top),
            size = Size(left, frameSize)
        )
        // Right
        drawRect(
            color = overlayColor,
            topLeft = Offset(right, top),
            size = Size(size.width - right, frameSize)
        )

        // Draw corner brackets with primary color
        val corners = listOf(
            // Top-left
            Pair(Offset(left, top + cornerLength), Offset(left, top + cornerRadius)),
            Pair(Offset(left + cornerRadius, top), Offset(left + cornerLength, top)),
            // Top-right
            Pair(Offset(right - cornerLength, top), Offset(right - cornerRadius, top)),
            Pair(Offset(right, top + cornerRadius), Offset(right, top + cornerLength)),
            // Bottom-right
            Pair(Offset(right, bottom - cornerLength), Offset(right, bottom - cornerRadius)),
            Pair(Offset(right - cornerRadius, bottom), Offset(right - cornerLength, bottom)),
            // Bottom-left
            Pair(Offset(left + cornerLength, bottom), Offset(left + cornerRadius, bottom)),
            Pair(Offset(left, bottom - cornerRadius), Offset(left, bottom - cornerLength))
        )

        corners.forEach { (start, end) ->
            drawLine(
                color = primaryColor,
                start = start,
                end = end,
                strokeWidth = strokeWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }

        // Draw corner arcs
        val arcStroke = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)

        // Top-left arc
        drawArc(
            color = primaryColor,
            startAngle = 180f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(left, top),
            size = Size(cornerRadius * 2, cornerRadius * 2),
            style = arcStroke
        )

        // Top-right arc
        drawArc(
            color = primaryColor,
            startAngle = 270f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(right - cornerRadius * 2, top),
            size = Size(cornerRadius * 2, cornerRadius * 2),
            style = arcStroke
        )

        // Bottom-right arc
        drawArc(
            color = primaryColor,
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(right - cornerRadius * 2, bottom - cornerRadius * 2),
            size = Size(cornerRadius * 2, cornerRadius * 2),
            style = arcStroke
        )

        // Bottom-left arc
        drawArc(
            color = primaryColor,
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(left, bottom - cornerRadius * 2),
            size = Size(cornerRadius * 2, cornerRadius * 2),
            style = arcStroke
        )
    }
}

@Suppress("DEPRECATION")
private fun triggerVibration(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        vibrator.vibrate(100)
    }
}

@Composable
private fun RatingDialog(
    onRate: () -> Unit,
    onLater: () -> Unit,
    onNever: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onLater,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        title = {
            Text(
                text = "アプリを気に入っていただけましたか？",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = "ぜひストアで評価をお願いします。\nご意見をいただけると励みになります！",
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(onClick = onRate) {
                Text("評価する")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onNever) {
                    Text("表示しない")
                }
                TextButton(onClick = onLater) {
                    Text("あとで")
                }
            }
        }
    )
}
