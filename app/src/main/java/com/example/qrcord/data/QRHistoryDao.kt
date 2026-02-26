package com.example.qrcord.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * QRコード履歴のData Access Object
 */
@Dao
interface QRHistoryDao {

    /**
     * 全履歴を新しい順で取得（Flowで監視）
     */
    @Query("SELECT * FROM qr_history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<QRHistory>>

    /**
     * 全履歴を新しい順で取得（一度だけ）
     */
    @Query("SELECT * FROM qr_history ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<QRHistory>

    /**
     * IDで履歴を取得
     */
    @Query("SELECT * FROM qr_history WHERE id = :id")
    suspend fun getById(id: Long): QRHistory?

    /**
     * 履歴を挿入
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: QRHistory): Long

    /**
     * 複数の履歴を挿入
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(histories: List<QRHistory>)

    /**
     * IDで履歴を削除
     */
    @Query("DELETE FROM qr_history WHERE id = :id")
    suspend fun delete(id: Long)

    /**
     * 全履歴を削除
     */
    @Query("DELETE FROM qr_history")
    suspend fun deleteAll()

    /**
     * 履歴の件数を取得
     */
    @Query("SELECT COUNT(*) FROM qr_history")
    suspend fun getCount(): Int

    /**
     * コンテンツで検索
     */
    @Query("SELECT * FROM qr_history WHERE content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun search(query: String): Flow<List<QRHistory>>

    /**
     * タイプで絞り込み
     */
    @Query("SELECT * FROM qr_history WHERE type = :type ORDER BY timestamp DESC")
    fun getByType(type: QRType): Flow<List<QRHistory>>
}
