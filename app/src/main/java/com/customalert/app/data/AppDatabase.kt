package com.customalert.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun fromSoundKind(value: SoundKind): String = value.name

    @TypeConverter
    fun toSoundKind(value: String): SoundKind = SoundKind.valueOf(value)

    @TypeConverter
    fun fromRuleScope(value: RuleScope): String = value.name

    @TypeConverter
    fun toRuleScope(value: String): RuleScope = RuleScope.valueOf(value)

    @TypeConverter
    fun fromMatchField(value: MatchField): String = value.name

    @TypeConverter
    fun toMatchField(value: String): MatchField = MatchField.valueOf(value)
}

@Database(
    entities = [SoundAsset::class, AppMapping::class, Rule::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun soundAssetDao(): SoundAssetDao
    abstract fun appMappingDao(): AppMappingDao
    abstract fun ruleDao(): RuleDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "custom_alert.db"
                ).build().also { instance = it }
            }
        }
    }
}
