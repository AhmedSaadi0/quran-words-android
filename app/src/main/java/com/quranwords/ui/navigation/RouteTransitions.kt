package com.quranwords.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import com.quranwords.core.util.ReadingModePreference
import com.quranwords.ui.theme.AppMotion

/**
 * Transition rules for the adjacent-surah switch, isolated from the navigation
 * graph so `AppNavigation` stays declarative: it declares *which* transitions a
 * destination uses (passing the reading mode explicitly — no VMs, no hidden
 * globals), and this file decides what they do.
 *
 * A navigate-with-`popUpTo` operation may be surfaced by navigation-compose
 * through either the forward (`enter`/`exit`) or the back (`popEnter`/`popExit`)
 * lambdas, so every slot below asks the same pair of entries: a transition is a
 * switch slide only when BOTH entries are SurahDetail screens and the target
 * marks a switch direction. Every other navigation keeps the app transitions.
 *
 * The slide rides the ACTIVE reading mode: vertical scroll (default) or the
 * book-paging mode, whose horizontal slide direction mirrors with the layout
 * direction (RTL mushaf: next arrives from the left while the old surah leaves
 * right).
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.switchDirection(): SurahEnter? {
    val initialIsSurah = initialState.destination.hasRoute<SurahDetail>()
    val targetIsSurah = targetState.destination.hasRoute<SurahDetail>()
    if (!initialIsSurah || !targetIsSurah) return null
    return targetState.toRoute<SurahDetail>().enter.takeIf { it != SurahEnter.Normal }
}

/** Horizontal book-page geometry for a switch [direction], per layout side. */
private data class HorizontalSlide(
    val enterFromLeft: Boolean,
    val exitToLeft: Boolean
)

private fun horizontalSlide(direction: SurahEnter, rtl: Boolean): HorizontalSlide = if (rtl) {
    HorizontalSlide(
        enterFromLeft = direction == SurahEnter.FromNext,
        exitToLeft = direction == SurahEnter.FromPrev
    )
} else {
    HorizontalSlide(
        enterFromLeft = direction == SurahEnter.FromPrev,
        exitToLeft = direction == SurahEnter.FromNext
    )
}

private fun switchEnter(direction: SurahEnter, readingMode: Int, rtl: Boolean): EnterTransition =
    if (readingMode == ReadingModePreference.PAGES) {
        AppMotion.horizontalEnter(fromLeft = horizontalSlide(direction, rtl).enterFromLeft)
    } else {
        AppMotion.verticalEnter(fromBottom = direction == SurahEnter.FromNext)
    }

private fun switchExit(direction: SurahEnter, readingMode: Int, rtl: Boolean): ExitTransition =
    if (readingMode == ReadingModePreference.PAGES) {
        AppMotion.horizontalExit(toLeft = horizontalSlide(direction, rtl).exitToLeft)
    } else {
        AppMotion.verticalExit(toBottom = direction == SurahEnter.FromPrev)
    }

/** Forward switch arrives from the bottom (scroll) or the mirrored side (pages). */
internal fun AnimatedContentTransitionScope<NavBackStackEntry>.surahDetailEnter(
    readingMode: Int,
    rtl: Boolean
): EnterTransition =
    switchDirection()?.let { switchEnter(it, readingMode, rtl) } ?: AppMotion.navEnterTransition()

/** The replaced surah leaves opposite to the incoming one. */
internal fun AnimatedContentTransitionScope<NavBackStackEntry>.surahDetailExit(
    readingMode: Int,
    rtl: Boolean
): ExitTransition =
    switchDirection()?.let { switchExit(it, readingMode, rtl) } ?: AppMotion.navExitTransition()

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.surahDetailPopEnter(
    readingMode: Int,
    rtl: Boolean
): EnterTransition =
    switchDirection()?.let { switchEnter(it, readingMode, rtl) } ?: AppMotion.navPopEnterTransition()

/**
 * Back navigation leaving a surah that was itself entered by a switch: it exits
 * the way it came (the same slide the switch used) while the entry point
 * beneath it is revealed by [revealFromAdjacentSwitch].
 */
internal fun AnimatedContentTransitionScope<NavBackStackEntry>.surahDetailPopExit(
    readingMode: Int,
    rtl: Boolean
): ExitTransition =
    switchDirection()?.let { switchExit(it, readingMode, rtl) }
        ?: backOutOfAdjacentSurah(readingMode, rtl)
        ?: AppMotion.navPopExitTransition()

/** True when this transition removes a surah that a switch had just entered. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.isLeavingAdjacentSwitch(): Boolean =
    initialState.destination.hasRoute<SurahDetail>() &&
        !targetState.destination.hasRoute<SurahDetail>() &&
        initialState.toRoute<SurahDetail>().enter != SurahEnter.Normal

private fun AnimatedContentTransitionScope<NavBackStackEntry>.backOutOfAdjacentSurah(
    readingMode: Int,
    rtl: Boolean
): ExitTransition? {
    if (!isLeavingAdjacentSwitch()) return null
    val enteredFrom = initialState.toRoute<SurahDetail>().enter
    return if (readingMode == ReadingModePreference.PAGES) {
        AppMotion.horizontalExit(toLeft = horizontalSlide(enteredFrom, rtl).exitToLeft)
    } else {
        AppMotion.verticalExit(toBottom = enteredFrom == SurahEnter.FromNext)
    }
}

/**
 * NavHost-level reveal: a screen underneath a surah that slides away in a
 * switch must fade in instead of also sliding in horizontally.
 */
internal fun AnimatedContentTransitionScope<NavBackStackEntry>.revealFromAdjacentSwitch(
    readingMode: Int
): EnterTransition =
    if (isLeavingAdjacentSwitch()) {
        fadeIn(tween(AppMotion.DurationMedium, easing = AppMotion.EasingStandard))
    } else {
        AppMotion.navPopEnterTransition()
    }
