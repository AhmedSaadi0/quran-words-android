package com.quranwords.ui.surah.detail.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quranwords.R
import com.quranwords.core.util.PillTransparencyPreference
import com.quranwords.core.util.ReadingProgress
import com.quranwords.ui.theme.AppMotion

/**
 * Height of the persistent cluster pill. The reading list reserves this
 * (+ the status bar) as its top content padding, so hiding/showing the chrome
 * can never shift the text — zero layout jumps by construction.
 */
val ReadingChromePillHeight = 56.dp

private val PillShape = RoundedCornerShape(20.dp)

/** Back-button scrim (its own surface tint — never the pill's alpha). */
private const val BackScrimAlpha = 0.95f

/**
 * Obsidian-like reading chrome — two independent layers floating over the
 * full-screen list:
 *
 *  (a) **Back layer** (`TopEnd`): the only part that fades out — driven by
 *      [showBar], which is false while immersed. System back always works.
 *
 *  (b) **Cluster pill** (`TopStart`, never hides): the two progress rings —
 *      the page ring opens the page-jump menu — and the options button that
 *      opens the font-size sheet. Exactly TWO alpha states (user spec):
 *      fully opaque while the status bar is visible, and the persisted
 *      [pillTransparency] while immersed — CONSTANT during scrolling, with
 *      no scroll-driven dimming. The transition runs only on the rare
 *      bar-visibility change and is read in the DRAW phase through
 *      graphicsLayer, so it never recomposes anything.
 *
 *  (c) **Status-bar backdrop**: an opaque strip of the app background whose
 *      height equals the real status-bar inset — solid while the bars are
 *      visible, zero-height while immersed (Android 15+ enforces transparent
 *      window bars, so the app paints its own instead).
 *
 * [readingProgress] and [currentPage] are lambdas: per-frame scroll updates
 * recompute only inside the rings/menu, never this screen (AGENTS §17).
 */
@Composable
fun ReadingTopBarOverlay(
    showBar: Boolean,
    readingProgress: () -> ReadingProgress?,
    pages: List<Int>,
    currentPage: () -> Int?,
    pillTransparency: Float,
    onBack: () -> Unit,
    onOpenFontSize: () -> Unit,
    onPageSelected: (Int) -> Unit
) {
    var pageMenuExpanded by remember { mutableStateOf(false) }
    var optionsMenuExpanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize()) {
        // Solid backdrop for the visible status bar: on targetSdk 36 the
        // window bar itself is always transparent (edge-to-edge enforced on
        // Android 15+), so the app paints its own background behind it. The
        // strip's HEIGHT equals the real inset — opaque while the bars are
        // shown, collapses to nothing the moment they hide, so immersion
        // stays untouched and reading never jumps.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(MaterialTheme.colorScheme.background)
        )

        // (a) Back — fades with the bar; cluster never does. TopEnd +
        // padding(end) are layout-direction-aware: they flip RTL/LTR with
        // the app locale, and the margins keep it off the screen edges.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 12.dp, top = 4.dp)
        ) {
            AnimatedVisibility(
                visible = showBar,
                enter = fadeIn(tween(AppMotion.DurationShort, easing = AppMotion.EasingStandard)),
                exit = fadeOut(tween(AppMotion.DurationShort, easing = AppMotion.EasingExit))
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = BackScrimAlpha),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    shadowElevation = 2.dp
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            }
        }

        // (b) Cluster pill — persistent with exactly two alpha states:
        // opaque whenever the status bar is visible, and the user-chosen
        // transparency whenever it is hidden — including while scrolling and
        // while the transient bars are swiped in (those are overlays: they
        // never change the statusBars inset, so the pill does not move).
        // The alpha transition is read in the draw phase via graphicsLayer,
        // so the rare bar-visibility change never recomposes anything.
        // TopStart + padding(start) flip RTL/LTR with the app locale; the
        // margins keep the pill off the top and side edges even while
        // immersed (statusBarsPadding collapses to 0 there).
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 12.dp, top = 4.dp)
        ) {
            val density = LocalDensity.current
            val statusBarVisible = WindowInsets.statusBars.getTop(density) > 0
            val pillAlpha by animateFloatAsState(
                targetValue = if (statusBarVisible) {
                    1f
                } else {
                    PillTransparencyPreference.alphaFor(pillTransparency)
                },
                animationSpec = tween(AppMotion.DurationShort, easing = AppMotion.EasingStandard),
                label = "clusterPillAlpha"
            )
            Surface(
                shape = PillShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                shadowElevation = 3.dp,
                modifier = Modifier.graphicsLayer { alpha = pillAlpha }
            ) {
                Row(
                    modifier = Modifier
                        .height(ReadingChromePillHeight)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Rings + their stable DropdownMenu anchor (page jump).
                    Box(modifier = Modifier.padding(start = 6.dp)) {
                        ReadingProgressRings(
                            readingProgress = readingProgress,
                            onPageRingClick = if (pages.isEmpty()) {
                                null
                            } else {
                                { pageMenuExpanded = true }
                            }
                        )
                        if (pages.isNotEmpty()) {
                            DropdownMenu(
                                expanded = pageMenuExpanded,
                                onDismissRequest = { pageMenuExpanded = false }
                            ) {
                                pages.forEach { page ->
                                    val isCurrent = page == currentPage()
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.page_label, page)) },
                                        leadingIcon = if (isCurrent) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = null
                                                )
                                            }
                                        } else {
                                            null
                                        },
                                        onClick = {
                                            pageMenuExpanded = false
                                            onPageSelected(page)
                                        },
                                        modifier = Modifier.testTag("page_menu_item_$page")
                                    )
                                }
                            }
                        }
                    }

                    // Options: font-size sheet today; extensible later.
                    Box {
                        IconButton(
                            onClick = { optionsMenuExpanded = true },
                            modifier = Modifier.testTag("reading_options_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.reading_options_cd)
                            )
                        }
                        DropdownMenu(
                            expanded = optionsMenuExpanded,
                            onDismissRequest = { optionsMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.reading_menu_settings)) },
                                onClick = {
                                    optionsMenuExpanded = false
                                    onOpenFontSize()
                                },
                                modifier = Modifier.testTag("reading_menu_settings")
                            )
                        }
                    }
                }
            }
        }
    }
}
