package io.github.ahmedsaadi0.quranwords.ui.mushaf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ahmedsaadi0.quranwords.core.font.MushafFontManager
import io.github.ahmedsaadi0.quranwords.core.util.Result
import io.github.ahmedsaadi0.quranwords.domain.model.Ayah
import io.github.ahmedsaadi0.quranwords.domain.model.MushafPage
import io.github.ahmedsaadi0.quranwords.domain.model.WordToken
import io.github.ahmedsaadi0.quranwords.domain.repository.QuranRepository
import io.github.ahmedsaadi0.quranwords.domain.usecase.GetMushafPageUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns Mushaf preview state: per-page layout + font families, word
 * selection with AI summary (same flow as SurahDetail's bottom sheet).
 * Pages load on pager settle; entries stay cached for the session.
 */
@HiltViewModel
class MushafViewModel @Inject constructor(
    private val getMushafPage: GetMushafPageUseCase,
    private val repository: QuranRepository,
    private val fonts: MushafFontManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MushafUiState())
    val uiState: StateFlow<MushafUiState> = _uiState.asStateFlow()

    fun onEvent(event: MushafEvent) {
        when (event) {
            is MushafEvent.PageSettled -> {
                _uiState.update { it.copy(currentPage = event.page, error = null) }
                loadPage(event.page)
            }
            is MushafEvent.WordTapped -> selectWord(event.wordAyahId)
            MushafEvent.DismissWord -> clearSelectedWord()
            MushafEvent.Retry -> loadPage(_uiState.value.currentPage, force = true)
            MushafEvent.WordLookupFailedShown -> {
                _uiState.update { it.copy(wordLookupFailed = false) }
            }
        }
    }

    private fun loadPage(page: Int, force: Boolean = false) {
        val state = _uiState.value
        if (!force && (state.loaded.containsKey(page) || state.loadingPages.contains(page))) return
        _uiState.update { it.copy(loadingPages = it.loadingPages + page, error = null) }
        viewModelScope.launch {
            val font = try {
                fonts.fontForPage(page)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loadingPages = it.loadingPages - page,
                        error = e.message ?: "Font missing for page $page"
                    )
                }
                return@launch
            }
            // Basmalah font is decorative: failure degrades to the Unicode
            // fallback string instead of failing the whole page.
            val basmalah = try {
                fonts.basmalahFont()
            } catch (_: Exception) {
                null
            }
            val pageResult: Result<MushafPage> =
                try {
                    getMushafPage(page)
                } catch (e: Exception) {
                    Result.Error(e.message ?: "Failed to load page $page", e)
                }
            when (pageResult) {
                is Result.Success -> _uiState.update {
                    it.copy(
                        loadingPages = it.loadingPages - page,
                        loaded = it.loaded + (
                            page to MushafPageData(pageResult.data, font, basmalah)
                            )
                    )
                }
                is Result.Error -> _uiState.update {
                    it.copy(loadingPages = it.loadingPages - page, error = pageResult.message)
                }
            }
        }
    }

    private fun selectWord(wordAyahId: Int) {
        viewModelScope.launch {
            val pair: Pair<WordToken, Ayah>? = try {
                repository.getWordTokenByWordAyahId(wordAyahId)
            } catch (_: Exception) {
                null
            }
            if (pair == null) {
                // No morphology without the main database — surfaced as a
                // snackbar by the Route, never a silent dead tap.
                _uiState.update { it.copy(wordLookupFailed = true) }
                return@launch
            }
            _uiState.update {
                it.copy(
                    selectedWord = pair.first,
                    selectedWordAyah = pair.second,
                    aiSummary = null,
                    aiModel = null,
                    aiGeneratedAt = null,
                    isAiLoading = pair.first.rootId != null
                )
            }
            val rootId = pair.first.rootId ?: return@launch
            // AI summary is supplementary: on failure the sheet shows its fallback.
            val detail = try {
                repository.getRootDetail(rootId)
            } catch (_: Exception) {
                null
            }
            _uiState.update {
                it.copy(
                    aiSummary = detail?.aiSummary,
                    aiModel = detail?.aiModel,
                    aiGeneratedAt = detail?.aiGeneratedAt,
                    isAiLoading = false
                )
            }
        }
    }

    private fun clearSelectedWord() {
        _uiState.update {
            it.copy(
                selectedWord = null,
                selectedWordAyah = null,
                aiSummary = null,
                aiModel = null,
                aiGeneratedAt = null,
                isAiLoading = false
            )
        }
    }
}
