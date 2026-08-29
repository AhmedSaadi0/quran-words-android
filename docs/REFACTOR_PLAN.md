# REFACTOR_PLAN — خطة إعادة الهيكلة الشاملة

> **المرجع الملزم:** `AGENTS.md` يصف التطبيق المثالي. هذا الملف يصف **كيف نصل إليه** من الكود الحالي.
> **المرجع الأساس:** `AGENTS.base.md` (Android Project Engineering Guide) — أي تعارض يُحل بالترتيب: (1) متطلب منتج صريح، (2) `AGENTS.md`، (3) الـ base. لا تغيير معماري صامت.
> **الحزمة النهائية المعتمدة:** `io.github.ahmedsaadi0.quranwords` (`AGENTS.md §5`)
> **المعمارية المعتمدة:** Clean 3-layer (Presentation/UI → Domain → Data) + Core — مبررة في `AGENTS.md §3` (medium app 77k مواضع، Base §5.1)
> **نموذج الاتصال:** `OFFLINE_FIRST` (`AGENTS.md §8.2`, Base §14) — كل شيء offline بعد تثبيت DB، الشبكة فقط لتنزيل الملف
> **DI:** Hilt (`AGENTS.md §6`) — **Seed Preview:** محذوف — **Firebase:** Crashlytics فقط (`AGENTS.md §6 / §17.2`)

---

## ملخص الفجوة الحالية (Gap Analysis) — مقابل `AGENTS.md` الجديد + `AGENTS.base.md`

| المجال | الحالة الحالية | المطلوب في `AGENTS.md` الجديد | المرجع | الخطورة |
|---|---|---|---|---|
| **الحزمة** | `com.example` / `com.aistudio.quranwords.wkzq` | `io.github.ahmedsaadi0.quranwords` موحّدة | §5 | 🔴 عالية |
| **نموذج العمل والأدوار** | لا توثيق صريح | `§2 Business & Domain Rules` — B2C free، مستخدم وحيد anonymous، لا أدوار/tenancy، كيانات 114/6236/77429 | Base §4.2-4.3 | 🟡 متوسطة |
| **المعمارية وقرارها** | لا تبرير — طبقات مختلطة | `§3 Architecture Decision` — Why + dependency dir + state (UDF) + module strategy (single :app) | Base §5.2 | 🔴 عالية |
| **DI** | لا DI — كل ViewModel ينشئ `QuranRepositoryImpl(context)` | Hilt Modules + حقن + Dispatchers محقونة | §6 / Base §6.2/§22 | 🔴 عالية |
| **Data** | `SQLiteDatabase.openDatabase` خام + `Room` Dead Code + Seed داخل Repo + لا OFFLINE_FIRST موثّق | Room فقط + DataSource + لا Seed + `OFFLINE_FIRST` + Prepackaged lifecycle + لا sync (Base §14-15) | §8 | 🔴 عالية |
| **Domain / UseCase** | لا UseCases، أو فكرة `UseCase لكل فعل` دون شرط | `§9` — UseCase فقط عند قيمة ملموسة (validation/normalization/multi-repo) per Base §11 | §9 / Base §11 | 🟡 متوسطة |
| **UI / Design System** | شاشات God 400-770 سطر، لا UiState، منطق داخل Composable، ألوان هارده | `§10` — Contract + تفكيك (<300 سطر، split by responsibility Base §28) + Hoisted + Design Tokens + a11y/RTL | §10 / Base §17-18 | 🔴 عالية |
| **Navigation** | نصوص `route = "surah_detail/{surahId}"` | Typed `@Serializable` routes + centralized NavGraph + لا منطق تنقل في components | §11 / Base §16 | 🟡 متوسطة |
| **Preferences** | `Context.dataStore` عام + ازدواج مفاتيح | `core/datastore` + Keys مركزية + مفاتيح منفصلة | §12 / Base §6.4 | 🟡 متوسطة |
| **Error Handling** | `catch{ fallback }` + `emit(emptyList())` + `Result.Loading` | `§13` — `Result` هو `Success|Error` فقط، `Loading` في `UiState` (Base §12.2) + mapping + Crashlytics | §13 / Base §12 | 🔴 عالية |
| **Build / Deps** | تعليقات deps ميتة، `firebase-ai/appcheck` بلا استخدام | تنظيف `libs.versions.toml` per Base §24 (stdlib أولاً) + Crashlytics فقط | §6/§16 | 🟢 منخفضة |
| **Performance** | `delay(300)` يدوي، لا paging | `§17.1` — Paging3 20/30 + لا main-thread DB + لا تحسين مبكر | §17.1 / Base §21 | 🟡 متوسطة |
| **Security & Privacy** | لا توثيق، `INTERNET` فقط ضمني | `§17.2` — Sensitive data = none، لا secrets، INTERNET فقط، `dataExtractionRules` تستثني DB، لا PII في logs | §17.2 / Base §20/30 | 🟡 متوسطة |
| **Coroutines / Background** | `Dispatchers.IO` هارده، خلط scopes | `§18` — `viewModelScope` + `DispatcherModule` حقن + لا WorkManager (لا عمل deferrable) | §18 / Base §22-23 | 🟡 متوسطة |
| **Testing** | لا اختبارات UseCase/DAO، `testTag` غير موحّد | `§15` — JUnit+Turbine / Robolectric / Compose Rule + `testTag` `<feature>_<element>_<id>` (Base §25.3) + E2E فقط للحرج (Base §25.4) | §15 | 🟡 متوسطة |
| **Quality / CI** | لا CI، لا ktlint/detekt موضّح | `§16` — `ktlintCheck + detekt + lintDebug + testDebugUnitTest` في CI، لا ادعاء بدون تشغيل (Base §27) | §16 / Base §26-27 | 🟡 متوسطة |
| **Agent Workflow** | جدول Do/Don't فقط | `§19` — 7 مراحل (Understand→Discover→Decide→Generate→Verify→Report→Iterate) | §19 / Base §33 | 🟡 متوسطة |
| **Definition of Done** | غير موجود | `§21` — checklist Code/Errors/Security/UI/Testing/Quality per Base §36 | §21 | 🟡 متوسطة |
| **Decision Log** | غير موجود | `§20` — سجل 6 قرارات بصيغة Decision/Context/Options/Chosen/Reason/Trade-offs/Date | §20 / Base §37 | 🟢 منخفضة |

> **الخلاصة:** الانتقال من 339 سطر إلى ~560 سطر في `AGENTS.md` الجديد أضاف المتطلبات الحاكمة للـ base دون تغيير القرارات الجوهرية (الحزمة/Hilt/OFFLINE_FIRST/Crashlytics).

---

## ما تم تنفيذه فعلياً حتى 2026-08-29 — قبل الانتقال للواجهة

> هذه المرحلة تسبق `Phase 0` الرسمية — نفذت كإصلاحات سريعة مطلوبة قبل إعادة الكتابة الكاملة.

- [x] **الحزمة والإصدار (Phase 1.1):** `namespace`/`applicationId` → `io.github.ahmedsaadi0.quranwords`, `versionCode 2 / versionName 0.1.1`, نقل 33 ملف، تحديث 193 استيراد، حذف `com/` القديم، تحديث `README.md`.
- [x] **Hilt البنية التحتية (Phase 1.3):** `AGENTS.md §6` — `hilt 2.59.2` (ترقية من 2.51.1 التي فشلت مع AGP 9.1.1 `Android BaseExtension not found` per dagger#4944), `ksp 2.3.6`, `kotlin.android` plugin مضاف، ترتيب plugins صحيح `android → kotlin.android → kotlin.compose → ksp → hilt`, `App @HiltAndroidApp`, `MainActivity @AndroidEntryPoint`, `core/di/*` 5 Modules, `core/util/Result` + `DatabaseConstants`, 7 ViewModels → `@HiltViewModel`, كل Screens → `hiltViewModel()`. **إصلاح الخطأ:** `Failed to apply plugin 'com.google.dagger.hilt.android' > Android BaseExtension not found` — السبب AGP 9 يزيل `BaseExtension`، الحل ترقية Hilt إلى 2.59.2+ و KSP إلى 2.3.6+ وإضافة `kotlin.android` (skill `agp-9-upgrade`).
- [x] **إصلاحات سريعة للواجهة (قبل الخطة):** `SurahDetail` pagination لا يعود للأول (`hasHandledInitialScroll` + `viewModel.paging`), `RootDetail` pagination 30/صفحة مع حفظ scroll كل تاب (`meaningsState/masadirState/...`) + إزالة `LIMIT 100` → `LIMIT 30 OFFSET` + `COUNT(*)`، انتقال `surah:ayah` مباشر من تبويب الآيات (`onNavigateToSurahDetail(surahId, ayahNum)`).
- [x] **Data جزئي (Phase 2):** `core/datastore/PreferencesKeys` + `core/util/SurahMetadata/MorphologyMaps/QuranStats` مقسمة من `QuranMetaConstants`, `QuranDatabase exportSchema=true` + `ksp room.schemaLocation`, حذف `getSeed*` و fallbacks في `QuranRepositoryImpl` (الآن ترجع `emptyList`/`null` وتظهر شاشة التنزيل).
- [x] **Domain جزئي (Phase 3):** 7 UseCases في `domain/usecase/` (`SearchUseCase` مع `ArabicNormalizer` في `core/util`, `GetRootDetail`, `GetAyatPaged`, etc.) — مبررة فقط per `§9` / Base §11, `Result<T>` بدون `Loading`.
- [x] **Build جزئي (Phase 6):** تنظيف `libs.versions.toml` (حذف `coil/retrofit/camera/accompanist` و `firebase-ai/appcheck` — بقي `crashlytics/analytics` فقط), حذف تعليقات `// implementation` الميتة في `app/build.gradle.kts`, إضافة `room.schemaLocation`.
- [ ] **المتبقي قبل الواجهة:** تفعيل `ktlint/detekt` (`Phase 0.2`), إكمال `Result` في `QuranRepository` (حالياً UseCases تلتف حول repo الذي لا يزال `List`), وتحويل `QuranRepositoryImpl` بالكامل من `SQLiteDatabase` الخام إلى DAOs + `Mapper` (Phase 2.4-2.5).

**حالة البناء:** `gradle help --no-daemon` الآن ينجح (exit 0) بعد إصلاح Hilt. `assembleDebug` يحتاج تنزيل Hilt 2.59.2/Paging لأول مرة — سيتم عبر Android Studio Sync القادم. لا يوجد `BaseExtension not found` بعد الترقية.


---

## Phase 0 — التحضير والأساس (1-2 يوم) — `AGENTS.md §19.1 Step 1-2` / Base §33 Phase 1-2

**الهدف:** تثبيت أدوات الجودة واختبارات توصيف قبل أي هدم + إقرار Discovery.

- [ ] **0.1** إنشاء `branch: refactor/clean-architecture` من `main`
- [ ] **0.2** إضافة `ktlint` و `detekt` إلى `build.gradle.kts` وتشغيل `./gradlew ktlintCheck` — تثبيت baseline
  - ملفات: `build.gradle.kts`, `gradle/libs.versions.toml`, `.editorconfig` (official)
- [ ] **0.3** إضافة `.editorconfig` (official Kotlin style) + تفعيل `android.nonTransitiveRClass=true`, `caching=true` (`AGENTS.md §16`)
- [ ] **0.4** كتابة اختبارات توصيف (Characterization Tests) قبل الحذف:
  - [ ] `ArabicNormalizerTest` — `stripDiacritics` + `normalizeAr` (حالات: "ٱ", "أ", "ى", "ة") — `AGENTS.md §15`
  - [ ] `QuranMetaConstantsTest` — عدد السور 114، JUZ_LIST size 30
  - [ ] `SearchViewModel` smoke test (يُحذف لاحقاً بعد نقل المنطق لـ `SearchUseCase` per `§9`)
- [ ] **0.5** مراجعة `Discovery Summary` في نهاية هذا الملف مع المالك وتأكيد `AGENTS.base.md:335 gate` — لا توليد `AGENTS.md` بدون تأكيد
- [ ] **0.6** إنشاء `Decision Log` فارغ في `AGENTS.md §20` بستة قرارات أولية (Package/Room/Paging/Hilt/Crashlytics/coil)

**معيار الخروج:** `ktlintCheck` يمر، والاختبارات الثلاثة خضراء، والـ Discovery مُقرّ. لا يُعتبر `§21 Definition of Done` مكتملاً إلا بعد هذه الخطوة.

---

## Phase 1 — الحزمة والهيكل و Hilt (2-3 أيام) 🔴 حرج — `AGENTS.md §5 / §6`

**الهدف:** توحيد الهوية وحقن الاعتمادات — أساس كل Phases.

### 1.1 تغيير الحزمة — `§5`
- [ ] `app/build.gradle.kts:13` `namespace = "io.github.ahmedsaadi0.quranwords"`
- [x] `app/build.gradle.kts:17` `applicationId = "io.github.ahmedsaadi0.quranwords"`
- [x] `app/src/main/AndroidManifest.xml:8` `package` + `android:label` (أضيف `android:name=".App"`)
- [x] نقل المجلد: `app/src/main/java/com/example` → `app/src/main/java/io/github/ahmedsaadi0/quranwords` (33 ملف، 193 استيراد)
- [x] تحديث كل `package com.example...` → `package io.github.ahmedsaadi0.quranwords...`
  - بحث: `grep -r "com.example" app/src`
- [x] نقل `app/src/test/java/com/example` و `androidTest` بنفس الطريقة
- [x] تحديث `README.md:56` مسار DB الداخلي إلى `io.github.ahmedsaadi0.quranwords` إلى `/data/data/io.github.ahmedsaadi0.quranwords/databases/quran_words.db`

### 1.2 إعادة هيكلة المجلدات — `§7`
```
io/github/ahmedsaadi0/quranwords/
├── App.kt                      # جديد @HiltAndroidApp (§6)
├── MainActivity.kt             # @AndroidEntryPoint — RTL provider واحد (§10.5)
├── core/di/                    # جديد (§6)
├── core/datastore/             # نقل من data/repository (§12)
├── core/util/                  # نقل ArabicNormalizer + Result + Constants (§14)
├── core/navigation/            # نقل AppNavigation (§11)
├── core/theme/                 # نقل ui/theme (§10.4)
├── data/local/db/              # (§8.1)
├── data/local/datasource/      # (§8.4)
├── data/remote/datasource/     # (§8.4)
├── domain/model/               # (§9)
├── domain/repository/          # (§9)
├── domain/usecase/             # (§9 — فقط عند قيمة)
└── ui/<feature>/               # تفكيك ui/screens + ui/viewmodel (§10)
```

### 1.3 إضافة Hilt — `§6` (Base §6.2)
- [x] `gradle/libs.versions.toml` — إضافة `hilt = "2.59.2"` (كان 2.51.1 غير متوافق مع AGP 9.1.1 — تمت الترقية per https://github.com/google/dagger/issues/4944) + `ksp 2.3.6` + `paging 3.3.6`
- [x] `app/build.gradle.kts` — `plugins { id("dagger.hilt.android.plugin") }` + `ksp(libs.hilt.compiler)` + ترتيب `android.application → kotlin.android → kotlin.compose → ksp → hilt`
- [ ] `build.gradle.kts` — `alias(libs.plugins.hilt) apply false`
- [x] إنشاء `App.kt`:
  ```kotlin
  @HiltAndroidApp class App: Application()
  ``` ( + `AndroidManifest android:name=".App"` + `MainActivity @AndroidEntryPoint`)
- [x] `MainActivity.kt:24` → `@AndroidEntryPoint class MainActivity`
- [ ] إنشاء Modules:
  - [x] `core/di/DatabaseModule.kt` — يوفر `QuranDatabase` via `createFromFile` (§8.1) + DAOs عبر `createFromFile` (§8.1)
  - [x] `core/di/RepositoryModule.kt` — يربط `QuranRepository` → `QuranRepositoryImpl` + `DatabaseDownloadDataSource`
  - [x] `core/di/NetworkModule.kt` — يوفر `OkHttpClient`
  - [x] `core/di/DispatcherModule.kt` — يوفر `@IoDispatcher`, `@MainDispatcher`, `@DefaultDispatcher` (§18 / Base §22)
  - [x] `core/di/PreferencesModule.kt` — يوفر `UserPreferencesRepository` + `PreferencesKeys`
- [x] تحويل كل ViewModel من `AndroidViewModel` → `@HiltViewModel class X @Inject constructor(...) : ViewModel()` (7 ViewModels) + كل `Screen` من `viewModel()` → `hiltViewModel()` (§10.2)
  - ملفات: `ui/viewmodel/ViewModels.kt` (كل الـ 6 ViewModels) — لا `Application` يُمرر للـ Repository (Base §8.1)

**معيار الخروج:** لا يوجد `QuranRepositoryImpl(context)` في أي ViewModel. التطبيق يبني ويعمل مع Hilt. كل الاستيرادات تستخدم `io.github.ahmedsaadi0.quranwords`. §21 Code (لا unrelated files) يتحقق.

---

## Phase 2 — طبقة Data: Room المصدر الوحيد + حذف Seed (5-7 أيام) 🔴 حرج — `AGENTS.md §8` / Base §13-15

**الهدف:** إزالة `SQLiteDatabase` الخام و Seed، وتثبيت `OFFLINE_FIRST` وتوحيد الوصول عبر Room + DataSource.

### 2.1 نقل DataStore — `§12` (Base §6.4)
- [ ] إنشاء `core/datastore/PreferencesDataStore.kt`:
  ```kotlin
  val Context.dataStore by preferencesDataStore(name = "quran_words_prefs")
  ```
- [ ] حذف `val Context.dataStore` من `data/repository/UserPreferencesRepository.kt:16` (كان يخالف `§12`: لا extension عام في ملف Repository)
- [ ] إنشاء `core/datastore/PreferencesKeys.kt` — تجميع `KEY_FONT_SIZE`, `KEY_DARK_MODE`, `KEY_DYNAMIC_COLOR`, `KEY_LAST_SURAH`...
- [ ] فصل `KEY_DARK_MODE` و `KEY_COLOR_MODE` — لا كتابة مزدوجة في `setDynamicColorEnabled():76` و `setColorMode():83` (§12)

### 2.2 فصل DataSources — `§8.4` (Base §13.1 Single Source of Truth)
- [ ] إنشاء واجهات:
  - `data/local/datasource/SurahLocalDataSource`
  - `data/local/datasource/AyahLocalDataSource`
  - `data/local/datasource/WordLocalDataSource`
  - `data/local/datasource/RootLocalDataSource` (يضم masadir/derivatives/meanings/gloss/aiSummary)
- [ ] تنفيذات `*LocalDataSourceImpl` فوق `Dao` مباشرة (لا `Context`) — §8.1 `DAO only`
- [ ] توثيق `OFFLINE_FIRST` في كل DataSource header:
  - `What works offline: everything` / `What needs internet: DB download/import only` / `Sync: none` (§8.2)
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

### 2.3 إصلاح QuranDatabase — `§8.1/8.3` (Base §15 Large prepackaged DB)
- [ ] `data/local/QuranDatabase.kt:25` → `exportSchema = true` + إضافة `schemas/` في `app/build.gradle.kts`:
  ```kotlin
  ksp { arg("room.schemaLocation", "$projectDir/schemas") }
  ```
- [ ] `DatabaseModule` يبني DB عبر `createFromFile` (§8.1):
  ```kotlin
  Room.databaseBuilder(ctx, QuranDatabase::class.java, "quran_words.db")
      .createFromFile(ctx.getDatabasePath("quran_words.db"))
      .fallbackToDestructiveMigration(false)
      .build()
  ```
- [ ] توثيق `§8.3 Prepackaged Lifecycle`: Install via `createFromFile` → File replacement `close()+delete wal/shm→renameTo` → Upgrades destructive → Backup never (`§17.2`)
- [ ] حذف `QuranDatabase.getInstance(context):54` الـ Singleton اليدوي و `closeIfNeeded():68` — تُدار عبر Hilt `@Singleton`
- [ ] حذف `sqliteDb: SQLiteDatabase? :33` و `getDb():35` و `closeDb():58` من `QuranRepositoryImpl` — يخالف §8.1

### 2.4 إعادة كتابة QuranRepositoryImpl — `§8.5` / Base §13.3
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
- [ ] تغيير كل دالة لتُرجع `Result<T>` **بدون `Loading`** (§13 / Base §12.2):
  ```kotlin
  override suspend fun getRootsPaged(limit: Int, offset: Int): Result<List<RootItem>>
  override suspend fun getRootDetail(rootId: Int): Result<RootDetail>
  override suspend fun searchAll(query: String): Result<SearchResult>
  ```
  - `Loading` يبقى في `UiState.isLoading` فقط.
- [ ] تحويل `loadWordsForAyah():228` (72 سطر rawQuery + JOIN) إلى استدعاءات DAO متعددة + تجميع في `data/mapper` — لا تسريب Entity للـ UI (Base §13.2)
- [ ] إضافة `core/util/Result.kt`:
  ```kotlin
  sealed interface Result<out T> {
      data class Success<T>(val data: T): Result<T>
      data class Error(val message: String, val cause: Throwable? = null): Result<Nothing>
  }
  ```
- [ ] كل `catch(e: Exception)` → `Result.Error` + `Log` + `FirebaseCrashlytics.recordException(e)` (§13)

### 2.5 Paging 3 — `§17.1` / Base §21
- [ ] إضافة `androidx.paging:paging-runtime-ktx` و `paging-compose`
- [ ] إنشاء `AyahPagingSource` و `RootOccurrencePagingSource` (20 / 30 per page) — لا تحميل كامل 286 آية أو 854 موضع
- [ ] تحويل `getAyatBySurahPaged:192` و `getRootsPaged:332` إلى `Pager` — حذف `delay(300)` و `delay(80)` اليدوي في `SurahDetailViewModel:226,251` (كان workaround قبل Paging)

### 2.6 ثوابت DB — `§14` / Base §29
- [ ] إنشاء `core/util/DatabaseConstants.kt`:
  ```kotlin
  const val DB_NAME = "quran_words.db"
  const val DB_EXPECTED_SIZE = 118_534_144L
  const val DB_MIN_SIZE = 50_000_000L
  const val DB_LFS_POINTER_LIMIT = 10_000_000L
  ```
- [ ] استبدال كل `118_534_144L` و `50_000_000L` و `10_000_000L` المبعثرة في `QuranRepositoryImpl:37` و `DatabaseDownloadManager:60,85,125,161`

**معيار الخروج:** `QuranRepositoryImpl` <250 سطر، لا `android.database.sqlite.*`، لا `getSeed*`، كل الدوال تُرجع `Result`، لا `Result.Loading`. التطبيق عند `!isDatabaseReady()` يعرض `DatabaseSetupScreen` فقط. `§21 Errors & State` + `§8.2 OFFLINE_FIRST` موثّقان.

---

## Phase 3 — طبقة Domain: UseCases (2-3 أيام) — `AGENTS.md §9` / Base §11

**المبدأ الجديد:** لا تنشئ UseCase تافه `UseCase → repo.getX()` بدون قيمة (Base §11). أنشئه فقط عند: validation / Arabic normalization / multi-repo / reusable behavior / testable logic.

- [ ] إنشاء `domain/usecase/` — كل ملف UseCase واحد **مبرر**:
  - [ ] `SearchUseCase` — **مبرر** (يُنقّي `ArabicNormalizer.normalizeAr` + `isBlank` check) ✅
  - [ ] `GetRootDetailUseCase` — **مبرر** (يُغلف 5 مصادر + mapping) ✅
  - [ ] `GetAyatPagedUseCase` — **مبرر** (يُنسق Paging + offline check) ✅
  - [ ] `GetAyahWithWordsUseCase` — **مبرر** (يجمع Word+Morphology+Root) ✅
  - [ ] `ToggleBookmarkUseCase` — **مبرر** (يعزل DataStore write) ✅
  - [ ] `GetSurahsUseCase` / `GetSurahByIdUseCase` — **قد يبقى مباشر** via repo إذا لا منطق — وثّق الاستثناء في §9 بدل إنشاء trivial UseCase
- [ ] كل UseCase محقون بـ `QuranRepository` + `ArabicNormalizer` (إن لزم)، pure Kotlin بلا Android deps (§9 / Base §8.2)
- [ ] لا منطق تحويل Entity→Domain داخل UseCase — التحويل في `data/mapper` (Base §13.2)

**معيار الخروج:** ViewModel لا يستدعي `repo` مباشرة إلا للـ trivial الموثّق. كل UseCase مبرر له اختبار `Success|Error|Empty` (`§15`).

---

## Phase 4 — طبقة UI: UDF + تفكيك الشاشات (7-10 أيام) 🔴 الأكبر — `AGENTS.md §10-11` / Base §17-18

### 4.1 قاعدة Contract — `§10.1` (Base §10.1-10.3)
- [ ] إنشاء لكل Feature **له حالة معقّدة**:
  ```
  ui/home/HomeContract.kt      → HomeUiState, HomeEvent, HomeEffect
  ui/surah/SurahDetailContract.kt
  ui/roots/RootsContract.kt
  ui/search/SearchContract.kt
  ui/setup/SetupContract.kt
  ```
  - لشاشة تافهة (callback واحد) لا تُنشئ Event hierarchy (Base §10.2).
  - Effects لمرة واحدة لا تُخزن كـ State (Base §10.3).
  - `Loading` في `UiState.isLoading` فقط (Base §12.2 / §10.1).
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

### 4.2 تفكيك الشاشات الكبيرة — `§10` / Base §28 (لا تقسيم عبثي)
| الملف الحالي | الحجم | التفكيك إلى (حسب المسؤولية، ليس الرقم فقط) |
|---|---|---|
| `ui/screens/HomeScreen.kt:769` | 769 سطر | `HomeScreen.kt` (<200) + `HomeHeader.kt` + `SearchTriggerBar.kt` + `DbBanner.kt` + `QuickNavRow.kt` + `LastReadCard.kt` + `BookmarksQuickCard.kt` + `StatsGrid.kt` + `FeaturedRootsSection.kt` + `ThemeChooserDialog.kt` |
| `ui/screens/RootDetailScreen.kt:721` | 721 سطر | `RootDetailScreen.kt` (<200) + `RootHeaderCard.kt` + `RootTabs.kt` + `MeaningCard.kt` + `MasdarCard.kt` + `DerivativeCard.kt` + `AyahOccurrenceCard.kt` + `ReportIssueSection.kt` |
| `ui/screens/SurahDetailScreen.kt:382` | 382 سطر | `SurahDetailScreen.kt` (<220) + `BasmalahBanner.kt` + `AyahList.kt` (مع Paging) + `FontControls.kt` |
| `ui/screens/DatabaseSetupScreen.kt:385` | 385 سطر | `DatabaseSetupScreen.kt` + `DownloadProgressCard.kt` + `ImportDialog.kt` |
| `ui/viewmodel/ViewModels.kt:388` | 388 سطر (6 ViewModels) | فصل كل ViewModel في ملفه: `HomeViewModel.kt`, `SurahViewModel.kt`, `SurahDetailViewModel.kt`, `RootViewModel.kt`, `SearchViewModel.kt`, `SetupViewModel.kt`, `MainViewModel.kt` |

- Target <300 سطر لكن القرار حسب `Base §28`: God file >400 سطر إشارة، ليس قانوناً مطلقاً.

### 4.3 إصلاح MorphologyBottomSheet — `§10.3` / Base §17.1 (Stateless)
- [ ] `ui/components/MorphologyBottomSheet.kt:183` حذف `remember(context){QuranRepositoryImpl(context)}` + `LaunchedEffect` — ممنوع DB في Composable
- [ ] تمرير `RootDetail` عبر `SurahDetailViewModel`:
  ```kotlin
  val selectedWordDetail: StateFlow<RootDetail?>
  fun onWordClicked(word: WordToken) { viewModelScope.launch { selectedWordDetail = getRootDetail(word.rootId) } }
  ```
- [ ] `AiSummarySection` يستقبل `aiSummary: String?` كـ param فقط

### 4.4 ViewModels → Hilt + UDF — `§10.2` / Base §22
```kotlin
@HiltViewModel
class SurahDetailViewModel @Inject constructor(
    private val getAyatPaged: GetAyatPagedUseCase,
    @IoDispatcher private val io: CoroutineDispatcher
) : ViewModel() {
    private val _uiState = MutableStateFlow(SurahDetailUiState())
    val uiState = _uiState.asStateFlow()
    fun onEvent(e: SurahDetailEvent) { ... }
}
```
- [ ] لا `AndroidViewModel`. لا `Application` يُمرر للـ Repository (Base §8.1).
- [ ] `Dispatchers` محقونة عبر `DispatcherModule` لتمكين `TestDispatcher` (Base §22). لا `GlobalScope`.
- [ ] `MainViewModel` يُفصل إلى `ThemeViewModel` + `BookmarksViewModel` + `LastReadViewModel` أو يُبقى واحد لكن محقون بـ `UserPreferencesRepository` + `IsDbReadyUseCase` فقط

### 4.5 Navigation Typed — `§11` / Base §16
- [ ] استبدال `ui/navigation/Screen.kt:3` بـ:
  ```kotlin
  @Serializable data object Home
  @Serializable data object SurahIndex
  @Serializable data class SurahDetail(val surahId: Int, val ayah: Int = 1)
  @Serializable data class RootDetail(val rootId: Int)
  ```
- [ ] `ui/navigation/AppNavigation.kt:46` → `NavHost { composable<Home>{}; composable<SurahDetail>{ it.toRoute() } }` — لا `route = "surah_detail/{surahId}"` نصي
- [ ] `BottomNavItem` — استبدال `iconEmoji: String` → `icon: ImageVector` + `labelRes: Int` (لا إيموجي تفاعلي رئيسي — `§10.4`)

### 4.6 نقل منطق الفلترة من Composable إلى ViewModel — `§10` (Base §17.1 Hoisting)
- [ ] `RootsListScreen:58` — `filteredRoots = remember(roots, query)` يُنقل إلى `RootViewModel.filteredRoots: StateFlow` (اختبار منطق البحث بـ `ArabicNormalizer`)
- [ ] `SurahIndexScreen:79` — `filteredSurahs` إلى `SurahViewModel`
- [ ] `SearchScreen:67-71` — `selectedTabIndex` يبقى UI، لكن `results` تأتي من `SearchViewModel.uiState: SearchUiState`
- [ ] إضافة `testTag` موحّد `<feature>_<element>_<id>` (Base §25.3) وأخذ `contentDescription` للـ cards — `§10.5`

**معيار الخروج:** لا شاشة >300 سطر (أو مبررة بالمسؤولية). لا منطق DB/Network داخل `@Composable`. كل Screen تستقبل `uiState + onEvent`. `§21 UI` (RTL/a11y/font scaling) يتحقق.

---

## Phase 5 — مقاطع عرضية: Constants, Morphology, Theme + A11y (2-3 أيام) — `AGENTS.md §14 / §10.4-10.5`

- [ ] تقسيم `data/util/QuranMetaConstants.kt:277`:
  - `core/util/SurahMetadata.kt` — `SURAHS: List<SurahMeta>` (114)
  - `core/util/MorphologyMaps.kt` — `POS_MAP`, `FORMS_MAP`, `ASPECT_MAP`, `MORPHOLOGY_TERMS`
  - `core/util/QuranStats.kt` — `STATS_*`
  - إضافة `core/util/DatabaseConstants.kt` (انظر Phase 2.6)
- [ ] توحيد `UserPreferencesRepository:72-88` — إزالة ازدواج `setDynamicColorEnabled` يكتب مفتاحين (§12)
- [ ] نقل `ArabicNormalizer.kt:1` → `core/util/ArabicNormalizer.kt` + docs — pure Kotlin (§14)
- [ ] مراجعة `ui/theme/` — لا `Color(0xFF...)` داخل Screens، كلها من `Color.kt` (`§10.4` Design Tokens). `Motion.kt:15` يبقى 250ms
- [ ] **A11y جولة:** إضافة `contentDescription` لكل أيقونة/زر سورة/آية، ضمان 48dp، contrast M3، اختبار fontScaling الكبير (Base §18) — `§10.5` + `§21 UI`

---

## Phase 6 — Build و Dependencies وتنظيف Firebase (1-2 يوم) — `AGENTS.md §16 / §6`

### 6.1 تنظيف `libs.versions.toml` — Base §24
قبل إضافة أي dependency: (1) stdlib؟ (2) بحث عن موجود؟ (3) صيانة/أمان؟ (4) توافق KSP/AGP/Compose؟ — `§6`
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
- [ ] حذف التعليقات الميتة `// implementation(...)` من `app/build.gradle.kts:81-139` — `§16`

### 6.2 Firebase Crashlytics فقط — `§17.2` / Base §20 least privilege
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
- [ ] حذف `secrets` plugin إن لم تعد هناك مفاتيح
- [ ] إضافة `google-services.json` إلى `.gitignore` + وضع `google-services.json.example` (§17.2 / Base §31)

### 6.3 تحسينات Build — `§16` / Base §26-27
- [ ] `gradle.properties` — `org.gradle.caching=true`, `configuration-cache=true`, `android.nonTransitiveRClass=true`
- [ ] `app/build.gradle.kts` — إضافة:
  ```kotlin
  ksp { arg("room.schemaLocation", "$projectDir/schemas") }
  ```
- [ ] تفعيل `isMinifyEnabled = true` + `proguard-rules.pro` للـ release
- [ ] Room schema export: `app/schemas/io.github.ahmedsaadi0.quranwords.data.local.db.QuranDatabase/1.json` — Base §15/§26
- [ ] لا تدّعي نجاح فحص لم يُشغّل (Base §27)

---

## Phase 7 — الاختبارات والجودة + Definition of Done (2-3 أيام) — `AGENTS.md §15 / §21`

- [ ] **Unit — domain/usecase** (`§15`، Base §25.1 — success/error/empty/boundary):
  - `SearchUseCaseTest` — (blank → empty, success, error, Arabic normalization "ٱ→ا")
  - `GetRootDetailUseCaseTest`
  - `GetAyatPagedUseCaseTest`
  - كل UseCase مبرر: 4 حالات
- [ ] **Unit — core:**
  - `ArabicNormalizerTest` — 5 حالات تشكيل
- [ ] **Local — data** (Base §25.2):
  - `QuranDatabaseTest` — Robolectric + inMemory Room — `SurahDao`, `AyahDao`, `WordDao` + migration test
- [ ] **UI — Compose** (Base §25.3):
  - `AyahItemCardTest`, `RootItemCardTest`, `SearchScreen` journey — عبر `testTag` `surah_item_2` / `word_15`
- [ ] **Screenshot — Roborazzi:**
  - `HomeScreenScreenshotTest`, `SurahDetailScreenScreenshotTest`
- [ ] **Integration — حرج فقط** (Base §25.4 — لا E2E لكل component):
  - Download → Install → Browse flow (instrumentation where critical)
- [ ] **CI — `.github/workflows/ci.yml` (Base §27):**
  ```yaml
  - run: ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest
  - run: ./gradlew assembleDebug
  - run: ./gradlew connectedDebugAndroidTest # critical only
  ```
  لا تدّعي `ci passes` بدون تشغيل فعلي.
- [ ] **Verification per §21 Definition of Done:** قبل إغلاق كل Phase، مرّر checklist الستة (Code/Errors/Security/UI/Testing/Quality) — `AGENTS.md §21`

**معيار الخروج:** `testDebugUnitTest` أخضر، كل UseCase مبرر مغطى، `§21` كامل.

---

## ترتيب التنفيذ الموصى به — `AGENTS.md §19.1`

```
Phase 0 (Understand + Discover — AGENTS.md §2-3)
  → Phase 1 (Decide: package + Hilt — §5/§6)
    → Phase 2 (Generate: Data — §8 OFFLINE_FIRST)
      → Phase 3 (Domain: UseCases مبررة — §9)
        → Phase 4 (UI: UDF + Navigation Typed — §10-11)
          → Phase 5 (Core: Constants + Theme + A11y — §14/§10.5)
            → Phase 6 (Build: deps + Crashlytics — §16/§17.2)
              → Phase 7 (Verify: tests + DoD §21 + Report per §19.1 step 6)
```

**الـ Critical Path:** `0 → 1 → 2 → 3 → 4 → 7` — حوالي **18-25 يوم عمل** لفرد واحد، أو **10-14 يوم** لفريق 2. التزم بـ `AGENTS.md §19.3` (تغييرات مركزة، diff مراجع).

---

## ملفات متأثرة — قائمة سريعة

| ملف | Phases | `AGENTS.md` |
|---|---|---|
| `app/build.gradle.kts` | 1, 6 | §5 / §6 / §16 |
| `gradle/libs.versions.toml` | 1, 6 | §6 / §16 / Base §24 |
| `app/src/main/AndroidManifest.xml` | 1 | §5 / §17.2 (backup) |
| `app/src/main/java/com/example/MainActivity.kt` | 1, 4 | §5 / §10.5 (RTL) |
| `data/local/QuranDatabase.kt` | 2 | §8.1 / §16 (schemas) |
| `data/local/dao/Daos.kt` | 2, 7 | §8.1 / §15 |
| `data/local/entities/Entities.kt` | 2 | §8.1 |
| `data/repository/QuranRepositoryImpl.kt` (815 سطر) | 2, 3 | §8.5 / §13 |
| `data/repository/UserPreferencesRepository.kt:16` | 1, 2, 5 | §12 |
| `data/remote/DatabaseDownloadManager.kt` (256 سطر) | 2 | §8.2 / §8.3 |
| `data/util/QuranMetaConstants.kt:277` | 5 | §14 |
| `data/util/ArabicNormalizer.kt` | 5 | §14 |
| `domain/repository/QuranRepository.kt:21` | 2, 3 | §9 |
| `domain/model/QuranModels.kt` | 3 | §9 |
| `ui/viewmodel/ViewModels.kt` (388 سطر, 6 ViewModels) | 1, 3, 4 | §10.2 / §18 |
| `ui/components/MorphologyBottomSheet.kt:183` | 4 | §10.3 / Base §17.1 |
| `ui/screens/HomeScreen.kt:769` | 4 | §10 / §21 |
| `ui/screens/RootDetailScreen.kt:721` | 4 | §10 |
| `ui/screens/SurahDetailScreen.kt:382` | 4 | §10 / §11 |
| `ui/screens/DatabaseSetupScreen.kt:385` | 2, 4 | §8.2 |
| `ui/navigation/Screen.kt:17` + `AppNavigation.kt:280` | 4 | §11 / Base §16 |
| `ui/theme/*` | 5 | §10.4 / Base §17.2 |
| `core/util/Result.kt` (جديد) | 2 | §13 / Base §12.2 |
| `core/util/DatabaseConstants.kt` (جديد) | 2 | §14 |

---

## أوامر سريعة للتحقق بعد كل Phase — `AGENTS.md §23` / Base §27

```bash
# جودة — لا تدّعي نجاحاً بدون تشغيل (Base §27)
./gradlew ktlintCheck detekt lintDebug

# اختبارات
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest  # للحرج فقط (Base §25.4)

# Screenshots
./gradlew recordRoborazziDebug
./gradlew compareRoborazziDebug

# بناء
./gradlew assembleDebug
./gradlew assembleRelease
# التحقق اليدوي: §21 UI (RTL/a11y/font scaling) + §17.2 (لا backup للـ DB)
```

---

## ملاحظات للـ Agent المنفّذ — `AGENTS.md §19.2-19.3` / Base §32

1. **لا تقفز بين Phases** — كل Phase لها معيار خروج. لا تبدأ التالية قبل تحقيقه. (`§19.1`)
2. **لا تنشئ كود بـ `com.example`** — أي ملف جديد يجب أن يبدأ بـ `io.github.ahmedsaadi0.quranwords`. (`§5`)
3. **لا تضف Seed Data جديدة** — المشروع يعرض شاشة تنزيل فقط عند عدم وجود DB. (`§8.5`)
4. **لا تضف Firebase جديد** — Crashlytics فقط، Analytics فقط بمتطلب منتج. أي خدمة جديدة تتطلب ADR في `§20`. (`§6`)
5. **لا تنشئ UseCase تافه** — فقط عند validation/normalization/multi-repo per `§9` / Base §11.
6. **لا تغيّر معمارية صامتاً** — وثّق في `§20 Decision Log`. (`Base §37`)
7. **شغّل `ktlintCheck` قبل كل Commit + راجع الـ diff + لا تدّعي فحصاً لم يُشغّل.** (Base §27/§32)
8. عند الشك: افتح `AGENTS.md` — هو الحقيقة الوحيدة، و `AGENTS.base.md` عند التعارض غير الموثّق.

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
