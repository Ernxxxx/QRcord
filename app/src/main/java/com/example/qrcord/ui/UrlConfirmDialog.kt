package com.example.qrcord.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.qrcord.domain.SafeUrlChecker

/**
 * URL確認ダイアログの最大表示文字数
 */
private const val MAX_URL_DISPLAY_LENGTH = 100

/**
 * URL開く前の確認ダイアログ
 *
 * @param url 開こうとしているURL
 * @param onConfirm 「開く」ボタン押下時のコールバック
 * @param onDismiss 「キャンセル」ボタン押下時またはダイアログ外タップ時のコールバック
 */
@Composable
fun UrlConfirmDialog(
    url: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val domain = SafeUrlChecker.getUrlDomain(url)
    val isSafe = SafeUrlChecker.isSafeUrl(url)
    val dangerReason = SafeUrlChecker.getDangerReason(url)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isSafe) "URLを開きますか？" else "警告",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 危険なURLの場合は警告を表示
                if (!isSafe) {
                    DangerWarningCard(dangerReason)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ドメイン名を表示
                if (domain != null) {
                    Text(
                        text = "ドメイン",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = domain,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // URL全文を表示（長い場合は省略）
                Text(
                    text = "URL",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = truncateUrl(url),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = isSafe // 危険なURLの場合は「開く」ボタンを無効化
            ) {
                Text(
                    text = "開く",
                    color = if (isSafe) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "キャンセル")
            }
        }
    )
}

/**
 * 危険なURL警告カード
 */
@Composable
private fun DangerWarningCard(reason: String?) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "警告",
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "危険なURLです",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                if (reason != null) {
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

/**
 * URLを指定文字数で省略する
 */
private fun truncateUrl(url: String): String {
    return if (url.length > MAX_URL_DISPLAY_LENGTH) {
        url.take(MAX_URL_DISPLAY_LENGTH) + "..."
    } else {
        url
    }
}
