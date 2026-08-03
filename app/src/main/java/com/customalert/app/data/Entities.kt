package com.customalert.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SoundKind {
    BUILTIN,
    CUSTOM
}

enum class RuleScope {
    GLOBAL,
    APP
}

enum class MatchField {
    TITLE,
    TEXT,
    BOTH
}

@Entity(tableName = "sound_assets")
data class SoundAsset(
    @PrimaryKey val id: String,
    val displayName: String,
    val kind: SoundKind,
    val rawName: String? = null,
    val filePath: String? = null
)

@Entity(tableName = "app_mappings")
data class AppMapping(
    @PrimaryKey val packageName: String,
    val label: String,
    val enabled: Boolean = true,
    val defaultSoundId: String? = null
)

@Entity(tableName = "rules")
data class Rule(
    @PrimaryKey val id: String,
    val scope: RuleScope,
    val packageName: String? = null,
    val name: String,
    val pattern: String,
    val matchField: MatchField = MatchField.BOTH,
    val soundId: String,
    val priority: Int = 0,
    val enabled: Boolean = true
)
