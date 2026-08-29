# AGENTS.md — Quran Words (كلمات القرآن)

> **This file is the single binding reference for every Agent and contributor.** It describes the **ideal final application** under Clean Code / Clean Architecture standards. Any existing code that does not comply is considered **technical debt to be fixed** — do not document it as reality; correct it to what is described here. See `docs/REFACTOR_PLAN.md` for the remediation roadmap.

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

---

## 2) Non-Negotiable Principles

1.  **Clean Architecture — Dependency Rule:** `domain` knows nothing about `data` or `ui`. `data` depends on `domain` via interfaces. `ui` depends only on `domain`.
2.  **SOLID:** Single Responsibility (SRP), Open/Closed (OCP), Dependency Inversion (DIP).
3.  **DRY / KISS / YAGNI:** No duplication, no premature complexity, no unrequested features.
4.  **UDF (Unidirectional Data Flow):** `Event → ViewModel → UiState → Screen`. No DB/network logic inside a `@Composable`.
5.  **Fail Fast & Explicit:** No silent `catch` + `emit(emptyList())`. Every failure is mapped to `Result.Error`, surfaced to the user or logged via Crashlytics.
6.  **Testability First:** Any code that cannot be tested without the Android framework needs refactoring.

---

## 3) Package & Identity

```
Unified package (namespace + applicationId + every Kotlin package):
  io.github.ahmedsaadi0.quranwords

Source: reverse DNS of the repository github.com/AhmedSaadi0/quran-words-android
```

- Every Kotlin file starts with `package io.github.ahmedsaadi0.quranwords.<layer>.<feature>`
- `com.example` must not exist anywhere.
- `AndroidManifest.xml` and `app/build.gradle.kts` have identical `namespace` and `applicationId`.

---

## 4) Tech Stack — Ideal

| Layer | Technology |
|---|---|
| **Language** | Kotlin 2.2, Coroutines + Flow, Serialization |
| **UI** | Compose BOM, Material3, Navigation Compose **Typed** (`@Serializable` routes), Paging3 |
| **DI** | **Hilt** (Dagger) — `@HiltAndroidApp`, `@AndroidEntryPoint`, `@Module @InstallIn(SingletonComponent::class)` |
| **Data** | Room 2.7 (pre-packaged DB), DataStore Preferences, OkHttp, Retrofit (if needed), Moshi/Kotlinx Serialization |
| **Monitoring** | **Firebase Crashlytics only** + Analytics (no AI/AppCheck/Auth/Firestore) |
| **Quality** | ktlint (official), detekt, Android Lint, R8, Room schema export |
| **Testing** | JUnit, Turbine, Mockk, Robolectric, Compose Test Rule, Roborazzi screenshot |

> **Removals:** `firebase-ai`, `firebase-appcheck-*`, `accompanist-permissions`, `camera-*`, `coil`, `play-services-location` — remove from `libs.versions.toml` and `app/build.gradle.kts` unless a documented need appears.

---

## 5) Ideal Folder Structure

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
- No file exceeds **300 lines**. Functions < 40 lines. Screens < 250 lines (split into components).
- `core` does not depend on `data/domain/ui`. `domain` does not depend on `data/ui`.

---

## 6) Data Layer — Rules

### 6.1 Golden Rule: Room Is the Only Source
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
- Raw `SQLiteDatabase.openDatabase(..., OPEN_READONLY)` is **forbidden**. Every query goes through a `DAO`.
- `QuranDatabase` owns all entities and exports its `schema` (`exportSchema = true`).

### 6.2 DataSource
- `LocalDataSource` is a thin interface over DAOs — no mapping logic.
- `RemoteDataSource` is an interface for download/import — implemented as `OkHttpDownloadDataSource` injected with `OkHttpClient`.
- File replacement goes through `QuranDatabase.close()` + deleting `wal/shm` before `renameTo`.

### 6.3 Repository
- `QuranRepositoryImpl` depends only on `DataSource` objects, not directly on `Context`.
- No seed data inside the repository. No `getSeedAyat()` / `getSeedRoots()`. When `!isDatabaseReady()` it returns `Result.Error(DbNotReady)` and the UI shows `DatabaseSetupScreen`.
- Every function returns `Result<T>` or `Flow<Result<T>>`, never a silent `emptyList()`.

### 6.4 Toggles
- `isDatabaseReady()` checks `file.exists() && length > 50 MB` in exactly one DataSource.

---

## 7) Domain Layer — Rules

```kotlin
// Each use case has a single responsibility
class SearchUseCase @Inject constructor(
    private val repo: QuranRepository,
    private val normalizer: ArabicNormalizer
) {
    suspend operator fun invoke(query: String): Result<SearchResult> {
        val normalized = normalizer.normalize(query) // normalization lives here, not in repository
        if (normalized.isBlank()) return Result.Success(SearchResult())
        return repo.searchAll(normalized)
    }
}
```

- `domain/model` is pure Kotlin — no `@Entity` or `@ColumnInfo`.
- `domain/repository` contains interfaces only.
- One `usecase` per user action — testable without Android. ViewModels only map `Result → UiState`.

---

## 8) UI Layer — Rules

### 8.1 Contract (Required for Every Screen)
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

### 8.2 ViewModel
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
- No `AndroidViewModel` except when strictly necessary. Do not pass `Application` to a repository.
- `Dispatchers` are injected via `DispatcherModule` to allow `TestDispatcher`.

### 8.3 Composable
- A screen receives `uiState: UiState` + `onEvent: (Event) -> Unit` only. No `viewModel()` inside deeply nested components.
- **Forbidden:** `remember { QuranRepositoryImpl(context) }` inside `MorphologyBottomSheet` — data is passed down from the ViewModel.
- Decomposition: `HomeScreen` (<200 lines) composes `HomeHeader`, `DbBanner`, `QuickNavRow`, `StatsGrid`, `FeaturedRoots`.
- `Modifier.animateItem()` and `Crossfade` are unified via `AppMotion.DurationMedium = 250ms`.

### 8.4 Design System
- Colors come from `Color.kt` only (Natural Tones). No hardcoded `Color(0xFF...)` inside screens.
- Shapes are unified: `ShapeSmall 12dp / ShapeMedium 16dp / ShapeLarge 20dp`.
- Icons are vector `Icons.Filled.*`, not emoji as a primary interactive element. Emoji is secondary decoration only.
- Typography comes from `Typography` only. Ayah font sizes flow via `fontSize: StateFlow<Float>` from `UserPreferences`.

---

## 9) Navigation

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

- No string routes like `route = "surah_detail/{surahId}?ayah={ayah}"`. All routes are type-safe.
- `BottomNavItem` holds `icon: ImageVector` + `labelRes: Int` + `route: Any`.

---

## 10) State & Preferences

- `UserPreferencesRepository` is an interface with a single `DataStore` implementation in `core/datastore`.
- Keys are centralized in a `PreferencesKeys` object. No global `Context.dataStore` extension in a repository file.
- `darkMode: 0=system/1=light/2=dark` and `dynamicColor: Boolean` are clearly separate — do not write two keys in the same `edit` block.

---

## 11) Error Handling

```kotlin
sealed interface Result<out T> {
    data class Success<T>(val data: T): Result<T>
    data class Error(val message: String, val cause: Throwable? = null): Result<Nothing>
    data object Loading: Result<Nothing>
}
```

- Repository catches exceptions → `Result.Error` + `Log` + `Crashlytics.recordException`.
- ViewModel maps `Result` → `UiState(error=...)`.
- Screens show an `ErrorCard` with a retry button — no silent `emptyList`.

---

## 12) Utilities

- **`ArabicNormalizer`** in `core/util` — `stripDiacritics` + `normalizeAr` with mandatory unit tests.
- **`QuranMetaData`** — split `QuranMetaConstants` into `SurahMetadata` (114 surahs) + `MorphologyMaps` (POS/Forms) + `Stats`.
- No magic constants (`118_534_144L`, `50_000_000L`) scattered around — define as `const val DB_EXPECTED_SIZE = 118_534_144L` in `DatabaseConstants`.

---

## 13) Testing

| Level | What | Tool |
|---|---|---|
| **Unit** | `ArabicNormalizer`, every `UseCase`, `ViewModel` (with `Turbine`) | JUnit + Mockk + Turbine |
| **Local Data** | DAO + `LocalDataSource` | Robolectric + Room inMemory |
| **UI** | `AyahItemCard`, `RootItemCard`, `SearchScreen` | Compose Test Rule + unified `testTag` |
| **Screenshot** | `HomeScreen`, `SurahDetailScreen` | Roborazzi |

- Every `UseCase` must have `Success` + `Error` + `Empty` tests.
- `testTag` format: `"<feature>_<element>_<id>"` (e.g. `surah_item_2`, `word_15`).

---

## 14) Build & Quality

- `libs.versions.toml` is clean — no dead `// implementation(...)` comments.
- `gradle.properties`: `org.gradle.caching=true`, `configuration-cache=true`, `nonTransitiveRClass=true`.
- CI (GitHub Actions): `./gradlew ktlintCheck detekt lintDebug testDebugUnitTest`.
- `Room` exports its schema to `app/schemas/`.

---

## 15) Performance & Security

- Ayat are loaded via `Paging3` (20 per page), not manual `getAyatBySurahPaged` + `delay(300)`.
- Images (if any) via `coil` with `rememberAsyncImagePainter` + `crossfade`.
- No secrets in code. `google-services.json` is in `.gitignore` — only `Crashlytics` is enabled.
- `android:allowBackup="true"` with specific `dataExtractionRules` — never back up `quran_words.db`.

---

## 16) Agent Workflow — Do / Don't

| Do ✅ | Don't ❌ |
|---|---|
| Read `AGENTS.md` before any change | Never create new code under `com.example` |
| Use Hilt to inject every dependency | Never `new QuranRepositoryImpl(context)` in a ViewModel/Composable |
| Add `UiState/Event/Effect` for every new screen | Never put DB/network logic inside a `@Composable` |
| Add a test for every new UseCase | Never silently `catch(e: Exception){ /*fallback*/ }` |
| Use `Result<T>` for error handling | Never `emit(emptyList())` to hide failure |
| Split files > 300 lines | Never ship a God screen > 400 lines |
| Use the unified `testTag` convention | Never use emoji as a primary interactive icon |
| Run `ktlintCheck` before committing | Never leave dead commented code like `// implementation(...)` |

---

## 17) Refactor Roadmap

The current codebase does not yet comply with several points above. **Do not treat the current code as the reference** — this document is the correct one.

Full phased tasks, affected files, and timeline are documented in:

**`docs/REFACTOR_PLAN.md`**

> Any Agent starting a task must open `REFACTOR_PLAN.md`, pick a Phase, and execute its tasks in order. Do not jump between Phases.

---

## 18) Quick Commands

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

<p align="center"><sub>Crafted with care for the Book of Allah — clean standard, clean code.</sub></p>
