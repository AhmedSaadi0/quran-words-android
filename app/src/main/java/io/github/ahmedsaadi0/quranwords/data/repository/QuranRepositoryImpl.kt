package io.github.ahmedsaadi0.quranwords.data.repository

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import io.github.ahmedsaadi0.quranwords.data.remote.DatabaseDownloadManager
import io.github.ahmedsaadi0.quranwords.data.util.ArabicNormalizer
import io.github.ahmedsaadi0.quranwords.data.util.QuranMetaConstants
import io.github.ahmedsaadi0.quranwords.di.IoDispatcher
import io.github.ahmedsaadi0.quranwords.domain.model.Ayah
import io.github.ahmedsaadi0.quranwords.domain.model.AyahOccurrenceModel
import io.github.ahmedsaadi0.quranwords.domain.model.DerivativeModel
import io.github.ahmedsaadi0.quranwords.domain.model.MasdarModel
import io.github.ahmedsaadi0.quranwords.domain.model.RootDetail
import io.github.ahmedsaadi0.quranwords.domain.model.RootItem
import io.github.ahmedsaadi0.quranwords.domain.model.RootMeaningModel
import io.github.ahmedsaadi0.quranwords.domain.model.SearchResult
import io.github.ahmedsaadi0.quranwords.domain.model.Surah
import io.github.ahmedsaadi0.quranwords.domain.model.WordToken
import io.github.ahmedsaadi0.quranwords.domain.repository.QuranRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

class QuranRepositoryImpl @Inject constructor(
    private val context: Context,
    private val downloadManager: DatabaseDownloadManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : QuranRepository {

    @Volatile
    private var sqliteDb: SQLiteDatabase? = null

    private fun getDb(): SQLiteDatabase? {
        val file = downloadManager.getDatabaseFile()
        if (!file.exists() || file.length() < 10_000_000L) {
            return null
        }
        if (sqliteDb?.isOpen == true) {
            return sqliteDb
        }
        return synchronized(this) {
            if (sqliteDb?.isOpen == true) return sqliteDb
            try {
                sqliteDb = SQLiteDatabase.openDatabase(
                    file.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS
                )
                sqliteDb
            } catch (e: Exception) {
                null
            }
        }
    }

    fun closeDb() {
        synchronized(this) {
            try {
                sqliteDb?.close()
            } catch (_: Exception) {
            }
            sqliteDb = null
        }
        try {
            io.github.ahmedsaadi0.quranwords.data.local.QuranDatabase.closeIfNeeded()
        } catch (_: Exception) {
        }
    }

    override fun isDatabaseReady(): Boolean = downloadManager.isDatabaseReady()

    override fun getSurahs(): Flow<List<Surah>> = flow {
        val db = getDb()
        if (db != null) {
            val list = mutableListOf<Surah>()
            try {
                val cursor = db.rawQuery(
                    "SELECT id, name_ar, name_en, ayah_count, revelation_type, juz_start FROM surahs ORDER BY id ASC",
                    null
                )
                cursor.use {
                    while (it.moveToNext()) {
                        list.add(
                            Surah(
                                id = it.getInt(0),
                                nameAr = it.getString(1) ?: "",
                                nameEn = it.getString(2) ?: "",
                                ayahCount = it.getInt(3),
                                revelationType = it.getString(4) ?: "",
                                juzStart = it.getInt(5)
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Fallback to meta
            }
            if (list.isNotEmpty()) {
                emit(list)
                return@flow
            }
        }

        // Fallback Surahs
        val fallback = QuranMetaConstants.SURAHS.map {
            Surah(it.id, it.nameAr, it.nameEn, it.ayahCount, it.revelationType, it.juzStart)
        }
        emit(fallback)
    }.flowOn(ioDispatcher)

    override suspend fun getSurahById(id: Int): Surah? = withContext(ioDispatcher) {
        val db = getDb()
        if (db != null) {
            try {
                val cursor = db.rawQuery(
                    "SELECT id, name_ar, name_en, ayah_count, revelation_type, juz_start FROM surahs WHERE id = ? LIMIT 1",
                    arrayOf(id.toString())
                )
                cursor.use {
                    if (it.moveToNext()) {
                        return@withContext Surah(
                            id = it.getInt(0),
                            nameAr = it.getString(1) ?: "",
                            nameEn = it.getString(2) ?: "",
                            ayahCount = it.getInt(3),
                            revelationType = it.getString(4) ?: "",
                            juzStart = it.getInt(5)
                        )
                    }
                }
            } catch (e: Exception) {
                // fallback
            }
        }
        QuranMetaConstants.SURAHS.firstOrNull { it.id == id }?.let {
            Surah(it.id, it.nameAr, it.nameEn, it.ayahCount, it.revelationType, it.juzStart)
        }
    }

    override fun getAyatBySurah(surahId: Int): Flow<List<Ayah>> = flow {
        val db = getDb()
        if (db != null) {
            val ayatList = mutableListOf<Ayah>()
            try {
                val cursor = db.rawQuery(
                    "SELECT id, surah, ayah, text_uthmani, text_uthmani_plain, text_imlaei, word_count, juz, hizb, rub_el_hizb, page_number FROM ayat WHERE surah = ? ORDER BY ayah ASC",
                    arrayOf(surahId.toString())
                )
                cursor.use { c ->
                    while (c.moveToNext()) {
                        val ayahId = c.getInt(0)
                        val ayahNum = c.getInt(2)
                        val textUth = c.getString(3) ?: ""
                        val textUthPlain = c.getString(4) ?: ""
                        val textIml = c.getString(5) ?: ""
                        val wCount = c.getInt(6)

                        val words = loadWordsForAyah(db, ayahId)
                        ayatList.add(
                            Ayah(
                                id = ayahId,
                                surah = surahId,
                                ayah = ayahNum,
                                textUthmani = textUth,
                                textUthmaniPlain = textUthPlain,
                                textImlaei = textIml,
                                wordCount = wCount,
                                words = words,
                                juz = if (c.isNull(7)) null else c.getInt(7),
                                hizb = if (c.isNull(8)) null else c.getInt(8),
                                rubElHizb = if (c.isNull(9)) null else c.getInt(9),
                                pageNumber = if (c.isNull(10)) null else c.getInt(10)
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Fallback
            }
            if (ayatList.isNotEmpty()) {
                emit(ayatList)
                return@flow
            } else {
                // No seed fallback - emit empty so UI shows download prompt smoothly without stutter
                emit(emptyList())
                return@flow
            }
        }

        // No DB - emit empty, UI will show download required (no preview to avoid stutter)
        emit(emptyList())
    }.flowOn(ioDispatcher)

    override suspend fun getAyatBySurahPaged(surahId: Int, limit: Int, offset: Int): List<Ayah> = withContext(ioDispatcher) {
        val db = getDb() ?: return@withContext emptyList()
        val list = mutableListOf<Ayah>()
        try {
            val cursor = db.rawQuery(
                "SELECT id, surah, ayah, text_uthmani, text_uthmani_plain, text_imlaei, word_count, juz, hizb, rub_el_hizb, page_number FROM ayat WHERE surah = ? ORDER BY ayah ASC LIMIT ? OFFSET ?",
                arrayOf(surahId.toString(), limit.toString(), offset.toString())
            )
            cursor.use { c ->
                while (c.moveToNext()) {
                    val ayahId = c.getInt(0)
                    val ayahNum = c.getInt(2)
                    val textUth = c.getString(3) ?: ""
                    val textUthPlain = c.getString(4) ?: ""
                    val textIml = c.getString(5) ?: ""
                    val wCount = c.getInt(6)
                    val words = loadWordsForAyah(db, ayahId)
                    list.add(
                        Ayah(
                            id = ayahId,
                            surah = surahId,
                            ayah = ayahNum,
                            textUthmani = textUth,
                            textUthmaniPlain = textUthPlain,
                            textImlaei = textIml,
                            wordCount = wCount,
                            words = words,
                            juz = if (c.isNull(7)) null else c.getInt(7),
                            hizb = if (c.isNull(8)) null else c.getInt(8),
                            rubElHizb = if (c.isNull(9)) null else c.getInt(9),
                            pageNumber = if (c.isNull(10)) null else c.getInt(10)
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }
        return@withContext list
    }

    private fun loadWordsForAyah(db: SQLiteDatabase, ayahId: Int): List<WordToken> {
        val words = mutableListOf<WordToken>()
        val sql = """
            SELECT 
                w.id,
                wa.id,
                wa.position,
                w.text,
                w.text_clean,
                w.translation,
                wm.root_id,
                r.root,
                wm.pos,
                wm.form,
                wm.aspect,
                wm.mood,
                wm.voice,
                wm.person,
                wm.gender,
                wm.number,
                wm.grammatical_case,
                wm.state,
                wm.derivation,
                wm.special,
                wm.segments
            FROM word_ayah wa
            JOIN words w ON w.id = wa.word_id
            LEFT JOIN word_morphology wm ON wm.word_ayah_id = wa.id
            LEFT JOIN roots r ON r.id = wm.root_id
            WHERE wa.ayah_id = ?
            ORDER BY wa.position ASC
        """.trimIndent()

        val cursor = db.rawQuery(sql, arrayOf(ayahId.toString()))
        cursor.use { c ->
            while (c.moveToNext()) {
                val posCode = c.getString(8)
                val formCode = c.getString(9)
                val posAr = QuranMetaConstants.POS_MAP[posCode] ?: posCode
                val formAr = QuranMetaConstants.FORMS_MAP[formCode] ?: formCode

                words.add(
                    WordToken(
                        wordId = c.getInt(0),
                        wordAyahId = c.getInt(1),
                        position = c.getInt(2),
                        text = c.getString(3) ?: "",
                        textClean = c.getString(4) ?: "",
                        translation = c.getString(5) ?: "",
                        rootId = if (c.isNull(6)) null else c.getInt(6),
                        rootText = c.getString(7),
                        pos = posCode,
                        posNameAr = posAr,
                        form = formCode,
                        formNameAr = formAr,
                        aspect = c.getString(10),
                        mood = c.getString(11),
                        voice = c.getString(12),
                        person = c.getString(13),
                        gender = c.getString(14),
                        number = c.getString(15),
                        grammaticalCase = c.getString(16),
                        state = c.getString(17),
                        derivation = c.getString(18),
                        special = c.getString(19),
                        segments = c.getString(20)
                    )
                )
            }
        }
        return words
    }

    override suspend fun getAyahWithWords(surahId: Int, ayahNum: Int): Ayah? = withContext(ioDispatcher) {
        val db = getDb()
        if (db != null) {
            try {
                val cursor = db.rawQuery(
                    "SELECT id, surah, ayah, text_uthmani, text_uthmani_plain, text_imlaei, word_count, juz, hizb, rub_el_hizb, page_number FROM ayat WHERE surah = ? AND ayah = ? LIMIT 1",
                    arrayOf(surahId.toString(), ayahNum.toString())
                )
                cursor.use { c ->
                    if (c.moveToNext()) {
                        val ayahId = c.getInt(0)
                        val words = loadWordsForAyah(db, ayahId)
                        return@withContext Ayah(
                            id = ayahId,
                            surah = surahId,
                            ayah = ayahNum,
                            textUthmani = c.getString(3) ?: "",
                            textUthmaniPlain = c.getString(4) ?: "",
                            textImlaei = c.getString(5) ?: "",
                            wordCount = c.getInt(6),
                            words = words,
                            juz = if (c.isNull(7)) null else c.getInt(7),
                            hizb = if (c.isNull(8)) null else c.getInt(8),
                            rubElHizb = if (c.isNull(9)) null else c.getInt(9),
                            pageNumber = if (c.isNull(10)) null else c.getInt(10)
                        )
                    }
                }
            } catch (e: Exception) {
                // fallback
            }
        }
        getSeedAyat(surahId).firstOrNull { it.ayah == ayahNum }
    }

    override suspend fun getRootsPaged(limit: Int, offset: Int): List<RootItem> = withContext(ioDispatcher) {
        val db = getDb()
        if (db != null) {
            val list = mutableListOf<RootItem>()
            try {
                val sql = """
                    SELECT 
                        r.id, 
                        r.root, 
                        rg.gloss_ar, 
                        rg.gloss_en, 
                        ra.summary_ar,
                        (SELECT COUNT(*) FROM masadir m WHERE m.root_id = r.id),
                        (SELECT COUNT(*) FROM derivatives d WHERE d.root_id = r.id),
                        (SELECT COUNT(DISTINCT wa.ayah_id) FROM word_morphology wm JOIN word_ayah wa ON wa.id = wm.word_ayah_id WHERE wm.root_id = r.id)
                    FROM roots r
                    LEFT JOIN root_glosses rg ON rg.root_id = r.id
                    LEFT JOIN root_ai_summary ra ON ra.root_id = r.id
                    ORDER BY r.id ASC
                    LIMIT ? OFFSET ?
                """.trimIndent()

                val cursor = db.rawQuery(sql, arrayOf(limit.toString(), offset.toString()))
                cursor.use { c ->
                    while (c.moveToNext()) {
                        list.add(
                            RootItem(
                                id = c.getInt(0),
                                root = c.getString(1) ?: "",
                                glossAr = c.getString(2),
                                glossEn = c.getString(3),
                                aiSummary = c.getString(4),
                                masadirCount = c.getInt(5),
                                derivativesCount = c.getInt(6),
                                occurrencesCount = c.getInt(7)
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // fallback
            }
            if (list.isNotEmpty()) return@withContext list
        }

        getSeedRoots().drop(offset).take(limit)
    }

    override suspend fun getRootDetail(rootId: Int): RootDetail? = withContext(ioDispatcher) {
        val db = getDb()
        if (db != null) {
            try {
                var rootText = ""
                var glossAr: String? = null
                var glossEn: String? = null
                var summaryAr: String? = null
                var summaryModel: String? = null
                var summaryGeneratedAt: String? = null

                val rCursor = db.rawQuery(
                    """
                    SELECT r.id, r.root, rg.gloss_ar, rg.gloss_en, ra.summary_ar, ra.model, ra.generated_at
                    FROM roots r 
                    LEFT JOIN root_glosses rg ON rg.root_id = r.id
                    LEFT JOIN root_ai_summary ra ON ra.root_id = r.id
                    WHERE r.id = ? LIMIT 1
                    """.trimIndent(),
                    arrayOf(rootId.toString())
                )
                rCursor.use {
                    if (it.moveToNext()) {
                        rootText = it.getString(1) ?: ""
                        glossAr = it.getString(2)
                        glossEn = it.getString(3)
                        summaryAr = it.getString(4)
                        summaryModel = it.getString(5)
                        summaryGeneratedAt = it.getString(6)
                    }
                }

                if (rootText.isNotBlank()) {
                    // Meanings
                    val meanings = mutableListOf<RootMeaningModel>()
                    val mCursor = db.rawQuery(
                        "SELECT id, definition, book_name, source_url FROM root_meanings WHERE root_id = ? ORDER BY id ASC",
                        arrayOf(rootId.toString())
                    )
                    mCursor.use {
                        while (it.moveToNext()) {
                            meanings.add(
                                RootMeaningModel(
                                    id = it.getInt(0),
                                    definition = it.getString(1) ?: "",
                                    bookName = it.getString(2) ?: "",
                                    sourceUrl = it.getString(3)
                                )
                            )
                        }
                    }

                    // Masadir
                    val masadir = mutableListOf<MasdarModel>()
                    val masdarCursor = db.rawQuery(
                        "SELECT id, form, masdar_ar, pattern, is_attested, source FROM masadir WHERE root_id = ? ORDER BY id ASC",
                        arrayOf(rootId.toString())
                    )
                    masdarCursor.use {
                        while (it.moveToNext()) {
                            masadir.add(
                                MasdarModel(
                                    id = it.getInt(0),
                                    form = it.getString(1),
                                    masdarAr = it.getString(2) ?: "",
                                    pattern = it.getString(3),
                                    isAttested = it.getInt(4) == 1,
                                    source = it.getString(5)
                                )
                            )
                        }
                    }

                    // Derivatives
                    val derivatives = mutableListOf<DerivativeModel>()
                    val derivCursor = db.rawQuery(
                        "SELECT id, form_ar, pattern, derivative_type, is_quranic, source FROM derivatives WHERE root_id = ? ORDER BY id ASC",
                        arrayOf(rootId.toString())
                    )
                    derivCursor.use {
                        while (it.moveToNext()) {
                            derivatives.add(
                                DerivativeModel(
                                    id = it.getInt(0),
                                    formAr = it.getString(1) ?: "",
                                    pattern = it.getString(2) ?: "",
                                    derivativeType = it.getString(3) ?: "",
                                    isQuranic = it.getInt(4) == 1,
                                    source = it.getString(5)
                                )
                            )
                        }
                    }

                    // Ayat occurrences — first page only (30) with proper pagination support
                    val occurrencesPageSize = 30
                    val occurrences = mutableListOf<AyahOccurrenceModel>()
                    val occSql = """
                        SELECT a.surah, s.name_ar, a.ayah, a.text_uthmani, REPLACE(GROUP_CONCAT(DISTINCT w.text), ',', '، ')
                        FROM word_morphology wm
                        JOIN word_ayah wa ON wa.id = wm.word_ayah_id
                        JOIN ayat a ON a.id = wa.ayah_id
                        JOIN surahs s ON s.id = a.surah
                        JOIN words w ON w.id = wa.word_id
                        WHERE wm.root_id = ?
                        GROUP BY a.id
                        ORDER BY a.surah ASC, a.ayah ASC
                        LIMIT ? OFFSET ?
                    """.trimIndent()
                    val occCursor = db.rawQuery(occSql, arrayOf(rootId.toString(), occurrencesPageSize.toString(), "0"))
                    occCursor.use {
                        while (it.moveToNext()) {
                            occurrences.add(
                                AyahOccurrenceModel(
                                    surahId = it.getInt(0),
                                    surahNameAr = it.getString(1) ?: "",
                                    ayahNum = it.getInt(2),
                                    textUthmani = it.getString(3) ?: "",
                                    matchedWordText = it.getString(4) ?: ""
                                )
                            )
                        }
                    }
                    // Total count for this root (not limited) — distinct ayat count
                    var totalOccurrences = occurrences.size
                    try {
                        val countCursor = db.rawQuery(
                            "SELECT COUNT(DISTINCT wa.ayah_id) FROM word_morphology wm JOIN word_ayah wa ON wa.id = wm.word_ayah_id WHERE wm.root_id = ?",
                            arrayOf(rootId.toString())
                        )
                        countCursor.use {
                            if (it.moveToNext()) totalOccurrences = it.getInt(0)
                        }
                    } catch (_: Exception) {}

                    val rootItem = RootItem(
                        id = rootId,
                        root = rootText,
                        glossAr = glossAr,
                        glossEn = glossEn,
                        aiSummary = summaryAr,
                        masadirCount = masadir.size,
                        derivativesCount = derivatives.size,
                        occurrencesCount = totalOccurrences
                    )

                    return@withContext RootDetail(
                        item = rootItem,
                        aiSummary = summaryAr,
                        aiModel = summaryModel,
                        aiGeneratedAt = summaryGeneratedAt,
                        meanings = meanings,
                        masadir = masadir,
                        derivatives = derivatives,
                        ayatOccurrences = occurrences
                    )
                }
            } catch (e: Exception) {
                // fallback
            }
        }

        getSeedRootDetail(rootId)
    }

    override suspend fun getRootOccurrencesPaged(rootId: Int, limit: Int, offset: Int): List<AyahOccurrenceModel> = withContext(ioDispatcher) {
        val db = getDb() ?: return@withContext emptyList()
        val list = mutableListOf<AyahOccurrenceModel>()
        try {
            val sql = """
                SELECT a.surah, s.name_ar, a.ayah, a.text_uthmani, REPLACE(GROUP_CONCAT(DISTINCT w.text), ',', '، ')
                FROM word_morphology wm
                JOIN word_ayah wa ON wa.id = wm.word_ayah_id
                JOIN ayat a ON a.id = wa.ayah_id
                JOIN surahs s ON s.id = a.surah
                JOIN words w ON w.id = wa.word_id
                WHERE wm.root_id = ?
                GROUP BY a.id
                ORDER BY a.surah ASC, a.ayah ASC
                LIMIT ? OFFSET ?
            """.trimIndent()
            val cursor = db.rawQuery(sql, arrayOf(rootId.toString(), limit.toString(), offset.toString()))
            cursor.use {
                while (it.moveToNext()) {
                    list.add(
                        AyahOccurrenceModel(
                            surahId = it.getInt(0),
                            surahNameAr = it.getString(1) ?: "",
                            ayahNum = it.getInt(2),
                            textUthmani = it.getString(3) ?: "",
                            matchedWordText = it.getString(4) ?: ""
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return@withContext list
    }

    override suspend fun getAllRootOccurrences(rootId: Int): List<AyahOccurrenceModel> = withContext(ioDispatcher) {
        val db = getDb() ?: return@withContext emptyList()
        val list = mutableListOf<AyahOccurrenceModel>()
        try {
            val sql = """
                SELECT a.surah, s.name_ar, a.ayah, a.text_uthmani, REPLACE(GROUP_CONCAT(DISTINCT w.text), ',', '، ')
                FROM word_morphology wm
                JOIN word_ayah wa ON wa.id = wm.word_ayah_id
                JOIN ayat a ON a.id = wa.ayah_id
                JOIN surahs s ON s.id = a.surah
                JOIN words w ON w.id = wa.word_id
                WHERE wm.root_id = ?
                GROUP BY a.id
                ORDER BY a.surah ASC, a.ayah ASC
            """.trimIndent()
            val cursor = db.rawQuery(sql, arrayOf(rootId.toString()))
            cursor.use {
                while (it.moveToNext()) {
                    list.add(
                        AyahOccurrenceModel(
                            surahId = it.getInt(0),
                            surahNameAr = it.getString(1) ?: "",
                            ayahNum = it.getInt(2),
                            textUthmani = it.getString(3) ?: "",
                            matchedWordText = it.getString(4) ?: ""
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return@withContext list
    }

    override suspend fun getRootOccurrencesCount(rootId: Int): Int = withContext(ioDispatcher) {
        val db = getDb() ?: return@withContext 0
        try {
            val cursor = db.rawQuery(
                "SELECT COUNT(DISTINCT wa.ayah_id) FROM word_morphology wm JOIN word_ayah wa ON wa.id = wm.word_ayah_id WHERE wm.root_id = ?",
                arrayOf(rootId.toString())
            )
            cursor.use { if (it.moveToNext()) return@withContext it.getInt(0) }
        } catch (_: Exception) {}
        return@withContext 0
    }

    override suspend fun getRootByText(rootText: String): RootDetail? = withContext(ioDispatcher) {
        val db = getDb()
        if (db != null) {
            try {
                val clean = ArabicNormalizer.normalizeAr(rootText)
                val cursor = db.rawQuery(
                    "SELECT id FROM roots WHERE root = ? OR root = ? LIMIT 1",
                    arrayOf(rootText, clean)
                )
                cursor.use {
                    if (it.moveToNext()) {
                        return@withContext getRootDetail(it.getInt(0))
                    }
                }
            } catch (e: Exception) {
                // fallback
            }
        }
        getSeedRoots().firstOrNull { it.root == rootText }?.let {
            getRootDetail(it.id)
        }
    }

    override suspend fun searchAll(query: String): SearchResult = withContext(ioDispatcher) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext SearchResult()

        val normalized = ArabicNormalizer.normalizeAr(trimmed)
        val db = getDb()

        if (db != null) {
            val matchingRoots = mutableListOf<RootItem>()
            val matchingMasadir = mutableListOf<MasdarModel>()
            val matchingDerivatives = mutableListOf<DerivativeModel>()
            val matchingWords = mutableListOf<WordToken>()
            val matchingAyat = mutableListOf<Ayah>()

            try {
                // 1. Search Roots
                val rCursor = db.rawQuery(
                    """
                    SELECT r.id, r.root, rg.gloss_ar, rg.gloss_en, ra.summary_ar,
                    (SELECT COUNT(*) FROM masadir m WHERE m.root_id = r.id),
                    (SELECT COUNT(*) FROM derivatives d WHERE d.root_id = r.id)
                    FROM roots r
                    LEFT JOIN root_glosses rg ON rg.root_id = r.id
                    LEFT JOIN root_ai_summary ra ON ra.root_id = r.id
                    WHERE r.root LIKE ? OR r.root LIKE ?
                    LIMIT 20
                    """.trimIndent(),
                    arrayOf("%$trimmed%", "%$normalized%")
                )
                rCursor.use {
                    while (it.moveToNext()) {
                        matchingRoots.add(
                            RootItem(
                                id = it.getInt(0),
                                root = it.getString(1) ?: "",
                                glossAr = it.getString(2),
                                glossEn = it.getString(3),
                                aiSummary = it.getString(4),
                                masadirCount = it.getInt(5),
                                derivativesCount = it.getInt(6)
                            )
                        )
                    }
                }

                // 2. Search Masadir
                val mCursor = db.rawQuery(
                    "SELECT id, form, masdar_ar, pattern, is_attested, source FROM masadir WHERE masdar_plain LIKE ? OR masdar_ar LIKE ? LIMIT 20",
                    arrayOf("%$normalized%", "%$trimmed%")
                )
                mCursor.use {
                    while (it.moveToNext()) {
                        matchingMasadir.add(
                            MasdarModel(
                                id = it.getInt(0),
                                form = it.getString(1),
                                masdarAr = it.getString(2) ?: "",
                                pattern = it.getString(3),
                                isAttested = it.getInt(4) == 1,
                                source = it.getString(5)
                            )
                        )
                    }
                }

                // 3. Search Derivatives
                val dCursor = db.rawQuery(
                    "SELECT id, form_ar, pattern, derivative_type, is_quranic, source FROM derivatives WHERE form_plain LIKE ? OR form_ar LIKE ? LIMIT 20",
                    arrayOf("%$normalized%", "%$trimmed%")
                )
                dCursor.use {
                    while (it.moveToNext()) {
                        matchingDerivatives.add(
                            DerivativeModel(
                                id = it.getInt(0),
                                formAr = it.getString(1) ?: "",
                                pattern = it.getString(2) ?: "",
                                derivativeType = it.getString(3) ?: "",
                                isQuranic = it.getInt(4) == 1,
                                source = it.getString(5)
                            )
                        )
                    }
                }

                // 4. Search Words
                val wCursor = db.rawQuery(
                    "SELECT id, text, text_clean, translation FROM words WHERE text_plain LIKE ? OR text_clean LIKE ? LIMIT 20",
                    arrayOf("%$normalized%", "%$trimmed%")
                )
                wCursor.use {
                    while (it.moveToNext()) {
                        matchingWords.add(
                            WordToken(
                                wordId = it.getInt(0),
                                wordAyahId = 0,
                                position = 0,
                                text = it.getString(1) ?: "",
                                textClean = it.getString(2) ?: "",
                                translation = it.getString(3) ?: ""
                            )
                        )
                    }
                }

                // 5. Search Ayat
                val aCursor = db.rawQuery(
                    "SELECT id, surah, ayah, text_uthmani, text_uthmani_plain, text_imlaei, word_count, juz, hizb, rub_el_hizb, page_number FROM ayat WHERE text_uthmani_plain LIKE ? OR text_imlaei LIKE ? LIMIT 20",
                    arrayOf("%$normalized%", "%$trimmed%")
                )
                aCursor.use {
                    while (it.moveToNext()) {
                        matchingAyat.add(
                            Ayah(
                                id = it.getInt(0),
                                surah = it.getInt(1),
                                ayah = it.getInt(2),
                                textUthmani = it.getString(3) ?: "",
                                textUthmaniPlain = it.getString(4) ?: "",
                                textImlaei = it.getString(5) ?: "",
                                wordCount = it.getInt(6),
                                juz = if (it.isNull(7)) null else it.getInt(7),
                                hizb = if (it.isNull(8)) null else it.getInt(8),
                                rubElHizb = if (it.isNull(9)) null else it.getInt(9),
                                pageNumber = if (it.isNull(10)) null else it.getInt(10)
                            )
                        )
                    }
                }

                return@withContext SearchResult(
                    roots = matchingRoots,
                    masadir = matchingMasadir,
                    derivatives = matchingDerivatives,
                    words = matchingWords,
                    ayat = matchingAyat
                )
            } catch (e: Exception) {
                // fallback
            }
        }

        // Fallback search
        val seedRoots = getSeedRoots().filter {
            it.root.contains(trimmed) || it.root.contains(normalized)
        }
        val seedAyat = getSeedAyat(1).filter {
            it.textUthmaniPlain.contains(normalized) || it.textImlaei.contains(trimmed)
        }
        SearchResult(roots = seedRoots, ayat = seedAyat)
    }

    override suspend fun getPagesForSurah(surahId: Int): List<Int> = withContext(ioDispatcher) {
        val db = getDb() ?: return@withContext emptyList()
        try {
            val cursor = db.rawQuery(
                "SELECT DISTINCT page_number FROM ayat WHERE surah = ? AND page_number IS NOT NULL ORDER BY page_number ASC",
                arrayOf(surahId.toString())
            )
            cursor.use {
                val list = mutableListOf<Int>()
                while (it.moveToNext()) {
                    val p = if (it.isNull(0)) null else it.getInt(0)
                    if (p != null) list.add(p)
                }
                return@withContext list
            }
        } catch (_: Exception) {
            return@withContext emptyList()
        }
    }

    // Seed Data Providers for offline preview / fallback
    private fun getSeedAyat(surahId: Int): List<Ayah> {
        return if (surahId == 1) {
            listOf(
                Ayah(1, 1, 1, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", "بسم الله الرحمن الرحيم", "بسم الله الرحمن الرحيم", 4, listOf(
                    WordToken(1, 1, 1, "بِسْمِ", "بسم", "In the name of", 1, "سمو", "P+N", "اسم مجرور", null, null, null, null, null, null, null, null, "GEN", null, null, null),
                    WordToken(2, 2, 2, "اللَّهِ", "الله", "Allah", 2, "اله", "PN", "اسم علم", null, null, null, null, null, null, null, null, "GEN", "DEF", null, null),
                    WordToken(3, 3, 3, "الرَّحْمَٰنِ", "الرحمن", "The Most Gracious", 3, "رحم", "ADJ", "صفة مشبهة", null, null, null, null, null, null, null, null, "GEN", "DEF", null, null),
                    WordToken(4, 4, 4, "الرَّحِيمِ", "الرحيم", "The Most Merciful", 3, "رحم", "ADJ", "صفة مشبهة", null, null, null, null, null, null, null, null, "GEN", "DEF", null, null)
                )),
                Ayah(2, 1, 2, "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ", "الحمد لله رب العالمين", "الحمد لله رب العالمين", 4, listOf(
                    WordToken(5, 5, 1, "الْحَمْدُ", "الحمد", "All praise is due to", 4, "حمد", "N", "اسم", null, null, null, null, null, null, null, null, "NOM", "DEF", null, null),
                    WordToken(6, 6, 2, "لِلَّهِ", "لله", "Allah", 2, "اله", "P+PN", "جار ومجرور", null, null, null, null, null, null, null, null, "GEN", "DEF", null, null),
                    WordToken(7, 7, 3, "رَبِّ", "رب", "Lord of", 5, "ربب", "N", "صفة / نعت", null, null, null, null, null, null, null, null, "GEN", null, null, null),
                    WordToken(8, 8, 4, "الْعَالَمِينَ", "العالمين", "the worlds", 6, "علم", "N", "مضاف إليه", null, null, null, null, null, null, null, "MP", "GEN", "DEF", null, null)
                )),
                Ayah(3, 1, 3, "الرَّحْمَٰنِ الرَّحِيمِ", "الرحمن الرحيم", "الرحمن الرحيم", 2, listOf(
                    WordToken(9, 9, 1, "الرَّحْمَٰنِ", "الرحمن", "The Most Gracious", 3, "رحم", "ADJ", "صفة", null, null, null, null, null, null, null, null, "GEN", "DEF", null, null),
                    WordToken(10, 10, 2, "الرَّحِيمِ", "الرحيم", "The Most Merciful", 3, "رحم", "ADJ", "صفة", null, null, null, null, null, null, null, null, "GEN", "DEF", null, null)
                )),
                Ayah(4, 1, 4, "مَالِكِ يَوْمِ الدِّينِ", "مالك يوم الدين", "مالك يوم الدين", 3, listOf(
                    WordToken(11, 11, 1, "مَالِكِ", "مالك", "Master of", 7, "ملك", "ACTPCPL", "اسم فاعل", null, null, null, null, null, null, null, null, "GEN", null, "ACTPCPL", null),
                    WordToken(12, 12, 2, "يَوْمِ", "يوم", "the Day of", 8, "يوم", "N", "مضاف إليه", null, null, null, null, null, null, null, null, "GEN", null, null, null),
                    WordToken(13, 13, 3, "الدِّينِ", "الدين", "Judgment", 9, "دين", "N", "مضاف إليه", null, null, null, null, null, null, null, null, "GEN", "DEF", null, null)
                )),
                Ayah(5, 1, 5, "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", "إياك نعبد وإياك نستعين", "إياك نعبد وإياك نستعين", 4, listOf(
                    WordToken(14, 14, 1, "إِيَّاكَ", "إياك", "You alone", 10, null, "PRON", "ضمير نصب منفصل", null, null, null, null, null, null, null, null, "ACC", null, null, null),
                    WordToken(15, 15, 2, "نَعْبُدُ", "نعبد", "we worship", 11, "عبد", "V", "فعل مضارع", "I", "فعل ثلاثي مجرد", "IMPF", "IND", "ACT", "1", "M", "P", null, null, null, null),
                    WordToken(16, 16, 3, "وَإِيَّاكَ", "وإياك", "and You alone", 10, null, "CONJ+PRON", "معطوف", null, null, null, null, null, null, null, null, "ACC", null, null, null),
                    WordToken(17, 17, 4, "نَسْتَعِينُ", "نستعين", "we ask for help", 12, "عون", "V", "فعل استفعال", "X", "الاستفعال", "IMPF", "IND", "ACT", "1", "M", "P", null, null, null, null)
                )),
                Ayah(6, 1, 6, "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ", "اهدنا الصراط المستقيم", "اهدنا الصراط المستقيم", 3, listOf(
                    WordToken(18, 18, 1, "اهْدِنَا", "اهدنا", "Guide us to", 13, "هدي", "V+PRON", "فعل أمر دعائي", "I", "مجرد ثلاثي", "IMPV", null, "ACT", "2", "M", "S", null, null, null, null),
                    WordToken(19, 19, 2, "الصِّرَاطَ", "الصراط", "the straight path", 14, "صرط", "N", "مفعول به ثان", null, null, null, null, null, null, null, null, "ACC", "DEF", null, null),
                    WordToken(20, 20, 3, "الْمُسْتَقِيمَ", "المستقيم", "straight", 15, "قوم", "ACTPCPL", "اسم فاعل", "X", "الاستفعال", null, null, null, null, null, null, "ACC", "DEF", "ACTPCPL", null)
                )),
                Ayah(7, 1, 7, "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ", "صراط الذين أنعمت عليهم غير المغضوب عليهم ولا الضالين", "صراط الذين أنعمت عليهم غير المغضوب عليهم ولا الضالين", 9, listOf(
                    WordToken(21, 21, 1, "صِرَاطَ", "صراط", "The path of", 14, "صرط", "N", "بدل", null, null, null, null, null, null, null, null, "ACC", null, null, null),
                    WordToken(22, 22, 2, "الَّذِينَ", "الذين", "those", 16, null, "REL", "اسم موصول", null, null, null, null, null, null, null, null, "GEN", null, null, null),
                    WordToken(23, 23, 3, "أَنْعَمْتَ", "أنعمت", "You have blessed", 17, "نعم", "V", "فعل ماض", "IV", "الإفعال", "PERF", null, "ACT", "2", "M", "S", null, null, null, null),
                    WordToken(24, 24, 4, "عَلَيْهِمْ", "عليهم", "upon them", 18, null, "P+PRON", "جار ومجرور", null, null, null, null, null, null, null, null, null, null, null, null),
                    WordToken(25, 25, 5, "غَيْرِ", "غير", "not of", 19, "غير", "N", "بدل / نعت", null, null, null, null, null, null, null, null, "GEN", null, null, null),
                    WordToken(26, 26, 6, "الْمَغْضُوبِ", "المغضوب", "those who incurred anger", 20, "غضب", "PASSPCPL", "اسم مفعول", "I", "مجرد ثلاثي", null, null, null, null, null, null, "GEN", "DEF", "PASSPCPL", null),
                    WordToken(27, 27, 7, "عَلَيْهِمْ", "عليهم", "upon them", 18, null, "P+PRON", "جار ومجرور", null, null, null, null, null, null, null, null, null, null, null, null),
                    WordToken(28, 28, 8, "وَلَا", "ولا", "and not of", 21, null, "CONJ+NEG", "حرف عطف ونفي", null, null, null, null, null, null, null, null, null, null, null, null),
                    WordToken(29, 29, 9, "الضَّالِّينَ", "الضالين", "those who are astray", 22, "ضلل", "ACTPCPL", "اسم فاعل", "I", "مجرد ثلاثي", null, null, null, null, "M", "P", "GEN", "DEF", "ACTPCPL", null)
                ))
            )
        } else {
            listOf(
                Ayah(surahId * 1000 + 1, surahId, 1, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", "بسم الله الرحمن الرحيم", "بسم الله الرحمن الرحيم", 4, listOf(
                    WordToken(1, 1, 1, "بِسْمِ", "بسم", "In the name of", 1, "سمو", "P+N", "اسم مجرور"),
                    WordToken(2, 2, 2, "اللَّهِ", "الله", "Allah", 2, "اله", "PN", "اسم علم"),
                    WordToken(3, 3, 3, "الرَّحْمَٰنِ", "الرحمن", "The Most Gracious", 3, "رحم", "ADJ", "صفة مشبهة"),
                    WordToken(4, 4, 4, "الرَّحِيمِ", "الرحيم", "The Most Merciful", 3, "رحم", "ADJ", "صفة مشبهة")
                ))
            )
        }
    }

    private fun getSeedRoots(): List<RootItem> {
        return listOf(
            RootItem(1, "كتب", "الكِتابة والخَطّ وفرض الشيء وإيجابه", "To write, prescribe, record", "جذر يدل على جمع الشيء إلى الشيء، ومنه الكتابة والكتيبة، وورد في القرآن بمعنى الفريضة والإلزام والتسجيل.", 6, 18, 319),
            RootItem(2, "علم", "إدراك الشيء بحقيقته واليقين والمعرفة", "To know, perceive, learn", "أصل صحيح يدل على أثر بالشيء يتميز به عن غيره، ومنه العلم وهو نقيض الجهل، والعلامة والعالم.", 9, 24, 854),
            RootItem(3, "رحم", "الرِّقَّةُ والعَطْفُ والمَغْفِرَةُ والإحسان", "To show mercy, have compassion", "يدل على الرقة والعطف والشفقة، ومنه أسماء الله الحسنى: الرحمن الرحيم، والرحم القرابة.", 4, 12, 339),
            RootItem(4, "حمد", "الثَّناءُ بالجَمِيلِ على جِهَةِ التَّعْظِيم", "To praise, commend, thank", "نقيض الذم، وهو الثناء على المحمود بجميل صفاته وأفعاله، والحمد أعم من الشكر.", 3, 8, 68),
            RootItem(5, "قرا", "الجَمْعُ والضَّمُّ وتِلاوَةُ الكَلام", "To read, recite, proclaim", "أصله الجمع والضم، ومنه قراءة القرآن لأن القارئ يضم الحروف والكلمات بعضها إلى بعض في النطق.", 4, 10, 88),
            RootItem(6, "هدي", "الإرْشادُ والدَّلالَةُ والتَّوْجِيهُ للحق", "To guide, direct, lead", "يدل على التقدم للإرشاد والدلالة على طريق الرشاد، ومنه الهدى والهدية.", 5, 15, 316),
            RootItem(7, "نزل", "الانْحِطاطُ مِن عُلْوٍ والوُرُودُ والضِّيافة", "To descend, send down, reveal", "يدل على هبوط الشيء من علو إلى سفل، ومنه تنزيل القرآن وإنزاله والمنزل.", 7, 21, 293),
            RootItem(8, "عبد", "الخُضُوعُ والتَّذَلُّلُ والطَّاعَة", "To worship, serve, obey", "أصل يدل على الذل والانقياد، ومنه طريق معبد، والعبادة أقصى غايات الخضوع لله وحده.", 4, 14, 275),
            RootItem(9, "خلق", "التَّقْدِيرُ وإيجادُ الشَّيْءِ على غَيْرِ مِثال", "To create, measure, fashion", "أصلان: أحدهما تقدير الشيء، والآخر ملاسة الشيء، ومنه خلق الله تعالى المخلوقات.", 5, 16, 261),
            RootItem(10, "أمن", "السَّكِينَةُ وطُمَأْنِينَةُ النَّفْسِ والتَّصْدِيق", "To be safe, trust, believe", "أصل يدل على سكون القلب وطمأنينته ونفي الخوف، ومنه الإيمان والأمانة والأمن.", 8, 22, 879)
        )
    }

    private fun getSeedRootDetail(rootId: Int): RootDetail {
        val rootItem = getSeedRoots().firstOrNull { it.id == rootId } ?: getSeedRoots()[0]
        return RootDetail(
            item = rootItem,
            aiSummary = rootItem.aiSummary,
            aiModel = "seed-preview",
            aiGeneratedAt = "2026-08-25T00:00:00+00:00",
            meanings = listOf(
                RootMeaningModel(1, "لسان العرب: الكَتْبُ: الضم والجمع، وكَتَبَ الشيءَ يَكْتُبُهُ كَتْباً وكِتاباً وكِتابةً: خطه، وسُمّي الكِتابُ كِتاباً لاجتماع حُرُوفه وكَلِماته.", "لسان العرب لابن منظور"),
                RootMeaningModel(2, "الصحاح في اللغة: الكَتْبُ: الخَرزُ، والكِتابُ والكِتابَةُ معروفان، واستكتَبْته: سأَلته أَن يَكْتُبَ لي، والكَتيبةُ: الجَيشُ لاجتماعهم.", "الصحاح للجوهري"),
                RootMeaningModel(3, "معجم مقاييس اللغة: الكاف والتاء والباء أصلٌ صحيحٌ واحد يدلّ على جمع شيء إلى شيء، ومنه الكِتاب والكِتابة، والكَتيبة من الخيل.", "مقاييس اللغة لابن فارس")
            ),
            masadir = listOf(
                MasdarModel(1, "I", "كِتَابَة", "فِعَالَة", true, "quran_vn"),
                MasdarModel(2, "I", "كَتْب", "فَعْل", true, "quran_vn"),
                MasdarModel(3, "II", "تَكْتِيب", "تَفْعِيل", false, "pattern"),
                MasdarModel(4, "III", "مُكَاتَبَة", "مُفَاعَلَة", true, "quran_vn"),
                MasdarModel(5, "VIII", "اكْتِتَاب", "افْتِعَال", true, "quran_vn"),
                MasdarModel(6, "X", "اسْتِكْتَاب", "اسْتِفْعَال", false, "pattern")
            ),
            derivatives = listOf(
                DerivativeModel(1, "كَاتِب", "فَاعِل", "اسم فاعل", true, "quranic"),
                DerivativeModel(2, "مَكْتُوب", "مَفْعُول", "اسم مفعول", true, "quranic"),
                DerivativeModel(3, "كِتَاب", "فِعَال", "اسم عين / مصدر", true, "quranic"),
                DerivativeModel(4, "كُتُب", "فُعُل", "جمع تكسير", true, "quranic"),
                DerivativeModel(5, "مَكْتَبَة", "مَفْعَلَة", "اسم مكان", false, "camel"),
                DerivativeModel(6, "مُكْتَتِب", "مُفْتَعِل", "اسم فاعل مزيد", false, "pattern")
            ),
            ayatOccurrences = listOf(
                AyahOccurrenceModel(2, "البقرة", 183, "يَا أَيُّهَا الَّذِينَ آمَنُوا كُتِبَ عَلَيْكُمُ الصِّيَامُ كَمَا كُتِبَ عَلَى الَّذِينَ مِن قَبْلِكُمْ", "كُتِبَ"),
                AyahOccurrenceModel(2, "البقرة", 282, "يَا أَيُّهَا الَّذِينَ آمَنُوا إِذَا تَدَايَنتُم بِدَيْنٍ إِلَىٰ أَجَلٍ مُّسَمًّى فَاكْتُبُوهُ ۚ وَلْيَكْتُب بَّيْنَكُمْ كَاتِبٌ بِالْعَدْلِ", "فَاكْتُبُوهُ"),
                AyahOccurrenceModel(2, "البقرة", 2, "ذَٰلِكَ الْكِتَابُ لَا رَيْبَ ۛ فِيهِ ۛ هُدًى لِّلْمُتَّقِينَ", "الْكِتَابُ"),
                AyahOccurrenceModel(4, "النساء", 103, "إِنَّ الصَّلَاةَ كَانَتْ عَلَى الْمُؤْمِنِينَ كِتَابًا مَّوْقُوتًا", "كِتَابًا"),
                AyahOccurrenceModel(6, "الأنعام", 12, "قُل لِّمَن مَّا فِي السَّمَاوَاتِ وَالْأَرْضِ ۖ قُل لِّلَّهِ ۚ كَتَبَ عَلَىٰ نَفْسِهِ الرَّحْمَةَ", "كَتَبَ")
            )
        )
    }
}
