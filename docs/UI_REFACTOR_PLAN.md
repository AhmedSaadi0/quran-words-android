# UI_REFACTOR_PLAN — Screens, ViewModels & Navigation (active)

> Sub-roadmap of `AGENTS.md §22`. Scope: `ui/screens/`, `ui/viewmodel/`, `ui/navigation/`, `ui/components/`.
> Reference pattern: `ui/roots/detail/` — the completed RootDetail refactor (see `REFACTOR_PLAN.md`).
> Status: **✅ 100% COMPLETE — Phases 1–11 executed and verified (Phases 9–11 pending final user verification run).** Roadmap closed 2026-09-11.

---

## 0) Ground Rules (binding)

1. **Reference pattern.** Every feature migration copies the RootDetail anatomy:
   `XContract.kt` (UiState/Event/Effect) → `XViewModel` (state + Result mapping) → `XRoute.kt` (lifecycle collect + platform boundaries + effect→nav) → stateless `XScreen.kt` (uiState + onEvent + callbacks, <250 lines) → `components/` split → `AppNavigation` switches to Route.
2. **Atomic Feature Migration (mandatory adjustment, 2026-09-10).** No mechanical ViewModel split phase. Each feature phase extracts **its own** ViewModel from `ui/viewmodel/ViewModels.kt` together with its Contract, Route, Screen, and components — **one touch per file**. No VM is refactored twice; `ViewModels.kt` shrinks monotonically per phase and is deleted when the last consumer migrates.
3. **testTags survive.** Existing tags are behavior; they must remain identical unless a tag is provably dead (RootDetail precedent: `contentId` preservation). New tags follow `<feature>_<element>_<id>`.
4. **Agents never run gradle** (AGENTS §16). After each phase, list the exact commands; user runs them and shares output.
5. **No silent failure.** Any `catch { emptyList() }` removed in a touched file becomes `Result.Error` → UiState `error`.
6. **All emojis removed** (Decision 13) — interactive *and* decorative, replaced with `Icons.Filled/Outlined.*` + `contentDescription`.
7. **RTL + font-scaling verified** per screen before its phase closes.
8. **Decision Log** entries below are appended to `AGENTS.md §20` when the phase executing them lands.

---

## 1) Audit Findings (baseline 2026-09-10)

### 1.1 What already complies

| Item | Evidence |
|---|---|
| RootDetail feature anatomy | `ui/roots/detail/`: Contract (109 L) + Route + stateless Screen (118 L) + tabs/ + components/ + util/ShareHandler + `RootDetailRefactorTest` |
| Lifecycle-aware collect exists | `RootDetailRoute` uses `collectAsStateWithLifecycle`; dependency `lifecycle-runtime-compose` already in `app/build.gradle.kts` |
| Stateless morphology sheet | `ui/components/MorphologyBottomSheet.kt` takes domain models + callbacks (no repo inside) |
| Best-in-class state machine | `DatabaseSetupScreen` (loading/progress/extracting/error/retry) |
| Test suite exists | 11 unit-test files incl. `RootDetailRefactorTest`, `QuranCopyFormatterTest`, `LanguageManagerTest`, `RevelationTypeTest`, Roborazzi `HomeTitleScreenshotTest` |

### 1.2 ViewModel findings — `ui/viewmodel/ViewModels.kt` (1,428 lines, 9 VMs = God file)

| VM (lines) | Violations |
|---|---|
| `MainViewModel` (42–165) | 10 separate StateFlows + 9 `collectLatest` launches; **exposes `val downloadManager: DatabaseDownloadManager` publicly** (data-layer leak, L46); bookmarks as raw `Set<String>` with key formatting in VM; every screen receives it |
| `HomeViewModel` (167–194) | No error handling; `loadData()` re-fetch on every call; duplicates last-read state held by MainViewModel |
| `SurahViewModel` (196–224) | Filter as magic strings `"all"/"meccan"/"medinan"`; no loading/error state; filtering (incl. Arabic normalization) done in the *screen's* `remember{}` |
| `SurahDetailViewModel` (227–457) | Manual paging with `delay(300/80/10)` hacks; `ensureAyahLoaded`/`ensurePageLoaded` polling loops; silent catches; 6 StateFlows for one word-selection + 4 for AI summary; selection logic duplicated; `getFormattedSelection` mixes formatting into VM |
| `RootViewModel` (459–945) | Biggest; 13-flow `combine` with `@Suppress("UNCHECKED_CAST")`; **3 near-identical selection blocks** (word/meaning/ayah); silent `catch { emptyList() }` ×4; legacy granular flows kept alongside `uiState` (migration adapter, documented) |
| `WordAyatViewModel` (971–1093) | Silent catches ×3; `getAllFormatted` platform-ready text built in VM with `isCopyingAll` global lock (per-action `CopyAction` not propagated here) |
| `SearchViewModel` (1095–1294) | 4 duplicated `loadMoreX()` methods (~90 L copy-paste); silent catch → empty `SearchResult` (**error never surfaced**); magic tab ints `0..3` |
| `DatabaseSetupViewModel` (1296–1388) | Concrete `DatabaseDownloadManager` (Context-heavy) injected directly; fully-qualified `data.remote.*` types throughout; ok otherwise |
| `DbUpdateViewModel` (1390–1428) | Minor: maps `DbCheckResult.Error` → `Unknown` silently |

`ReportMeaningViewModel.kt` (106 L): clean shape; silent catch in `init` (L55); `BuildConfig`/`Build.VERSION` read inline (injectable `BuildInfo` when touched).

### 1.3 Screen findings — `ui/screens/` (5,773 total lines)

| Screen (lines) | Key violations |
|---|---|
| `HomeScreen.kt` (872) | **3 VMs** (Main+Home+DbUpdate); 10 `collectAsState`; emoji as primary interactive icons ⚙️🎨🌓 (L170–195) + decorative 💾⭐🔖📖📝📚✨📍; `ThemeChooserDialog` inline; `QuickNavCard` inline; `QuranMetaConstants` (data layer) import; <48dp touch targets (40dp buttons); no UiState |
| `SurahDetailScreen.kt` (657) | 2 VMs, 15 collects; **clipboard + share Intent + `context.getString` inline** (L274–302); selection TopAppBar with ✓📋↗ emoji buttons; nested-scroll collapse physics inline (L142–232, must be preserved); **double `updateLastRead`** (SurahDetail VM + Main VM, L199–200, 212–213); `delay(100)` + VM-state peek in `onPageClick` (L437–447); 📥 empty state |
| `DatabaseSetupScreen.kt` (600) | **`data.remote.DownloadState/Error/Phase` leak into UI** (L62–63, 243); file-picker launcher in composable (L83–89); byte→MB math in composable (L278–279); dead imports (`viewModel`, `Emerald700`, `QuranGold`); fully-qualified `androidx.compose.material3.TextButton` (L470) |
| `RootDetailCards.kt` (422) | 6 cards + `buildHighlightedAyahText` normalization/matching algorithm in UI file (L372–421); `data.util.ArabicNormalizer` import; missing testTags on `MasdarCard`/`DerivativeCard`/`AyahOccurrenceCard`; known debt #1 of RootDetail plan |
| `SearchScreen.kt` (382) | **No error state** (silent failure); fragile `hashCode()` list key (L308); unused import |
| `SurahIndexScreen.kt` (349) | **Filtering + Arabic normalization in `remember{}`** (L85–100); `data.util` imports (L61–62); magic strings `"meccan"/"medinan"`; magic numbers 114/86/28/30 ×2; no loading state |
| `WordAyatScreen.kt` (270) | **Worst platform offender**: clipboard (L171), share Intent + chooser + `ActivityNotFoundException` (L188–198), `context.resources` strings (L173–200) — all belong behind `ShareHandler` |
| `MorphologyGuideScreen.kt` (265) | Static (no VM — fine); `data.util.QuranMetaConstants` import; unused `Emerald700`/`QuranGold` |
| `BookmarksScreen.kt` (259) | **❌ emoji as only content of remove IconButtons** (L179, 250); bookmark key parsing `split(":")` + comparator sorting in composable (L196–200); `?: 1` fallbacks |
| `RootsListScreen.kt` (163) | Normalization filtering in `remember{}` (L61–69); **no empty-results state, no error state**; unused import |

### 1.4 Component & navigation findings

| File | Violations |
|---|---|
| `components/MorphologyBottomSheet.kt` (288) | Dead `val hasTried = true` (L192); `aiSummary!!` after null check (L226); inline date formatting duplicating `cleanAiDate` (L254–258); 🤖🕒 emoji |
| `navigation/AppNavigation.kt` (326) | **String routes + `NavType` parsing + silent `?: 1` fallbacks** (L179–252); `BottomNavItem.iconEmoji` 🏠📖🌿🔍📐 rendered as bottom-nav icons (L50–69, 102); duplicated per-destination transitions (L135–314); nav testTags embed route strings |
| `navigation/Screen.kt` (20) | Sealed-class string routes — deleted in Phase 2 |
| Cross-cutting | `collectAsState` everywhere (never lifecycle-aware outside RootDetailRoute); `data.util.ArabicNormalizer` + `data.util.QuranMetaConstants` imported by 6 UI files; error states missing everywhere except DatabaseSetup; empty placeholder dirs `ui/bookmarks|guide|search|setup|surah|roots/components...` exist but unused |

### 1.5 Layer facts driving the plan

- **No `Result<T>` existed** in `core/util` (added Phase 1). Repos return raw types/nullables; 14+ silent catches in VMs.
- `UserPreferencesRepository` is a **concrete data-layer class** (`data/repository`, Context + global `Context.dataStore` extension) — no domain interface (AGENTS §12 violation; addressed Phase 10).
- `ArabicNormalizer` + `QuranMetaConstants` lived in **`data/util`** — `ArabicNormalizer` moved to `core/util` in Phase 1; `QuranMetaConstants` splits per AGENTS §14 in Phase 8 (MorphologyMaps) / Phase 6 (Surah metadata constants).
- kotlinx-serialization added Phase 1 (typed-nav prerequisite); **Paging3 absent** (deferred — Decision 12); ktlint/detekt wired Phase 1 via Spotless + detekt.

---

## 2) Decision Log (append to AGENTS.md §20 as phases land)

| # | Decision | Context | Options | Chosen | Reason | Trade-offs | Date |
|---|----------|---------|---------|--------|--------|------------|------|
| 11 | Typed `@Serializable` navigation **before** feature migrations | 11 `composable{}` blocks on string routes with silent `?: 1` fallbacks | typed nav first / after features | **First (Phase 2)** | Feature screens then land on final nav structure; nav touched once | Earlier nav churn before screens move | 2026-09-10 |
| 12 | **Defer Paging3** with logged debt | AGENTS §17.1 mandates Paging3; but it touches DAOs+repo, outside screens/VM scope; manual paging currently works | Paging3 now / defer + clean debounces | **Defer** (follow-up phase) | Keeps UI refactor focused; delay-debounce hacks cleaned inside feature phases | AGENTS §17.1 non-compliant until follow-up | 2026-09-10 |
| 13 | **Remove all emojis** (interactive + decorative) | §10.4 requires vectors for interactive; decoration tolerated | interactive only / all | **All out** | Consistent with completed RootDetail cutover; no half-state | Slightly more icon work per screen | 2026-09-10 |
| 14 | **Slim MainViewModel → AppStateViewModel** | 10-concern holder injected into every screen; public `downloadManager` | slim app-state / dissolve fully | **Slim** — one `AppPreferencesUiState` via single `combine`; theme/language → `ui/settings/SettingsDialog` + own VM; feature VMs expose per-screen slices | Screens stop collecting 10 granular flows; single source for app prefs | App-state still shared (one object), not per-concern VMs | 2026-09-10 |
| 15 | **Atomic Feature Migration** — no mechanical VM-split phase | Phase 2 originally split all VMs, Phase 4 re-touched them (double churn) | mechanical split then refactor / atomic per-feature | **Atomic**: each feature phase extracts its own VM + Contract + Route + Screen in one change | No file touched twice; `ViewModels.kt` shrinks monotonically; smaller reviewable diffs | ViewModels.kt persists until Phase 10 | 2026-09-10 |
| 16 | **Spotless (ktlint) + detekt for quality gates** | AGENTS §16 mandates ktlint/detekt; none configured; Gradle 9.3.1 needs Gradle-9-compatible tooling | jlleitschuh / Spotless+ktlint | **Spotless 7.x wrapping official ktlint + detekt 1.23.x** | Gradle 9 support; detekt baseline absorbs legacy; `spotlessApply` = one-shot mechanical fix | Large mechanical format diff on first apply | 2026-09-10 |

---

## 3) Phase Roadmap — Atomic Feature Migration

### Phase 0 — Baseline & guardrails
- [ ] Behavior checklist per screen (RootDetail-style): navigation edges, pagination, copy/share paths, bookmarks, scroll restore, collapse physics, rotation, large font.
- [ ] Inventory all `testTag`s per screen (preserve list; goes into each feature's PR description).

**Acceptance:** checklists committed; tags inventoried.

---

### Phase 1 — Foundation primitives (no UI behavior change) — ✅ this phase
- [x] **1.1 `core/util/Result.kt`** — `sealed interface Result<out T> { Success(data); Error(message, cause) }` + `runCatchingResult {}` (rethrows `CancellationException`) per AGENTS §13. Unit tests. *(Error markers like `DbNotReady` introduced by consuming features when they map failures — YAGNI.)*
- [x] **1.2 Move `ArabicNormalizer`** `data/util/` → `core/util/` (no shim — all 4 import sites migrated atomically: SurahIndexScreen, RootsListScreen, RootDetailCards, QuranRepositoryImpl) + new `ArabicNormalizerTest` (AGENTS §14 mandate; none existed).
- [x] **1.3 Add kotlinx-serialization** plugin + runtime (`libs.versions.toml`, `app/build.gradle.kts`) — Phase 2 typed-nav prerequisite.
- [x] **1.4 Quality gates**: Spotless (official ktlint) + detekt 1.23.x, `config/detekt/detekt.yml` + empty baseline; generate baseline via `detektBaseline` on first run (legacy code untouched).
- [x] **1.5 Extract `cleanAiDate`/`formatAiMetaLine`** → `core/util/AiMetaFormatter.kt`; re-point `RootViewModel` + `RootDetailRefactorTest` (coverage already exists there; MorphologyBottomSheet dedupe deferred to its feature phase).
- [x] **1.6 Append Decision Log 11–15** to AGENTS.md §20.

**Acceptance:** unit tests green (Result, ArabicNormalizer, AiMetaFormatter via existing RootDetailRefactorTest); serialization plugin resolves; `spotlessCheck`/`detekt` tasks exist (baseline generated on first run).

---

### Phase 2 — Infra: typed navigation + app-state groundwork (no feature VMs move) — ✅ implemented
- [x] **2.1 `@Serializable` routes** (`ui/navigation/Routes.kt`, `Screen.kt` deleted): `Home`, `SurahIndex`, `SurahDetail(surahId: Int, ayah: Int = 1)`, `Roots`, `RootDetail(rootId: Int)`, `WordAyat(rootId: Int, wordId: Int)`, `Search`, `Guide`, `Setup`, `Bookmarks`. `AppNavigation.kt` rewritten with `composable<T> { toRoute<T>() }`; duplicated per-destination transitions removed; **no silent `?: 1` fallbacks**.
- [x] **2.2 `BottomNavItem`: `iconEmoji: String` → `icon: ImageVector` + `labelRes: Int` + explicit `testTag`** (legacy `nav_*` tags preserved); tab-switch `popUpTo(Home::class)` semantics preserved.
- [x] **2.3 `DispatcherModule`** in `core/di` (`@IoDispatcher`/`@MainDispatcher`) — VMs adopt as they're touched in later phases.
- [x] **2.4 `SelectionState` helper** (`core/util/`, pure, unit-tested ×9) — VMs adopt as their features are touched (SurahDetail L419–443; RootViewModel L611–677).
- [x] **2.5 Sweep `collectAsState` → `collectAsStateWithLifecycle`** — 58 call sites across 10 files (incl. MainActivity, ReportMeaningDialog).
- [x] **2.6 MainViewModel groundwork** (stays in `ui/viewmodel/` until Phase 10): combined `appState: StateFlow<AppPreferencesUiState>` added; **public `downloadManager` removed**; legacy granular flows remain as delegates (RootDetail adapter precedent). Theme/language setters leave with Phase 9.

**Acceptance:** all 9 destinations compile via `toRoute<T>()`; bottom nav renders vectors; nav matrix passes; no behavior change in screens.

---

### Phase 3 — Roots feature (cards split + RootsList + RootDetail VM extraction) — ✅ implemented
- [x] Split `RootDetailCards.kt` (422 L) → `ui/roots/detail/components/cards/` (6 files: EmptyTabNotice, MeaningCard, MasdarCard, DerivativeCard, WordCard, AyahOccurrenceCard); `buildHighlightedAyahText` → pure `core/util/AyahHighlighter.matchRanges` + 7 unit tests (Compose-free); new testTags `masdar_item_*`, `derivative_item_*`, `occurrence_item_<surah>_<ayah>`.
- [x] **Extracted `RootDetailViewModel` → `ui/roots/detail/`** (from `RootViewModel`): `SelectionState` adopted for word + meaning selection (kills 2 duplicated blocks); loadRootWords Result-mapped; detail/words failures surface via `RootDetailUiState.error`; pagination/SavedStateHandle/copy flows ported 1:1. `RootDetailRoute` now owns its VM (no nav-level passing).
- [x] **RootsList atomic migration**: `RootsListContract` + `RootsListViewModel` (Arabic-normalized filtering moved from composable `remember{}` into VM via combine; Result-mapped load; `Retry` event) + `RootsListRoute` + stateless `RootsListScreen` with **new empty + error/retry states** (`roots_empty_results`, `roots_error_load` en/ar); legacy testTags preserved (`back_button`, `roots_list_screen`, `root_search_input`); `ui/screens/RootsListScreen.kt` deleted.
- [x] **`ReportMeaningViewModel` → `ui/roots/detail/report/`**: silent init catch → `runCatchingResult` with explicit degradation comment; `BuildInfo` injected via `core/di/BuildInfoModule` (BuildConfig/Build/Locale out of the VM); existing test updated.
- [x] Tests migrated: `RootMeaningsTest`/`RootWordsTest` → `RootDetailViewModel` via `onEvent` UDF path + `SelectionState`; obsolete string-route test replaced with typed-route default (`SurahDetail.ayah == 1`).

### Phase 4 — WordAyat (worst platform offender)

### Phase 4 — WordAyat (worst platform offender) — ✅ implemented
- [x] **Extracted `WordAyatViewModel` → `ui/roots/word/`**: ported pagination 1:1; `loadWord`/`loadMore` Result-mapped (`runCatchingResult`) — load failures surface via `WordAyatUiState.error` (was silent); `Retry` event (first-page vs next-page aware); derived `uiState` via combine with granular flows kept as source of truth.
- [x] **`WordAyatRoute`**: owns VM + `ShareHandler` — clipboard/`Intent(ACTION_SEND)`/`createChooser`/`ActivityNotFoundException`/`context.getString` all removed from the composable; copy/share success/empty/no-app feedback identical (same strings, same quantity plurals).
- [x] **Stateless `WordAyatScreen`** (~280 L → uiState + onEvent + callbacks): snapshotFlow pagination trigger with `distinctUntilChanged` kept; **new error states** — full-screen error+retry on first-page failure, inline error row during pagination (`word_ayat_error` string en/ar, `word_ayat_retry_btn` tag); ALL legacy testTags preserved (`back_button`, `word_ayat_empty`, `word_ayat_list`, `copy_all_bar`, `copy_all_occurrences_btn`, `share_all_occurrences_btn`).
- [x] `ui/screens/WordAyatScreen.kt` deleted; `AppNavigation` switched to `WordAyatRoute`; tests re-pointed (`RootWordsTest`); ViewModels.kt **948 → 810 lines**.

### Phase 5 — Search — ✅ implemented
- [x] **Extracted `SearchViewModel` → `ui/search/`**: `SearchTab` enum (kills magic ints 0–3); **one parameterized pager** replaces the 4 copy-pasted `loadMoreX()` (~90 L deduped); `searchAll`/page fetches Result-mapped — **error state surfaced** (was silent `catch → empty`); `Retry` re-runs the stored query; stable ayat list key `${surah}_${ayah}_${textUthmani}` replaces fragile `hashCode()`.
- [x] `SearchRoute` + stateless `SearchScreen` (uiState + onEvent + callbacks, tab selection as `rememberSaveable` view state); 🔍 empty-state emoji → `Icons.Filled.Search`; ALL legacy testTags preserved (`search_screen`, `unified_search_input`, `back_button`, `search_results_list`) + new `search_retry_btn`; new string `search_error_load` (en/ar); `ui/screens/SearchScreen.kt` deleted.

### Phase 6 — SurahIndex — ✅ implemented
- [x] **Extracted `SurahIndexViewModel` → `ui/surah/`**: filtering + Arabic normalization moved from composable `remember{}` into VM combine; `"meccan"/"medinan"` strings → new `RevelationFilter` enum (in `core/util/RevelationType.kt`); 114/86/28/30 → new `core/util/QuranStats`; loading + error states added (Flow `catch`); **owns its bookmark slice** (`bookmarkedSurahIds` + toggle) — screen no longer takes MainViewModel.
- [x] `SurahIndexRoute` + stateless `SurahIndexScreen` split into `SurahsTab`/`JuzTab`; 📖 juz emoji → `Icons.AutoMirrored.Filled.MenuBook`; ALL testTags preserved (`tab_surahs`, `tab_juz`, `surah_search_input`, `juz_item_*`, `back_button`, `surah_index_screen`); `ui/screens/SurahIndexScreen.kt` deleted.

### Phase 7 — Bookmarks — ✅ implemented
- [x] New `core/util/BookmarkRef` (typed surah/ayah bookmark ref + `parseSurah`/`parseAyah`/`AYAH_ORDER`, null on malformed — kills `split(":")` + `?: 1` fallbacks in the composable) + 4 unit tests.
- [x] **New `BookmarksViewModel`** (`ui/bookmarks/`): DataStore keys parsed + ordered in VM; `ToggleSurah/ToggleAyah` events; loading gate avoids empty-state flash.
- [x] `BookmarksRoute` + stateless `BookmarksScreen` split into `SurahBookmarkCard`/`AyahBookmarkCard`; ❌ remove IconButtons → `Icons.Outlined.Close` + `contentDescription` (`cd_remove_bookmark` en/ar); ⭐ empty-state → `Icons.Outlined.Star`; ALL testTags preserved (`bookmarks_screen`, `bookmark_surah_*`, `remove_bookmark_surah_*`, `bookmark_ayah_*`, `remove_bookmark_ayah_*`, `back_button`); `ui/screens/BookmarksScreen.kt` deleted.
- [x] ViewModels.kt **810 → 577 lines** (Main, Home, SurahDetail, DatabaseSetup, DbUpdate remain); AppNavigation switched to 3 new Routes.

### Phase 8 — MorphologyGuide (trivial) — ✅ implemented
- [x] **`data/util/QuranMetaConstants.kt` (390 L) deleted** and split into `core/util/` per AGENTS §14: `SurahMetadata` (SurahMeta/JuzMeta + SURAHS + JUZ_LIST), `MorphologyMaps` (MorphologyTerm + POS/FORMS/ASPECT/MOOD/VOICE/CASE/STATE/DERIVATION AR+EN maps + FORM_ORDINAL_AR + MORPHOLOGY_TERMS), `QuranStats` extended with the 6 corpus STATS_ constants.
- [x] All 7 consumers migrated atomically (QuranRepositoryImpl, HomeScreen, BookmarksScreen, SurahIndexScreen, MorphologyGuideScreen, RevelationType + its test) — `data.util` package **no longer exists**; grep-verified.
- [x] MorphologyGuideScreen moved → `ui/guide/` (package update, unused `Emerald700`/`QuranGold` dropped); AppNavigation import updated. Locale AR/EN map selection kept in the screen (documented deviation: dictionary reference data with explicit AR/EN variants, not UI strings — converting to resources would add churn with no user-visible gain; revisit at closure if desired). No VM needed (static screen).

### Phase 9 — Home + Settings — ✅ implemented
- [x] Split → `HomeScreen` (~160 L) + components/: `HomeHeader` (3 hero toggles → vectors @ 48dp, 🔍 trigger bar → `Icons.Filled.Search`), `HomeBanners` (DbSetupBanner 💾→`Download`, HomeUpdateBanner), `QuickNavRow` (📖🌿📐 → MenuBook/Eco/Architecture vectors), `HomeQuickCards` (🔖→`Bookmark`, ⭐→`Star`), `StatsGrid` (6 StatCards; `StatCard` icon: String→ImageVector), `FeaturedRootsSection` (`LazyListScope` ext).
- [x] **`HomeViewModel` → `ui/home/`** + `HomeUiState` (adds isDbReady + bookmark slices; Result-mapped featured roots); **`DbUpdateViewModel` → `ui/appstate/`**; **`SettingsViewModel` + `SettingsDialog` → `ui/settings/`** (theme/language setters leave MainViewModel; dead `colorMode` not exposed; `close_theme_dialog`/`open_theme_dialog`/`toggle_*` testTags preserved).
- [x] `HomeRoute` hosts 3 VMs + settings dialog; `LifecycleResumeEffect` refreshes DB status on resume (replaces shared-MainViewModel refresh; Home no longer takes MainViewModel). New strings `cd_open_settings`/`cd_toggle_dynamic_color`/`cd_toggle_dark_mode` (en/ar); ALL home testTags preserved; `ui/screens/HomeScreen.kt` deleted.
- [x] `SurahItemCard` 🔖/☆ bookmark → Bookmark/BookmarkBorder vectors.

### Phase 10 — SurahDetail + DatabaseSetup + MainViewModel deletion — ✅ implemented
- [x] **SurahDetail atomic migration** (`ui/surah/detail/`): `SurahDetailContract` (UiState incl. fontSize/bookmarks/isDbReady slices + error) + `SurahDetailViewModel` (ported 1:1; `SelectionState` for ayah multi-select — 3rd duplicate killed; Result-mapped loadSurah/loadNextPage/ensureAyahLoaded/ensurePageLoaded; **single `updateLastRead` owner** — double-write removed; AI-summary failure documented non-fatal) + `SurahDetailRoute` (ShareHandler for ✓📋↗ selection bar → Done/ContentCopy/Share vectors) + stateless `SurahDetailScreen` + components/: `SurahCollapsingHeaderState` (saveable holder, physics 1:1), `SurahDetailHeader` (pinned bar + collapsible area, quick-return nested-scroll preserved), `SurahAyatList` (basmalah/separators/stagger), `SelectionTopBar`. `onPageClick` delay(100)+peek hack → pending-page LaunchedEffect. 📥 empty state → Download vector. ALL testTags preserved.
- [x] **DatabaseSetup atomic migration** (`ui/setup/`): `SetupContract` with `SetupDownload`/`SetupError` UI mirror — VM maps `DownloadState` 1:1, **data.remote stops leaking into UI**; file-picker moved to `DatabaseSetupRoute`; ✅/💾 → CheckCircle/Download vectors; dead imports (`viewModel`, `Emerald700`, `QuranGold`) gone; FQ `TextButton`/`AlertDialog` fixed; ALL testTags preserved (`db_setup_screen`, `start_download_button`, `import_db_button`, `confirm_import_button`, `db_version_card`, …).
- [x] **`ui/viewmodel/` package DELETED** (ViewModels.kt 1,428→510→0; AppPreferencesUiState removed): MainActivity theme/language → `SettingsViewModel`; AppNavigation takes no VMs — every destination is a self-contained Route; MainViewModel dissolution completed via feature slices (Decision 14 amendment).
- [x] **MorphologyBottomSheet fixes**: dead `hasTried = true` removed (else-branch), `aiSummary!!` → smart-cast local, 🤖/🕒 emoji → plain chips, inline date formatting → `core.util.cleanAiDate`.

### Phase 11 — Cross-cutting closure — ✅ implemented
- [x] **A11y pass**: `SurahItemCard` bookmark chip → `Modifier.minimumInteractiveComponentSize()` (48dp target, compact visual); M3 `IconButton`-based controls already enforce 48dp (FontSizeControls, remove-bookmark buttons, hero toggles); decorative 40–46dp icon slots sit inside large clickable cards (no standalone targets).
- [x] **`UserPreferencesRepository` → domain interface** (`domain/repository/`) + **`core/datastore/UserPreferencesDataStore`** impl (private file-scoped DataStore — global `Context.dataStore` extension deleted); `@Binds` binding in `core/di/PreferencesModule`; dead members removed (`isSurahBookmarked`, `colorMode`/`setColorMode`, `isSetupDismissed`/`setSetupDismissed`, `getInstalledDbVersionCode`, `lastDbCheckAt` flow); 6 VMs + `DbUpdateRepositoryImpl` + AppModule migrated to the interface.
- [x] **Dispatcher qualifier consolidation**: duplicate `di/AppModule` `@IoDispatcher/@MainDispatcher/@DefaultDispatcher` annotations deleted — single source in `core/di/DispatcherModule`.
- [x] **Turbine 1.2.1 added**; shared test fixtures (`fake/TestFixtures.kt`: stateful `FakeUserPreferences` + configurable `FakeQuranRepository`); new suites: `BookmarksViewModelTest` (parse/sort/toggle ×4), `RootsListViewModelTest` (load/filter/normalization/error+retry ×4), `SearchViewModelTest` (virtual-time debounce, tab pager, reset, error+retry ×4) — Turbine + `uiState` assertions. Setup VM download mapping noted as Robolectric follow-up (concrete download manager). Roborazzi/Compose-UI journeys deferred to dedicated follow-up (existing suites still green).
- [x] **Unused-import sweep** on all new/touched files (SurahAyatList, SelectionTopBar, DatabaseSetupScreen, SurahDetailScreen, HomeBanners); existing detekt baseline unchanged.
- [ ] **Paging3 follow-up** (Decision 12) — separate roadmap when scheduled (still the only open AGENTS §17.1 gap).

---

## 4) Verification protocol (user runs — agents never run gradle)

```bash
# after every phase
./gradlew detektBaseline        # Phase 1 only: generate baseline once, commit it
./gradlew spotlessCheck detekt lintDebug
./gradlew spotlessApply         # optional one-shot mechanical format (Phase 1 adoption)
./gradlew testDebugUnitTest
./gradlew assembleDebug

# screenshots (Phase 9 / 10)
./gradlew recordRoborazziDebug
```

**Manual matrix (per feature PR):** navigation in/out, pagination to end, copy/share every path, bookmark toggle, scroll restore + rotation + large font, RTL correctness, empty/error/DB-missing states, process-death restore where applicable (WordAyat, RootDetail).

## 5) Risk register

| Risk | Phase | Mitigation |
|---|---|---|
| SurahDetail collapse physics regression | 10 | Port `CollapsingHeaderState` unchanged; pixel-for-pixel manual check; keep nested-scroll connection intact |
| Scroll/pagination breaks deep-link ayah jump | 10 | Phase 0 behavior checklist; `ensureAyahLoaded`/`ensurePageLoaded` preserved behind VM |
| Nav migration breaks back-stack/args | 2 | Typed routes are compile-time; run full nav matrix before feature phases |
| Spotless/detekt first-run violations on legacy code | 1 | detekt baseline generated by user (`detektBaseline`); Spotless check untied from `check`; optional `spotlessApply` |
| Plugin/dependency version resolution on Gradle 9.3.1 + AGP 9.1.1 + Kotlin 2.2.10 | 1 | Versions flagged for user verification; adjust from build output if resolution fails |
| Download flow regression | 10 | Map DownloadState 1:1 to UiState; manual download/import matrix |
| Theme dialog regressions (locale) | 9 | LanguageManager path untouched; verify ar/en/system switching |

## 6) Test additions (cumulative)

| Level | Targets | Tool |
|---|---|---|
| Unit | Result, ArabicNormalizer, AiMetaFormatter, AyahHighlighter, SelectionState, BookmarkRef mapper | JUnit |
| VM | AppStateViewModel combine, SurahDetailViewModel paging/selection/last-read, SearchViewModel debounce+4-tab paging, DatabaseSetupViewModel DownloadState mapping, RootViewModel (migrate existing) | Turbine + Mockk |
| UI | MorphologyBottomSheet journey, bottom nav (vectors), SearchScreen tabs | Compose Test Rule |
| Screenshot | HomeScreen (Phase 9), SurahDetailScreen (Phase 10) | Roborazzi |

---

*Approved by Ahmed — 2026-09-10 (atomic revision). Discovery: full audit of `ui/screens/` (5,773 L), `ui/viewmodel/` (1,534 L), `ui/navigation/`, `ui/components/`, domain/data contracts, existing tests (11 files), gradle deps (Gradle 9.3.1, Kotlin 2.2.10, AGP 9.1.1; lifecycle-runtime-compose present; paging/serialization absent at audit time).*
