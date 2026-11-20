# UI_AUDIT_FIX_PLAN — UI Audit Remediation (plan of record)

> **Status:** planned, not started. **Source report:** `docs/UI_AUDIT_FIX_PLAN.md` (this file) supersedes the chat audit dated 2026-11-18 for tracking purposes.
> **Scope:** `app/src/main/java/com/quranwords/ui/**`, plus the small domain/dependency changes needed to fix UI error paths (Phase 1).
> **Rules:** `AGENTS.md` §4 (dependency rule), §6 (fail fast), §9–§13 (domain/UI/errors), §16 (agent never runs Gradle), §20 (Decision Log). Execute phases in order; do not mix phases.
>
> **Docs naming verified:** existing root-level plans use `UPPER_SNAKE_CASE.md` — `docs/REFACTOR_PLAN.md`, `docs/UI_REFACTOR_PLAN.md`, `docs/SURAH_DETAIL_PROGRESS_AND_SWITCH_PLAN.md`, `docs/DB_RELEASE.md`. Feature plans that are still being designed live in `docs/plan/current/<kebab-case>.md`. This file intentionally follows the root-level convention.
>
> **Build config verified:** `app/build.gradle.kts` applies `io.gitlab.arturbosch.detekt` and `com.diffplug.spotless` with `ktlint("1.5.0")`. There is **no** standalone `ktlint` plugin, so `spotlessCheck` is the real formatting task; `ktlintCheck` is not defined by this build (it is only mentioned in `AGENTS.md` §16/§23 and is stale there). See the Verification Checklist.

## 0) Traceability — every audit finding is assigned

Previous audit (A1–A20), the first follow-up round (N1–N8), and this round (N9–N19). All 39 findings are confirmed against current source; no task is duplicated. N15 is a reviewed, accepted exception with no code task.

| ID | Finding (short) | Task(s) |
|---|---|---|
| A1 | Search ayah card renders fake `﴿٠﴾` | P0-1 |
| A2 | `UpdateFailedBanner` is empty → failure invisible | P0-2 |
| A3 | Bookmark selection toggles existing bookmarks off | P0-3 |
| A4 | RootDetail load/paging errors unhandled → endless spinner | P0-4 |
| A5 | Reader `error`/`isDbReady` never rendered | P0-5 |
| A6 | WordAyat Retry is a no-op after paging error | P0-6 |
| A7 | Search load-more error wipes results + disables paging | P0-7 |
| A8 | `DatabaseSetupViewModel` imports `data.remote` | P1-1 |
| A9 | Copy/share exceptions swallowed (`catch -> ""`) | P1-2 |
| A10 | Six God files / 852-char lines | P3-2 … P3-7 |
| A11 | Positional `Any` casts in `combine`, duplicate flows | P2-1, P2-2 |
| A12 | UDF bypass: platform code in Composables, dialog VM, Home contract | P2-3, P2-4, P2-5 |
| A13 | Hardcoded user strings (AR/EN), Latin stats digits | P4-1, P4-3 |
| A14 | Raw colors/shapes, <48dp targets | P4-4, P4-5 |
| A15 | Almost no Compose tests for risky flows | P5-1 … P5-9 |
| A16 | Manual paging + `delay(80)`/`delay(200)` in 3 VMs | P2-7 |
| A17 | DB-missing flow contradicts AGENTS §8.5 | P1-5 |
| A18 | Dead/stale code, missing "copy all AI" decision | P4-7 |
| A19 | Small duplication clusters (spec/Basmalah/cards) | P4-8 |
| A20 | Confusing names + duplicate state plumbing | P2-2, P2-8 |
| N1 | `CheckDbUpdateUseCase` converts throws to `Success(Unknown)` | P1-3 |
| N2 | `MushafFlowBlock.pointerInput` stale `activeWordAction` capture | P0-8 |
| N3 | Last-read missed when page content arrives after `PageChanged` | P0-9 |
| N4 | Duplicate mushaf `AnnotatedString` builders | P3-1, P5-7 |
| N5 | TalkBack actions rebuilt every recomposition; `isActiveSurah` contradiction | P1-4, P4-2 |
| N6 | `ShareHandler.openUrl` silently swallows no-browser error | P1-2 |
| N7 | Search bookmark IconButton always says "remove bookmark" | P4-6 |
| N8 | `first.pageNumber!!` NPE risk in `SurahAyatList` | P0-10 |
| N9 | Two settings APIs + false 3-state toggle KDoc | P2-9 |
| N10 | `CitedBodyText` fresh-collection `remember` key | P3-8 |
| N11 | `ShareHandler.copyImmediate` double clipboard write | P1-2 (merged) |
| N12 | `DatabaseSetupViewModel.checkForUpdate` drops `Error` to `Unit` | P1-6 |
| N13 | `QuickJumpDialog` magic-string error sentinels | P2-10 |
| N14 | `HomeUpdateBanner` no-op pass-through | P4-7 (merged) |
| N15 | `MorphologyGuideScreen` locale branch in composable | documented exception (no task) |
| N16 | Duplicate byte-size formatters (`formatDbSize` / `formatVersionSize`) | P4-9 |
| N17 | `Typography` name shadowing + duplicate shape tokens | P4-10 |
| N18 | Remaining emojis in strings/banner (Decision #13) | P4-11 |
| N19 | Hardcoded `24f` font defaults + raw/fractional `sp` | P4-12 |

---

## Phase 0 — Critical user-facing bugs

> Rule: fix the behaviour first, with one focused regression test per task. No refactors, no renames, no style changes in this phase.

- [x] **P0-1 — Real ayah marker in search results** (`app/src/main/java/com/quranwords/ui/search/components/SearchResultCards.kt`: `pageJuzSuffix` L95, `HighlightedAyahText` L107, `buildAnnotatedStringFix` L122–135; callers: `AyahSearchCard` L90 and `SearchAllSectionsView` ayah row).
  - **Root cause:** `HighlightedAyahText(text, query)` never receives the ayah number; both the match and no-match paths append `0.toEasternArabicDigits()` as the marker, so every result displays `﴿٠﴾`.
  - **Fix:** change the signature to `HighlightedAyahText(text: String, query: String, ayahNumber: Int, ...)`; append `ayahNumber.toEasternArabicDigits()` inside the `﴿ … ﴾` marker in both branches; update both call sites to pass `ayah.ayah`. Reuse the marker constant from `MushafFlowBlock` only if it can be done without pulling Compose text code into the card (otherwise leave duplicated for Phase 3).
  - **Acceptance:** matched and unmatched queries both show the result's real ayah number in Eastern digits; no `٠` marker for any ayah; RTL rendering unchanged.
  - **Test:** add `app/src/test/java/com/quranwords/ui/search/SearchResultCardsTest.kt::real_ayah_marker_is_rendered_for_match_and_no_match` (Compose test using `setContent` + `onNodeWithTag("search_ayah_text")`), or a pure formatter test if the marker string is extracted to a pure helper.

- [x] **P0-2 — Restore the failed-update banner** (`app/src/main/java/com/quranwords/ui/update/UpdateBanner.kt`: `UpdateFailedBanner` L139–158 is entirely commented out; caller `app/src/main/java/com/quranwords/ui/update/InAppUpdateHandler.kt`: `UpdateBannerState.Failed -> UpdateFailedBanner(...)` L81–84).
  - **Root cause:** the host switches to `Failed` but the composable renders nothing, so the user gets no message and no dismiss action (`UpdateEvent.Dismiss` can never be produced from this state).
  - **Fix:** restore a minimal `BannerSurface` with `R.string.update_banner_failed_message`, a dismiss `IconButton` with `R.string.cd_update_banner_close`, and testTag `UpdateBannerTags.DISMISS_ACTION`; delete the commented block.
  - **Acceptance:** simulated `AppUpdateResult.Failed` shows a readable banner; tapping dismiss sends `UpdateEvent.Dismiss` and the banner hides.
  - **Test:** `app/src/test/java/com/quranwords/ui/update/UpdateBannerTest.kt::failed_banner_renders_message_and_dismiss_action` (Compose) plus VM assertion that `Dismiss` on `Failed` sets `UpdateBannerState.Hidden`.

- [x] **P0-3 — Bookmark selection must add, never toggle** (`app/src/main/java/com/quranwords/ui/surah/detail/SurahDetailViewModel.kt`: `onEvent` L200, `bookmarkSelection` L218–224; `app/src/main/java/com/quranwords/ui/roots/detail/...` unaffected; data semantics in `app/src/main/java/com/quranwords/core/datastore/UserPreferencesDataStore.kt`: `toggleAyahBookmark` L188; snackbar in `app/src/main/java/com/quranwords/ui/surah/detail/SurahDetailRoute.kt`: `onBookmarkSelection`).
  - **Root cause:** `bookmarkSelection()` calls a *toggle* for every selected ref; an already-bookmarked ayah is removed while the snackbar still claims it was saved. N separate DataStore transactions also allow partial application.
  - **Fix:** add domain method `setAyahBookmarked(surahId, ayahNum, bookmarked: Boolean)` to `UserPreferencesRepository` and `UserPreferencesDataStore` (single DataStore `edit` that unions the keys for all selected refs); call it once from `bookmarkSelection()`. Keep `toggleAyahBookmark` for single-verse toggles (search/bookmarks screens). Report failure through `Result` to the Route instead of fire-and-forget `launch`.
  - **Acceptance:** pressing Bookmark on a selection containing mixed states leaves **all** selected ayat bookmarked; snackbar count matches; no ayah is removed.
  - **Test:** `app/src/test/java/com/quranwords/ui/surah/detail/SurahDetailViewModelTest.kt::bookmark_selection_preserves_existing_bookmarks` with a fake preferences repo capturing the written key set.

- [x] **P0-4 — RootDetail DB failures must reach the UI** (`app/src/main/java/com/quranwords/ui/roots/detail/RootDetailViewModel.kt`: `loadRootDetail` L109–160 (`repository.getRootDetail` L133, `getRootOccurrencesPaged` L145), `loadMoreOccurrences` L181–198 (`getRootOccurrencesPaged` L187)).
  - **Root cause:** these calls are unwrapped; the only error path is `loadRootWords` L162–171. An exception cancels the coroutine after `_isLoading = true` → permanent spinner; `_error` stays null.
  - **Fix:** wrap both in `runCatchingResult { ... }`; on `Result.Error` set `_error = result.message`, `_isLoading = false`, `_isOccurrencesLoadingMore = false`; ensure the existing `RootDetailEvent.Retry` path re-enters `loadRootDetail` cleanly. Logged/Crashlytics reporting follows the repository boundary (Phase 1).
  - **Acceptance:** fake repository throwing on detail or page fetch yields a visible error state (not a spinner) and Retry recovers.
  - **Test:** `app/src/test/java/com/quranwords/ui/RootDetailErrorTest.kt::detail_load_error_is_exposed_and_retry_recovers` + `::paging_error_is_exposed`.

- [x] **P0-5 — Reader error UI instead of false "DB missing"** (`app/src/main/java/com/quranwords/ui/surah/detail/SurahDetailViewModel.kt`: error set at L304, pages error swallowed at L311; `app/src/main/java/com/quranwords/ui/surah/detail/SurahDetailScreen.kt`: `when` L429–474 renders only loading/empty/pager; `SurahDetailContract.kt`: `error` L30 and `isDbReady` L46 are never read).
  - **Root cause:** query failures put a message in state, but the screen treats any empty ayat list as `SurahDbMissingState`; no Retry action is dispatched.
  - **Fix:** add an explicit `uiState.error != null` branch before the empty branch with message + Retry button that sends `SurahDetailEvent.Load(surahId, targetAyah, immediate = true)`; distinguish real "DB not ready" via `isDbReady` (or delete it if the product rule in P1-5 removes the case). Fix the L311 branch to surface the pages error rather than silently substituting `emptyList()`.
  - **Acceptance:** forcing `getAyatBySurahFull` to throw shows the error and Retry; forcing `getPagesForSurah` to throw does not silently erase page chrome.
  - **Test:** `app/src/test/java/com/quranwords/ui/surah/detail/SurahDetailViewModelTest.kt::load_error_sets_error_state_and_retry_reloads` + Compose `SurahDetailErrorUiTest.kt::error_message_and_retry_are_rendered`.

- [x] **P0-6 — Make WordAyat Retry work after a paging error** (`app/src/main/java/com/quranwords/ui/roots/word/WordAyatViewModel.kt`: `loadMore` error L183–190 sets `_hasMore = false`; `retry` L144–152 calls `loadMore()`; guard `if (_isLoadingMore.value || !_hasMore.value) return` L164).
  - **Root cause:** the error turns paging off, then Retry immediately returns because paging is off.
  - **Fix:** on error keep `_hasMore = true` and show an inline retry row (the screen already renders `uiState.error` as an inline item at L222–232); `retry()` should clear `_error` and re-run the same offset page. Only set `hasMore = false` for a genuinely empty page.
  - **Acceptance:** after a transient failure, Retry loads the next page and the error row disappears; a truly empty final page still shows "all shown".
  - **Test:** `app/src/test/java/com/quranwords/ui/WordAyatViewModelTest.kt::paging_error_keeps_has_more_and_retry_loads_next_page`.

- [x] **P0-7 — Search paging errors must not destroy results** (`app/src/main/java/com/quranwords/ui/search/SearchViewModel.kt`: `loadMore` L213–261, error L257; `app/src/main/java/com/quranwords/ui/search/SearchScreen.kt`: global error branch L129–131).
  - **Root cause:** a load-more failure sets `hasMore=false` and `_error`; the screen replaces the entire result list with `SearchErrorView`, and Retry (`retry()` L169) restarts the query from scratch.
  - **Fix:** keep `hasMore` unchanged on paging errors; render an inline retry item in the active list (pattern: `WordAyatScreen` L222–232) that re-dispatches `SearchEvent.LoadMore(tab)`; reserve the global error view for the initial search only.
  - **Acceptance:** after 10 loaded rows and one failed page, all 10 rows and scroll position remain; pressing inline Retry resumes from the saved offset.
  - **Test:** `app/src/test/java/com/quranwords/ui/SearchViewModelTest.kt::paging_error_preserves_results_and_offset` (extend existing fake) + Compose `SearchPagingErrorTest.kt::inline_retry_visible_with_results`.

- [x] **P0-8 — Fix stale `activeWordAction` capture in `pointerInput`** (`app/src/main/java/com/quranwords/ui/surah/detail/components/MushafFlowBlock.kt`: `handleTap` L207–223 checks `activeWordAction`, `pointerInput(isSelectionMode, group.key, group.firstAyahIndex)` L285; caller `app/src/main/java/com/quranwords/ui/surah/detail/SurahDetailScreen.kt`: `WordInteractionCallbacks` L207–224 whose `onToggleChrome` also closes over `wordAction`).
  - **Root cause (verified):** the pointer-input lambda is remembered under keys that do **not** include `activeWordAction` or `wordInteraction`. The file itself documents this staleness for `justification` (L197–199) and fixes that with `rememberUpdatedState`, but not for the toolbar. After a long press opens the popup, an outside tap is handled by the stale lambda that still sees `activeWordAction == null`, so it toggles immersive chrome while the popup independently dismisses itself.
  - **Fix:** add `val currentWordAction by rememberUpdatedState(activeWordAction)` and `val currentInteraction by rememberUpdatedState(wordInteraction)`; read those inside `handleTap`/`handleLongPress` instead of the captured parameters (or add both to the `pointerInput` keys — prefer `rememberUpdatedState` to avoid restarting gesture detection on every toolbar change). Keep the existing `currentJustification` pattern.
  - **Acceptance:** with the floating word toolbar open, tapping outside dismisses the popup only — chrome stays in its current state; with the toolbar closed, a tap still toggles chrome; long-press still opens the toolbar.
  - **Test:** `app/src/test/java/com/quranwords/ui/surah/detail/components/WordActionPointerTest.kt::outside_tap_after_long_press_does_not_toggle_chrome` (Compose test with `performClick` after `performTouchInput { longClick() }`, assert chrome callback invocation count == 0 after dismissal).

- [x] **P0-9 — Last-read must fire when an uncached page finishes loading** (`app/src/main/java/com/quranwords/ui/surah/detail/SurahDetailScreen.kt`: last-read effect L343–362 uses `snapshotFlow { uiState.currentPage }` L346 and returns early when `uiState.pageAyahs` is empty L349–350; VM publisher `app/src/main/java/com/quranwords/ui/surah/detail/SurahDetailViewModel.kt`: `onPageChanged` L280–320 publishes `currentPage` with empty `pageAyahs`, then fills `pageAyahs` at L298–309 with the same page number).
  - **Root cause (verified):** `snapshotFlow` observes only `currentPage`; the later `pageAyahs` change does not re-emit, and the effect restart key includes `currentPage` which did not change. Arriving on an uncached page therefore never records last-read for that page.
  - **Fix:** observe both values, e.g. `snapshotFlow { uiState.currentPage to uiState.pageAyahs }.distinctUntilChanged()`, or publish last-read from the VM at the moment page content is attached; keep the `hasHandledInitialScroll` gate and active-surah preference. Avoid re-emitting identical `(surah, ayah)` pairs (VM already de-dupes in `updateLastRead`).
  - **Acceptance:** swiping to a page that was never cached records the first active-surah ayah on that page; revisiting a cached page still records correctly; no extra DataStore writes per recomposition.
  - **Test:** `app/src/test/java/com/quranwords/ui/surah/detail/SurahDetailViewModelTest.kt::last_read_recorded_when_page_content_arrives_after_page_change` (or Compose test at screen level if the flow is kept in the screen).

- [x] **P0-10 — Remove `!!` from the ayah list** (`app/src/main/java/com/quranwords/ui/surah/detail/components/SurahAyatList.kt`: `MushafPageContent` uses `first.pageNumber!!` at L179 and L211; guard already exists in `mushafPageChrome` at `app/src/main/java/com/quranwords/ui/surah/detail/components/AyahFlowGrouping.kt`: `showHeader`/`showPageNumber` check `pageNumber != null`).
  - **Root cause (verified):** two unchecked assertions on legacy/unpaged corpus data (`pageNumber == null`). Today the chrome flags usually hide those rows, so it rarely crashes, but any future flag change turns it into an NPE.
  - **Fix:** carry the nullable page number in `MushafPageChromeFlags` (add `pageNumber: Int?`) or use `first.pageNumber?.let { ... }` at both call sites; remove the `!!` entirely. Update `AyahFlowGroupingTest` fixtures for unpaged groups.
  - **Acceptance:** rendering a group with `pageNumber = null` never throws and shows neither page header nor page separator.
  - **Test:** `app/src/test/java/com/quranwords/ui/surah/detail/components/AyahFlowGroupingTest.kt::unpaged_group_renders_without_page_number` + Compose `SurahAyatListUnpagedTest.kt::no_crash_for_null_page_number`.

---

## Phase 1 — Architecture & error boundaries

- [x] **P1-1 — Remove the UI → Data dependency in setup** (`app/src/main/java/com/quranwords/ui/setup/DatabaseSetupViewModel.kt`: imports L6–9; fields `downloadManager` L26, `startDownload` L88, `importDatabase` L111, `DownloadState.toUi` L130, `DownloadError.toUi` L144; `app/src/main/java/com/quranwords/ui/setup/SetupContract.kt` header references the data type).
  - **Root cause:** AGENTS §4.1 requires `ui → domain`; the ViewModel injects `com.quranwords.data.remote.DatabaseDownloadManager` and maps its `DownloadState`/`DownloadError`/`DownloadPhase` directly.
  - **Fix:** define a domain port `DatabaseDownloadRepository` with domain `DownloadState`/`DownloadError`/`DownloadPhase` models (copy the sealed shapes, no behaviour change); make the existing manager implement it; inject the interface in the VM and bind in `app/src/main/java/com/quranwords/di/AppModule.kt` or `core/di`; keep `toUi()` mapping data-free. Update the contract KDoc.
  - **Acceptance:** `rg "import com\.quranwords\.data\." app/src/main/java/com/quranwords/ui` returns zero hits; setup VM tests use a fake domain repository.
  - **Test:** `app/src/test/java/com/quranwords/ui/DatabaseSetupViewModelTest.kt::progress_and_completed_states_map_to_ui_without_data_types` (fake domain repository).

- [x] **P1-2 — Typed copy/share/open errors and visible feedback** (`app/src/main/java/com/quranwords/ui/roots/detail/RootDetailViewModel.kt`: five `catch (_: Exception) { "" }` at L237, L257, L270, L290, L312; `app/src/main/java/com/quranwords/ui/roots/word/WordAyatViewModel.kt` L201; `app/src/main/java/com/quranwords/ui/roots/detail/util/ShareHandler.kt`: `copyFormatted` L81–96, `shareFormatted` L99–114, `openUrl` L124–136).
  - **Root cause:** exceptions from formatting/DB and the "no browser installed" case are all converted to blank text or silence. `ShareHandler.copyFormatted` then shows "nothing to copy", which is wrong for a real failure; `openUrl` (N6) never shows any snackbar. **Also confirmed (N11):** `ShareHandler.copyImmediate` writes the same text twice — Android `ClipboardManager.setPrimaryClip` inside `try { ... } catch (_: Exception) { }` (`copyImmediate` L37–53, write L46, catch L47–48) and then `clipboardManager.setText(...)` (L50) — so the first write's failure is swallowed and the second silently overwrites it.
  - **Fix:** change VM formatters to return `Result<String>`; ShareHandler takes the result and shows a distinct failure message on `Error` (new `R.string.*_failed` in both locales). Give `openUrl` a `noBrowserMessage` parameter and catch `ActivityNotFoundException` with `scope.launch { snackbarHostState.showSnackbar(...) }`. Remove duplicate catches; log the exception at the boundary. **For N11:** keep exactly one clipboard write — prefer Compose `ClipboardManager.setText` (already the in-app paste source); if the platform `ClipData` write must stay for API-level reliability, make it the single write and report its failure instead of falling through silently.
  - **Acceptance:** clipboard/DB failure is distinguishable from empty data; tapping the GitHub report link with no browser produces a snackbar; all existing copy/share paths still show their success messages; exactly one clipboard write per copy action is observable in a fake ClipboardManager.
  - **Test:** `app/src/test/java/com/quranwords/ui/ShareHandlerTest.kt::open_url_without_browser_shows_snackbar`, `::provider_error_shows_failure_not_empty_message`, and `::copy_immediate_writes_clipboard_once` (count writes in a fake); update `RootDetailRefactorTest`/Route tests for the new signatures.

- [x] **P1-3 — Stop hiding DB-update exceptions** (`app/src/main/java/com/quranwords/domain/usecase/CheckDbUpdateUseCase.kt`: `try/catch` L11–16 returns `DbCheckResult.Success(DbUpdateState.Unknown)`; `app/src/main/java/com/quranwords/ui/appstate/DbUpdateViewModel.kt`: `Error -> Unknown` L38–41; `app/src/main/java/com/quranwords/data/repository/DbUpdateRepositoryImpl.kt`: typed `Error` L71; test `app/src/test/java/com/quranwords/DbUpdateTest.kt::network failure maps to unknown not crash` L161–168).
  - **Root cause (verified):** the repository already returns typed `DbCheckResult.Error`; the use case's catch additionally converts any thrown exception into a *success* with `Unknown`, so VM Error handling is never reached for throws and no banner appears.
  - **Fix:** delete the `try/catch` (let the repository/use case contract carry errors); add `DbUpdateState.CheckFailed(message: String)` to `app/src/main/java/com/quranwords/domain/model/DbReleaseModels.kt` and map `DbCheckResult.Error` to it in the VM; render a small failure banner on Home (reuse `app/src/main/java/com/quranwords/ui/home/components/HomeBanners.kt`) with Retry that calls `checkOnce()` again. Update the existing test to assert `Error`/`CheckFailed` instead of `Unknown`.
  - **Acceptance:** throwing repo → `DbCheckResult.Error` → visible failure + Retry; no path silently maps a failure to `Unknown`.
  - **Test:** rewrite `DbUpdateTest.kt::network_failure_is_surfaced_as_check_failed` + `DbUpdateViewModelTest.kt::error_state_is_exposed_for_retry`.

- [x] **P1-4 — Resolve the Decision #37 / #42 `isActiveSurah` contradiction** (`app/src/main/java/com/quranwords/ui/surah/detail/components/AyahFlowGrouping.kt`: `MushafPageSegment.isActiveSurah` L91 and `splitPageIntoSegments` L95–110; stale Javadoc on `buildBlockIndexOfAyah` L63–66 mentions an `activeSurahId` parameter that does not exist; caller `app/src/main/java/com/quranwords/ui/surah/detail/components/SurahPagesPager.kt` L157 passes `activeSurahId = null`; tests `AyahFlowGroupingTest.kt` L128/L141/L165).
  - **Root cause (verified):** `isActiveSurah` is computed and unit-tested but never consumed; the pager forces `null` so every surah on a shared page is marked active, matching Decision #42 ("all surahs on a page are fully interactive") but contradicting Decision #37's field doc ("only the active surah owns selection/bookmarks/last-read/TalkBack").
  - **Fix:** make an explicit product decision and record it in `AGENTS.md` §20: either (a) supersede #37 for Pages mode with #42 and delete the unused field plus stale Javadoc and its three tests, or (b) thread `uiState.activeSurahId` into `splitPageIntoSegments` and gate selection/TalkBack by `segment.isActiveSurah`. **Recommended: (a)** — it deletes dead code and preserves current, intentional behaviour.
  - **Acceptance:** exactly one behaviour is documented; no field with a misleading doc survives; tests updated accordingly.
  - **Test:** if (a), remove the three `isActiveSurah` assertions; if (b), add `SurahPagesPager` Compose test for neighbour-surah selection being blocked.

- [x] **P1-5 — Decide and document the DB-missing product rule** (`app/src/main/java/com/quranwords/ui/home/HomeScreen.kt`: DB banner L122–129 and preview/navigation items; `app/src/main/java/com/quranwords/ui/setup/DatabaseSetupScreen.kt`: `db_preview_action` L341–350; AGENTS §8.5 says only the download screen shows when the DB is not ready).
  - **Root cause:** the app permits a preview flow through the whole UI while the stated rule says otherwise; this causes downstream false "DB missing" states (P0-5).
  - **Fix:** choose one rule with the owner and append a Decision Log entry: (a) keep preview and make every screen handle `DbNotReady` explicitly via `Result.Error`, or (b) gate all non-`Setup` routes on repository readiness in `app/src/main/java/com/quranwords/ui/navigation/AppNavigation.kt` and remove the preview button. **No code change in this task until the decision is recorded.**
  - **Acceptance:** AGENTS §8.5, README, and runtime behaviour agree; manual QA follows the chosen path.
  - **Test:** navigation test for the chosen rule (setup-only start, or preview still functional with explicit error states).

- [x] **P1-6 — Surface setup manifest/version-check failures** (`app/src/main/java/com/quranwords/ui/setup/DatabaseSetupViewModel.kt`: `checkForUpdate` L70–84 with `is DbCheckResult.Error -> Unit` L78; `resolveReleaseForDownload` L100–110 already maps errors to `SetupDownload.Error(MANIFEST_FAILED)`; screen `app/src/main/java/com/quranwords/ui/setup/DatabaseSetupScreen.kt`: `VersionStatusCard` L363–480 and generic `db_manifest_error` text L543).
  - **Root cause (verified):** the manual "Check for update" branch drops `DbCheckResult.Error` into `Unit`. `latestRelease` stays null, so the user sees a generic "check connection" line with no reason, no failure state and no retry button; it is indistinguishable from "not checked yet".
  - **Fix:** add `manifestError: String?` to `SetupUiState`; set it from `DbCheckResult.Error.message` in `checkForUpdate`, clear it while checking/on success; render a dedicated error line + Retry in `VersionStatusCard` that re-dispatches `SetupEvent.CheckForUpdate`. Leave the existing download-path error mapping untouched.
  - **Acceptance:** a failing manifest check shows a retryable error; a later success clears it; no `Result.Error -> Unit` remains in the setup VM.
  - **Test:** `app/src/test/java/com/quranwords/ui/DatabaseSetupViewModelTest.kt::manifest_check_error_is_exposed_and_retry_clears_it` + Compose `DatabaseSetupScreenTest.kt::manifest_error_shows_retry`.

---

## Phase 2 — State consolidation & UDF cleanup

- [x] **P2-1 — Replace positional `combine { args[i] as ... }` casts** (`RootDetailViewModel.kt` `uiState` L332–380 (14 args), `SurahDetailViewModel.kt` `screenState` L168–181 (7 args), `SearchViewModel.kt` `uiState` L77–99 (11 args), `WordAyatViewModel.kt` `uiState` L59–77 (8 args), `HomeViewModel.kt` `uiState` L46–60 (6 args)).
  - **Root cause:** `combine` with `List<Any>` + unchecked casts breaks at runtime if a flow is reordered or removed; the compiler cannot help.
  - **Fix:** use the typed `combine(a, b, c) { a, b, c -> ... }` overloads (up to the supported arity) or introduce one private data holder emitted by a single `map`/`combine` of typed pairs. Keep the resulting public `UiState` identical.
  - **Acceptance:** zero `as Boolean`/`as List<...>` casts in UI ViewModels; all five UiStates produce byte-equal values for the same inputs before/after.
  - **Test:** existing VM tests (`HomeViewModelTest`, `SearchViewModelTest`, `SurahDetailViewModelTest`, `RootsListViewModelTest`, new `RootDetailUiStateTest`) must pass unchanged.

- [x] **P2-2 — One authoritative state flow per feature** (`SurahDetailViewModel.kt`: `uiState` L134 + `screenState` L168 (only `screenState` is collected by `SurahDetailRoute` L30); `RootDetailViewModel.kt`: `_isCopyingAll` L56 with "backward compat" comment L60, `ensureOccurrencesLoadedForCount()` L201 (no callers); `RootDetailContract.kt`: `isCopying` L66 (no consumers); `SearchScreen.kt`: `bookmarkedAyat` passed both inside `SearchUiState` and as a separate parameter L64–65).
  - **Root cause:** parallel flows and duplicated parameters make the source of truth ambiguous (A11/A20).
  - **Fix:** expose one `uiState: StateFlow<...>` + `onEvent` + effects per feature; delete dead public members; pass `bookmarkedAyat` only through `SearchUiState`. Keep granular private flows only where they feed the single state.
  - **Acceptance:** no public flow named `uiState` that is unused by its Route; `rg` shows no `isCopyingAll` from RootDetail contracts/VM; SearchScreen has no duplicate bookmark parameter.
  - **Test:** update VM constructor/state tests; add `UiStateOwnershipTest.kt` asserting one collectible state flow per feature (reflection-free: compile-time usage in Routes).

- [x] **P2-3 — Move platform reads/effects out of `SurahDetailScreen`** (`app/src/main/java/com/quranwords/ui/surah/detail/SurahDetailScreen.kt`: Activity walk L198–202, `Settings.Global.getFloat` L306, `WindowInsetsControllerCompat` L256–272, `LocalContext`/`LocalView` L196–197; caller `SurahDetailRoute.kt` L26–31).
  - **Root cause:** the stateless screen reads system settings and drives window insets; AGENTS §10.3 forbids platform logic in Composables and makes this untestable.
  - **Fix:** add a small UI-side controller (e.g. `ui/surah/detail/ReaderSystemBarsController`) created in the Route with `LocalContext`/`LocalView`; pass `setImmersive(enabled)` and `animationsEnabled: Boolean` as plain values/callbacks to the screen. Keep the DisposableEffect that restores bars in the Route/controller.
  - **Acceptance:** `SurahDetailScreen.kt` has no `android.*`, `WindowInsetsControllerCompat`, `Settings`, or `ContextWrapper` references; preview with fake state works; rotation preserves bars state.
  - **Test:** Robolectric/Compose `ReaderSystemBarsControllerTest.kt::immersive_toggle_hides_and_restores_bars` + existing `SurahDetailViewModelTest` immersive tests unchanged.

- [x] **P2-4 — Hoist `ReportMeaningDialog`'s ViewModel** (`app/src/main/java/com/quranwords/ui/components/ReportMeaningDialog.kt`: default parameter `viewModel: ReportMeaningViewModel = hiltViewModel()` L69, collected flows L72–75, `currentMarkdown()`/`dismissAndReset()` L77–90; caller `app/src/main/java/com/quranwords/ui/roots/detail/RootDetailScreen.kt` L113–117).
  - **Root cause:** a leaf component owns a Hilt VM and state, contradicting the Route/Screen split used everywhere else; the dialog is not previewable without Hilt.
  - **Fix:** move `hiltViewModel()` and the four collected flows into `RootDetailRoute`; pass `reportType/description/suggestion/canSubmit` + `onEvent(...)` into the dialog, or fold them into `RootDetailUiState` via a `ReportMeaningState`. Route handles `Reset` alongside `DismissReport`.
  - **Acceptance:** dialog has zero VM/Context/Hilt imports; previews render with plain state; copy/share/report URL callbacks unchanged.
  - **Test:** `ReportMeaningDialogTest.kt::dialog_renders_with_plain_state_and_emits_callbacks` (Compose, no Hilt container).

- [x] **P2-5 — Home contract completeness** (`app/src/main/java/com/quranwords/ui/home/HomeContract.kt` has only `HomeUiState`; `app/src/main/java/com/quranwords/ui/home/HomeScreen.kt` takes 14 callbacks; `HomeRoute.kt` hosts three ViewModels).
  - **Root cause:** AGENTS §10.1 expects an Event contract for meaningful state; Home uses callback-only navigation (acceptable for trivial callbacks) but has no documented decision.
  - **Fix:** either add `HomeEvent` (OpenSettings, ToggleDarkMode, ToggleDynamicColor, DismissUpdate, Navigate*) routed through `HomeViewModel.onEvent`, or append a Decision Log note that Home's callbacks are pure navigation/preferences already owned by dedicated VMs and are the documented exception. Prefer the note — adding pass-through events would create trivial use cases (AGENTS §9).
  - **Acceptance:** AGENTS §10.1 deviation is explicit; no new indirection without behaviour value.
  - **Test:** no new test required if the documented-exception option is chosen; otherwise update `HomeViewModelTest`.

- [x] **P2-6 — Move meaningful screen-local state into contracts** (`SurahDetailScreen.kt`: `showFontSheet` L129, `wordAction` L130, `pendingPage` L128, pulse flags L125–127; `DatabaseSetupScreen.kt`: `showImportDialog`/`showNotes` L71–72).
  - **Root cause:** UDF is bypassed for state that affects what the user sees; only transient animation state belongs in a Composable.
  - **Fix:** add fields/events to `SurahDetailContract` (`wordAction`, `pendingPage`, `fontSheetVisible`, `pulseCompleted` where rotation persistence matters) and `SetupContract` (`importDialogVisible`, `releaseNotesExpanded`); keep pure animation values (`Animatable`, one-shot flags used only inside `LaunchedEffect`) local. Route forwards events to VMs.
  - **Acceptance:** no `mutableStateOf` in screens that represents user-visible state; restore-after-rotation test for the font sheet and popup.
  - **Test:** `SurahDetailViewModelTest.kt::word_action_and_font_sheet_state_survive_rotation` (SavedStateHandle-backed), Compose `DatabaseSetupDialogTest.kt::dialog_state_restored`.

- [x] **P2-7 — Unify paging state and drop artificial delays** (`SearchViewModel.kt` L131 `delay(200)`, L202 `delay(80)`; `RootDetailViewModel.kt` L186 `delay(80)`; `WordAyatViewModel.kt` L160 `delay(80)`; each has its own `offset`/`hasMore`/`isLoadingMore` trio).
  - **Root cause:** three copies of the same state machine drifted (A16; WORDS tab pagination was already broken once). Fixed delays are not debouncing at the data layer.
  - **Fix:** extract one small pure `PagedListState` holder (offset, hasMore, loading) in `ui/`/`core/util/` reused by the three VMs; remove the `delay(80)` calls. Keep the 200 ms search debounce but express it as a named constant and test with a TestDispatcher. Paging3 migration remains a separate ADR (Decision #12).
  - **Acceptance:** no `delay(80)` in UI VMs; identical paging behaviour for search/root/word; tests use `runTest` + scheduler advance, no real sleeps.
  - **Test:** `PagedListStateTest.kt` (pure boundary cases: empty page, partial page, error), plus existing paging tests.

- [x] **P2-8 — Remove duplicate state plumbing/naming confusion** (`SearchRoute.kt` records recents for nav callbacks L33–45 while `SearchScreen.kt` `onWordSelected` L82–85 also calls `onRecordRecent`; two different `MushafPageContent` functions in `SurahAyatList.kt` and `SurahPagesPager.kt`; `pageRangeAyat` in `SurahDetailContract.kt` L8 marked legacy/fallback).
  - **Root cause:** duplicated writes and ambiguous names make changes error-prone (A20).
  - **Fix:** record each recent query in exactly one place (Route); rename the pager-local composable to `SurahMushafPage`; delete `pageRangeAyat` if Phase 1 rule (b) is not chosen, or document it as the unpaged fallback only.
  - **Acceptance:** one `recordRecent` call per user action (`FakePreferences` assertion: one MRU entry, not two); no duplicate top-level composable names in the package.
  - **Test:** `SearchViewModelTest.kt::recent_recorded_once_per_navigation`.

- [x] **P2-9 — One settings API, one dark-mode cycle** (`app/src/main/java/com/quranwords/ui/settings/SettingsViewModel.kt`: stale KDoc "Cycles system → dark → light" L82, `toggleDarkMode` L83–92 (`1 -> 2`, `2 -> 1`, `else -> 2`), `toggleDynamicColor` L94–98, `onEvent` L72–80; `app/src/main/java/com/quranwords/ui/settings/SettingsDialog.kt`: radio events L54–65 / L78–91; `app/src/main/java/com/quranwords/ui/home/HomeRoute.kt` L57–58 passes VM methods directly; `app/src/main/java/com/quranwords/MainActivity.kt` L38–41 maps 0/1/2).
  - **Root cause (verified):** the dialog writes exact values via `SettingsEvent.DarkModeChanged(0|1|2)` through `onEvent`, while HomeHeader calls `toggleDarkMode()` / `toggleDynamicColor()` directly. The quick toggle can never return to System (`else -> 2`) even though its KDoc promises a 3-state cycle. The two paths do not write conflicting values, so this is **Phase 2, not Phase 0**: duplicated APIs plus a false documented cycle.
  - **Fix:** add `SettingsEvent.ToggleDarkMode` / `ToggleDynamicColor` to `SettingsContract.kt`; move the cycle into a pure `nextDarkMode(current)` helper (core/util or VM) with one branch test; make `HomeRoute` pass `settingsViewModel::onEvent` and delete the public toggle methods; fix the KDoc. Dialog radios keep setting exact values through the same `onEvent`.
  - **Acceptance:** all theme writes flow through `SettingsViewModel.onEvent`; the cycle is consistent with its KDoc; no public toggle methods remain.
  - **Test:** `app/src/test/java/com/quranwords/ui/SettingsViewModelTest.kt::toggle_event_cycles_all_three_modes_and_dialog_event_sets_exact_mode`.

- [x] **P2-10 — Typed QuickJump error state** (`app/src/main/java/com/quranwords/ui/surah/components/QuickJumpDialog.kt`: `var error by remember { mutableStateOf<String?>(null) }` L43, `error = "page"` L70, `error = "ayah"` L101, single rendering branch L108–113 using `R.string.quick_jump_invalid`).
  - **Root cause (verified):** two magic strings encode which input failed but are only compared against null; both render the same message, so the values carry no information and can drift silently.
  - **Fix:** replace with an enum/sealed `QuickJumpError { PAGE, SURAH_AYAH }` (local or in `SurahIndexContract.kt`), or delete the value entirely in favour of `invalidInput: Boolean` if no distinct message is wanted. Keep clearing on input change.
  - **Acceptance:** no stringly typed sentinels in the dialog; both invalid paths still show `quick_jump_invalid` and clear when the user edits a field.
  - **Test:** Compose `app/src/test/java/com/quranwords/ui/surah/QuickJumpDialogTest.kt::invalid_page_and_invalid_ayah_show_error_and_clear_on_edit`.

> **Documented exception (no task) — `MorphologyGuideScreen`** (`app/src/main/java/com/quranwords/ui/guide/MorphologyGuideScreen.kt`: locale branch L47–50 reads `LocalConfiguration.current.locales` (L50) to choose `MorphologyMaps.FORMS_MAP` vs `FORMS_MAP_EN`). Verified: this is a static, stateless screen with one navigation callback; AGENTS §10.1 explicitly allows no Event hierarchy for such screens, and reading configuration in composition is not a platform side-effect. No Contract/ViewModel is warranted. Keep the locale choice next to the data it selects; extract a pure `isArabicLocale(...)` helper only if a locale bug appears.

---

## Phase 3 — De-duplication & God-file decomposition

> Rule: behaviour-preserving moves only. Each split lands with the existing tests still green; no new features, no visual changes.

- [ ] **P3-1 — Single shared mushaf text builder** (`app/src/main/java/com/quranwords/ui/surah/detail/components/MushafFlowBlock.kt`: `buildAnnotatedString { ... }` inside `annotated` remember L105–146 with the "mirrored by mushafBlockLayoutText" comment L112–113; `app/src/main/java/com/quranwords/ui/surah/detail/components/MushafFontSizeFit.kt`: `mushafBlockLayoutText` L105–121 with "Keep both builders in sync" comment L111).
  - **Root cause (verified):** the render builder and the fit-measurement builder duplicate character construction (sanitized words, inter-ayah/inter-word spaces, `\u00A0﴿n﴾` markers). Any drift makes the fit measure text that is not what is drawn.
  - **Fix:** extract a pure `buildMushafAnnotatedString(ayat, fontSize, options)` in a new `MushafTextBuilder.kt` in the same package. Options control paint-only spans/annotations (selection, bookmark, word/marker tags, long-press focus); the character stream and layout-affecting `SpanStyle(fontSize = fontSize*0.9, bold)` are produced by one code path. Render and fit both call it; fit passes no annotations.
  - **Acceptance:** `mushafBlockLayoutText(...).text == renderBuilder(...).text` for paged/unpaged, single/multi-surah and wordless-ayah fixtures; existing `MushafFontSizeFitTest` unchanged.
  - **Test:** `MushafTextBuilderTest.kt::character_equivalence_between_render_and_fit_builders` (P5-7).

- [ ] **P3-2 — Split `SurahDetailScreen.kt` (567 lines)** (symbols: `rememberSurahReadingScroll` call L134, derived progress L163–180, system-bars L256–272, `BackHandler`s L277–281, load L283, initial placement L311, pulse L330, last-read L343, page chip L364, mode flip L374–391, content switch L429–474, chrome L478–540).
  - **Root cause:** one Composable owns seven unrelated jobs (A10).
  - **Fix:** extract, in this order and without behaviour change: `ReaderPlacementEffects.kt` (initial scroll, pulse, last-read, page chip), `ReaderChromeHost.kt` (AnimatedContent, scrim, ReadingTopBarOverlay/SelectionTopBar), `ReaderContentSwitch.kt` (pager vs list + loading/empty states), `WordInteractionCallbacks.kt` (move the existing data class out of `SurahPagesPager.kt` into its own file). Screen keeps orchestration only, target <200 lines.
  - **Acceptance:** screen <200 lines; all current LaunchedEffect keys and ordering preserved; existing reader tests pass (especially P0-8/P0-9).
  - **Test:** existing `SurahDetailViewModelTest`, `SurahReadingScrollTest`, plus new Compose smoke test `SurahDetailScreenSmokeTest.kt::scroll_and_pages_modes_render`.

- [ ] **P3-3 — Split `MushafFlowBlock.kt` (559 lines)** (symbols: annotated builder L105–146, pulse geometry L365–450, `drawWithContent` L350–430, `handleTap`/`handleLongPress` L207–248, semantics L292–345).
  - **Root cause:** rendering, hit testing, accessibility, pulse animation and popup hosting in one file (A10).
  - **Fix:** move pure helpers first: `pulseGeometry`, `ayahAt`, `lastLine*` into `MushafPulseGeometry.kt`; move gesture handlers into `MushafGestures.kt` (with the P0-8 updated-state fix); move the overlay drawing into `MushafPulseOverlay.kt`; keep annotated building in the shared builder from P3-1; accessibility actions get their own `rememberMushafActions` helper (P4-2).
  - **Acceptance:** block Composable <180 lines; pure helpers unit-tested; no visual/gesture regression.
  - **Test:** `MushafWordHitTestTest`, `SurahReadingScrollTest`, new `MushafPulseGeometryTest`.

- [ ] **P3-4 — Split `SurahDetailViewModel.kt` (497 lines)** (symbols: `ReaderCoreState` L43–68, `PageCache` L73–83, prefs init L118–124, `loadSurah` L227–316, `loadPageIfMissing` L323, `onPageChanged` L340–382, `retryPage`/`setEntryPage` L387–420, selection/copy L430–497).
  - **Root cause:** pages-mode cache, preferences, selection, copy and last-read in one VM.
  - **Fix:** extract `MushafPageCache.kt` (bounded LRU + pinned entry, pure/main-confined as today) and `SurahReaderSelection.kt` (selection + formatter helpers); keep the VM as orchestrator with `onEvent`. Do not split the VM into multiple VMs (would break shared state).
  - **Acceptance:** VM <300 lines; page-cache tests (`SurahDetailViewModelTest` L229–333) still pass unchanged.
  - **Test:** `PageCacheTest.kt::lru_eviction_keeps_pinned_entry`.

- [ ] **P3-5 — Split `RootDetailViewModel.kt` (461 lines)** (symbols: pagination fields L43–52, copy flags L56–73, selections L76–86, `loadRootDetail` L109, `loadRootWords` L162, `loadMoreOccurrences` L181, formatting L229–320, `uiState` combine L332–380, `onEvent` L404–462).
  - **Root cause:** detail loading, paging, copy formatting and event routing in one class (A10).
  - **Fix:** extract `RootOccurrencePaginator.kt` (offset/hasMore/savedState restoration) and `RootCopyFormatter.kt` (all `get*Formatted` methods returning `Result<String>` from P1-2); delete legacy copy flag after P2-2. VM keeps UiState assembly + events.
  - **Acceptance:** VM <300 lines; error paths from P0-4 and copy paths from P1-2 still covered.
  - **Test:** `RootOccurrencePaginatorTest.kt`, plus existing `RootDetailRefactorTest`/`RootWordsTest`.

- [ ] **P3-6 — Split `DatabaseSetupScreen.kt` (575 lines)** (symbols: state branches inside `Crossfade` L177–319, `VersionStatusCard` L363–480, `downloadErrorMessage` L483, `formatVersionSize` L497).
  - **Root cause:** five download states + version card + dialog + helpers in one file (A10).
  - **Fix:** extract `SetupDownloadStates.kt` (Idle/Progress/Extracting/Completed/Error bodies as small stateless composables), `SetupVersionCard.kt`, `SetupImportDialog.kt`, `SetupFormatters.kt`. Screen keeps Scaffold/LazyColumn composition.
  - **Acceptance:** screen <200 lines; no change to testTags (`start_download_button`, `retry_download_button`, `finish_setup_button`, `db_version_card`, etc.).
  - **Test:** Compose `DatabaseSetupScreenTest.kt::each_download_state_renders_expected_cta` (tags asserted).

- [ ] **P3-7 — Split `RootDetailRoute.kt` event handler + format long lines** (`app/src/main/java/com/quranwords/ui/roots/detail/RootDetailRoute.kt`: top-level `handleRootDetailEvent` L60–154 with 12 platform branches; `aiSectionFor` L157–162; `app/src/main/java/com/quranwords/ui/roots/detail/RootDetailScreen.kt`: pager tab calls L100–110 are 584–852 characters).
  - **Root cause:** one function switches over every copy/share variant (A10), and tab call sites are unreadable.
  - **Fix:** group handlers into `handleMeaningsCopyShare`, `handleWordsCopyShare`, `handleOccurrencesCopyShare`, `handleReportActions` in a new `RootDetailPlatformEvents.kt`; format each tab call as a multi-line invocation with named arguments. No behaviour change.
  - **Acceptance:** no line >120 characters in these two files; Route body <60 lines.
  - **Test:** existing `RootDetailRefactorTest` + a Route-level Compose test for one copy and one share path.

- [ ] **P3-8 — Fix `CitedBodyText` citation-session memoization** (`app/src/main/java/com/quranwords/ui/roots/detail/components/cards/CitedBodyText.kt`: `val session = remember(body, mutableListOf<String>()) { CitationMatcher.Session(mutableListOf<String>()) }` L84; `fragmentsByBlock` L85–87; `CitedBlockText` `remember(block, fragments, style, colors)` L173 and `pointerInput(annotated)` L180; highlight reset L89–95).
  - **Root cause (verified):** a freshly allocated `mutableListOf<String>()` is used as a `remember` key, so it is never equal across recompositions. The `Session`, every cited `AnnotatedString`, and the pointer-input node are rebuilt on every recomposition (e.g. the 1.5 s highlight reset), defeating the cache its own comment promises. Grep confirms this is the only fresh-collection `remember` key in `ui/`.
  - **Fix:** `remember(body) { CitationMatcher.Session(mutableListOf()) }`; document that `body` is the session identity. No citation-numbering or rendering change intended.
  - **Acceptance:** with unchanged `body`, the same Session instance survives recomposition, citation numbers/refs are identical, and cite taps still highlight.
  - **Test:** `app/src/test/java/com/quranwords/ui/CitedBodyTextTest.kt::session_reused_when_body_unchanged_and_renumbered_when_body_changes` (Compose test asserting stable annotated-string/session identity across a highlight reset).

---

## Phase 4 — Localization, design tokens, accessibility & dead code

- [ ] **P4-1 — Move hardcoded user strings into resources** (`SearchResultCards.kt`: `pageJuzSuffix` L95–103 has literal `الصفحة`/`الجزء`; `AyahSurahFilterBar.kt`: L63 `"سورة $surahName"`, L72 `"${id}. سورة ${nameAr}"`; `FontSizeControls.kt`: L50 `"A-"`, L76 `"A+"`; `SurahCardBanner.kt`: `BISMILLAH` L54 is Arabic **reference data** and stays).
  - **Root cause:** English UI shows Arabic chrome (A13); AGENTS §10.5/§21 require resource strings.
  - **Fix:** add `search_page_suffix`, `search_juz_suffix`, `search_surah_filter_selected`, `search_surah_menu_item`, `font_decrease`, `font_increase` to `app/src/main/res/values/strings.xml` and `values-ar/strings.xml`; use `stringResource(...)`. Keep Arabic Quran text as reference data.
  - **Acceptance:** switching language to English shows no Arabic chrome in search/font controls; both locales compile.
  - **Test:** Compose `SearchChromeLocalizationTest.kt::english_locale_has_no_arabic_filter_labels` (or string-resource parity test).

- [ ] **P4-2 — Memoize TalkBack actions and align with the active-surah decision** (`app/src/main/java/com/quranwords/ui/surah/detail/components/MushafFlowBlock.kt`: `semantics(mergeDescendants = true)` L292, `customActions = buildList<CustomAccessibilityAction>` L304–344 builds one action per word + per ayah on every recomposition; decision context in P1-4).
  - **Root cause (verified):** no `remember` around the ~200-item list; every recomposition (pulse, selection, scroll-derived state) reallocates it. Callback captures also go stale unless keys are complete.
  - **Fix:** compute the list in `remember(group, selectedAyahs, bookmarkedAyahs, isSelectionMode, morphologyLabel, ayahActionLabel, rangeActionLabel) { ... }` with callbacks read via `rememberUpdatedState` wrappers; apply whichever gating P1-4 chose. Consider one action per ayah (opening a small action sheet) only if the decision log allows a UX change — otherwise keep the list but stop rebuilding it.
  - **Acceptance:** action count unchanged for a given state; no reallocation on unrelated recompositions (verify with a Compose recomposition counter test); labels localized (P4-1/P4-3).
  - **Test:** `MushafAccessibilityTest.kt::custom_actions_stable_across_recomposition` + TalkBack label assertions.

- [ ] **P4-3 — Eastern digits everywhere user-visible** (`app/src/main/java/com/quranwords/ui/components/StatCard.kt`: `NumberFormat.getNumberInstance(Locale.US)` L45; `app/src/main/java/com/quranwords/ui/surah/detail/components/ReadingProgressRings.kt` already uses `toEasternArabicDigits`; `app/src/main/res/values/strings.xml`: `number_plain` convention from Decision #31).
  - **Root cause:** Home counts stay Latin digits in Arabic while surah/page numbers are Eastern (A13).
  - **Fix:** format stats with a locale-aware formatter (`number_plain` plural/string resource with ICU digits, or `toEasternArabicDigits` on the formatted integer), remove the hardcoded `Locale.US`.
  - **Acceptance:** Arabic locale → `٧٧٬٤٢٩`-style digits; English locale → `77,429`; no locale hardcoded in composables.
  - **Test:** `StatCardTest.kt::arabic_locale_uses_eastern_digits` (Robolectric locale or Compose `LocalConfiguration` override).

- [ ] **P4-4 — Use design tokens instead of raw values** (`SearchResultCards.kt`: `Color(0xFF285C4F)` L127; `app/src/main/java/com/quranwords/ui/theme/Theme.kt`: raw `Color(0xFF293023)` L39 and `Color(0xFFE5EBDB)` L49; ~40 `RoundedCornerShape(...)` literals across UI, e.g. `DatabaseSetupScreen.kt` L10 occurrences, `SearchResultCards.kt` L7; `app/src/main/java/com/quranwords/ui/theme/Shapes.kt` defines tokens).
  - **Root cause:** theme changes and dark mode are inconsistent (A14); AGENTS §10.4 says colors come from `Color.kt` and shapes from `Shapes.kt`.
  - **Fix:** add the highlight color to `Color.kt`; move the two scheme colors there; replace rounded-corner literals with `MaterialTheme.shapes`/`AppShapes` tokens where a token exists (keep genuinely unique radii as named tokens rather than inline numbers).
  - **Acceptance:** zero `Color(0x` outside `ui/theme/Color.kt`/`Theme.kt` scheme declarations; no inline `RoundedCornerShape(12.dp)` duplicates of an existing token.
  - **Test:** static check (`rg`) plus Roborazzi screenshot `HomeScreen`/`SurahDetailScreen` in light+dark to catch contrast regressions.

- [ ] **P4-5 — 48dp interaction targets** (`AiSummaryTab.kt`: 28dp copy `IconButton` L103–112; `AyahSurahFilterBar.kt`: 30dp chip Surface L88; `FontSizeControls.kt`: 32dp IconButtons L40/L66; `VerseReferenceBanner.kt`: 36dp decorative Surface L49 is not interactive and may stay).
  - **Root cause:** touch targets below the AGENTS §10.5 minimum (A14).
  - **Fix:** give interactive nodes 48dp bounds via `Modifier.minimumInteractiveComponentSize()` or padding while keeping the visual icon/chip size unchanged; preserve testTags.
  - **Acceptance:** every clickable element has ≥48×48dp bounds; visual density unchanged.
  - **Test:** Compose `TouchTargetSizeTest.kt` asserting `getBoundingBox()` height/width ≥48 for the three tags.

- [ ] **P4-6 — State-aware bookmark TalkBack label in search** (`app/src/main/java/com/quranwords/ui/search/components/SearchResultCards.kt`: `IconButton` L79–89 uses `contentDescription = stringResource(R.string.cd_remove_bookmark)` for both states; compare correct pattern `app/src/main/java/com/quranwords/ui/surah/components/SurahRow.kt`: L93 selects `cd_unbookmark_surah`/`cd_bookmark_surah`; strings in `values/strings.xml` L93/L150 and `values-ar/strings.xml` L78/L128).
  - **Root cause (verified):** an unbookmarked ayah announces "Remove bookmark" (N7).
  - **Fix:** add `cd_bookmark_ayah` / `cd_unbookmark_ayah` to both locale files and select by `isBookmarked`; optionally add `contentDescription` to the whole AyahSearchCard click target.
  - **Acceptance:** TalkBack announces "Bookmark ayah" before saving and "Remove bookmark" after.
  - **Test:** Compose `SearchResultCardsTest.kt::bookmark_description_reflects_state` using `onNodeWithTag("bookmark_toggle_2_3")`.

- [ ] **P4-7 — Delete dead and stale code** (`UpdateBanner.kt` commented `UpdateFailedBanner` body (already restored in P0-2 — ensure no comment remains); `RootDetailContract.kt` L15 "Old granular flows stay (deprecated)" + L66 `isCopying`; `RootDetailViewModel.kt` L60 legacy comment + L201 dead method; `Theme.kt` KDoc L29–34 describes a `colorMode` parameter that no longer exists; Decision #34's `copy_all_ai_sections` action is absent from `AiSummaryTab.kt` and strings; **N14:** `app/src/main/java/com/quranwords/ui/home/components/HomeBanners.kt` `HomeUpdateBanner` L101–113 is a pass-through calling `DbUpdateBanner` with `installedName = ""`, used only by `HomeScreen.kt` L83).
  - **Root cause:** abandoned work and stale docs (A18); `HomeUpdateBanner` adds no behaviour and suppresses the installed-name argument (N14).
  - **Fix:** delete dead members/comments; fix the Theme KDoc; either implement the "نسخ الكل" AI bar (`CopyAllAiSections` event + `QuranCopyFormatter` all-sections payload + strings) or append a Decision Log note retiring Decision #34's copy-all requirement. For N14, call `DbUpdateBanner` directly from `HomeScreen` (passing the real installed-name value if it is meaningful, otherwise keep `""` at the call site) and delete `HomeUpdateBanner`.
  - **Acceptance:** no commented-out code blocks in `ui/`; no doc referring to removed parameters; no one-line wrapper components that only forward arguments; decision log matches the code.
  - **Test:** `QuranCopyFormatterTest.kt::all_ai_sections_payload_contains_every_visible_card` if implemented; `HomeTitleScreenshotTest` unchanged after removing the wrapper (banner testTag `db_update_banner` still present).

- [ ] **P4-8 — Small duplication cleanups** (`app/src/main/java/com/quranwords/ui/home/HomeScreen.kt`: private `placementSpec()` L133–136; `BookmarksScreen.kt`: same tween inlined 5× at L106/L136/L145/L156/L165; Basmalah literal in `SurahCardBanner.kt` L54 and `SurahAyatList.kt` L77; two near-copy bookmark cards in `BookmarksScreen.kt` L156–319).
  - **Root cause:** A19; values drift silently across screens.
  - **Fix:** add `AppMotion.itemPlacementSpec()` in `ui/theme/Motion.kt` used by both screens; add a single `BISMILLAH` constant in a shared reference-data file (Arabic data, not translatable); keep the two bookmark cards separate unless a shared shape/Row is extracted without adding parameters.
  - **Acceptance:** one placement-spec definition; one Basmalah constant; no new abstraction for the two distinct cards.
  - **Test:** existing `HomeTitleScreenshotTest` + `BookmarksViewModelTest` remain green.

- [ ] **P4-9 — One pure file-size formatter** (`app/src/main/java/com/quranwords/ui/components/DbUpdateBanner.kt`: `formatDbSize(bytes: Long, context: android.content.Context)` L31–37; `app/src/main/java/com/quranwords/ui/setup/DatabaseSetupScreen.kt`: `formatVersionSize(bytes: Long)` L570–576 plus inline `/ (1024 * 1024)` at L259–260; shared strings `R.string.db_size_mb` / `db_size_kb`).
  - **Root cause:** two formatters implement the same KB/MB rule — one needs a `Context`, one calls `stringResource` — and the setup screen also converts bytes inline, so thresholds can drift.
  - **Fix:** add a pure `app/src/main/java/com/quranwords/core/util/FileSizeFormatter.kt` returning `FileSize(value: Long, unit: SizeUnit)?` (null for `bytes <= 0`, MB when value >= 1 MB, otherwise KB, 1024 divisors); both composables map it to the existing string resources and the em-dash. Delete both old helpers and the inline division. No `Context` in `core/util`.
  - **Acceptance:** identical output at 0, 1 KB, 1 MB-1, 1 MB and 118 MB; no duplicated `1024` math in `ui/`/`core/di`.
  - **Test:** `app/src/test/java/com/quranwords/core/FileSizeFormatterTest.kt::boundaries_and_zero` + Compose `DbUpdateBannerTest.kt::shows_mb_size`.

- [ ] **P4-10 — Resolve design-system naming/token duplication** (`app/src/main/java/com/quranwords/ui/theme/Type.kt`: `val Typography = Typography(...)` L9 shadows the Material3 class; `app/src/main/java/com/quranwords/ui/theme/Shapes.kt`: `AppShapes` L8–14 and aliases `ShapeSmall/ShapeMedium/ShapeLarge/ShapeExtraLarge` L17–20 repeat the same radii; `app/src/main/java/com/quranwords/ui/theme/Theme.kt` installs `AppShapes`).
  - **Root cause:** the app typography value has the same name as the M3 class (confusing imports), and two shape sources must stay in sync.
  - **Fix:** rename the value to `AppTypography` and update `Theme.kt`; make the shape aliases derive from `AppShapes.small/medium/large/extraLarge` (or delete them and use `MaterialTheme.shapes.*` where theme-aware values work). Keep radii 8/12/16/20/28 unchanged.
  - **Acceptance:** one source of truth for shape radii; no top-level value shadowing `androidx.compose.material3.Typography`; screenshots pixel-identical.
  - **Test:** `app/src/test/java/com/quranwords/ui/theme/ThemeTokenTest.kt::shape_aliases_match_app_shapes` + existing Roborazzi/screenshot comparisons.

- [ ] **P4-11 — Remove remaining emoji glyphs (Decision #13)** (`app/src/main/java/com/quranwords/ui/components/DbUpdateBanner.kt`: `Text("⬆️")` L67; `app/src/main/res/values/strings.xml`: `db_update_available` L317, `db_import_title` L327, `bookmarks_empty_hint` L173 (`☆`); mirrored in `app/src/main/res/values-ar/strings.xml` L282/L292/L147; stale emoji comment in `app/src/main/java/com/quranwords/ui/home/components/HomeQuickCards.kt` L106).
  - **Root cause:** emoji/decorative glyphs still ship despite Decision #13 ("all emojis out — vectors + contentDescription").
  - **Fix:** replace the banner arrow and import-folder icon with `Icons.*` vectors (or drop the decorative icon where the adjacent text suffices); rewrite both `bookmarks_empty_hint` strings without `☆`; delete the stale comment. Quran reference symbols such as `﴿n﴾` are not emojis and stay.
  - **Acceptance:** no emoji code points in `values*/strings.xml` or UI source (grep from this task returns nothing); interactive icons keep content descriptions.
  - **Test:** `app/src/test/java/com/quranwords/ui/UiResourcesTest.kt::no_emoji_in_user_strings` + Compose `DbUpdateBannerTest.kt::update_banner_uses_vector_icon`.

- [ ] **P4-12 — Replace hardcoded font defaults and raw `sp` values** (`app/src/main/java/com/quranwords/ui/components/MorphologyBottomSheet.kt`: `fontSize: Float = 24f` L66; `app/src/main/java/com/quranwords/ui/roots/detail/RootDetailContract.kt` L59, `RootDetailViewModel.kt` L90, `app/src/main/java/com/quranwords/ui/surah/detail/SurahDetailContract.kt` L39, `SurahDetailViewModel.kt` L119; canonical `app/src/main/java/com/quranwords/core/util/FontSizePreference.kt`: `DEFAULT = 24f` L17; raw/fractional sp in `SearchScopeChips.kt` L66/L78, `SearchResultCards.kt` L284, `SearchExplorerEmptyView.kt` L50/L65, `VerseReferenceBanner.kt` L58, `AyahSurahFilterBar.kt` L100, `PageHeaderFooter.kt` L50/L65, `JuzHeaderRow.kt` L49/L55, and `SettingsDialog.kt` `FontOptionRow` L153–183 (18/16 sp previews at L174/L181)).
  - **Root cause:** five copies of the default font size can drift from `FontSizePreference.DEFAULT`; fractional `sp` literals bypass the M3 type scale (A14, Decision #26).
  - **Fix:** import `FontSizePreference.DEFAULT` in every contract/VM/sheet default; add named `AppTypography` tokens (from P4-10) for the repeated small sizes and replace call sites. Leave the `24f` in `app/src/main/java/com/quranwords/ui/surah/detail/components/SurahAyatList.kt` L82 alone — that is the Basmalah size multiplied by the quran font's line-height, not the user preference.
  - **Acceptance:** `rg "24f" app/src/main/java/com/quranwords/ui` returns only the Basmalah line and comments; no fractional `.sp` literals in feature components.
  - **Test:** `app/src/test/java/com/quranwords/ui/FontDefaultTest.kt::all_contracts_use_font_size_preference_default` + existing `ArabicTextScaleTest` unchanged.

---

## Phase 5 — Focused tests & final verification

> Rule: one good test per fixed bug, not coverage theatre. All tests run by the user (AGENTS §16).

- [ ] **P5-1 — Test harness for Composables** (`app/src/test/java/com/quranwords/...`, dependency `androidx.compose.ui.test.junit4` already in `app/build.gradle.kts`).
  - **Root cause:** only `HomeTitleScreenshotTest.kt` uses Compose today (A15).
  - **Fix:** add a small `TestTag` constants file if needed (convention `<feature>_<element>_<id>` per AGENTS §15) and a `setContent { MyApplicationTheme { ... } }` helper with LTR/RTL parameter; no new dependency.
  - **Acceptance:** helper compiles under Robolectric; existing screenshot test still passes.
  - **Test:** `ComposeTestFixtures.kt` self-check rendering a `Text` with a testTag.

- [ ] **P5-2 — Search card tests** (`SearchResultCards.kt`).
  - **Fix/tests:** `SearchResultCardsTest.kt::real_ayah_marker_for_matched_query`, `::real_ayah_marker_for_unmatched_query` (P0-1), `::bookmark_description_reflects_state` (P4-6), `::page_juz_suffix_uses_resources` (P4-1).
  - **Acceptance:** hardcoded `٠` and "Remove bookmark" regressions fail these tests.

- [ ] **P5-3 — Bookmark action test** (`SurahDetailViewModel.kt::bookmarkSelection`).
  - **Fix/tests:** `SurahDetailViewModelTest.kt::bookmark_selection_preserves_existing_bookmarks`, `::bookmark_selection_writes_once_for_all_refs`, `::bookmark_failure_is_surfaced` (P0-3).
  - **Acceptance:** mixed-state selection ends fully bookmarked; no toggle is called.

- [ ] **P5-4 — Reader behaviour tests** (`SurahDetailScreen.kt`, `MushafFlowBlock.kt`, `SurahAyatList.kt`).
  - **Fix/tests:** `WordActionPointerTest.kt::outside_tap_dismisses_only_popup` (P0-8), `SurahDetailViewModelTest.kt::last_read_after_uncached_page_loads` (P0-9), `SurahAyatListUnpagedTest.kt::null_page_number_is_safe` (P0-10), `SurahDetailErrorUiTest.kt::error_message_and_retry` (P0-5).
  - **Acceptance:** all four failure cases fail if their Phase 0 fix is reverted.

- [ ] **P5-5 — RootDetail and WordAyat failure/retry tests** (`RootDetailViewModel.kt`, `WordAyatViewModel.kt`).
  - **Fix/tests:** `RootDetailErrorTest.kt::detail_error_and_retry`, `::paging_error_and_retry`; `WordAyatViewModelTest.kt::paging_error_keeps_has_more_and_retry_loads_next_page` (P0-4/P0-6).
  - **Acceptance:** no spinner-only state; Retry advances one page.

- [ ] **P5-6 — Search paging failure test** (`SearchViewModel.kt`).
  - **Fix/tests:** `SearchViewModelTest.kt::paging_error_preserves_results_and_offset`, `::inline_retry_resumes_paging`, `::recent_recorded_once_per_navigation` (P0-7/P2-8).
  - **Acceptance:** global error view reserved for initial query failures only.

- [ ] **P5-7 — Mushaf text equivalence test** (`MushafTextBuilder.kt` from P3-1).
  - **Fix/tests:** `MushafTextBuilderTest.kt::character_equivalence_between_render_and_fit_builders` with cases: one ayah with words, ayah without words, multi-ayah page, two surahs on one page, Eastern-digit markers, selection/bookmark spans present vs absent (only `.text` compared).
  - **Acceptance:** removing the shared builder cannot reintroduce two divergent copies.

- [ ] **P5-8 — Update/DB-version tests** (`CheckDbUpdateUseCase.kt`, `DbUpdateViewModel.kt`, `UpdateBanner.kt`, `DatabaseSetupViewModel.kt`).
  - **Fix/tests:** rewrite `DbUpdateTest.kt::network_failure_is_surfaced_as_check_failed` (P1-3); `UpdateBannerTest.kt::failed_banner_renders_message_and_dismiss_action` (P0-2); `DatabaseSetupViewModelTest.kt::progress_and_completed_states_map_to_ui` (P1-1); `ShareHandlerTest.kt::open_url_without_browser_shows_snackbar` (P1-2).
  - **Acceptance:** no silent `Success(Unknown)` on exception; failure banners are testable.

- [ ] **P5-9 — Final verification (user runs; agents never run Gradle)** — see checklist below. Record exact command output in the PR description; do not claim a check passed without it.

---

## Verification Checklist

Formatting/lint (verify which task exists — `spotlessCheck` is configured via the Spotless plugin with ktlint 1.5.0; `ktlintCheck` is **not** configured in `app/build.gradle.kts`):

```bash
./gradlew spotlessCheck detekt lintDebug
```

Tests and build:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

If `AGENTS.md` §16/§23 is updated to match the build, replace `spotlessCheck` there too; do that as a separate documentation-only commit, not inside a code phase.

Manual QA — Phase 0 bugs (Arabic RTL **and** English LTR, light and dark):

- [ ] Search for any word: ayah result shows the real ayah number, never `﴿٠﴾`; highlight still works on matched text.
- [ ] Select a mix of bookmarked and unbookmarked ayat in the reader → Bookmark → all are bookmarked, snackbar count matches; reopen Bookmarks to confirm nothing was removed.
- [ ] Force a paging failure (offline/airplane with a stale DB manifest or a fake failing repo build): WordAyat and Search keep results and their Retry actually loads the next page.
- [ ] Force a DB query failure: reader shows an explicit error + Retry instead of "DB missing"; RootDetail never spins forever.
- [ ] Trigger a Play update failure (or fake `AppUpdateResult.Failed`): a failure banner appears and can be dismissed.
- [ ] Long-press a word to open the floating toolbar, tap outside: only the popup closes; the top bar does not appear/disappear.
- [ ] Pages mode: swipe to a page that is not cached; last-read on Home/Continue Reading updates to an ayah on that page.
- [ ] Open a surah with legacy/unpaged data (no `pageNumber`): no crash, no page header/separator.
- [ ] Report-meaning "open issue" with no browser installed: visible snackbar, no dead tap.
- [ ] Search bookmark button announces the correct action in TalkBack ("Bookmark ayah" vs "Remove bookmark").

Final gates from AGENTS §21: architecture boundaries intact (no `ui → data`), loading/success/empty/error explicit, no secrets, DB excluded from backup, no dead code, diff reviewed.
