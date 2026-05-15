package com.imdinkie.voiceslip.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

const val CATEGORY_PERSONAL = "personal"
const val CATEGORY_WORK = "work"
const val CATEGORY_EMAIL = "email"
const val CATEGORY_OTHER = "other"

const val STYLE_RAW = "raw"
const val STYLE_CLEAN = "clean"
const val STYLE_CASUAL = "casual"
const val STYLE_FORMAL = "formal"
const val STYLE_EXCITED = "excited"
const val STYLE_VERY_CASUAL = "very_casual"

const val PROMPT_CLEANUP_POLICY = "cleanup_policy"
const val GROQ_WHISPER_PROMPT_LIMIT = 896

@Entity(tableName = "dictionary_entries")
data class DictionaryEntryEntity(
    @PrimaryKey val id: String,
    val phrase: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

@Entity(tableName = "history_items")
data class HistoryItemEntity(
    @PrimaryKey val id: String,
    val createdAtMillis: Long,
    val audioPath: String,
    val durationMillis: Long,
    val status: String,
    val transcript: String?,
    val rawTranscript: String?,
    val finalText: String?,
    val detectedLanguage: String?,
    val error: String?,
    val provider: String,
    val model: String,
    val pipelineMode: String,
    val transcriptionProvider: String?,
    val transcriptionModel: String?,
    val audioModelProvider: String?,
    val audioModel: String?,
    val postProcessingProvider: String?,
    val postProcessingModel: String?,
    val stylePreset: String,
    val pipelineSummary: String?,
    val errorStage: String?,
    val metadataJson: String?,
    val retryCount: Int,
    val targetPackage: String?,
    val targetAppLabel: String?,
    val resolvedCategoryId: String?,
    val resolvedCategoryName: String?,
    val resolvedStyleId: String?,
    val resolvedStyleName: String?,
    val stylePromptSnapshot: String?,
    val dictionarySnapshot: String?,
    val pipelineConfigSnapshot: String?,
    val dictionaryRoutingSnapshot: String?
)

@Entity(tableName = "pipeline_config")
data class PipelineConfigEntity(
    @PrimaryKey val id: String = "current",
    val mode: String,
    @ColumnInfo(defaultValue = "'BUILT_IN'")
    val transcriptionEngineKind: String,
    val transcriptionEngine: String,
    @ColumnInfo(defaultValue = "''")
    val mistralTranscriptionEngine: String,
    @ColumnInfo(defaultValue = "''")
    val groqTranscriptionEngine: String,
    @ColumnInfo(defaultValue = "''")
    val openRouterAudioTranscriptionModel: String,
    @ColumnInfo(defaultValue = "'BUILT_IN'")
    val audioDirectEngineKind: String,
    val audioDirectEngine: String,
    @ColumnInfo(defaultValue = "''")
    val mistralAudioDirectEngine: String,
    val postProcessingProvider: String,
    val postProcessingModel: String,
    @ColumnInfo(defaultValue = "''")
    val groqPostProcessingModel: String,
    @ColumnInfo(defaultValue = "''")
    val openRouterPostProcessingModel: String,
    @ColumnInfo(defaultValue = "''")
    val cerebrasPostProcessingModel: String,
    @ColumnInfo(defaultValue = "''")
    val openRouterAudioDirectModel: String
)

@Entity(tableName = "styles")
data class StyleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val basePresetId: String?,
    val defaultPrompt: String,
    val userPromptOverride: String?,
    val isBuiltIn: Boolean,
    val createdAtMillis: Long
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val styleId: String,
    val isPreset: Boolean,
    val createdAtMillis: Long
)

@Entity(tableName = "app_assignments")
data class AppAssignmentEntity(
    @PrimaryKey val packageName: String,
    val categoryId: String?,
    val explicitlyUnassigned: Boolean,
    val updatedAtMillis: Long
)

@Entity(tableName = "installed_apps")
data class InstalledAppCacheEntity(
    @PrimaryKey val packageName: String,
    val label: String,
    val iconCacheKey: String,
    val lastSeenAtMillis: Long?,
    val updatedAtMillis: Long
)

@Entity(tableName = "engine_dictionary_routing")
data class EngineDictionaryRoutingEntity(
    @PrimaryKey val engineId: String,
    val sendDictionaryToTranscription: Boolean
)

@Entity(tableName = "prompt_settings")
data class PromptSettingEntity(
    @PrimaryKey val id: String,
    val defaultPrompt: String,
    val userPromptOverride: String?
)

@Dao
interface VoiceSlipDao {
    @Query("SELECT * FROM dictionary_entries ORDER BY lower(phrase)")
    fun listDictionary(): List<DictionaryEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertDictionary(entry: DictionaryEntryEntity)

    @Query("DELETE FROM dictionary_entries WHERE id = :id")
    fun deleteDictionary(id: String)

    @Query("SELECT * FROM history_items ORDER BY createdAtMillis DESC")
    fun listHistory(): List<HistoryItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertHistory(item: HistoryItemEntity)

    @Query("DELETE FROM history_items WHERE id = :id")
    fun deleteHistory(id: String)

    @Query("DELETE FROM history_items")
    fun clearHistory()

    @Query("SELECT * FROM pipeline_config WHERE id = 'current'")
    fun getPipelineConfig(): PipelineConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertPipelineConfig(config: PipelineConfigEntity)

    @Query("SELECT * FROM styles ORDER BY isBuiltIn DESC, CASE id WHEN 'very_casual' THEN 0 WHEN 'casual' THEN 1 WHEN 'formal' THEN 2 WHEN 'excited' THEN 3 ELSE 4 END, lower(name)")
    fun listStyles(): List<StyleEntity>

    @Query("SELECT * FROM styles WHERE id = :id")
    fun getStyle(id: String): StyleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertStyle(style: StyleEntity)

    @Query("DELETE FROM styles WHERE id = :id AND isBuiltIn = 0")
    fun deleteCustomStyle(id: String)

    @Query("DELETE FROM styles WHERE id IN (:ids) AND isBuiltIn = 1")
    fun deleteBuiltInStyles(ids: List<String>)

    @Query("SELECT * FROM categories ORDER BY isPreset DESC, CASE id WHEN 'personal' THEN 0 WHEN 'work' THEN 1 WHEN 'email' THEN 2 WHEN 'other' THEN 3 ELSE 4 END, lower(name)")
    fun listCategories(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    fun getCategory(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id AND isPreset = 0")
    fun deleteCustomCategory(id: String)

    @Query("SELECT * FROM app_assignments WHERE packageName = :packageName")
    fun getAssignment(packageName: String): AppAssignmentEntity?

    @Query("SELECT * FROM app_assignments")
    fun listAssignments(): List<AppAssignmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAssignment(assignment: AppAssignmentEntity)

    @Query("DELETE FROM app_assignments WHERE packageName = :packageName")
    fun deleteAssignment(packageName: String)

    @Query("SELECT * FROM installed_apps ORDER BY CASE WHEN lastSeenAtMillis IS NULL THEN 1 ELSE 0 END, lastSeenAtMillis DESC, lower(label)")
    fun listInstalledApps(): List<InstalledAppCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertInstalledApps(apps: List<InstalledAppCacheEntity>)

    @Query("UPDATE installed_apps SET lastSeenAtMillis = :seenAtMillis WHERE packageName = :packageName")
    fun updateRecentApp(packageName: String, seenAtMillis: Long)

    @Query("SELECT * FROM engine_dictionary_routing WHERE engineId = :engineId")
    fun getRouting(engineId: String): EngineDictionaryRoutingEntity?

    @Query("SELECT * FROM engine_dictionary_routing")
    fun listRouting(): List<EngineDictionaryRoutingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertRouting(routing: EngineDictionaryRoutingEntity)

    @Query("SELECT * FROM prompt_settings WHERE id = :id")
    fun getPromptSetting(id: String): PromptSettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertPromptSetting(setting: PromptSettingEntity)
}

@Database(
    entities = [
        DictionaryEntryEntity::class,
        HistoryItemEntity::class,
        PipelineConfigEntity::class,
        StyleEntity::class,
        CategoryEntity::class,
        AppAssignmentEntity::class,
        InstalledAppCacheEntity::class,
        EngineDictionaryRoutingEntity::class,
        PromptSettingEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class VoiceSlipDatabase : RoomDatabase() {
    abstract fun dao(): VoiceSlipDao

    companion object {
        @Volatile private var instance: VoiceSlipDatabase? = null

        fun get(context: Context): VoiceSlipDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    VoiceSlipDatabase::class.java,
                    "voiceslip_v3.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_5, MIGRATION_4_5, MIGRATION_5_6)
                    .allowMainThreadQueries()
                    .build()
                    .also { instance = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pipeline_config ADD COLUMN transcriptionEngineKind TEXT NOT NULL DEFAULT 'BUILT_IN'")
                db.execSQL("ALTER TABLE pipeline_config ADD COLUMN openRouterAudioTranscriptionModel TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE pipeline_config ADD COLUMN audioDirectEngineKind TEXT NOT NULL DEFAULT 'BUILT_IN'")
                db.execSQL("ALTER TABLE pipeline_config ADD COLUMN groqPostProcessingModel TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE pipeline_config ADD COLUMN openRouterPostProcessingModel TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE pipeline_config ADD COLUMN openRouterAudioDirectModel TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE pipeline_config SET groqPostProcessingModel = postProcessingModel WHERE postProcessingProvider = 'GROQ'")
                db.execSQL("UPDATE pipeline_config SET openRouterPostProcessingModel = postProcessingModel WHERE postProcessingProvider = 'OPENROUTER'")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pipeline_config ADD COLUMN mistralTranscriptionEngine TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE pipeline_config ADD COLUMN groqTranscriptionEngine TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE pipeline_config ADD COLUMN mistralAudioDirectEngine TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    """
                    UPDATE pipeline_config
                    SET mistralTranscriptionEngine = transcriptionEngine
                    WHERE transcriptionEngineKind = 'BUILT_IN'
                    AND transcriptionEngine IN (
                        'MISTRAL_VOXTRAL_MINI_TRANSCRIBE',
                        'MISTRAL_VOXTRAL_MINI_AUDIO',
                        'MISTRAL_VOXTRAL_SMALL_AUDIO'
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    UPDATE pipeline_config
                    SET groqTranscriptionEngine = transcriptionEngine
                    WHERE transcriptionEngineKind = 'BUILT_IN'
                    AND transcriptionEngine IN (
                        'GROQ_WHISPER_LARGE_V3',
                        'GROQ_WHISPER_LARGE_V3_TURBO'
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    UPDATE pipeline_config
                    SET mistralAudioDirectEngine = audioDirectEngine
                    WHERE audioDirectEngineKind = 'BUILT_IN'
                    AND audioDirectEngine IN (
                        'MISTRAL_VOXTRAL_MINI_AUDIO',
                        'MISTRAL_VOXTRAL_SMALL_AUDIO'
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_5 = object : Migration(3, 5) {
            override fun migrate(db: SupportSQLiteDatabase) = Unit
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pipeline_config RENAME TO pipeline_config_old")
                db.execSQL(
                    """
                    CREATE TABLE pipeline_config (
                        id TEXT NOT NULL,
                        mode TEXT NOT NULL,
                        transcriptionEngineKind TEXT NOT NULL DEFAULT 'BUILT_IN',
                        transcriptionEngine TEXT NOT NULL,
                        mistralTranscriptionEngine TEXT NOT NULL DEFAULT '',
                        groqTranscriptionEngine TEXT NOT NULL DEFAULT '',
                        openRouterAudioTranscriptionModel TEXT NOT NULL DEFAULT '',
                        audioDirectEngineKind TEXT NOT NULL DEFAULT 'BUILT_IN',
                        audioDirectEngine TEXT NOT NULL,
                        mistralAudioDirectEngine TEXT NOT NULL DEFAULT '',
                        postProcessingProvider TEXT NOT NULL,
                        postProcessingModel TEXT NOT NULL,
                        groqPostProcessingModel TEXT NOT NULL DEFAULT '',
                        openRouterPostProcessingModel TEXT NOT NULL DEFAULT '',
                        openRouterAudioDirectModel TEXT NOT NULL DEFAULT '',
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO pipeline_config (
                        id,
                        mode,
                        transcriptionEngineKind,
                        transcriptionEngine,
                        mistralTranscriptionEngine,
                        groqTranscriptionEngine,
                        openRouterAudioTranscriptionModel,
                        audioDirectEngineKind,
                        audioDirectEngine,
                        mistralAudioDirectEngine,
                        postProcessingProvider,
                        postProcessingModel,
                        groqPostProcessingModel,
                        openRouterPostProcessingModel,
                        openRouterAudioDirectModel
                    )
                    SELECT
                        id,
                        mode,
                        transcriptionEngineKind,
                        transcriptionEngine,
                        mistralTranscriptionEngine,
                        groqTranscriptionEngine,
                        openRouterAudioTranscriptionModel,
                        audioDirectEngineKind,
                        audioDirectEngine,
                        mistralAudioDirectEngine,
                        postProcessingProvider,
                        postProcessingModel,
                        groqPostProcessingModel,
                        openRouterPostProcessingModel,
                        openRouterAudioDirectModel
                    FROM pipeline_config_old
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE pipeline_config_old")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pipeline_config ADD COLUMN cerebrasPostProcessingModel TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
