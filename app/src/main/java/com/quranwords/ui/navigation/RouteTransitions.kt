package com.quranwords.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import com.quranwords.ui.theme.AppMotion

/**
 * Transition rules for the adjacent-surah switch, isolated from the navigation
 * graph so `AppNavigation` stays declarative: it declares *which* transitions a
 * destination uses, this file decides what they do.
 *
 * A navigate-with-`popUpTo` operation may be surfaced by navigation-compose
 * through either the forward (`enter`/`exit`) or the back (`popEnter`/`popExit`)
 * lambdas, so every slot below asks the same pair of entries: a transition is
 * vertical only when BOTH entries are SurahDetail screens and the target marks a
 * switch direction. Every other navigation keeps the horizontal app transitions.
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.switchDirection(): SurahEnter? {
    val initialIsSurah = initialState.destination.hasRoute<SurahDetail>()
    val targetIsSurah = targetState.destination.hasRoute<SurahDetail>()
    if (!initialIsSurah || !targetIsSurah) return null
    return targetState.toRoute<SurahDetail>().enter.takeIf { it != SurahEnter.Normal }
}

/** Forward switch arrives from the bottom, backward switch from the top. */
internal fun AnimatedContentTransitionScope<NavBackStackEntry>.surahDetailEnter(): EnterTransition =
    switchDirection()
        ?.let { AppMotion.verticalEnter(fromBottom = it == SurahEnter.FromNext) }
        ?: AppMotion.navEnterTransition()

/** The replaced surah leaves opposite to the incoming one. */
internal fun AnimatedContentTransitionScope<NavBackStackEntry>.surahDetailExit(): ExitTransition =
    switchDirection()
        ?.let { AppMotion.verticalExit(toBottom = it == SurahEnter.FromPrev) }
        ?: AppMotion.navExitTransition()

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.surahDetailPopEnter(): EnterTransition =
    switchDirection()
        ?.let { AppMotion.verticalEnter(fromBottom = it == SurahEnter.FromNext) }
        ?: AppMotion.navPopEnterTransition()

/**
 * Back navigation leaving a surah that was itself entered by a switch: it exits
 * the way it came (FromNext → downward, FromPrev → upward) while the entry point
 * beneath it is revealed by [revealFromAdjacentSwitch].
 */
internal fun AnimatedContentTransitionScope<NavBackStackEntry>.surahDetailPopExit(): ExitTransition =
    switchDirection()
        ?.let { AppMotion.verticalExit(toBottom = it == SurahEnter.FromPrev) }
        ?: backOutOfAdjacentSurah()
        ?: AppMotion.navPopExitTransition()

/** True when this transition removes a surah that a switch had just entered. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.isLeavingAdjacentSwitch(): Boolean =
    initialState.destination.hasRoute<SurahDetail>() &&
        !targetState.destination.hasRoute<SurahDetail>() &&
        initialState.toRoute<SurahDetail>().enter != SurahEnter.Normal

private fun AnimatedContentTransitionScope<NavBackStackEntry>.backOutOfAdjacentSurah(): ExitTransition? {
    if (!isLeavingAdjacentSwitch()) return null
    val enteredFromNext = initialState.toRoute<SurahDetail>().enter == SurahEnter.FromNext
    return AppMotion.verticalExit(toBottom = enteredFromNext)
}

/**
 * NavHost-level reveal: a screen underneath a surah that slides away vertically
 * must fade in instead of also sliding in horizontally.
 */
internal fun AnimatedContentTransitionScope<NavBackStackEntry>.revealFromAdjacentSwitch(): EnterTransition =
    if (isLeavingAdjacentSwitch()) {
        fadeIn(tween(AppMotion.DurationMedium, easing = AppMotion.EasingStandard))
    } else {
        AppMotion.navPopEnterTransition()
    }
