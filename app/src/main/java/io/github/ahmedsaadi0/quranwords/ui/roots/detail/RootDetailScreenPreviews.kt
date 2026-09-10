package io.github.ahmedsaadi0.quranwords.ui.roots.detail

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import io.github.ahmedsaadi0.quranwords.domain.model.AyahOccurrenceModel
import io.github.ahmedsaadi0.quranwords.domain.model.DerivativeModel
import io.github.ahmedsaadi0.quranwords.domain.model.MasdarModel
import io.github.ahmedsaadi0.quranwords.domain.model.RootDetail
import io.github.ahmedsaadi0.quranwords.domain.model.RootItem
import io.github.ahmedsaadi0.quranwords.domain.model.RootMeaningModel
import io.github.ahmedsaadi0.quranwords.domain.model.RootWordModel
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.util.RootDetailTab
import io.github.ahmedsaadi0.quranwords.ui.theme.MyApplicationTheme

private fun previewDetail() = RootDetail(
    item = RootItem(
        id = 1, root = "كتب", glossAr = "الكتابة وما يتصل بها",
        occurrencesCount = 319, masadirCount = 2, derivativesCount = 3
    ),
    aiSummary = "جذر يدل على الجمع والضم، ومنه الكتابة لما فيها من جمع الحروف.",
    aiModel = "gpt-4",
    aiGeneratedAt = "2024-01-02T15:04:05",
    meanings = listOf(
        RootMeaningModel(1, "الجمع والضم", "لسان العرب"),
        RootMeaningModel(2, "الخط بالقلم", "الصحاح")
    ),
    masadir = listOf(MasdarModel(1, "I", "كِتَابَة", "فِعَالَة", true)),
    derivatives = listOf(DerivativeModel(1, "كَاتِب", "فَاعِل", "اسم فاعل", true)),
    ayatOccurrences = listOf(
        AyahOccurrenceModel(2, "البقرة", 183, "آية النص", "كُتِبَ"),
        AyahOccurrenceModel(1, "الفاتحة", 2, "نص أول", "الحمد")
    )
)

private fun previewState() = RootDetailUiState(
    isLoading = false,
    detail = previewDetail(),
    rootText = "كتب",
    subtitleText = "جذر يدل على الجمع والضم، ومنه الكتابة لما فيها من جمع الحروف.",
    hasSubtitle = true,
    aiMetaLine = "gpt-4  •  2024-01-02 15:04",
    hasAiMeta = true,
    tabs = listOf(
        RootDetailTabUi(RootDetailTab.MEANINGS, 2),
        RootDetailTabUi(RootDetailTab.AYAT, 319),
        RootDetailTabUi(RootDetailTab.WORDS, 2),
        RootDetailTabUi(RootDetailTab.MASADIR, 1),
        RootDetailTabUi(RootDetailTab.DERIVATIVES, 1)
    ),
    meanings = MeaningsTabState(
        meanings = previewDetail().meanings, selectedIds = setOf(1), isSelectionMode = true
    ),
    words = WordsTabState(
        words = listOf(
            RootWordModel(10, "كَتَبَ", 12),
            RootWordModel(11, "كِتَاب", 45)
        ),
        isSelectionMode = false
    ),
    ayat = AyatTabState(
        occurrences = previewDetail().ayatOccurrences, totalCount = 319,
        hasMore = true, isLoadingMore = false
    )
)

@Preview(name = "RootDetail loading", showBackground = true)
@Composable
fun RootDetailLoadingPreview() {
    MyApplicationTheme {
        RootDetailScreen(
            uiState = RootDetailUiState(isLoading = true, detail = null),
            rootId = 1,
            onEvent = {},
            onNavigateBack = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(name = "RootDetail populated", showBackground = true)
@Composable
fun RootDetailPopulatedPreview() {
    MyApplicationTheme {
        RootDetailScreen(
            uiState = previewState(),
            rootId = 1,
            onEvent = {},
            onNavigateBack = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}
