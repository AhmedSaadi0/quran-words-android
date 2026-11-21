package com.quranwords.ui.surah.detail

import com.quranwords.core.util.Result
import com.quranwords.core.util.orEmptyIfError
import com.quranwords.core.util.runCatchingResult
import com.quranwords.domain.model.Ayah
import com.quranwords.domain.repository.QuranRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Pages-mode fetch/prefetch state machine, isolated from the VM (P3-4). */
internal class SurahReaderPageLoader(
    private val repository: QuranRepository,
    private val scope: CoroutineScope,
    private val cache: MushafPageCache,
    private val state: () -> ReaderCoreState,
    private val update: ((ReaderCoreState) -> ReaderCoreState) -> Unit
) {
    private var prefetchJob: Job? = null

    /** Page whose prefetch is in flight — duplicate events must not cancel it. */
    var activePrefetchTarget: Int? = null
        private set

    fun reset() {
        prefetchJob?.cancel()
        prefetchJob = null
        activePrefetchTarget = null
        cache.clear()
        cache.pinnedPage = null
    }

    fun prime(pages: Map<Int, List<Ayah>>, entry: Int) {
        reset()
        cache.putPages(pages)
        cache.pinnedPage = entry
    }

    fun snapshot(): Map<Int, List<Ayah>> = cache.toMap()

    fun has(page: Int): Boolean = cache.containsKey(page)

    operator fun get(page: Int): List<Ayah>? = cache[page]

    private suspend fun loadPageIfMissing(page: Int): Boolean {
        if (cache.containsKey(page)) return true
        val result = runCatchingResult { repository.getAyatOfPageFull(page).orEmptyIfError() }
        val ayat = if (result is Result.Success) result.data else emptyList()
        return if (ayat.isNotEmpty()) {
            cache.putPage(page, ayat)
            update { it.copy(pagesByNumber = cache.toMap(), failedPages = it.failedPages - page) }
            true
        } else {
            update { it.copy(failedPages = it.failedPages + page) }
            false
        }
    }

    /** Called when the unified pager settles on a real mushaf page. */
    fun onPageChanged(page: Int) {
        if (page < 1) return
        if (page == state().currentPage && (has(page) || activePrefetchTarget == page)) return

        val ayat = get(page).orEmpty()
        val activeId = ayat.firstOrNull()?.surah ?: state().activeSurahId
        update {
            it.copy(
                currentPage = page,
                pageAyahs = ayat,
                activeSurahId = activeId,
                activeSurah = if (activeId > 0) surahFor(activeId, it.surah) else it.activeSurah
            )
        }

        prefetchJob?.cancel()
        activePrefetchTarget = page
        prefetchJob = scope.launch {
            try {
                // Visible page first, then its neighbours.
                if (loadPageIfMissing(page)) publishLoaded(page)
                val total = state().totalPages
                val neighbours = listOf(page - 1, page + 1, page - 2, page + 2)
                    .filter { it in 1..total && !has(it) }
                for (neighbour in neighbours) loadPageIfMissing(neighbour)
            } finally {
                if (activePrefetchTarget == page) activePrefetchTarget = null
            }
        }
    }

    fun retryPage(page: Int) {
        if (page < 1) return
        update { it.copy(failedPages = it.failedPages - page) }
        scope.launch {
            loadPageIfMissing(page)
            if (page == state().currentPage) publishLoaded(page)
        }
    }

    fun setEntryPage(page: Int) {
        if (page < 1) return
        prefetchJob?.cancel()
        activePrefetchTarget = null
        update { it.copy(isPagesReady = false, entryPage = page, currentPage = page, pageAyahs = emptyList()) }
        scope.launch {
            val loaded = loadPageIfMissing(page)
            cache.pinnedPage = page
            val ayat = get(page).orEmpty()
            val activeId = ayat.firstOrNull()?.surah ?: state().activeSurahId
            update {
                it.copy(
                    pageAyahs = ayat,
                    isPagesReady = loaded,
                    activeSurahId = activeId,
                    activeSurah = if (activeId > 0) surahFor(activeId, it.surah) else it.activeSurah
                )
            }
        }
    }

    private fun publishLoaded(page: Int) {
        val loaded = get(page).orEmpty()
        val loadedId = loaded.firstOrNull()?.surah ?: state().activeSurahId
        update { current ->
            if (current.currentPage != page) current
            else current.copy(
                pageAyahs = loaded,
                activeSurahId = loadedId,
                activeSurah = if (loadedId > 0) surahFor(loadedId, current.surah) else current.activeSurah
            )
        }
    }
}
