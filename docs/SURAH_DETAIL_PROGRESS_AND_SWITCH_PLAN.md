# SURAH_DETAIL_PROGRESS_AND_SWITCH_PLAN (active)

Two user-facing enhancements to the surah reading screen
(`app/src/main/java/com/quranwords/ui/surah/detail/`):

1. **Top bar reading progress** — the bookmark button is replaced by two small
   circular progress rings: position within the whole surah, and the current
   juz with its remaining portion.
2. **Continuous adjacent-surah scrolling** — keep scrolling down at the end of
   a surah to continue into the next one (and up at the top into the previous
   one) with a Telegram-style vertical slide.

Confirmed decisions (user, 2026-10-14):

| # | Decision | Chosen |
|---|---|---|
| 1 | Surah-bookmark button in top bar | **Removed entirely** from SurahDetail (surah bookmarks still managed from SurahIndex + Bookmarks screens; persisted data untouched) |
| 2 | Switch mechanism | **Navigation + direction-aware vertical slide** (one surah in memory, no VM restructure) |
| 3 | Back after switching | **Return to entry point** — adjacent switch replaces the current `SurahDetail` entry (`popUpTo<SurahDetail> { inclusive = true }`) |
| 4 | Ring contents | Left ring: **mushaf page number** in center + arc = page position within the surah. Right ring: **juz number** in center + arc = progress **within** the juz (fills up as you read — revised 2026-10-14 after feedback: juz arc must not shrink as "remaining"; page ring replaces the ayah ring) |
| 5 | Pull feedback | **Included** — content follows the finger (draw-phase `graphicsLayer`, ≤56dp) + neighbor-name hint pill at the edge |

## Design notes

### A) Progress math — `core/util/ReadingProgress.kt` (pure, no DB)
- Prefix sums of `SurahMetadata.SURAHS` ayah counts → `globalAyahIndex(surahId, ayah)`
  (1..6236); `juzAt(global)` by binary search over `JUZ_LIST` (verified mid-surah
  starts); `juzLength(juz)` = next start − start (juz 30 → `TOTAL + 1 − start`).
- `ReadingProgress.of(surahId, blockFirstAyah, blockAyahCount, scrolledFraction,
  currentPage, surahPages)` → `currentPage`/`totalPages` + `pageFraction`
  (`(indexOf(page)+1)/totalPages`), `juz` + `juzFraction` (progress **within**
  the juz, interpolated from the fractional ayah — 0.0 at the juz's first ayah
  → 1.0 at its last; both arcs grow as the reader advances).
  Null when the surah/block is invalid or the page number is unknown.
- `adjacentSurahTarget(surahId, forward)` → next: `ayah = 1`; prev:
  `ayah = previousSurah.ayahCount`; `null` at surah 1 (up) / 114 (down).
- Unit tests: `core/ReadingProgressTest.kt` (juz 2/3/30 boundaries, total 6236,
  juz-progress growth, page-fraction arithmetic incl. missing/empty page lists,
  Al-Baqarah spanning 3 juz, adjacent-target edges).

### B) Rings — `ui/surah/detail/components/ReadingProgressRings.kt`
- 36dp rings; arc drawn in **draw phase** (`Canvas` params) so per-frame scroll
  updates redraw only the arcs; center `Text` recomposes only when the displayed
  number changes (no HarfBuzz reshaping every frame — AGENTS §17).
- Position is derived via `derivedStateOf` in the screen and passed down as a
  `() -> ReadingProgress?` provider that the rings row invokes **inside its own
  composition**, so only that node recomposes per frame.
- TalkBack: merged `contentDescription` per ring (new en + ar strings), Eastern
  Arabic digits in centers (`toEasternArabicDigits()`).

### C) Scroll connection — `ui/surah/detail/components/SurahSwitchGesture.kt`
One `SurahReadingScrollConnection` owns the whole screen's nested scroll. Two
separate connections used to share the job (collapse + switch pull), but the
framework offers a reverse drag to the header first
(`NestedScrollNode.onPreScroll` calls parent-before-self), so cancelling a pull
expanded the header while the pull stayed frozen. Merging them makes the
priority explicit and order-proof; `onPreScroll` phases, in order:
1. **Cancel a live pull** (touch only): unwind 1:1, taking only
   `min(|delta|, |accumulated|)`; the remainder keeps flowing.
2. **Quick-return header**: collapse on scroll down, reappear on scroll up
   (frozen in selection mode) — physics unchanged from the old connection.
3. Anything left reaches the list.
- Edge detection uses the official state APIs only: `!listState.canScrollForward`
  (next) and `!listState.canScrollBackward` (prev) — pixel-exact, no item-index
  heuristics (which fire while the last verse is still scrolling in).
- Accumulates **unconsumed** `onPostScroll` delta for `UserInput` only; fling
  leftovers (`Fling` source) are ignored — momentum never auto-advances.
- **Overscroll stays disabled for the list** (`CompositionLocalProvider(
  LocalOverscrollConfiguration provides null)` in `SurahDetailScreen`): the
  Android 12+ stretch effect consumes unconsumed edge deltas *before* parent
  connections observe them, which would starve the edge observer after the very
  first frame.
- Rubber band: the switch threshold is measured on **raw finger travel** (110dp)
  while the visual pull is a damped `0.4f` fraction clamped to ±60dp — the content
  lags the finger instead of tracking it 1:1.
- **Fires on release**, in `onPreFling` (the same hook Material3 pull-refresh
  uses), never mid-drag: below threshold the pull springs back with
  `AppMotion.SpringGentle`; above it the switch runs and `pullPx` stays frozen so
  the outgoing screen keeps its offset while the vertical slide takes over.
- The connection instance is keyed on `listState` + `collapseState` **only**;
  gates and the callback are read through `rememberUpdatedState`, so a gate flip
  (selection, sheet) can never recreate it and reset an in-progress pull.
- Gates per direction: no selection mode, no morphology sheet, non-empty list;
  "next" additionally requires `surahId < 114`; "prev" requires `surahId > 1`.
  (The whole surah is always loaded, so no pagination gate remains.)
- Edge hint pill (neighbour surah name + arrow icon) fades in past 10dp of pull.

### D) Navigation + transitions
- `Routes.kt`: `@Serializable enum class SurahEnter { Normal, FromNext, FromPrev }`
  added as `SurahDetail(surahId, ayah, enter = Normal)` — existing callers unchanged.
- `Motion.kt`: `verticalEnter(fromBottom)` / `verticalExit(toBottom)` (250ms,
  `EasingStandard`, slide + fade).
- `RouteTransitions.kt` (new, `ui/navigation/`) owns every switch transition rule
  as `AnimatedContentTransitionScope<NavBackStackEntry>` helpers, so
  `AppNavigation.kt` stays a declarative graph (`enterTransition = { surahDetailEnter() }`).
  Direction is resolved from **both** entries so the rules hold regardless of which
  transition variant navigation-compose picks for a navigate-with-`popUpTo` op:
  - vertical **iff** both `initialState` and `targetState` are `SurahDetail` and
    the target's `enter != Normal` (the switch itself) — same rule in all four slots;
  - `popExit` fallback: real back from an adjacent-entered `SurahDetail`
    (target not `SurahDetail`) → slide back the way it came;
  - NavHost `popEnterTransition`: revealing a screen while an adjacent-entered
    `SurahDetail` slides away → fade only (no horizontal slide mix);
  - all other navigation keeps today's horizontal `AppMotion.nav*` transitions.
- Back-stack: `navigate(...) { popUpTo<SurahDetail> { inclusive = true } }` only
  for adjacent switches → back always returns to the entry point (Decision 3).

### E) Interplay fixes
- Deep-link pulse suppressed when `enter != Normal` (no overlay pulse on
  backward entry at the last ayah).
- `Load(surahId, targetAyah, immediate)` — deep links and adjacent switches skip
  the warm-up delay so content lands inside the transition instead of flashing
  the spinner after the animation.
- Last-read: forward switch records as you scroll; backward switch immediately
  records the previous surah's last ayah (correct — you are at its end).
- Removed with the bookmark button: `ToggleSurahBookmark` event, VM
  `bookmarkedSurahs` slice, `isSurahBookmarked` state.

### F) Full-surah loading (pagination removed)
20-ayah paging was a premature optimization over finite static data
(Al-Baqarah, the longest: 286 ayat) that spawned the whole pagination state
machine (`_hasMore`, offsets, page loops, deadlocks). `LazyColumn` already
virtualizes rendering, so the surah now loads whole in one shot:
- `QuranRepository.getAyatBySurahFull(surahId)` — one ayat-text query plus
  morphology batched in `chunked(100)` IN-queries (≤4 round-trips worst case);
  `getAyatBySurahPaged` deleted from the interface, impl and test fake.
- ViewModel keeps no paging state: no `_hasMore`/`_isLoadingMore`/offsets, no
  `loadMoreIfNeeded`/`loadNextPage`/`appendAyat`/`ensure*` loops, no
  `initialAyatLimit` window rule. Deleted events: `NearingEnd`,
  `EnsureAyahLoaded`, `EnsurePageLoaded`; deleted state: `isLoadingMore`,
  `hasMoreAyat`.
- Screen: no `NearingEnd` collector, no `blockLastAyahListIndex`, no
  `loading_more` footer item; page chips scroll locally (every page present);
  range selection is a pure state update.
- Placement/pulse/last-read/rings/gesture gates work unchanged — with a full
  list the target block always resolves on first composition.

## File checklist
- [x] `docs/SURAH_DETAIL_PROGRESS_AND_SWITCH_PLAN.md` (this file)
- [x] `core/util/ReadingProgress.kt` (new)
- [x] `test/core/ReadingProgressTest.kt` (new)
- [x] `ui/surah/detail/components/ReadingProgressRings.kt` (new)
- [x] `ui/surah/detail/components/SurahSwitchGesture.kt` (merged `SurahReadingScrollConnection`: pull-cancel phase 1, header phase 2, edge observer)
- [x] `test/ui/surah/detail/components/SurahReadingScrollTest.kt` (pull/cancel/fire lifecycle + cancel-before-header priority regressions)
- [x] `ui/surah/detail/components/SurahSwitchHint.kt` (new — also hosts `BoxScope.SurahSwitchHints`)
- [x] `ui/surah/detail/components/ReadingPosition.kt` (new — screen derivations moved out to keep the screen slim)
- [x] `ui/surah/detail/components/SurahDbMissingState.kt` (new — empty-state extraction; keeps screen at 394 lines)
- [x] `ui/surah/detail/components/AyahFlowGrouping.kt` (block-index helper; pagination map helper deleted with paging)
- [x] `domain/repository/QuranRepository.kt` + `data/repository/QuranRepositoryImpl.kt` (`getAyatBySurahFull`, chunked morphology; paged API deleted)
- [x] `ui/surah/detail/components/SurahDetailHeader.kt` (rings replace bookmark)
- [x] `ui/surah/detail/SurahDetailContract.kt` (bookmark removal + `Load(immediate)`)
- [x] `ui/surah/detail/SurahDetailViewModel.kt` (Load immediate + bookmark slice removal)
- [x] `ui/surah/detail/SurahDetailScreen.kt` (gesture, progress, hint, adjacent callback)
- [x] `ui/surah/detail/SurahDetailRoute.kt` (`enter` + adjacent callback pass-through)
- [x] `ui/navigation/Routes.kt` (`SurahEnter`)
- [x] `ui/theme/Motion.kt` (vertical switch transitions)
- [x] `ui/navigation/AppNavigation.kt` (declarative graph only)
- [x] `ui/navigation/RouteTransitions.kt` (new — switch transition rules extracted from the graph)
- [x] `res/values/strings.xml`, `res/values-ar/strings.xml` (ring/hint strings added; dead `cd_save_surah` removed)
- [x] `AGENTS.md` §20 Decision Log entry (#25)

## Verification (user runs — agents never run gradle)

```bash
./gradlew spotlessCheck detekt lintDebug testDebugUnitTest assembleDebug
```

Note: the project lints via Spotless (`ktlint("1.5.0")`), not the standalone
`ktlintCheck` task. If `spotlessCheck` reports pre-existing import-order
violations in untouched files, run `./gradlew spotlessApply` only if you are
fine with the unrelated diff — or scope it to this feature's files.

Manual: rings update while scrolling (including juz change mid-surah) · switch
down at surah 5 end → surah 6 top with vertical slide · switch up at top →
previous surah's last ayah · back after switching → entry point · no switch in
selection mode / with sheet open / at surah 1 top / at surah 114 end · RTL +
TalkBack labels.
