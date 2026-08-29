package io.github.ahmedsaadi0.quranwords.data.repository

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import io.github.ahmedsaadi0.quranwords.data.remote.DatabaseDownloadManager
import io.github.ahmedsaadi0.quranwords.data.util.ArabicNormalizer
import io.github.ahmedsaadi0.quranwords.data.util.QuranMetaConstants
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class QuranRepositoryImpl(
    private val context: Context,
    private val downloadManager: DatabaseDownloadManager = DatabaseDownloadManager(context)
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
    }.flowOn(Dispatchers.IO)

    override suspend fun getSurahById(id: Int): Surah? = withContext(Dispatchers.IO) {
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
                    "SELECT id, surah, ayah, text_uthmani, text_uthmani_plain, text_imlaei, word_count FROM ayat WHERE surah = ? ORDER BY ayah ASC",
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
                                words = words
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
    }.flowOn(Dispatchers.IO)

    override suspend fun getAyatBySurahPaged(surahId: Int, limit: Int, offset: Int): List<Ayah> = withContext(Dispatchers.IO) {
        val db = getDb() ?: return@withContext emptyList()
        val list = mutableListOf<Ayah>()
        try {
            val cursor = db.rawQuery(
                "SELECT id, surah, ayah, text_uthmani, text_uthmani_plain, text_imlaei, word_count FROM ayat WHERE surah = ? ORDER BY ayah ASC LIMIT ? OFFSET ?",
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
                            words = words
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

    override suspend fun getAyahWithWords(surahId: Int, ayahNum: Int): Ayah? = withContext(Dispatchers.IO) {
        val db = getDb()
        if (db != null) {
            try {
                val cursor = db.rawQuery(
                    "SELECT id, surah, ayah, text_uthmani, text_uthmani_plain, text_imlaei, word_count FROM ayat WHERE surah = ? AND ayah = ? LIMIT 1",
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
                            words = words
                        )
                    }
                }
            } catch (e: Exception) {
                // fallback
            }
        }
        null // Seed removed per AGENTS.md §8.5 — show download screen when DB not ready
    }

    override suspend fun getRootsPaged(limit: Int, offset: Int): List<RootItem> = withContext(Dispatchers.IO) {
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
                        (SELECT COUNT(*) FROM word_morphology wm WHERE wm.root_id = r.id)
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

        emptyList() // Seed removed
    }

    override suspend fun getRootDetail(rootId: Int): RootDetail? = withContext(Dispatchers.IO) {
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
                        SELECT a.surah, s.name_ar, a.ayah, a.text_uthmani, w.text
                        FROM word_morphology wm
                        JOIN word_ayah wa ON wa.id = wm.word_ayah_id
                        JOIN ayat a ON a.id = wa.ayah_id
                        JOIN surahs s ON s.id = a.surah
                        JOIN words w ON w.id = wa.word_id
                        WHERE wm.root_id = ?
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
                    // Total count for this root (not limited)
                    var totalOccurrences = occurrences.size
                    try {
                        val countCursor = db.rawQuery(
                            "SELECT COUNT(*) FROM word_morphology WHERE root_id = ?",
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

        null // Seed removed
    }

    override suspend fun getRootOccurrencesPaged(rootId: Int, limit: Int, offset: Int): List<AyahOccurrenceModel> = withContext(Dispatchers.IO) {
        val db = getDb() ?: return@withContext emptyList()
        val list = mutableListOf<AyahOccurrenceModel>()
        try {
            val sql = """
                SELECT a.surah, s.name_ar, a.ayah, a.text_uthmani, w.text
                FROM word_morphology wm
                JOIN word_ayah wa ON wa.id = wm.word_ayah_id
                JOIN ayat a ON a.id = wa.ayah_id
                JOIN surahs s ON s.id = a.surah
                JOIN words w ON w.id = wa.word_id
                WHERE wm.root_id = ?
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

    override suspend fun getRootOccurrencesCount(rootId: Int): Int = withContext(Dispatchers.IO) {
        val db = getDb() ?: return@withContext 0
        try {
            val cursor = db.rawQuery("SELECT COUNT(*) FROM word_morphology WHERE root_id = ?", arrayOf(rootId.toString()))
            cursor.use { if (it.moveToNext()) return@withContext it.getInt(0) }
        } catch (_: Exception) {}
        return@withContext 0
    }

    override suspend fun getRootByText(rootText: String): RootDetail? = withContext(Dispatchers.IO) {
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
        null // Seed removed
    }

    override suspend fun searchAll(query: String): SearchResult = withContext(Dispatchers.IO) {
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
                    "SELECT id, surah, ayah, text_uthmani, text_uthmani_plain, text_imlaei, word_count FROM ayat WHERE text_uthmani_plain LIKE ? OR text_imlaei LIKE ? LIMIT 20",
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
                                wordCount = it.getInt(6)
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

        // No fallback — Seed removed, return empty when DB not ready
        SearchResult()
    }
}
