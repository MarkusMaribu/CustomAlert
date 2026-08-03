package com.customalert.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SoundAssetDao {
    @Query("SELECT * FROM sound_assets ORDER BY kind ASC, displayName ASC")
    fun observeAll(): Flow<List<SoundAsset>>

    @Query("SELECT * FROM sound_assets ORDER BY kind ASC, displayName ASC")
    suspend fun getAll(): List<SoundAsset>

    @Query("SELECT * FROM sound_assets WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SoundAsset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(asset: SoundAsset)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(assets: List<SoundAsset>)

    @Query("DELETE FROM sound_assets WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface AppMappingDao {
    @Query("SELECT * FROM app_mappings ORDER BY label COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<AppMapping>>

    @Query("SELECT * FROM app_mappings WHERE packageName = :packageName LIMIT 1")
    suspend fun getByPackage(packageName: String): AppMapping?

    @Query("SELECT * FROM app_mappings WHERE packageName = :packageName LIMIT 1")
    fun observeByPackage(packageName: String): Flow<AppMapping?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mapping: AppMapping)

    @Query("DELETE FROM app_mappings WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules ORDER BY priority ASC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Rule>>

    @Query("SELECT * FROM rules WHERE scope = 'GLOBAL' ORDER BY priority ASC, name COLLATE NOCASE ASC")
    fun observeGlobal(): Flow<List<Rule>>

    @Query(
        "SELECT * FROM rules WHERE scope = 'APP' AND packageName = :packageName " +
            "ORDER BY priority ASC, name COLLATE NOCASE ASC"
    )
    fun observeForApp(packageName: String): Flow<List<Rule>>

    @Query(
        "SELECT * FROM rules WHERE enabled = 1 AND (" +
            "(scope = 'APP' AND packageName = :packageName) OR scope = 'GLOBAL'" +
            ") ORDER BY " +
            "CASE WHEN scope = 'APP' THEN 0 ELSE 1 END, priority ASC"
    )
    suspend fun getEnabledMatchingCandidates(packageName: String): List<Rule>

    @Query("SELECT * FROM rules WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Rule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: Rule)

    @Update
    suspend fun update(rule: Rule)

    @Query("DELETE FROM rules WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query(
        "SELECT COALESCE(MAX(priority), -1) FROM rules WHERE " +
            "(scope = 'GLOBAL' AND :packageName IS NULL) OR " +
            "(scope = 'APP' AND packageName = :packageName)"
    )
    suspend fun maxPriority(packageName: String?): Int
}
