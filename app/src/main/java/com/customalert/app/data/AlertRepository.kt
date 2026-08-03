package com.customalert.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class AlertRepository(
    private val context: Context,
    private val db: AppDatabase,
    val preferences: UserPreferences
) {
    private val soundDao = db.soundAssetDao()
    private val appDao = db.appMappingDao()
    private val ruleDao = db.ruleDao()

    fun observeSounds(): Flow<List<SoundAsset>> = soundDao.observeAll()
    fun observeApps(): Flow<List<AppMapping>> = appDao.observeAll()
    fun observeGlobalRules(): Flow<List<Rule>> = ruleDao.observeGlobal()
    fun observeAppRules(packageName: String): Flow<List<Rule>> = ruleDao.observeForApp(packageName)
    fun observeApp(packageName: String): Flow<AppMapping?> = appDao.observeByPackage(packageName)

    suspend fun getSound(id: String): SoundAsset? = soundDao.getById(id)

    suspend fun getEnabledRulesForPackage(packageName: String): List<Rule> =
        ruleDao.getEnabledMatchingCandidates(packageName)

    suspend fun getAppMapping(packageName: String): AppMapping? = appDao.getByPackage(packageName)

    suspend fun upsertApp(mapping: AppMapping) = appDao.upsert(mapping)

    suspend fun ensureAppMapping(packageName: String, label: String): AppMapping {
        val existing = appDao.getByPackage(packageName)
        if (existing != null) return existing
        val created = AppMapping(packageName = packageName, label = label, enabled = true)
        appDao.upsert(created)
        return created
    }

    suspend fun setAppEnabled(packageName: String, label: String, enabled: Boolean) {
        val current = appDao.getByPackage(packageName)
            ?: AppMapping(packageName = packageName, label = label, enabled = enabled)
        appDao.upsert(current.copy(label = label, enabled = enabled))
    }

    suspend fun setAppDefaultSound(packageName: String, label: String, soundId: String?) {
        val current = appDao.getByPackage(packageName)
            ?: AppMapping(packageName = packageName, label = label)
        appDao.upsert(current.copy(label = label, defaultSoundId = soundId))
    }

    suspend fun getRule(id: String): Rule? = ruleDao.getById(id)

    suspend fun saveRule(rule: Rule) = ruleDao.upsert(rule)

    suspend fun deleteRule(id: String) = ruleDao.deleteById(id)

    suspend fun nextPriority(packageName: String?): Int = ruleDao.maxPriority(packageName) + 1

    suspend fun reorderRules(rules: List<Rule>) {
        rules.forEachIndexed { index, rule ->
            ruleDao.upsert(rule.copy(priority = index))
        }
    }

    suspend fun importSound(uri: Uri): SoundAsset = withContext(Dispatchers.IO) {
        val displayName = queryDisplayName(uri) ?: "Custom sound"
        val extension = displayName.substringAfterLast('.', "ogg").lowercase()
        require(extension in SUPPORTED_EXTENSIONS) {
            "Unsupported audio type: .$extension"
        }

        val id = "custom_${UUID.randomUUID()}"
        val dir = File(context.filesDir, "sounds").apply { mkdirs() }
        val dest = File(dir, "$id.$extension")
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Unable to read selected file")

        val asset = SoundAsset(
            id = id,
            displayName = displayName.substringBeforeLast('.').ifBlank { displayName },
            kind = SoundKind.CUSTOM,
            filePath = dest.absolutePath
        )
        soundDao.upsert(asset)
        asset
    }

    suspend fun deleteCustomSound(id: String) = withContext(Dispatchers.IO) {
        val asset = soundDao.getById(id) ?: return@withContext
        if (asset.kind != SoundKind.CUSTOM) return@withContext
        asset.filePath?.let { File(it).delete() }
        soundDao.deleteById(id)
    }

    suspend fun ensureBuiltinSounds() {
        soundDao.insertIgnore(BuiltinSounds.all)
    }

    private fun queryDisplayName(uri: Uri): String? {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                return cursor.getString(index)
            }
        }
        return uri.lastPathSegment
    }

    companion object {
        private val SUPPORTED_EXTENSIONS = setOf("ogg", "mp3", "wav", "m4a", "aac")
    }
}
