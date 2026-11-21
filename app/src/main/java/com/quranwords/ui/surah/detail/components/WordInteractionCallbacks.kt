package com.quranwords.ui.surah.detail.components

import com.quranwords.domain.model.Ayah
import com.quranwords.domain.model.WordToken

/** Shared gesture seam passed from the screen to both reading modes. */
data class WordInteractionCallbacks(
    /** TalkBack-only: opens the morphology sheet for this exact word/ayah. */
    val onOpenWord: (WordToken, Ayah) -> Unit,
    val onWordLongPressed: (WordActionTarget) -> Unit,
    /** Single tap outside selection mode: toggle the reading chrome. */
    val onToggleChrome: () -> Unit,
    val onDismissWordAction: () -> Unit,
    val onCopyWord: (String) -> Unit
)
