package com.example.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "surahs")
data class SurahEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "name_ar") val nameAr: String,
    @ColumnInfo(name = "name_en") val nameEn: String,
    @ColumnInfo(name = "ayah_count") val ayahCount: Int,
    @ColumnInfo(name = "revelation_type") val revelationType: String,
    @ColumnInfo(name = "juz_start") val juzStart: Int
)

@Entity(
    tableName = "ayat",
    indices = [
        Index(value = ["surah"]),
        Index(value = ["surah", "ayah"], unique = true)
    ]
)
data class AyahEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val surah: Int,
    val ayah: Int,
    @ColumnInfo(name = "text_uthmani") val textUthmani: String?,
    @ColumnInfo(name = "text_uthmani_plain") val textUthmaniPlain: String?,
    @ColumnInfo(name = "text_imlaei") val textImlaei: String?,
    @ColumnInfo(name = "word_count") val wordCount: Int?
)

@Entity(
    tableName = "words",
    indices = [
        Index(value = ["text"]),
        Index(value = ["text_clean"]),
        Index(value = ["text_plain"])
    ]
)
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    @ColumnInfo(name = "text_clean") val textClean: String?,
    @ColumnInfo(name = "text_plain") val textPlain: String?,
    val translation: String?,
    val transliteration: String?,
    @ColumnInfo(name = "position_in_ayah") val positionInAyah: Int?
)

@Entity(
    tableName = "word_ayah",
    indices = [
        Index(value = ["word_id"]),
        Index(value = ["ayah_id"]),
        Index(value = ["location"])
    ]
)
data class WordAyahEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "word_id") val wordId: Int,
    @ColumnInfo(name = "ayah_id") val ayahId: Int,
    val position: Int,
    val location: String?
)

@Entity(
    tableName = "roots",
    indices = [Index(value = ["root"], unique = true)]
)
data class RootEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val root: String
)

@Entity(tableName = "word_morphology")
data class MorphologyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "word_ayah_id") val wordAyahId: Int,
    val pos: String?,
    val form: String?,
    val aspect: String?,
    val mood: String?,
    val voice: String?,
    val person: String?,
    val gender: String?,
    val number: String?,
    @ColumnInfo(name = "grammatical_case") val grammaticalCase: String?,
    val state: String?,
    val derivation: String?,
    val special: String?,
    @ColumnInfo(name = "root_id") val rootId: Int?,
    @ColumnInfo(name = "lemma_id") val lemmaId: Int?,
    val segments: String?
)

@Entity(
    tableName = "masadir",
    indices = [
        Index(value = ["root_id"]),
        Index(value = ["masdar_plain"])
    ]
)
data class MasdarEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "root_id") val rootId: Int,
    val root: String,
    val form: String?,
    @ColumnInfo(name = "lemma_id") val lemmaId: Int?,
    @ColumnInfo(name = "masdar_ar") val masdarAr: String,
    @ColumnInfo(name = "masdar_plain") val masdarPlain: String,
    val pattern: String?,
    @ColumnInfo(name = "is_attested") val isAttested: Int?,
    val source: String?,
    val confidence: Double?
)

@Entity(
    tableName = "derivatives",
    indices = [
        Index(value = ["root_id"]),
        Index(value = ["form_plain"])
    ]
)
data class DerivativeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "root_id") val rootId: Int,
    val root: String,
    val pattern: String,
    @ColumnInfo(name = "derivative_type") val derivativeType: String,
    @ColumnInfo(name = "form_ar") val formAr: String,
    @ColumnInfo(name = "form_plain") val formPlain: String,
    val pos: String?,
    @ColumnInfo(name = "is_quranic") val isQuranic: Int?,
    @ColumnInfo(name = "camel_valid") val camelValid: Int?,
    @ColumnInfo(name = "example_word_id") val exampleWordId: Int?,
    val source: String?
)

@Entity(tableName = "root_meanings")
data class RootMeaningEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "root_id") val rootId: Int,
    val definition: String?,
    @ColumnInfo(name = "book_name") val bookName: String?,
    @ColumnInfo(name = "source_url") val sourceUrl: String?
)

@Entity(tableName = "root_glosses")
data class RootGlossEntity(
    @PrimaryKey @ColumnInfo(name = "root_id") val rootId: Int,
    @ColumnInfo(name = "gloss_ar") val glossAr: String?,
    @ColumnInfo(name = "gloss_en") val glossEn: String?,
    @ColumnInfo(name = "ar_source") val arSource: String?,
    @ColumnInfo(name = "en_source") val enSource: String?
)

@Entity(tableName = "root_ai_summary")
data class RootAiSummaryEntity(
    @PrimaryKey @ColumnInfo(name = "root_id") val rootId: Int,
    @ColumnInfo(name = "summary_ar") val summaryAr: String?,
    val model: String?,
    @ColumnInfo(name = "generated_at") val generatedAt: String?
)
