package io.github.ahmedsaadi0.quranwords.core.font

import android.content.Context
import android.graphics.Typeface as PlatformTypeface
import android.util.LruCache
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Typeface as ComposeTypeface
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.ahmedsaadi0.quranwords.core.di.IoDispatcher
import io.github.ahmedsaadi0.quranwords.core.util.MushafConstants
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * QPC page-font loader with a 7-entry in-memory LRU (current page ± 3).
 *
 * Resolution order per page: filesDir download cache (`fonts/qpc/p{P}.ttf`,
 * Phase 2 downloader) → bundled preview assets (spike: pages 1, 2, 3, 531,
 * 602) → throw (UI surfaces retry; full download manager lands in Phase 2).
 *
 * The single Basmalah font ([basmalahFont]) follows the same lane with its
 * own one-slot cache — every basmalah line on every page shares it.
 *
 * Platform typefaces wrap into Compose via `FontFamily(Typeface(...))` —
 * Compose `Font()` only accepts compiled resources, never loose files.
 */
@Singleton
class MushafFontManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val cache = LruCache<Int, FontFamily>(MushafConstants.FONT_CACHE_SIZE)

    @Volatile
    private var basmalahCached: FontFamily? = null

    suspend fun fontForPage(page: Int): FontFamily = withContext(ioDispatcher) {
        MushafConstants.requireValidPage(page)
        synchronized(cache) { cache[page] }?.let { return@withContext it }
        val loaded = load(page)
        synchronized(cache) { cache.put(page, loaded) }
        loaded
    }

    /** Shared `U+FDFD` font for every basmalah line; throws when unavailable. */
    suspend fun basmalahFont(): FontFamily = withContext(ioDispatcher) {
        basmalahCached?.let { return@withContext it }
        val platform = loadFile(MushafConstants.BASMALAH_FONT_FILE)
        FontFamily(ComposeTypeface(platform)).also { basmalahCached = it }
    }

    /** True without any I/O when the family is already cached. */
    fun isCached(page: Int): Boolean = synchronized(cache) { cache[page] } != null

    fun evictExcept(keep: Set<Int>) {
        synchronized(cache) {
            val snapshot = cache.snapshot()
            snapshot.keys.forEach { if (it !in keep) cache.remove(it) }
        }
    }

    private fun load(page: Int): FontFamily {
        val fileName = MushafConstants.fontFileName(page)
        return FontFamily(ComposeTypeface(loadFile(fileName)))
    }

    private fun loadFile(fileName: String): PlatformTypeface {
        val cached = File(File(context.filesDir, "fonts/qpc"), fileName)
        // Throws when neither the download cache nor the bundled preview
        // assets hold this file — the caller surfaces retry / fallback.
        return if (cached.exists() && cached.length() > 0L) {
            PlatformTypeface.createFromFile(cached)
        } else {
            PlatformTypeface.createFromAsset(context.assets, "fonts/qpc/$fileName")
        }
    }
}
