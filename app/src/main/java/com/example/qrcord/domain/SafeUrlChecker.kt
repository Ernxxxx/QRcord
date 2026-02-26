package com.example.qrcord.domain

import android.util.Patterns
import java.net.URI

/**
 * URLの安全性をチェックするユーティリティクラス
 */
object SafeUrlChecker {

    /**
     * ブロック対象のスキームリスト
     */
    private val blockedSchemes = listOf(
        "intent",
        "javascript",
        "file"
    )

    /**
     * 危険なスキームとその理由のマッピング
     */
    private val dangerReasons = mapOf(
        "intent" to "アプリの意図しない起動を引き起こす可能性があります",
        "javascript" to "悪意のあるスクリプトが実行される可能性があります",
        "file" to "端末内のファイルへの不正アクセスの可能性があります"
    )

    /**
     * 文字列がURLかどうかを判定する
     *
     * @param content 判定対象の文字列
     * @return URLの場合はtrue、それ以外はfalse
     */
    fun isUrl(content: String): Boolean {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return false

        // 標準的なURLパターンでチェック
        if (Patterns.WEB_URL.matcher(trimmed).matches()) {
            return true
        }

        // スキームを含むURLをチェック（intent://、javascript://など）
        return try {
            val uri = URI(trimmed)
            uri.scheme != null && uri.scheme.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * URLが安全かどうかを判定する
     *
     * @param url 判定対象のURL
     * @return 安全な場合はtrue、危険な場合はfalse
     */
    fun isSafeUrl(url: String): Boolean {
        val trimmed = url.trim().lowercase()

        // ブロック対象のスキームをチェック
        for (scheme in blockedSchemes) {
            if (trimmed.startsWith("$scheme://") || trimmed.startsWith("$scheme:")) {
                return false
            }
        }

        return true
    }

    /**
     * URLからドメイン名を取得する
     *
     * @param url 対象のURL
     * @return ドメイン名、取得できない場合はnull
     */
    fun getUrlDomain(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return null

        return try {
            val uri = URI(trimmed)
            uri.host?.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            // URIとして解析できない場合、簡易的にドメインを抽出
            extractDomainSimple(trimmed)
        }
    }

    /**
     * 危険なURLの場合、その理由を返す
     *
     * @param url 対象のURL
     * @return 危険な理由、安全な場合はnull
     */
    fun getDangerReason(url: String): String? {
        val trimmed = url.trim().lowercase()

        for ((scheme, reason) in dangerReasons) {
            if (trimmed.startsWith("$scheme://") || trimmed.startsWith("$scheme:")) {
                return reason
            }
        }

        return null
    }

    /**
     * 簡易的なドメイン抽出
     */
    private fun extractDomainSimple(url: String): String? {
        // http:// または https:// を除去
        val withoutProtocol = url
            .removePrefix("http://")
            .removePrefix("https://")
            .removePrefix("www.")

        // パスの前までを取得
        val domain = withoutProtocol.split("/").firstOrNull()

        return domain?.takeIf { it.isNotEmpty() && it.contains(".") }
    }
}
