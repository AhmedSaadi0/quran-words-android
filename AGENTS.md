# AGENTS.md — Quran Words (كلمات القرآن)

> **This file is the single binding reference for every Agent and contributor.** It describes the **ideal final application** under Clean Code / Clean Architecture standards. Any existing code that does not comply is considered **technical debt to be fixed** — do not document it as reality; correct it to what is described here. See `docs/REFACTOR_PLAN.md` for the remediation roadmap.
>
> **Base framework:** `AGENTS.base.md` (Android Project Engineering Guide) — discovery, decision discipline, and quality gates. When project-specific rules conflict with the base, follow: (1) explicit product requirement, (2) this file, (3) the base. Document any architectural deviation via Decision Log.

---

## 1) Product Overview

**Quran Words** — A comprehensive linguistic Quran dictionary, **100% offline** after the database is downloaded.

| Area | Details |
|---|---|
| **Content** | 114 surahs, 6236 ayat, 77,429 word occurrences, 21,295 unique words, 1,642 verified roots, 5,273 masadir, 16,245 derivatives |
| **Features** | Surah/Juz index, ayah viewer with per-word morphology, root dictionary with classical definitions (Lisan al-Arab / al-Sihah / Maqayis), masadir & derivatives with patterns, AI summary per root, comprehensive search (root/word/masdar/ayah) with Arabic normalization, bookmarks for surahs & ayat, last-read tracking, database management (download/import) |
| **Database** | Single file `quran_words.db` (~118 MB) — downloaded from GitHub Releases or imported from device storage. **No fake data.** If the database is not ready, only the download screen is shown |
| **Platform** | Android Min SDK 24, Target 36, Compose Material3, full Arabic RTL |
| **Repository** | `https://github.com/AhmedSaadi0/quran-words-android` — open data source `AhmedSaadi0/quran-words` |
| **Business model** | Offline reference utility, B2C free, no auth/billing/subscription, no ads — freemium is **not** applicable |
| **Distribution** | Google Play + GitHub Releases (APK) + direct DB download from `media.githubusercontent.com` / `github raw` |
| **Supported languages** | Arabic (primary, RTL required), English (surah names + transliteration secondary). No other locales at launch |

---

## 2) Business & Domain Rules

### 2.1 Users & Roles
- **Single role: anonymous offline user.** No registration, no login, no admin/moderator/tenancy. Base §4.3 *Anonymous user only* applies; multi-tenancy is **explicitly out of scope**.
- No permissions matrix — every installed app has full read access to the local corpus. Write is limited to local preferences (bookmarks, last-read, theme, font size) and DB file replacement.

### 2.2 Core Entities & Invariants
- `Surah(114)`, `Ayah(6236)`, `Word(21295 unique / 77429 positions)`, `Root(1642)`, `Masdar(5273)`, `Derivative(16245)`, `Morphology`, `Bookmark`, `LastRead`.
- `6236 ayat` and word positions are **immutable reference data** — never created/edited/deleted by the client except via atomic DB file replacement.
- Business invariants: ayah numbering per surah is canonical; `word_ayah.position` is contiguous `1..word_count`; root text is normalized Arabic without diacritics.

### 2.3 Critical User Journeys
1. **First launch → DB missing → Download/import DB (118MB) → Home**
2. **Browse surahs by Surah/Juz → Open SurahDetail (paged 20) → Scroll with pagination → Tap word → Morphology sheet → Navigate to RootDetail**
3. **Search (Arabic-normalized) → Tabs: Roots/Masadir/Derivatives/Ayat → Open result**
4. **RootDetail → Tabs: Meanings/Masadir/Derivatives/AyatOccurrences (paged 30) → Tap ayah → SurahDetail at exact ayah**
5. **Bookmark surah/ayah → View Bookmarks → Resume last-read**

Success: user finds any word’s root, its classical meanings, and every Quran occurrence with context. Failure cases: DB not ready, network fails during download, search with blank/normalized-empty query.

### 2.4 Functional Scope (what is / is not)
- Includes: browsing, paging, search/filter, bookmarks, preferences, DB download/import, Arabic normalization, deep linking to `SurahDetail(surahId, ayah)`.
- Excludes: auth, payments, realtime, push, WorkManager, file uploads, social, sync, multi-device.

### 2.5 Non-Functional Requirements
- Offline-first, text-only, large read-only dataset, accessibility for Arabic readers, startup <2s cold on mid-range device.

---

## 3) Architecture Decision

```
Architecture:           Clean 3-layer (Presentation/UI → Domain → Data) + Core
Why:                    Medium app — 77k word positions + 11 tables + search with Arabic
                        normalization + pagination + bookmarks justify testability and
                        boundary clarity. Simpler 2-layer (UI→Data) was considered and
                        rejected: it would leak Room/SQLite into Composables (seen in
                        MorphologyBottomSheet) and make business logic untestable.
                        Full multi-module was deferred (single :app with feature folders;
                        extract :core/:data only if team >3).
Main dependency dir:    Presentation knows Domain contracts; Domain is pure Kotlin with
                        no Android deps; Data implements Domain contracts. Core has no
                        dependency on Data/Domain/UI. (Base §2.4 / §5.1 medium app).
State management:       UDF + StateFlow UiState/Event/Effect per feature; ViewModel → UseCase → Repository.
Module strategy:        Single module :app; feature-first folders; shared components only when truly reusable.
Data strategy:          OFFLINE_FIRST, Room single source of truth (prepackaged file), no sync.
```

Document any future architectural deviation (e.g., adding multi-module, Ktor, WorkManager) via Decision Log §20.

---

## 4) Non-Negotiable Principles

1.  **Clean Architecture — Dependency Rule:** `domain` knows nothing about `data` or `ui`. `data` depends on `domain` via interfaces. `ui` depends only on `domain`.
2.  **SOLID:** Single Responsibility (SRP), Open/Closed (OCP), Dependency Inversion (DIP).
3.  **DRY / KISS / YAGNI:** No duplication, no premature complexity, no unrequested features. Do not introduce abstractions solely for theoretical purity (Base §2.2).
4.  **UDF (Unidirectional Data Flow):** `Event → ViewModel → UiState → Screen`. No DB/network logic inside a `@Composable`.
5.  **Fail Fast & Explicit:** No silent `catch` + `emit(emptyList())`. Every failure is mapped to `Result.Error`, surfaced to the user or logged via Crashlytics.
6.  **Testability First:** Any code that cannot be tested without the Android framework needs refactoring.

---

## 5) Package & Identity

```
Unified package (namespace + applicationId + every Kotlin package):
  io.github.ahmedsaadi0.quranwords

Source: reverse DNS of the repository github.com/AhmedSaadi0/quran-words-android
```

- Every Kotlin file starts with `package io.github.ahmedsaadi0.quranwords.<layer>.<feature>`
- `com.example` must not exist anywhere.
- `AndroidManifest.xml` and `app/build.gradle.kts` have identical `namespace` and `applicationId`.

---

## 6) Tech Stack — Ideal

| Layer | Technology | Notes |
|---|---:|---|
| **Language** | Kotlin 2.2, Coroutines + Flow, Serialization | Gradle Kotlin DSL |
| **UI** | Compose BOM, Material3, Navigation Compose **Typed** (`@Serializable` routes), Paging3 | Material3 + Natural Tones tokens |
| **DI** | **Hilt** — `@HiltAndroidApp`, `@AndroidEntryPoint`, `@Module @InstallIn(SingletonComponent::class)` | Strong default for medium app (Base §6.2); simpler DI rejected due to 6+ ViewModels + testability |
| **Local data** | Room 2.7 (pre-packaged DB), DataStore Preferences | Room is single source of truth; DataStore for settings/bookmarks |
| **Remote data** | OkHttp (DB download/import) | Retrofit/Moshi only when a JSON API appears; not needed today |
| **Serialization** | Kotlinx Serialization / Moshi | As needed for API DTOs |
| **Background work** | Coroutines only (`viewModelScope`, `Dispatchers.IO` injected) | `WorkManager`/foreground services **not** needed — no durable deferrable work (Base §6.6/§23) |
| **Monitoring** | **Firebase Crashlytics only** + Analytics (no AI/AppCheck/Auth/Firestore) | No analytics without product requirement (Base §30) |
| **Quality** | ktlint (official), detekt, Android Lint, R8, Room schema export |  |
| **Testing** | JUnit, Turbine, Mockk, Robolectric, Compose Test Rule, Roborazzi screenshot |  |

> **Removals:** `firebase-ai`, `firebase-appcheck-*`, `accompanist-permissions`, `camera-*`, `coil`, `play-services-location` — remove from `libs.versions.toml` and `app/build.gradle.kts` unless a documented need appears (see §17 for the `coil` exception). Evaluate any new dependency against Base §24 (stdlib first, maintenance/security, version compat).

---

## 7) Ideal Folder Structure

```
app/src/main/java/io/github/ahmedsaadi0/quranwords/
├── App.kt                          # @HiltAndroidApp
├── MainActivity.kt                 # @AndroidEntryPoint — single RTL provider
│
├── core/
│   ├── di/                         # AppModule, DatabaseModule, NetworkModule, DispatcherModule
│   ├── navigation/                 # NavGraph, Typed Routes, BottomNavItems
│   ├── datastore/                  # PreferencesDataStore (no global Context.dataStore extension)
│   ├── theme/                      # Color.kt, Type.kt, Shapes.kt, Theme.kt, Motion.kt
│   └── util/                       # Result.kt, ArabicNormalizer.kt, Constants.kt, QuranMetaData.kt
│
├── data/
│   ├── local/
│   │   ├── db/                     # QuranDatabase (Room), Entities, Daos
│   │   └── datasource/             # SurahLocalDataSource, AyahLocalDataSource, RootLocalDataSource
│   ├── remote/
│   │   └── datasource/             # DatabaseDownloadDataSource (interface + OkHttp impl)
│   ├── preferences/
│   │   └── UserPreferencesDataSource
│   └── repository/                 # QuranRepositoryImpl, UserPreferencesRepositoryImpl
│
├── domain/
│   ├── model/                      # Surah, Ayah, WordToken, RootItem, RootDetail... (pure Kotlin)
│   ├── repository/                 # QuranRepository (interface), UserPreferencesRepository (interface)
│   └── usecase/                    # GetSurahsUseCase, GetAyatPagedUseCase, GetRootDetailUseCase, SearchUseCase...
│
└── ui/
    ├── home/                       # HomeContract (UiState/Event/Effect) + HomeViewModel + HomeScreen + components
    ├── surah/                      # SurahIndex + SurahDetail (separate folder per screen)
    ├── roots/                      # RootsList + RootDetail
    ├── search/                     # SearchScreen
    ├── bookmarks/                  # BookmarksScreen
    ├── setup/                      # DatabaseSetupScreen
    ├── guide/                      # MorphologyGuideScreen
    └── components/                 # AyahItemCard, RootItemCard, WordChip... (<150 lines per file)
```

**Rules:**
- Each feature has its own folder containing `Contract + ViewModel + Screen + Components`.
- Target file <300 lines, function <40 lines, screen <250 lines — split by **responsibility, not number alone** (Base §28: *Do not split into meaningless fragments*). God files >400 lines are a signal, not an absolute law.
- `core` does not depend on `data/domain/ui`. `domain` does not depend on `data/ui`.
- This structure matches Base §7 default for medium Compose apps; for a truly small app fewer layers would have been chosen — not this one.

---

## 8) Data Layer — Rules & Strategy

### 8.1 Golden Rule: Room Is the Only Source
```kotlin
// DatabaseModule — the only ideal way
@Module @InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): QuranDatabase =
        Room.databaseBuilder(ctx, QuranDatabase::class.java, "quran_words.db")
            .createFromFile(ctx.getDatabasePath("quran_words.db")) // pre-packaged 118 MB
            .fallbackToDestructiveMigration(false)
            .build()
}
```
- Raw `SQLiteDatabase.openDatabase(..., OPEN_READONLY)` is **forbidden**. Every query goes through a `DAO` (Base §15).
- `QuranDatabase` owns all entities and exports its `schema` (`exportSchema = true`).

### 8.2 Source of Truth & Connectivity (Base §13-14)
- **Model:** `OFFLINE_FIRST`. Source of truth is local `Room` after DB install. No remote API as source of truth; network is used only to fetch the DB file.
- **What works offline:** everything — browsing, morphology, search, bookmarks, preferences. **What needs internet:** DB download/import only.
- **Sync:** none (read-only corpus). No conflict resolution, no retry queue, no bidirectional sync (Base §14 — *Do not implement sync complexity without requirements*). Future sync (e.g., user-contributed meanings) would require a new ADR.
- **Cache/stale policy:** N/A — file is immutable versioned asset.

### 8.3 Prepackaged Database Lifecycle (Base §15 — large prepackaged DB)
- **Install:** `createFromFile(getDatabasePath())`; file is `118_534_144L` expected, `>50_000_000L` ready-check in one DataSource only.
- **File replacement:** `QuranDatabase.close()` → delete `wal/shm` → `renameTo()` (or `copyTo` fallback on cross-filesystem).
- **Upgrades:** destructive recreation is acceptable (no user-generated DB content to migrate except prefs/bookmarks which are DataStore, not DB). Document in `DatabaseConstants`.
- **Backup:** `allowBackup=true` with `dataExtractionRules` that **never** back up `quran_words.db` (`§17 Security`).

### 8.4 DataSource
- `LocalDataSource` is a thin interface over DAOs — no mapping logic.
- `RemoteDataSource` is an interface for download/import — implemented as `OkHttpDownloadDataSource` injected with `OkHttpClient`.
- Mapping `Entity ↔ Domain` is explicit in `data/mapper` or repository; do not leak entities to UI (Base §13.2).

### 8.5 Repository
- `QuranRepositoryImpl` depends only on `DataSource` objects, not directly on `Context`.
- No seed data inside the repository. No `getSeedAyat()` / `getSeedRoots()`. When `!isDatabaseReady()` it returns `Result.Error(DbNotReady)` and the UI shows `DatabaseSetupScreen`.
- Every function returns `Result<T>` or `Flow<Result<T>>`, never a silent `emptyList()`. Repositories coordinate data access, not business rules (Base §13.3).

### 8.6 Toggles
- `isDatabaseReady()` checks `file.exists() && length > 50 MB` in exactly one DataSource — canonical constant in `DatabaseConstants`.

---

## 9) Domain Layer — Rules

```kotlin
// Good: has business value (Arabic normalization lives in UseCase, not repository)
class SearchUseCase @Inject constructor(
    private val repo: QuranRepository,
    private val normalizer: ArabicNormalizer
) {
    suspend operator fun invoke(query: String): Result<SearchResult> {
        val normalized = normalizer.normalize(query)
        if (normalized.isBlank()) return Result.Success(SearchResult())
        return repo.searchAll(normalized)
    }
}
```

- `domain/model` is pure Kotlin — no `@Entity` or `@ColumnInfo`, no Android deps (Base §8.2).
- `domain/repository` contains interfaces only.
- Create a UseCase only when it adds business value: validation, Arabic normalization, multi-repo orchestration, reusable behavior, or testable logic. **Avoid trivial `UseCase → repo.getX()`** with no value (Base §11). For this project: `SearchUseCase` and `GetRootDetailUseCase` are justified; plain `GetSurahById` without logic may delegate directly — document the exception.
- ViewModels only map `Result → UiState`.

---

## 10) UI Layer — Rules

### 10.1 Contract (Required for Every Screen with Meaningful State)
```kotlin
data class SurahDetailUiState(
    val isLoading: Boolean = true,
    val surah: Surah? = null,
    val ayat: List<Ayah> = emptyList(),
    val isLoadingMore: Boolean = false,
    val error: String? = null
)
sealed interface SurahDetailEvent { data class WordClicked(val word: WordToken): SurahDetailEvent }
sealed interface SurahDetailEffect { data class NavigateToRoot(val id: Int): SurahDetailEffect }
```
- For a trivial screen with one callback, a full Event hierarchy is not required (Base §10.2).
- Effects are one-time (navigation, snackbar) — do not store them as persistent state (Base §10.3).

### 10.2 ViewModel
```kotlin
@HiltViewModel
class SurahDetailViewModel @Inject constructor(
    private val getAyatPaged: GetAyatPagedUseCase,
    @IoDispatcher private val io: CoroutineDispatcher
) : ViewModel() {
    private val _uiState = MutableStateFlow(SurahDetailUiState())
    val uiState: StateFlow<SurahDetailUiState> = _uiState.asStateFlow()
    // never `new QuranRepositoryImpl()` — injection only
}
```
- No `AndroidViewModel` except when strictly necessary. Do not pass `Application` to a repository (Base §8.1).
- `Dispatchers` are injected via `DispatcherModule` to allow `TestDispatcher` (Base §22: injected dispatchers when testability benefits).

### 10.3 Composable
- A screen receives `uiState: UiState` + `onEvent: (Event) -> Unit` only. No `viewModel()` inside deeply nested components. Stateless + hoisted state (Base §17.1).
- **Forbidden:** `remember { QuranRepositoryImpl(context) }` inside `MorphologyBottomSheet` — data is passed down from the ViewModel.
- Decomposition: `HomeScreen` (<200 lines) composes `HomeHeader`, `DbBanner`, `QuickNavRow`, `StatsGrid`, `FeaturedRoots`.
- `Modifier.animateItem()` and `Crossfade` are unified via `AppMotion.DurationMedium = 250ms`.

### 10.4 Design System & Tokens (Base §17.2)
- Centralize colors, typography, shapes, dimensions, motion — do not hardcode arbitrary values per screen.
- Colors come from `Color.kt` only (Natural Tones). No hardcoded `Color(0xFF...)` inside screens.
- Shapes are unified: `ShapeSmall 12dp / ShapeMedium 16dp / ShapeLarge 20dp`.
- Icons are vector `Icons.Filled.*`, not emoji as a primary interactive element. Emoji is secondary decoration only.
- Typography comes from `Typography` only. Ayah font sizes flow via `fontSize: StateFlow<Float>` from `UserPreferences`.
- Provide `@Preview` for key components where useful.

### 10.5 Accessibility & Responsiveness (Base §18 / §17.4)
- Semantics: `contentDescription` for surah/ayah/bookmark actions; minimum 48dp touch targets; not color-only signals; contrast per Material3.
- Support font scaling (DataStore `fontSize`); test with large fonts and long translations.
- Responsive: phones primary; tablets via adaptive padding (`WindowInsets`, no fixed dp assumptions).

---

## 11) Navigation

```kotlin
@Serializable data object Home
@Serializable data object SurahIndex
@Serializable data class SurahDetail(val surahId: Int, val ayah: Int = 1)
@Serializable data class RootDetail(val rootId: Int)

NavHost(startDestination = Home) {
    composable<Home> { HomeScreen(...) }
    composable<SurahDetail> { backStackEntry.toRoute<SurahDetail>() }
}
```

- Centralized, type-safe routes (Base §16). No string routes like `route = "surah_detail/{surahId}?ayah={ayah}"`.
- Navigation decisions come from ViewModel effects / screen callbacks, not from reusable leaf components (Base §16).
- `BottomNavItem` holds `icon: ImageVector` + `labelRes: Int` + `route: Any`. Explicit arguments; document deep links when added.

---

## 12) State & Preferences

- `UserPreferencesRepository` is an interface with a single `DataStore` implementation in `core/datastore`. Prefer DataStore over SharedPreferences for new code (Base §6.4).
- Keys are centralized in a `PreferencesKeys` object. No global `Context.dataStore` extension in a repository file.
- `darkMode: 0=system/1=light/2=dark` and `dynamicColor: Boolean` are clearly separate — do not write two keys in the same `edit` block.

---

## 13) Error Handling

```kotlin
sealed interface Result<out T> {
    data class Success<T>(val data: T): Result<T>
    data class Error(val message: String, val cause: Throwable? = null): Result<Nothing>
}
```

- `Result` is `Success|Error`; `Loading` belongs in `UiState.isLoading`, not as a terminal Result (Base §12.2).
- Map low-level exceptions to domain errors (`DbNotReady`, `DownloadFailed`) where useful; do not expose raw exceptions (Base §12.3).
- Repository catches exceptions → `Result.Error` + `Log` + `Crashlytics.recordException`.
- ViewModel maps `Result` → `UiState(error=...)`. Screens handle `loading/success/empty/error` explicitly with retry where appropriate (Base §12.4) — no silent `emptyList`.

---

## 14) Utilities & Constants

- **`ArabicNormalizer`** in `core/util` — `stripDiacritics` + `normalizeAr` with mandatory unit tests — pure Kotlin, no Android deps.
- **`QuranMetaData`** — split `QuranMetaConstants` into `SurahMetadata` (114 surahs) + `MorphologyMaps` (POS/Forms) + `Stats`.
- No magic constants (`118_534_144L`, `50_000_000L`) scattered — define as `const val DB_EXPECTED_SIZE = 118_534_144L` in `DatabaseConstants`.

---

## 15) Testing

| Level | What | Tool |
|---|---:|---|
| **Unit** | `ArabicNormalizer`, UseCases (success/error/empty/boundary), mappers, ViewModel with Turbine | JUnit + Mockk + Turbine |
| **Local Data** | DAO + `LocalDataSource`, migrations | Robolectric + Room inMemory |
| **UI** | `AyahItemCard`, `RootItemCard`, `SearchScreen` journeys | Compose Test Rule + unified `testTag` |
| **Screenshot** | `HomeScreen`, `SurahDetailScreen` | Roborazzi |
| **Integration** | Download → Install → Browse flow | Instrumentation where critical (Base §25.4 — only critical journeys) |

- `testTag` format: `"<feature>_<element>_<id>"` (e.g. `surah_item_2`, `word_15`) per Base §25.3.
- Do not create expensive E2E for every minor component (Base §25.4).

---

## 16) Build & Quality

- `libs.versions.toml` is clean — no dead `// implementation(...)` comments; unused dependencies are removed (Base §24).
- `gradle.properties`: `org.gradle.caching=true`, `configuration-cache=true`, `nonTransitiveRClass=true`.
- CI pipeline (GitHub Actions — Base §27): `Build → ktlintCheck + detekt + lintDebug → testDebugUnitTest → assemble`. Do not claim a check passes without running it (Base §27).
- `Room` exports its schema to `app/schemas/`. Dependency checks via `detekt`/`lint` locally and in CI (Base §26).
- **Agent build policy — manual verification only:** Agents must **never** run `./gradlew`, `gradle`, `assembleDebug`, `ktlintCheck`, `detekt`, `lintDebug`, or `test*` commands automatically. Instead list the exact command(s) for the user to run and wait for their output. This avoids heavy local builds and respects the user's environment.

---

## 17) Performance & Security

### 17.1 Performance (Base §21)
- Ayat via `Paging3` (20/page) and ayat occurrences via 30/page — not manual `delay(300)`. Large lists use efficient lazy lists and paging (Base §21).
- Avoid main-thread DB; use `Dispatchers.IO` injected. No premature optimization without evidence.
- Images: **currently none** — the app is text-only. No image library is needed today, so `coil` stays removed per §6. If remote images are ever introduced, add `coil` then via ADR and use `rememberAsyncImagePainter` + `crossfade`.

### 17.2 Security & Privacy (Base §20)
- **Sensitive data:** none — Quran text is public; no PII, financial, health, location, or auth tokens. No secure storage needed beyond DataStore for prefs/bookmarks.
- **Hardcoding:** never hardcode secrets, credentials, or API keys; no secrets are committed.
- **Permissions:** `INTERNET` only. No location/camera/media/biometrics. Do not request unnecessary permissions (Base §20).
- **Logging:** never log passwords/tokens/PII/secrets; Crashlytics logs are scrubbed (Base §30).
- **Network:** HTTPS only for DB download via OkHttp; cert pinning not required for public GitHub assets — document if private source is added.
- **Backup:** `android:allowBackup="true"` with `dataExtractionRules` that **never** back up `quran_words.db` (large asset + reproducible).
- **Data collection:** no analytics without explicit product requirement (Base §30).

---

## 18) Coroutines, Dispatchers & Background Work

- Structured concurrency: `viewModelScope` / `lifecycleScope`; no `GlobalScope` for app work (Base §22).
- Injected dispatchers (`@IoDispatcher`, `@MainDispatcher`) via `DispatcherModule` — testable and avoids hardcoding (Base §22).
- No `WorkManager`/foreground services/background workers for this project — the only long work (DB download) is a coroutine Flow with progress; durable deferrable work does not exist (Base §23).

---

## 19) Agent Workflow

### 19.1 Seven-Phase Workflow (Base §33)
1. **Understand** — read `AGENTS.md`, `README.md`, `docs/REFACTOR_PLAN.md`, relevant feature code, `build.gradle.kts`.
2. **Discover** — identify business model, users, journeys, data strategy, offline model, security (already documented in §2-3; re-confirm if product changes).
3. **Decide** — document architecture/DI/state/navigation choices with reasons (Decision Log §20).
4. **Generate / Implement** — follow `Domain → Data → Presentation/VM → UI → Navigation → Tests` order (adjust when required).
5. **Verify** — **do not run builds yourself**; instead list the exact `ktlintCheck`/`detekt`/`lintDebug`/`assembleDebug`/`testDebugUnitTest` commands for the user to run, then wait for their output. Verify loading/error/empty handled, RTL/a11y respected, and never claim a check passes without user-provided output.
6. **Report** — list Implemented/Changed/Tests/Checks/Limitations/Debt; never claim untested checks.
7. **Iterate** — keep changes focused; avoid mixing unrelated features (Base §32).

### 19.2 Do / Don't (quick reference)

| Do ✅ | Don't ❌ |
|---|---|
| Read `AGENTS.md` before any change | Never create new code under `com.example` |
| Use Hilt to inject every dependency | Never `new QuranRepositoryImpl(context)` in a ViewModel/Composable |
| Add `UiState/Event/Effect` for every screen with meaningful state | Never put DB/network logic inside a `@Composable` |
| Add a test for every UseCase with business value | Never silently `catch(e: Exception){ /*fallback*/ }` |
| Use `Result<T>` for error handling; keep `Loading` in `UiState` | Never `emit(emptyList())` to hide failure |
| Split files by responsibility (target <300 lines) | Never ship a God screen >400 lines |
| Use the unified `testTag` convention | Never use emoji as a primary interactive icon |
| Run `ktlintCheck` before committing | Never leave dead commented code like `// implementation(...)` |
| Document architectural decisions | Never silently change architecture |

### 19.3 Change Discipline (Base §32)
- Search existing code before adding abstractions. Focused diffs only. Preserve unrelated behavior. Review diff, remove debug code and unused imports, verify formatting.

---

## 20) Decision Log

| # | Decision | Context | Options considered | Chosen | Reason | Trade-offs | Date |
|---|---|---|---|---|---|---|---|
| 1 | Package `io.github.ahmedsaadi0.quranwords` | Repo `AhmedSaadi0/quran-words-android`, need Play-safe unified namespace | `com.example` / `com.aistudio.quranwords.wkzq` / `io.github...` | `io.github.ahmedsaadi0.quranwords` | Reverse DNS of GitHub repo, globally unique, matches discovery product identity | Longer package, renames ~30 files | 2026-08-29 |
| 2 | Room as single source of truth + prepackaged `quran_words.db` | 77k positions, 11 tables, 118MB read-only corpus, offline-first | `SQLiteDatabase.openDatabase` raw / Room only / Room + raw fallback | Room `createFromFile` only | Type-safe DAOs, schema export, transactions; raw access was leaking into ViewModels | Must handle `wal/shm` on replace; destructive migration acceptable | 2026-08-29 |
| 3 | Paging3 for ayat (20/page) and ayat-occurrences (30/page) | 286 ayat/surah max, 854 occurrences/root max; loading all at once causes jank | Load all / manual `LIMIT/OFFSET` + `delay` / Paging3 | Paging3 + injected Dispatchers | Efficient lazy lists (Base §21), smooth pagination per §17 | Extra PagingSource boilerplate, but needed at scale | 2026-08-29 |
| 4 | Hilt DI | 6+ ViewModels, Room, OkHttp, DataStore | Manual construction / Koin / Hilt | Hilt | Constructor injection, centralized `SingletonComponent` modules, testable `TestDispatcher` | Gradle KSP overhead, but justified for medium app (Base §6.2) | 2026-08-29 |
| 5 | Crashlytics only (remove firebase-ai/appcheck/auth) | No auth/realtime feature today | Keep all Firebase / Minimal Crashlytics | Crashlytics + Analytics only | Least privilege, smaller APK, no unused deps (Base §24) | Re-add via ADR if server features appear | 2026-08-29 |
| 6 | Remove `coil`/camera/location/Permissions deps | Text-only app, no images/location | Keep commented deps / Remove | Remove | YAGNI, avoid accumulation (Base §24) | Re-add with ADR when image feature is justified | 2026-08-29 |

Add future significant choices here in the same format (Decision/Context/Options/Chosen/Reason/Trade-offs/Date) and never change rules silently.

---

## 21) Definition of Done

A task is complete only when these checklists pass (Base §36):

**Code**
- [ ] Matches requirements and `AGENTS.md` architecture; no unnecessary abstractions; no unrelated files changed.

**Errors & State**
- [ ] Loading / Success / Empty / Error handled explicitly; retry where appropriate; no hidden `emptyList()` fallback.

**Security**
- [ ] No secrets committed; no sensitive data logged; permissions justified; auth/session (N/A) remains correct; `dataExtractionRules` excludes DB.

**UI**
- [ ] Arabic RTL verified; localization via resources (no hardcoded user strings); accessibility (semantics, 48dp targets, contrast); font scaling respected.

**Testing**
- [ ] Relevant tests added (success/error/empty + boundary) and actually run; important journeys covered.

**Quality**
- [ ] Build succeeds; `ktlintCheck`/`detekt`/`lintDebug` pass; no dead code or unused dependencies; diff reviewed; no secrets added; architecture boundaries intact.

---

## 22) Refactor Roadmap

The current codebase does not yet comply with several points above. **Do not treat the current code as the reference** — this document is the correct one.

Full phased tasks, affected files, and timeline are documented in:

**`docs/REFACTOR_PLAN.md`**

> Any Agent starting a task must open `REFACTOR_PLAN.md`, pick a Phase, and execute its tasks in order. Do not jump between Phases.
> Discovery Summary for the initial generation is archived at the end of `docs/REFACTOR_PLAN.md`.

---

## 23) Quick Commands

```bash
# Quality checks
./gradlew ktlintCheck detekt lintDebug

# Tests
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest

# Screenshots
./gradlew recordRoborazziDebug
./gradlew compareRoborazziDebug

# Build
./gradlew assembleDebug
```

---

<p align="center"><sub>Crafted with care for the Book of Allah — clean standard, clean code. Base: AGENTS.base.md</sub></p>
