package com.example.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset

// Telegram-like balanced motion - 250ms, smooth and lively
object AppMotion {
    const val DurationShort = 200
    const val DurationMedium = 250
    const val DurationLong = 300
    const val DurationExtraLong = 350

    // Balanced easing - Telegram style: smooth but responsive
    val EasingStandard = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f) // Emphasized Easing
    val EasingEmphasized = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EasingEnter = CubicBezierEasing(0.0f, 0.0f, 0.0f, 1.0f)
    val EasingExit = CubicBezierEasing(0.3f, 0.0f, 1.0f, 1.0f)

    // Spring specs for lively micro-interactions
    val SpringDefault = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
    val SpringGentle = spring<Float>(dampingRatio = 0.8f, stiffness = 300f)
    val SpringSnappy = spring<Float>(dampingRatio = 0.7f, stiffness = 400f)

    // Navigation transitions - slide + fade, 250ms balanced
    fun navEnterTransition() = slideInHorizontally(
        initialOffsetX = { it / 6 },
        animationSpec = tween(DurationMedium, easing = EasingStandard)
    ) + fadeIn(animationSpec = tween(DurationMedium, easing = EasingStandard))

    fun navExitTransition() = slideOutHorizontally(
        targetOffsetX = { -it / 6 },
        animationSpec = tween(DurationMedium, easing = EasingStandard)
    ) + fadeOut(animationSpec = tween(180))

    fun navPopEnterTransition() = slideInHorizontally(
        initialOffsetX = { -it / 6 },
        animationSpec = tween(DurationMedium, easing = EasingStandard)
    ) + fadeIn(animationSpec = tween(DurationMedium))

    fun navPopExitTransition() = slideOutHorizontally(
        targetOffsetX = { it / 6 },
        animationSpec = tween(DurationMedium, easing = EasingStandard)
    ) + fadeOut(animationSpec = tween(180))

    // Stagger for list appearance like Telegram
    const val StaggerDelay = 30
    const val StaggerDelayStep = 20L
}
