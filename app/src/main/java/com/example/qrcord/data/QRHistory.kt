package com.example.qrcord.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * QRコードの種類を表すEnum
 */
enum class QRType {
    URL,
    TEXT,
    EMAIL,
    PHONE,
    WIFI,
    VCARD,
    SMS,
    GEO,
    CALENDAR,
    BARCODE,
    OTHER
}

/**
 * QRコード履歴のエンティティ
 */
@Entity(tableName = "qr_history")
data class QRHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val content: String,

    val type: QRType,

    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * コンテンツからQRTypeを判定する
         */
        fun detectType(content: String): QRType {
            return when {
                content.startsWith("http://", ignoreCase = true) ||
                content.startsWith("https://", ignoreCase = true) -> QRType.URL

                content.startsWith("mailto:", ignoreCase = true) -> QRType.EMAIL

                content.startsWith("tel:", ignoreCase = true) -> QRType.PHONE

                content.startsWith("WIFI:", ignoreCase = true) -> QRType.WIFI

                content.startsWith("BEGIN:VCARD", ignoreCase = true) -> QRType.VCARD

                content.startsWith("smsto:", ignoreCase = true) ||
                content.startsWith("sms:", ignoreCase = true) -> QRType.SMS

                content.startsWith("geo:", ignoreCase = true) -> QRType.GEO

                content.startsWith("BEGIN:VEVENT", ignoreCase = true) ||
                content.startsWith("BEGIN:VCALENDAR", ignoreCase = true) -> QRType.CALENDAR

                content.matches(Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) -> QRType.EMAIL

                content.matches(Regex("^\\+?[0-9\\-\\s()]+$")) && content.length >= 7 && content.length <= 15 -> QRType.PHONE

                // Barcode detection (EAN-13, EAN-8, UPC-A, UPC-E, etc.)
                content.matches(Regex("^[0-9]{8,14}$")) -> QRType.BARCODE

                else -> QRType.TEXT
            }
        }
    }
}
