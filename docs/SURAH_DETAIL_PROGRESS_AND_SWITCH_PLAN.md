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

### C) Switch gesture — `ui/surah/detail/components/SurahSwitchGesture.kt`
Observational only — it never consumes, so the list keeps every pixel it can
scroll and the reading position is never hijacked mid-list.
- Second `NestedScrollConnection` chained after the collapse connection.
- Edge detection uses the official state APIs only: `!listState.canScrollForward`
  (next) and `!listState.canScrollBackward` (prev) — pixel-exact, no item-index
  heuristics (which fire while the last verse is still scrolling in).
- Accumulates **unconsumed** `onPostScroll` delta for `UserInput` only; fling
  leftovers (`Fling` source) are ignored — momentum never auto-advances.
- **Overscroll must be disabled for the list** (`CompositionLocalProvider(
  LocalOverscrollConfiguration provides null)` in `SurahDetailScreen`): the
  Android 12+ stretch effect consumes unconsumed edge deltas *before* parent
  connections observe them (`AndroidOverscroll.applyToScroll` pulls them into the
  stretch first), which starves this observer after the very first frame.
- Rubber band: the switch threshold is measured on **raw finger travel** (110dp)
  while the visual pull is a damped `0.4f` fraction clamped to ±60dp — the content
  lags the finger instead of tracking it 1:1.
- **Fires on release**, in `onPreFling` (the same hook Material3 pull-refresh
  uses), never mid-drag: below threshold the pull springs back with
  `AppMotion.SpringGentle`; above it the switch runs and `pullPx` stays frozen so
  the outgoing screen keeps its offset while the vertical slide takes over.
- **Cancelling is intercepted in `onPreScroll`** (the only place this connection
  consumes): once a pull is live the child list *can* scroll back, so a reverse
  drag would be swallowed by the child and never reach `onPostScroll` — the pull
  would stay armed and release would still switch. It therefore takes only
  `min(|delta|, |accumulated|)`, i.e. exactly what the pull needs to unwind, and
  hands the remainder back to the list: dragging back never locks the view, the
  badge fades with the pull, and releasing after a cancel no longer navigates.
- The connection instance is keyed on `listState` **only**; gates and the callback
  are read through `rememberUpdatedState`, so a gate flip (pagination, selection,
  sheet) can never recreate it and reset an in-progress pull.
- Gates per direction: no selection mode, no morphology sheet, not loading more,
  list non-empty; "next" additionally requires `!uiState.hasMoreAyat` (the
  repository's own pagination flag = true end of the surah) and `surahId < 114`;
  "prev" requires `surahId > 1`.
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
- `Load(surahId, immediate)` — adjacent switches skip the `delay(300)` warm-up so
  the first page lands inside the 250ms slide instead of flashing the spinner
  after the animation.
- Last-read: forward switch records as you scroll; backward switch immediately
  records the previous surah's last ayah (correct — you are at its end).
- Removed with the bookmark button: `ToggleSurahBookmark` event, VM
  `bookmarkedSurahs` slice, `isSurahBookmarked` state.

## File checklist
- [x] `docs/SURAH_DETAIL_PROGRESS_AND_SWITCH_PLAN.md` (this file)
- [x] `core/util/ReadingProgress.kt` (new)
- [x] `test/core/ReadingProgressTest.kt` (new)
- [x] `ui/surah/detail/components/ReadingProgressRings.kt` (new)
- [x] `ui/surah/detail/components/SurahSwitchGesture.kt` (new)
- [x] `test/ui/surah/detail/components/SurahSwitchGestureTest.kt` (new — pull/cancel/fire lifecycle, including the cancel-before-release regression)
- [x] `ui/surah/detail/components/SurahSwitchHint.kt` (new — also hosts `BoxScope.SurahSwitchHints`)
- [x] `ui/surah/detail/components/ReadingPosition.kt` (new — screen derivations moved out to keep the screen slim)
- [x] `ui/surah/detail/components/SurahDbMissingState.kt` (new — empty-state extraction; keeps screen at 394 lines)
- [x] `ui/surah/detail/components/AyahFlowGrouping.kt` (buildBlock* map helpers moved out of screen)
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
