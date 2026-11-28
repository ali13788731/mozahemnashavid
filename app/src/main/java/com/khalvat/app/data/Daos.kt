package com.khalvat.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PrivateContactDao {
    @Query("SELECT * FROM private_contacts ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<PrivateContact>>

    @Query("SELECT * FROM private_contacts")
    suspend fun getAll(): List<PrivateContact>

    @Query("SELECT * FROM private_contacts WHERE numberKey = :key LIMIT 1")
    suspend fun findByKey(key: String): PrivateContact?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(contact: PrivateContact): Long

    @Update
    suspend fun update(contact: PrivateContact)

    @Delete
    suspend fun delete(contact: PrivateContact)
}

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules ORDER BY id DESC")
    fun observeAll(): Flow<List<Rule>>

    @Query("SELECT * FROM rules WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Rule?

    @Query("SELECT * FROM rules WHERE kind = :kind AND enabled = 1 ORDER BY id")
    suspend fun getEnabled(kind: Int): List<Rule>

    @Query("SELECT COUNT(*) FROM rules WHERE kind = :kind AND enabled = 1")
    fun observeActiveCount(kind: Int): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: Rule): Long

    @Delete
    suspend fun delete(rule: Rule)
}

@Dao
interface ArchivedCallDao {
    @Query("SELECT * FROM archived_calls ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<ArchivedCall>>

    @Query("SELECT COUNT(*) FROM archived_calls")
    fun observeCount(): Flow<Int>

    @Insert
    suspend fun insert(call: ArchivedCall): Long

    @Delete
    suspend fun delete(call: ArchivedCall)

    @Query("DELETE FROM archived_calls")
    suspend fun deleteAll()

    @Query("DELETE FROM archived_calls WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)
}

@Dao
interface ArchivedSmsDao {
    @Query("SELECT * FROM archived_sms ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<ArchivedSms>>

    @Query("SELECT COUNT(*) FROM archived_sms")
    fun observeCount(): Flow<Int>

    @Insert
    suspend fun insert(sms: ArchivedSms): Long

    @Delete
    suspend fun delete(sms: ArchivedSms)

    @Query("DELETE FROM archived_sms")
    suspend fun deleteAll()

    @Query("DELETE FROM archived_sms WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)
}
