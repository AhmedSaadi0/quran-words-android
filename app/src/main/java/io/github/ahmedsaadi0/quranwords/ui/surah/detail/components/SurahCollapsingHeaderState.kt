package io.github.ahmedsaadi0.quranwords.ui.surah.detail.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Saveable collapse state for the surah detail header (quick-return /
 * enter-always). Ported 1:1 from the legacy inline rememberSaveable pair so
 * rotation and back-nav keep the exact collapse physics.
 */
@Stable
class SurahCollapsingHeaderState(
    collapsibleHeightPx: Int,
    headerOffsetPx: Float
) {
    var collapsibleHeightPx: Int by mutableIntStateOf(collapsibleHeightPx)
    var headerOffsetPx: Float by mutableFloatStateOf(headerOffsetPx)

    /** Collapse progress in 0..1 (0 = expanded, 1 = fully collapsed). */
    val collapseProgress: Float
        get() = if (collapsibleHeightPx > 0) {
            (1f + (headerOffsetPx / collapsibleHeightPx.toFloat())).coerceIn(0f, 1f)
        } else {
            1f
        }

    fun reset() {
        headerOffsetPx = 0f
    }

    companion object {
        val Saver = listSaver(
            save = { listOf(it.collapsibleHeightPx, it.headerOffsetPx) },
            restore = { SurahCollapsingHeaderState(it[0] as Int, it[1] as Float) }
        )
    }
}

@Composable
fun rememberSurahCollapsingHeaderState(): SurahCollapsingHeaderState =
    rememberSaveable(saver = SurahCollapsingHeaderState.Saver) {
        SurahCollapsingHeaderState(0, 0f)
    }