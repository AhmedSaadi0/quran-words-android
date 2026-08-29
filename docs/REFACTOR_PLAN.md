# REFACTOR_PLAN — خطة إعادة الهيكلة الشاملة

> **المرجع الملزم:** `AGENTS.md` يصف التطبيق المثالي. هذا الملف يصف **كيف نصل إليه** من الكود الحالي.
> **الحزمة النهائية المعتمدة:** `io.github.ahmedsaadi0.quranwords`
> **DI:** Hilt — **Seed Preview:** محذوف — **Firebase:** Crashlytics فقط

---

## ملخص الفجوة الحالية (Gap Analysis)

| المجال | الحالة الحالية | المطلوب في AGENTS.md | الخطورة |
|---|---|---|---|
| الحزمة | `com.example` / `com.aistudio.quranwords.wkzq` | `io.github.ahmedsaadi0.quranwords` موحّدة | 🔴 عالية |
| DI | لا DI — كل ViewModel ينشئ `QuranRepositoryImpl(context)` | Hilt Modules + حقن | 🔴 عالية |
| Data | `SQLiteDatabase.openDatabase` خام + `Room` Dead Code + Seed داخل Repo | Room فقط + DataSource + لا Seed | 🔴 عالية |
| Domain | لا UseCases | UseCase لكل فعل | 🟡 متوسطة |
| UI | شاشات God 400-770 سطر، لا UiState، منطق داخل Composable | Contract + تفكيك + UDF | 🔴 عالية |
| Navigation | نصوص `route = "surah_detail/{surahId}"` | Typed `@Serializable` routes | 🟡 متوسطة |
| Preferences | `Context.dataStore` عام + ازدواج مفاتيح | `core/datastore` + Keys مركزية | 🟡 متوسطة |
| Build | تعليقات deps ميتة، `firebase-ai/appcheck` بلا استخدام | تنظيف `libs.versions.toml` + Crashlytics فقط | 🟢 منخفضة |
| Errors | `catch{ fallback }` + `emit(emptyList())` صامت | `Result<T>` + Crashlytics | 🔴 عالية |
| Tests | لا اختبارات UseCase/DAO | JUnit+Turbine+Robolectric+Roborazzi | 🟡 متوسطة |

---

## Phase 0 — التحضير والأساس (1-2 يوم)

**الهدف:** تثبيت أدوات الجودة واختبارات توصيف قبل أي هدم.

- [ ] **0.1** إنشاء `branch: refactor/clean-architecture` من `main`
- [ ] **0.2** إضافة `ktlint` و `detekt` إلى `build.gradle.kts` وتشغيل `./gradlew ktlintCheck` — تثبيت baseline
  - ملفات: `build.gradle.kts`, `gradle/libs.versions.toml`
- [ ] **0.3** إضافة `.editorconfig` (official Kotlin style)
- [ ] **0.4** كتابة اختبارات توصيف (Characterization Tests) قبل الحذف:
  - [ ] `ArabicNormalizerTest` — stripDiacritics + normalizeAr (حالات: "ٱ", "أ", "ى", "ة")
  - [ ] `QuranMetaConstantsTest` — عدد السور 114، JUZ_LIST size 30
  - [ ] `SearchViewModel` smoke test (يُحذف لاحقاً بعد نقل المنطق لـ UseCase)
- [ ] **0.5** توثيق `docs/ARCHITECTURE.md` مختصر (رسم طبقات)

**معيار الخروج:** `ktlintCheck` يمر، والاختبارات الثلاثة خضراء.

---

## Phase 1 — الحزمة والهيكل و Hilt (2-3 أيام) 🔴 حرج

**الهدف:** توحيد الهوية وحقن الاعتمادات — أساس كل Phases.

### 1.1 تغيير الحزمة
- [ ] `app/build.gradle.kts:13` `namespace = "io.github.ahmedsaadi0.quranwords"`
- [ ] `app/build.gradle.kts:17` `applicationId = "io.github.ahmedsaadi0.quranwords"`
- [ ] `app/src/main/AndroidManifest.xml:8` `package` + `android:label`
- [ ] نقل المجلد: `app/src/main/java/com/example` → `app/src/main/java/io/github/ahmedsaadi0/quranwords`
- [ ] تحديث كل `package com.example...` → `package io.github.ahmedsaadi0.quranwords...` (حوالي 30 ملف)
  - بحث: `grep -r "com.example" app/src`
- [ ] نقل `app/src/test/java/com/example` و `androidTest` بنفس الطريقة
- [ ] تحديث `README.md:58` مسار DB الداخلي

### 1.2 إعادة هيكلة المجلدات
```
io/github/ahmedsaadi0/quranwords/
├── App.kt                      # جديد @HiltAndroidApp
├── MainActivity.kt
├── core/di/                    # جديد
├── core/datastore/             # نقل من data/repository
├── core/util/                  # نقل ArabicNormalizer + Result + Constants
├── core/navigation/            # نقل AppNavigation
├── core/theme/                 # نقل ui/theme
├── data/local/db/
├── data/local/datasource/
├── data/remote/datasource/
├── domain/model/
├── domain/repository/
├── domain/usecase/
└── ui/<feature>/               # تفكيك ui/screens + ui/viewmodel
```

### 1.3 إضافة Hilt
- [ ] `gradle/libs.versions.toml` — إضافة `hilt = "2.51.1"`, `hilt-compiler`
- [ ] `app/build.gradle.kts` — `plugins { id("dagger.hilt.android.plugin") }` + `ksp(libs.hilt.compiler)`
- [ ] `build.gradle.kts` — `alias(libs.plugins.hilt) apply false`
- [ ] إنشاء `App.kt`:
  ```kotlin
  @HiltAndroidApp class App: Application()
  ```
- [ ] `MainActivity.kt:24` → `@AndroidEntryPoint class MainActivity`
- [ ] إنشاء Modules:
  - `core/di/DatabaseModule.kt` — يوفر `QuranDatabase`
  - `core/di/RepositoryModule.kt` — يربط `QuranRepository` → `QuranRepositoryImpl`
  - `core/di/NetworkModule.kt` — يوفر `OkHttpClient`
  - `core/di/DispatcherModule.kt` — يوفر `@IoDispatcher`, `@MainDispatcher`
  - `core/di/PreferencesModule.kt` — يوفر `UserPreferencesRepository`
- [ ] تحويل كل ViewModel من `AndroidViewModel` → `@HiltViewModel class X @Inject constructor(...) : ViewModel()`
  - ملفات: `ui/viewmodel/ViewModels.kt` (كل الـ 6 ViewModels)

**معيار الخروج:** لا يوجد `QuranRepositoryImpl(context)` في أي ViewModel. التطبيق يبني ويعمل مع Hilt. كل الاستيرادات تستخدم `io.github.ahmedsaadi0.quranwords`.

---

## Phase 2 — طبقة Data: Room المصدر الوحيد + حذف Seed (5-7 أيام) 🔴 حرج

**الهدف:** إزالة `SQLiteDatabase` الخام و Seed، وتوحيد الوصول عبر Room + DataSource.

### 2.1 نقل DataStore
- [ ] إنشاء `core/datastore/PreferencesDataStore.kt`:
  ```kotlin
  val Context.dataStore by preferencesDataStore(name = "quran_words_prefs")
  ```
- [ ] حذف `val Context.dataStore` من `data/repository/UserPreferencesRepository.kt:16`
- [ ] إنشاء `core/datastore/PreferencesKeys.kt` — تجميع `KEY_FONT_SIZE`, `KEY_DARK_MODE`, `KEY_DYNAMIC_COLOR`, `KEY_LAST_SURAH`...
- [ ] فصل `KEY_DARK_MODE` و `KEY_COLOR_MODE` — لا كتابة مزدوجة في `setDynamicColorEnabled():76` و `setColorMode():83`

### 2.2 فصل DataSources
- [ ] إنشاء واجهات:
  - `data/local/datasource/SurahLocalDataSource`
  - `data/local/datasource/AyahLocalDataSource`
  - `data/local/datasource/WordLocalDataSource`
  - `data/local/datasource/RootLocalDataSource` (يضم masadir/derivatives/meanings/gloss/aiSummary)
- [ ] تنفيذات `*LocalDataSourceImpl` فوق `Dao` مباشرة (لا `Context`)
- [ ] إنشاء `data/remote/datasource/DatabaseDownloadDataSource`:
  ```kotlin
  interface DatabaseDownloadDataSource {
      fun isDatabaseReady(): Boolean
      fun getDatabaseFile(): File
      fun downloadDatabase(): Flow<DownloadState>
      fun importDatabase(uri: Uri): Flow<DownloadState>
  }
  ```
  - نقل منطق `data/remote/DatabaseDownloadManager.kt` (256 سطر) إلى `OkHttpDatabaseDownloadDataSource`
  - حقن `OkHttpClient` + `@ApplicationContext Context`

### 2.3 إصلاح QuranDatabase
- [ ] `data/local/QuranDatabase.kt:25` → `exportSchema = true` + إضافة `schemas/` في `app/build.gradle.kts`:
  ```kotlin
  ksp { arg("room.schemaLocation", "$projectDir/schemas") }
  ```
- [ ] `DatabaseModule` يبني DB عبر `createFromFile`:
  ```kotlin
  Room.databaseBuilder(ctx, QuranDatabase::class.java, "quran_words.db")
      .createFromFile(ctx.getDatabasePath("quran_words.db"))
      .fallbackToDestructiveMigration(false)
      .build()
  ```
- [ ] حذف `QuranDatabase.getInstance(context):54` الـ Singleton اليدوي و `closeIfNeeded():68` — تُدار عبر Hilt `@Singleton`
- [ ] حذف `sqliteDb: SQLiteDatabase? :33` و `getDb():35` و `closeDb():58` من `QuranRepositoryImpl`

### 2.4 إعادة كتابة QuranRepositoryImpl
- [ ] تغيير المُنشئ إلى:
  ```kotlin
  class QuranRepositoryImpl @Inject constructor(
      private val surahDs: SurahLocalDataSource,
      private val ayahDs: AyahLocalDataSource,
      private val rootDs: RootLocalDataSource,
      private val downloadDs: DatabaseDownloadDataSource
  ) : QuranRepository
  ```
  - لا `Context` مباشر
- [ ] **حذف Seed بالكامل** — إزالة من `data/repository/QuranRepositoryImpl.kt`:
  - [ ] `getSeedAyat():704` (57 سطر)
  - [ ] `getSeedRoots():763` (14 سطر)
  - [ ] `getSeedRootDetail():778` (37 سطر)
  - [ ] كل استدعاءات `getSeed*` في `getSurahs():106`, `getAyatBySurah():182`, `getAyahWithWords:329`, `getRootsPaged:377`, `getRootDetail:529`, `getRootByText:551`, `searchAll:693`
- [ ] تغيير كل دالة لتُرجع `Result<T>`:
  ```kotlin
  override suspend fun getRootsPaged(limit: Int, offset: Int): Result<List<RootItem>>
  override suspend fun getRootDetail(rootId: Int): Result<RootDetail>
  override suspend fun searchAll(query: String): Result<SearchResult>
  ```
- [ ] تحويل `loadWordsForAyah():228` (72 سطر rawQuery + JOIN) إلى استدعاءات DAO متعددة + تجميع في Mapper
- [ ] إضافة `core/util/Result.kt`:
  ```kotlin
  sealed interface Result<out T> {
      data class Success<T>(val data: T): Result<T>
      data class Error(val message: String, val cause: Throwable? = null): Result<Nothing>
      data object Loading: Result<Nothing>
  }
  ```
- [ ] كل `catch(e: Exception)` → `Result.Error` + `FirebaseCrashlytics.recordException(e)`

### 2.5 Paging 3
- [ ] إضافة `androidx.paging:paging-runtime-ktx` و `paging-compose`
- [ ] إنشاء `AyahPagingSource` و `RootPagingSource`
- [ ] تحويل `getAyatBySurahPaged:192` و `getRootsPaged:332` إلى `Pager`
- [ ] حذف `delay(300)` و `delay(80)` اليدوي في `SurahDetailViewModel:226,251`

### 2.6 ثوابت DB
- [ ] إنشاء `core/util/DatabaseConstants.kt`:
  ```kotlin
  const val DB_NAME = "quran_words.db"
  const val DB_EXPECTED_SIZE = 118_534_144L
  const val DB_MIN_SIZE = 50_000_000L
  const val DB_LFS_POINTER_LIMIT = 10_000_000L
  ```
- [ ] استبدال كل `118_534_144L` و `50_000_000L` و `10_000_000L` المبعثرة في `QuranRepositoryImpl:37` و `DatabaseDownloadManager:60,85,125,161`

**معيار الخروج:** `QuranRepositoryImpl` <250 سطر، لا `android.database.sqlite.*`، لا `getSeed*`، كل الدوال تُرجع `Result`. التطبيق عند `!isDatabaseReady()` يعرض `DatabaseSetupScreen` فقط.

---

## Phase 3 — طبقة Domain: UseCases (2-3 أيام)

- [ ] إنشاء `domain/usecase/` — كل ملف UseCase واحد:
  - [ ] `GetSurahsUseCase` — `Flow<Result<List<Surah>>>` — (يُغلف `repo.getSurahs()`)
  - [ ] `GetSurahByIdUseCase`
  - [ ] `GetAyatPagedUseCase` — (يُستبدل `getAyatBySurahPaged` اليدوي)
  - [ ] `GetAyahWithWordsUseCase`
  - [ ] `GetRootsPagedUseCase` — مع Paging
  - [ ] `GetRootDetailUseCase`
  - [ ] `GetRootByTextUseCase`
  - [ ] `SearchUseCase` — **ينقل `ArabicNormalizer.normalizeAr` من Repository إلى هنا** + validation `query.isBlank()`
  - [ ] `ObserveLastReadUseCase` + `UpdateLastReadUseCase`
  - [ ] `ToggleBookmarkUseCase` (Surah/Ayah)
  - [ ] `GetBookmarksUseCase`
- [ ] كل UseCase محقون بـ `QuranRepository` + `ArabicNormalizer` (إن لزم)
- [ ] لا منطق تحويل Entity→Domain داخل UseCase — التحويل في `data/repository/Mappers.kt`

**معيار الخروج:** ViewModel لا يستدعي `repo` مباشرة أبداً — فقط UseCases. كل UseCase له اختبار.

---

## Phase 4 — طبقة UI: UDF + تفكيك الشاشات (7-10 أيام) 🔴 الأكبر

### 4.1 قاعدة Contract
- [ ] إنشاء لكل Feature:
  ```
  ui/home/HomeContract.kt      → HomeUiState, HomeEvent, HomeEffect
  ui/surah/SurahDetailContract.kt
  ui/roots/RootsContract.kt
  ui/search/SearchContract.kt
  ui/setup/SetupContract.kt
  ```
  - مثال `SurahDetailContract`:
    ```kotlin
    data class SurahDetailUiState(
        val isLoading: Boolean = true,
        val surah: Surah? = null,
        val ayat: List<Ayah> = emptyList(),
        val isLoadingMore: Boolean = false,
        val error: String? = null,
        val targetAyah: Int = 1
    )
    ```

### 4.2 تفكيك الشاشات الكبيرة
| الملف الحالي | الحجم | التفكيك إلى |
|---|---|---|
| `ui/screens/HomeScreen.kt:769` | 769 سطر | `HomeScreen.kt` (<200) + `HomeHeader.kt` + `SearchTriggerBar.kt` + `DbBanner.kt` + `QuickNavRow.kt` + `LastReadCard.kt` + `BookmarksQuickCard.kt` + `StatsGrid.kt` + `FeaturedRootsSection.kt` + `ThemeChooserDialog.kt` |
| `ui/screens/RootDetailScreen.kt:721` | 721 سطر | `RootDetailScreen.kt` (<200) + `RootHeaderCard.kt` + `RootTabs.kt` + `MeaningCard.kt` + `MasdarCard.kt` + `DerivativeCard.kt` + `AyahOccurrenceCard.kt` + `ReportIssueSection.kt` |
| `ui/screens/SurahDetailScreen.kt:382` | 382 سطر | `SurahDetailScreen.kt` (<220) + `BasmalahBanner.kt` + `AyahList.kt` (مع Paging) + `FontControls.kt` |
| `ui/screens/DatabaseSetupScreen.kt:385` | 385 سطر | `DatabaseSetupScreen.kt` + `DownloadProgressCard.kt` + `ImportDialog.kt` |
| `ui/viewmodel/ViewModels.kt:388` | 388 سطر (6 ViewModels) | فصل كل ViewModel في ملفه: `HomeViewModel.kt`, `SurahViewModel.kt`, `SurahDetailViewModel.kt`, `RootViewModel.kt`, `SearchViewModel.kt`, `SetupViewModel.kt`, `MainViewModel.kt` |

### 4.3 إصلاح MorphologyBottomSheet
- [ ] `ui/components/MorphologyBottomSheet.kt:183` حذف `remember(context){QuranRepositoryImpl(context)}` + `LaunchedEffect`
- [ ] تمرير `RootDetail` عبر `SurahDetailViewModel`:
  ```kotlin
  // ViewModel يحمل
  val selectedWordDetail: StateFlow<RootDetail?>
  fun onWordClicked(word: WordToken) { viewModelScope.launch { selectedWordDetail = getRootDetail(word.rootId) } }
  ```
- [ ] `AiSummarySection` يستقبل `aiSummary: String?` كـ param فقط

### 4.4 ViewModels → Hilt + UDF
```kotlin
@HiltViewModel
class SurahDetailViewModel @Inject constructor(
    private val getAyatPaged: GetAyatPagedUseCase,
    private val getSurah: GetSurahByIdUseCase,
    @IoDispatcher private val io: CoroutineDispatcher
) : ViewModel() {
    private val _uiState = MutableStateFlow(SurahDetailUiState())
    val uiState = _uiState.asStateFlow()
    fun onEvent(e: SurahDetailEvent) { ... }
}
```
- [ ] لا `AndroidViewModel`. لا `Application`.
- [ ] `MainViewModel` يُفصل إلى `ThemeViewModel` + `BookmarksViewModel` + `LastReadViewModel` أو يُبقى واحد لكن محقون بـ `UserPreferencesRepository` + `IsDbReadyUseCase` فقط (لا `QuranRepositoryImpl`)

### 4.5 Navigation Typed
- [ ] استبدال `ui/navigation/Screen.kt:3` بـ:
  ```kotlin
  @Serializable data object Home
  @Serializable data object SurahIndex
  @Serializable data class SurahDetail(val surahId: Int, val ayah: Int = 1)
  @Serializable data class RootDetail(val rootId: Int)
  @Serializable data object Roots
  @Serializable data object Search
  ```
- [ ] `ui/navigation/AppNavigation.kt:46` → `NavHost { composable<Home>{}; composable<SurahDetail>{ it.toRoute() } }`
- [ ] `BottomNavItem` — استبدال `iconEmoji: String` → `icon: ImageVector` + `labelRes: Int`

### 4.6 نقل منطق الفلترة من Composable إلى ViewModel
- [ ] `RootsListScreen:58` — `filteredRoots = remember(roots, query)` يُنقل إلى `RootViewModel.filteredRoots: StateFlow`
- [ ] `SurahIndexScreen:79` — `filteredSurahs` إلى `SurahViewModel`
- [ ] `SearchScreen:67-71` — `selectedTabIndex` يبقى UI، لكن `results` تأتي من `SearchViewModel.uiState: SearchUiState`

**معيار الخروج:** لا شاشة >300 سطر. لا منطق DB/Network داخل `@Composable`. كل Screen تستقبل `uiState + onEvent`.

---

## Phase 5 — مقاطع عرضية: Constants, Morphology, Theme (2-3 أيام)

- [ ] تقسيم `data/util/QuranMetaConstants.kt:277`:
  - `core/util/SurahMetadata.kt` — `SURAHS: List<SurahMeta>` (114)
  - `core/util/MorphologyMaps.kt` — `POS_MAP`, `FORMS_MAP`, `ASPECT_MAP`, `MORPHOLOGY_TERMS`
  - `core/util/QuranStats.kt` — `STATS_*`
- [ ] إنشاء `core/util/DatabaseConstants.kt` (انظر Phase 2.6)
- [ ] توحيد `UserPreferencesRepository:72-88` — إزالة ازدواج `setDynamicColorEnabled` يكتب مفتاحين. القرار: `dynamicColor: Boolean` واحد فقط أو `colorMode: Int` واحد — لا كلاهما
- [ ] نقل `ArabicNormalizer.kt:1` → `core/util/ArabicNormalizer.kt` + إضافة `normalizeAr()` + `stripDiacritics()` مع docs
- [ ] مراجعة `ui/theme/` — التأكد: لا `Color(0xFF...)` داخل Screens، كلها من `Color.kt`. `Motion.kt:15` يبقى مرجع 250ms

---

## Phase 6 — Build و Dependencies وتنظيف Firebase (1-2 يوم)

### 6.1 تنظيف libs.versions.toml
- [ ] **حذف** من `gradle/libs.versions.toml`:
  ```
  accompanistPermissions, playServicesLocation,
  cameraCamera2/Core/Lifecycle/View, coilCompose,
  firebase-ai, firebase-appcheck-recaptcha/debug
  ```
  وإبقاء فقط:
  ```
  firebase-bom + firebase-crashlytics + firebase-analytics
  ```
- [ ] حذف التعليقات الميتة `// implementation(...)` من `app/build.gradle.kts:81-139`

### 6.2 Firebase Crashlytics فقط
- [ ] `app/build.gradle.kts`:
  ```kotlin
  plugins {
      id("com.google.gms.google-services")
      id("com.google.firebase.crashlytics")
  }
  dependencies {
      implementation(platform(libs.firebase.bom))
      implementation(libs.firebase.crashlytics)
      implementation(libs.firebase.analytics)
  }
  ```
- [ ] حذف `libs.firebase.ai:103`, `firebase.appcheck.*:113-114`, `firebase.firestore:105-106`, `firebase.auth:109`, `accompanist-permissions:77`
- [ ] حذف `secrets` plugin إن لم تعد هناك مفاتيح (أو الإبقاء لاستخدام مستقبلي)
- [ ] إضافة `google-services.json` إلى `.gitignore` + وضع `google-services.json.example`

### 6.3 تحسينات Build
- [ ] `gradle.properties` — التأكد: `org.gradle.caching=true`, `configuration-cache=true`, `android.nonTransitiveRClass=true`
- [ ] `app/build.gradle.kts` — إضافة:
  ```kotlin
  android {
      buildFeatures { buildConfig = true }
      room { schemaDirectory("$projectDir/schemas") }
  }
  ksp { arg("room.schemaLocation", "$projectDir/schemas") }
  ```
- [ ] تفعيل `isMinifyEnabled = true` + `proguard-rules.pro` للـ release
- [ ] Room schema export: `app/schemas/io.github.ahmedsaadi0.quranwords.data.local.db.QuranDatabase/1.json`

---

## Phase 7 — الاختبارات والجودة (2-3 أيام)

- [ ] **Unit — domain/usecase:**
  - `SearchUseCaseTest` — (blank → empty, success, error, normalization)
  - `GetRootDetailUseCaseTest`
  - `GetAyatPagedUseCaseTest`
  - كل UseCase: `Success` + `Error` + `Empty` + `Normalized`
- [ ] **Unit — core:**
  - `ArabicNormalizerTest` — حالات التشكيل والهمزات
- [ ] **Local — data:**
  - `QuranDatabaseTest` — Robolectric + inMemory Room — اختبار DAO: `SurahDao`, `AyahDao`, `WordDao`
- [ ] **UI — Compose:**
  - `AyahItemCardTest` — عرض كلمات + testTag `word_${id}`
  - `RootItemCardTest`
  - `SearchScreenTest` — إدخال query → عرض نتائج
- [ ] **Screenshot — Roborazzi:**
  - `HomeScreenScreenshotTest`, `SurahDetailScreenScreenshotTest`
- [ ] **CI — `.github/workflows/ci.yml`:**
  ```yaml
  - run: ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest
  - run: ./gradlew connectedDebugAndroidTest # على emulator
  ```
- [ ] إضافة `testTag` موحّد في كل Components (موجود جزئياً لكن يحتاج توحيد `feature_element_id`)

**معيار الخروج:** `testDebugUnitTest` أخضر، كل UseCase مغطى.

---

## ترتيب التنفيذ الموصى به

```
Phase 0 (تحضير)
  → Phase 1 (حزمة + Hilt)          ← لا تبدأ 2 قبل 1
    → Phase 2 (Data)               ← الأساس لنقل المنطق
      → Phase 3 (UseCases)
        → Phase 4 (UI)             ← يعتمد على UseCases + Data
          → Phase 5 (Constants)    ← يمكن بالتوازي مع 4
            → Phase 6 (Build)      ← يمكن مبكراً لكن بعد 1
              → Phase 7 (Tests)    ← بعد كل شيء
```

**الـ Critical Path:** `0 → 1 → 2 → 3 → 4 → 7` — حوالي **18-25 يوم عمل** لفرد واحد، أو **10-14 يوم** لفريق 2.

---

## ملفات متأثرة — قائمة سريعة

| ملف | Phases |
|---|---|
| `app/build.gradle.kts` | 1, 6 |
| `gradle/libs.versions.toml` | 1, 6 |
| `app/src/main/AndroidManifest.xml` | 1 |
| `app/src/main/java/com/example/MainActivity.kt` | 1, 4 |
| `data/local/QuranDatabase.kt` | 2 |
| `data/local/dao/Daos.kt` | 2, 7 |
| `data/local/entities/Entities.kt` | 2 |
| `data/repository/QuranRepositoryImpl.kt` (815 سطر) | 2, 3 |
| `data/repository/UserPreferencesRepository.kt:16` | 1, 2, 5 |
| `data/remote/DatabaseDownloadManager.kt` (256 سطر) | 2 |
| `data/util/QuranMetaConstants.kt:277` | 5 |
| `data/util/ArabicNormalizer.kt` | 5 |
| `domain/repository/QuranRepository.kt:21` | 2, 3 |
| `domain/model/QuranModels.kt` | 3 |
| `ui/viewmodel/ViewModels.kt` (388 سطر, 6 ViewModels) | 1, 3, 4 |
| `ui/components/MorphologyBottomSheet.kt:183` | 4 |
| `ui/screens/HomeScreen.kt:769` | 4 |
| `ui/screens/RootDetailScreen.kt:721` | 4 |
| `ui/screens/SurahDetailScreen.kt:382` | 4 |
| `ui/screens/DatabaseSetupScreen.kt:385` | 2, 4 |
| `ui/navigation/Screen.kt:17` + `AppNavigation.kt:280` | 4 |
| `ui/theme/*` | 5 |

---

## أوامر سريعة للتحقق بعد كل Phase

```bash
# جودة
./gradlew ktlintCheck detekt lintDebug

# اختبارات
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest

# Screenshots
./gradlew recordRoborazziDebug
./gradlew compareRoborazziDebug

# بناء
./gradlew assembleDebug
./gradlew assembleRelease
```

---

## ملاحظات للـ Agent المنفّذ

1. **لا تقفز بين Phases** — كل Phase لها معيار خروج. لا تبدأ التالية قبل تحقيقه.
2. **لا تنشئ كود بـ `com.example`** — أي ملف جديد يجب أن يبدأ بـ `io.github.ahmedsaadi0.quranwords`.
3. **لا تضف Seed Data جديدة** — المشروع يعرض شاشة تنزيل فقط عند عدم وجود DB.
4. **لا تضف Firebase جديد** — Crashlytics فقط. أي خدمة جديدة تتطلب ADR.
5. **شغّل `ktlintCheck` قبل كل Commit.**
6. عند الشك: افتح `AGENTS.md` — هو الحقيقة الوحيدة.

---

---

## Discovery Summary (Initial Generation — 2026-08-29)

Archived per `AGENTS.md:22 / AGENTS.base.md:309` — required gate before generating project-specific `AGENTS.md`.

```
Product:            Quran Words — offline Arabic linguistic dictionary (114 surahs / 6236 ayat / 77k word positions)
Business Model:     Offline reference utility, B2C free, no auth/billing/subscription/ads — freemium N/A
                    Distribution: Google Play + GitHub Releases (APK) + direct DB download (media.githubusercontent.com)
Users/Roles:        Single anonymous offline user. No registration/login/admin/moderator/tenancy. Multi-tenancy explicitly out of scope (AGENTS.md §2.1)
Core Journeys:      1) First launch DB missing → Download/import DB → Home
                    2) Browse Surahs by Surah/Juz → SurahDetail paged 20 → Tap word → Morphology sheet → RootDetail
                    3) Search (Arabic-normalized) → Tabs Roots/Masadir/Derivatives/Ayat → Open result
                    4) RootDetail Tabs Meanings/Masadir/Derivatives/AyatOccurrences paged 30 → Tap ayah → SurahDetail at exact ayah
                    5) Bookmark surah/ayah → Bookmarks → Resume last-read
Platform:           Android only, Min 24 Target 36, Compose Material3, single :app module, existing repository
Backend/Data:       No REST API. Data source is prepackaged SQLite quran_words.db 118MB (11 tables), downloaded from AhmedSaadi0/quran-words raw
                    Room is single source of truth after install; 6236 ayat immutable
Connectivity:       OFFLINE_FIRST (Base §14). Everything works offline after DB. What needs internet: DB download/import only. No sync, no conflict, no retry queue
Auth:               None. No tokens/sessions/OAuth/OTP/biometrics. No roles/permissions (AGENTS.md §2.1)
Multi-Tenancy:      None
Security/Privacy:   No sensitive data — Quran text is public. No PII/financial/health/location/auth tokens. No secure storage beyond DataStore for prefs/bookmarks. INTERNET permission only. dataExtractionRules must exclude DB (AGENTS.md §17.2)
Localization:       Arabic primary (RTL required, full), English secondary (surah names + transliteration). No other locales. Font scaling via DataStore. Accessibility for Arabic readers (AGENTS.md §10.5)
State Management:   UDF + StateFlow UiState/Event/Effect per feature; ViewModel → UseCase → Repository
Architecture:       Medium app → Clean 3-layer (Presentation/UI → Domain → Data) + Core. Justified vs 2-layer (would leak Room into Composables) and vs full multi-module (deferred until team>3). Hilt DI justified for 6+ ViewModels (AGENTS.md §3/6)
Testing:            JUnit + Turbine + Mockk (UseCases/ViewModels), Robolectric+Room inMemory (DAOs), Compose Test Rule (components), Roborazzi screenshots
                    Required tags: <feature>_<element>_<id> per Base §25.3. Integration only for critical Download→Browse journey (Base §25.4)
Performance/Scale:  286 ayat/surah max, 854 occurrences/root max → Paging3 20/30 required. No heavy media/realtime. Startup <2s. No premature optimization (Base §21)
Observability:      Firebase Crashlytics only (+ Analytics). No AI/AppCheck/Auth/Firestore. No analytics without product requirement (Base §30)
Constraints/Debt:   Current package com.example mismatched, no DI (manual new QuranRepositoryImpl in 6 ViewModels), SQLite raw + Room dead code + Seed data inside repository, God screens >400 lines, string routes, Context.dataStore global, silent catch+emptyList, commented dead deps (coil/camera/etc). All documented as tech debt to be fixed via this plan
Open Questions:     None after 2026-08-29 decisions: package io.github.ahmedsaadi0.quranwords, Hilt confirmed, seed removed (download screen only), Crashlytics only
Assumptions:        None pending — all critical discovery questions answered and confirmed on 2026-08-29 before AGENTS.md was generated
Confirmation:       Developer confirmed Discovery Summary on 2026-08-29 and authorized AGENTS.md generation (AGENTS.base.md:335 gate passed)
Date:               2026-08-29
Base:               AGENTS.base.md (Android Project Engineering Guide)
```

> This summary is the single source of truth for the initial discovery. Any change to product/business/connectivity/auth assumptions requires an ADR in `AGENTS.md §20 Decision Log`.

<p align="center"><sub>صُنع بعناية لخدمة كتاب الله — كود نظيف لمعجم نظيف. Base: AGENTS.base.md</sub></p>
