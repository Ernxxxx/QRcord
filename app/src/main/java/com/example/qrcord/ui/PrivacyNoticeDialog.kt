package com.example.qrcord.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 初回起動時のプライバシー通知ダイアログ
 *
 * @param onDismiss 「OK」ボタン押下時のコールバック
 */
@Composable
fun PrivacyNoticeDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* ダイアログ外タップでは閉じない */ },
        title = {
            Text(
                text = "プライバシーについて",
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
                Text(
                    text = "このアプリをご利用いただく前に、以下の点についてお知らせします。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // カメラの使用について
                PrivacyItem(
                    title = "カメラの使用",
                    description = "カメラはQRコードの読み取りのみに使用されます。撮影した画像がサーバーに送信されることはありません。"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // データの保存について
                PrivacyItem(
                    title = "データの保存",
                    description = "読み取ったQRコードの内容は、お使いの端末内にのみ保存されます。外部サーバーへの送信は行いません。"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // クラッシュ情報について
                PrivacyItem(
                    title = "品質改善のための情報収集",
                    description = "アプリの品質改善を目的として、クラッシュ情報（匿名）が自動的に送信される場合があります。個人を特定できる情報は含まれません。"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "OK",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

/**
 * プライバシー項目の表示コンポーネント
 *
 * @param title 項目のタイトル
 * @param description 項目の説明
 */
@Composable
private fun PrivacyItem(
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
