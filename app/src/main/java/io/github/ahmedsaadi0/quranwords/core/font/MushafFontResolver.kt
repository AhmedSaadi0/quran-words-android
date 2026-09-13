package io.github.ahmedsaadi0.quranwords.core.font

import io.github.ahmedsaadi0.quranwords.core.util.MushafConstants
import java.io.File

/**
 * Pure resolution logic for QPC page fonts (no Android deps, fully testable).
 *
 * Runtime loading (Phase 2 `MushafFontManager`) uses
 * `android.graphics.Typeface.createFromFile()` wrapped via
 * `androidx.compose.ui.text.font.Typeface(platform)` into a `FontFamily` —
 * Compose `Font()` only accepts compiled resources, never loose files
 * (verified against ui-text 1.7.2/1.11.0 sources), so `Font(File)` is not
 * an option on this stack.
 *
 * Fonts are fetched on demand into `[filesDir]/fonts/qpc/p{page}.ttf`
 * (LRU 7 in RAM); the optional bulk pack downloads all 604 once.
 */
object MushafFontResolver {

    /** Disk location for a cached page font under the app files dir. */
    fun cachedFile(filesDir: File, page: Int): File {
        MushafConstants.requireValidPage(page)
        return File(File(filesDir, "fonts/qpc"), MushafConstants.fontFileName(page))
    }

    /** Remote URL for a page font. */
    fun remoteUrl(page: Int): String = MushafConstants.fontUrl(page)

    /** True when a usable cached font file exists for [page]. */
    fun isCached(filesDir: File, page: Int): Boolean {
        if (!MushafConstants.isValidPage(page)) return false
        val f = cachedFile(filesDir, page)
        return f.exists() && f.length() > 0L
    }

    /**
     * Pages the reader should prefetch around [page] (current ± 3),
     * clamped to 1..604 — matches [MushafConstants.FONT_CACHE_SIZE].
     */
    fun prefetchWindow(page: Int, radius: Int = 3): List<Int> {
        MushafConstants.requireValidPage(page)
        return ((page - radius)..(page + radius))
            .filter { MushafConstants.isValidPage(it) }
    }
}
