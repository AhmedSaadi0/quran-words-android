# REFACTOR_PLAN — RootDetail (executed 2026-09-09, completed 2026-09-10)

> Sub-roadmap of `AGENTS.md §22`. Target: `RootDetailScreen.kt` + direct deps.
> Status: all phases complete — Stateful/Stateless split landed, God file deleted.

## Phases

- [x] 0 Baseline — behavior checklist (5 tabs, collapse, pagination 30/page, copy/share ×5, word→WordAyat, occurrence→SurahDetail, report dialog).
- [x] 1a `RootDetailTab` enum (`ui.roots.detail.util`) — pager index == ordinal, contentId preserves `root_detail_screen_*` tags.
- [x] 1b `PaddedPagerItem` (`LazyItemScope`, padding 16/5 + animateItem) — 9 wrappers migrated.
- [x] 1c Emoji removal — `📋/🤖/🕒` → `Icons.Outlined.ContentCopy` + plain meta line; new `cd_copy_ai_summary` (en/ar).
- [x] 2a/b `ShareHandler` (`ui.roots.detail.util`) — single copy/share impl (clipboard + ACTION_SEND + ActivityNotFoundException → snackbar); 5 paths rerouted.
- [x] 2c `ReportMeaningDialog` hoisted — `(onCopyReport, onShareReport, onOpenUrl)` callbacks; dialog no longer touches clipboard/Context; caller uses `ShareHandler` (Toast→Snackbar unification, same strings).
- [x] 3 Contract (`ui.roots.detail.RootDetailContract`: `CopyAction`, `UiState`, `Event`, `Effect`) + VM `uiState` (combine) + `copyingAction` per-action + `cleanAiDate`/`formatAiMetaLine` pure helpers + `RootDetailRoute` (same nav signature) + `AppNavigation` switched to Route.
- [x] 4 Tabs extracted (`ui.roots.detail.tabs`: Meanings/Masadir/Derivatives/Words/Ayat, each owns LazyListState) + `ActionBars.kt` generics (`SelectionActionBar`, `CopyAllActionBar` with identical testTags); parent `when` is 5 one-liners.
- [x] 5a Collapse — `CollapsingHeaderState` (saveable, stable connection) + `CollapsingHeader.kt` (`RootDetailHeader` takes precomputed `rootText/subtitleText/aiMetaLine/tabCounts` — no `RootDetail`, no date logic; `onSizeChanged`, height+offset+alpha, no layout-write).
- [x] 5b Pagination — trigger inside `AyatTab` (`snapshotFlow` + `distinctUntilChanged` + `filter`, `onNearEnd` → `AyatNearingEnd` event → VM).
- [x] 5c Highlight — `AyahOccurrenceCard` remember keys `(text, matched, ayahNum, bg, fg)` (no SpanStyle identity).
- [x] 6 Final decoupling (2026-09-10) — `RootViewModel.onEvent` + `effect` (nav Effects); stateless `ui.roots.detail.RootDetailScreen` (118 lines, `uiState + rootId + onEvent + onNavigateBack + snackbarHostState`, zero VM/Context/platform refs) + `RootDetailScreenPreviews` (loading + populated); `RootDetailRoute` (single `collectAsStateWithLifecycle`, ShareHandler/Context routing, Effect → nav); legacy `ui.screens.RootDetailScreen` (602-line God file) deleted; `RootDetailRefactorTest` (tab order, contentIds, date/meta helpers).

## Limitations / Debt (follow-up)

1. **Card split**: `RootDetailCards.kt` (868 lines) still hosts 5 cards + legacy bars (used by `WordAyatScreen`). Split to `ui.roots.detail.components.cards/` + migrate `WordAyatScreen` imports in follow-up.
2. **Report dialog Toast→Snackbar** for copy/share (same strings, unified feedback) — confirm UX.
3. **Full header UI cutover verified**: `RootDetailHeader` is now the single header (called by the stateless Screen); legacy inline header deleted with the God file. Confirm collapse physics on device (fling up/down) + rotation + large font.
4. **Large-font trap fix**: subtitle capped to 40% viewport (`effectiveHeightPx`, `LocalConfiguration`-derived, rotation-aware) + direct `draggable` drive on the header (touch-starvation fix, selection-guarded) + Read-More dialog (`read_more_summary_btn`, `R.string.read_more`) for clipped summaries. No `verticalScroll` inside the collapsing container (anti-pattern: pre-scroll would starve it).

## Decision Log (append to AGENTS.md §20)

| # | Decision | Context | Options | Chosen | Reason | Trade-offs | Date |
|---|----------|---------|---------|--------|--------|------------|------|
| 7 | `ui.roots.detail/` feature package | AGENTS §7, flat `ui/screens/` God files | stay flat / move | move | Clean Architecture feature-first, <300 lines/file | import churn in nav/tabs | 2026-09-09 |
| 8 | `copyingAction: CopyAction?` per-action | global `isCopyingAll` locked all tabs | global / per-action | per-action (single-flight, per-bar spinner) | fixes cross-tab lockout, preserves DB guard | share/copy pair still mutually exclusive | 2026-09-09 |
| 9 | Preserve pixel-collapse physics, `graphicsLayer` + `onSizeChanged` | layout-write + per-pixel parent recomposition | TopAppBar behavior / custom state | custom `CollapsingHeaderState` + onSizeChanged | zero visual regression, isolates recomposition | full UI cutover deferred one step for build verification | 2026-09-09 |
| 10 | `ShareHandler` platform boundary | 5× copy/share duplication + Intent in composables | keep inline / handler | handler (copy/share/openUrl, snackbar) | testable, single catch, hoists dialog too | dialog Toast→Snackbar (same strings) | 2026-09-09 |

## Verification (user runs — agents never run gradle)

```bash
./gradlew ktlintCheck detekt lintDebug
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Manual: 5 tabs + counts, header collapse/expand fling, ayat paginate to total, copy/share all 5 paths + report, word→WordAyat, occurrence→SurahDetail, rotation + font-scale header, back-nav restores.
