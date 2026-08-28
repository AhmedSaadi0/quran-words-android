package com.example.domain.model

data class Surah(
    val id: Int,
    val nameAr: String,
    val nameEn: String,
    val ayahCount: Int,
    val revelationType: String,
    val juzStart: Int
)

data class Ayah(
    val id: Int,
    val surah: Int,
    val ayah: Int,
    val textUthmani: String,
    val textUthmaniPlain: String,
    val textImlaei: String,
    val wordCount: Int,
    val words: List<WordToken> = emptyList()
)

data class WordToken(
    val wordId: Int,
    val wordAyahId: Int,
    val position: Int,
    val text: String,
    val textClean: String,
    val translation: String,
    val rootId: Int? = null,
    val rootText: String? = null,
    val pos: String? = null,
    val posNameAr: String? = null,
    val form: String? = null,
    val formNameAr: String? = null,
    val aspect: String? = null,
    val mood: String? = null,
    val voice: String? = null,
    val person: String? = null,
    val gender: String? = null,
    val number: String? = null,
    val grammaticalCase: String? = null,
    val state: String? = null,
    val derivation: String? = null,
    val special: String? = null,
    val segments: String? = null
)

data class RootItem(
    val id: Int,
    val root: String,
    val glossAr: String? = null,
    val glossEn: String? = null,
    val aiSummary: String? = null,
    val masadirCount: Int = 0,
    val derivativesCount: Int = 0,
    val occurrencesCount: Int = 0
)

data class RootDetail(
    val item: RootItem,
    val aiSummary: String? = null,
    val aiModel: String? = null,
    val aiGeneratedAt: String? = null,
    val meanings: List<RootMeaningModel> = emptyList(),
    val masadir: List<MasdarModel> = emptyList(),
    val derivatives: List<DerivativeModel> = emptyList(),
    val words: List<WordToken> = emptyList(),
    val ayatOccurrences: List<AyahOccurrenceModel> = emptyList()
)

data class RootMeaningModel(
    val id: Int,
    val definition: String,
    val bookName: String,
    val sourceUrl: String? = null
)

data class MasdarModel(
    val id: Int,
    val form: String?,
    val masdarAr: String,
    val pattern: String?,
    val isAttested: Boolean,
    val source: String? = null
)

data class DerivativeModel(
    val id: Int,
    val formAr: String,
    val pattern: String,
    val derivativeType: String,
    val isQuranic: Boolean,
    val source: String? = null
)

data class AyahOccurrenceModel(
    val surahId: Int,
    val surahNameAr: String,
    val ayahNum: Int,
    val textUthmani: String,
    val matchedWordText: String
)

data class SearchResult(
    val roots: List<RootItem> = emptyList(),
    val masadir: List<MasdarModel> = emptyList(),
    val derivatives: List<DerivativeModel> = emptyList(),
    val words: List<WordToken> = emptyList(),
    val ayat: List<Ayah> = emptyList()
)
