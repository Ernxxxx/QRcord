package com.example.qrcord.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

/**
 * QRType用のTypeConverter
 */
class QRTypeConverters {
    @TypeConverter
    fun fromQRType(type: QRType): String {
        return type.name
    }

    @TypeConverter
    fun toQRType(value: String): QRType {
        return try {
            QRType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            QRType.OTHER
        }
    }
}

/**
 * QRコード履歴のRoom Database
 */
@Database(
    entities = [QRHistory::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(QRTypeConverters::class)
abstract class QRHistoryDatabase : RoomDatabase() {

    abstract fun qrHistoryDao(): QRHistoryDao

    companion object {
        private const val DATABASE_NAME = "qr_history_database"

        @Volatile
        private var INSTANCE: QRHistoryDatabase? = null

        /**
         * シングルトンインスタンスを取得
         */
        fun getInstance(context: Context): QRHistoryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): QRHistoryDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                QRHistoryDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
