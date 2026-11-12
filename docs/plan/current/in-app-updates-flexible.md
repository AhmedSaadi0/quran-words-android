# Plan — Google Play In-App Updates (Flexible Flow)

> **Date:** 2026-10-18 · **Status:** Approved (revised plan) · **Scope:** Play-only
> **Feature:** On every app start (and every resume), check Google Play for a newer versionCode; if one exists, show a non-blocking Arabic-first banner with the flexible update prompt. When the download finishes (including while the app was backgrounded), ask the user to "Restart to complete".

---

## 1) Approved decisions

| Decision | Choice |
|---|---|
| Update mode | **Flexible** banner ("update available" → "restart to complete"), non-blocking |
| Check frequency | Every app start + fresh re-check on every `STARTED` (resume) |
| Sideload/GitHub APK users | Not covered (YAGNI); `UPDATE_UNAVAILABLE` → nothing renders |
| State flow | `Flow<AppUpdateResult>` (cold) collected under `repeatOnLifecycle(STARTED)`; banner state in `UpdateViewModel` via `StateFlow` |
| Failure policy | `Failed` banner state; no silent catches (AGENTS §13) |
| Dependency | `com.google.android.play:app-update:2.1.0` (single artifact; no `-ktx`) — confirm newest stable before merging |

## 2) Review fixes baked into this plan

1. **No Activity context in Data/VM** — Data layer holds only `AppUpdateManager` (Hilt, `@ApplicationContext`). The launch is invoked in the **composable** via its own `rememberLauncherForActivityResult`; the data layer merely fetches fresh `AppUpdateInfo`, validates, and starts by **delegating to the UI-owned launcher object**.
2. **No listener leaks** — `InstallStateUpdatedListener` registered inside a cold `callbackFlow`, unregistered in `awaitClose`. One live listener at most, per subscription.
3. **Background-completed downloads detected on resume** — collection under `repeatOnLifecycle(STARTED)` re-subscribes the cold flow → fresh `appUpdateInfo` query each resume → `installStatus() == DOWNLOADED` surfaces the restart prompt immediately.
4. **Modern API only** — `rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult())` + `AppUpdateOptions`. Fresh `AppUpdateInfo` per attempt (instances are single-use per Play docs).

**Deliberate DIP decision (Decision Log #29):** the launch contract is **owned by the UI layer** (`ui/update/AppUpdateFlowLauncher`) and **implemented by the data layer** — avoids `ui → data` imports outside DI wiring, mirrors how `domain` interfaces are implemented by `data`.

## 3) File map

```
domain/model/AppUpdateResult.kt                  new  (pure Kotlin)
domain/repository/AppUpdateRepository.kt         new  (pure Kotlin)
data/remote/datasource/PlayAppUpdateDataSource.kt new
core/di/UpdateModule.kt                          new
ui/update/AppUpdateFlowLauncher.kt               new  (UI-owned port)
ui/update/UpdateContract.kt                      new
ui/update/UpdateViewModel.kt                     new
ui/update/InAppUpdateHandler.kt                  new
ui/update/UpdateBanner.kt                        new
MainActivity.kt / AppNavigation.kt               3-line wiring
res/values*/strings.xml                          ar (default) + en
app/src/test/.../UpdateViewModelTest.kt          new
libs.versions.toml + app/build.gradle.kts        1 dependency
```

---

## 4) Code

### 4.1 Dependency

```toml
# libs.versions.toml
[versions]
play-app-update = "2.1.0"   # confirm newest stable before merging

[libraries]
play-app-update = { group = "com.google.android.play", name = "app-update", version.ref = "play-app-update" }
```

```kotlin
// app/build.gradle.kts
implementation(libs.play.app.update)
```

### 4.2 Domain (pure Kotlin — no Android/Play types)

```kotlin
// domain/model/AppUpdateResult.kt
package com.quranwords.domain.model

/** Pure domain state of an in-app update. Doubles as the failure carrier — failures are never hidden. */
sealed interface AppUpdateResult {
    data object NotAvailable : AppUpdateResult
    data object Available : AppUpdateResult
    data class InProgress(val percent: Int = 0) : AppUpdateResult
    data object Downloaded : AppUpdateResult
    data class Failed(val reason: UpdateFailureReason) : AppUpdateResult
}

enum class UpdateFailureReason { NOT_ALLOWED, CHECK_FAILED, DOWNLOAD_FAILED, UNKNOWN }
```

```kotlin
// domain/repository/AppUpdateRepository.kt
package com.quranwords.domain.repository

import com.quranwords.core.util.Result
import com.quranwords.domain.model.AppUpdateResult
import kotlinx.coroutines.flow.Flow

interface AppUpdateRepository {
    /**
     * Cold flow: every subscription performs a FRESH Play availability check and
     * then stays subscribed to install-state updates until cancellation.
     * Collected under repeatOnLifecycle(STARTED), each resume re-detects
     * InstallStatus.DOWNLOADED that happened while the app was backgrounded.
     */
    fun checkUpdateAvailability(): Flow<AppUpdateResult>

    /** Completes a downloaded flexible update (Play restarts the app). */
    suspend fun completeUpdate(): Result<Unit>
}
```

### 4.3 Data layer

```kotlin
// data/remote/datasource/PlayAppUpdateDataSource.kt
package com.quranwords.data.remote.datasource

import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.tasks.Task
import com.quranwords.core.util.Result
import com.quranwords.domain.model.AppUpdateResult
import com.quranwords.domain.model.UpdateFailureReason
import com.quranwords.domain.repository.AppUpdateRepository
import com.quranwords.ui.update.AppUpdateFlowLauncher
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
class PlayAppUpdateDataSource @Inject constructor(
    private val appUpdateManager: AppUpdateManager,
) : AppUpdateRepository, AppUpdateFlowLauncher {

    /** Fresh check + live install-state events; listener is unregistered in awaitClose (review fix #2). */
    override fun checkUpdateAvailability(): Flow<AppUpdateResult> = callbackFlow {
        val listener = InstallStateUpdatedListener { state ->
            state.toAppUpdateResultOrNull()?.let(::trySend)
        }
        appUpdateManager.registerListener(listener)

        try {
            trySend(appUpdateManager.appUpdateInfo.await().toAppUpdateResult())
        } catch (e: Exception) {
            Log.w(TAG, "Availability check failed", e)
            trySend(AppUpdateResult.Failed(UpdateFailureReason.CHECK_FAILED))
        }

        awaitClose { appUpdateManager.unregisterListener(listener) }
    }

    /** Starts Play's flexible sheet through the UI-owned launcher. No Activity/Context here (review fix #1). */
    override suspend fun startFlexibleUpdate(
        launcher: ActivityResultLauncher<IntentSenderRequest>,
    ): Boolean = try {
        // AppUpdateInfo instances are single-use: always fetch fresh for retries.
        val info = appUpdateManager.appUpdateInfo.await()
        val options = AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
        val startable = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
            info.isUpdateTypeAllowed(options)
        if (startable) appUpdateManager.startUpdateFlowForResult(info, launcher, options) else false
    } catch (e: Exception) {
        Log.w(TAG, "Start flexible update failed", e)
        false
    }

    override suspend fun completeUpdate(): Result<Unit> = try {
        appUpdateManager.completeUpdate().await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Log.w(TAG, "Complete update failed", e)
        Result.Error("complete_update_failed", e)
    }

    // ---------- mapping ----------

    private fun AppUpdateInfo.toAppUpdateResult(): AppUpdateResult {
        if (updateAvailability() == UpdateAvailability.UPDATE_NOT_AVAILABLE) {
            return AppUpdateResult.NotAvailable
        }
        return when (installStatus()) {
            InstallStatus.DOWNLOADED -> AppUpdateResult.Downloaded      // review fix #3 resume path
            InstallStatus.PENDING, InstallStatus.DOWNLOADING, InstallStatus.INSTALLING ->
                AppUpdateResult.InProgress(progressPercent(bytesDownloaded(), totalBytesToDownload()))
            InstallStatus.CANCELED -> AppUpdateResult.Available
            InstallStatus.FAILED -> AppUpdateResult.Failed(UpdateFailureReason.DOWNLOAD_FAILED)
            else -> AppUpdateResult.Available                           // fresh update / developer-triggered
        }
    }

    private fun InstallState.toAppUpdateResultOrNull(): AppUpdateResult? = when (installStatus()) {
        InstallStatus.PENDING -> AppUpdateResult.InProgress(0)
        InstallStatus.DOWNLOADING, InstallStatus.INSTALLING ->
            AppUpdateResult.InProgress(progressPercent(bytesDownloaded(), totalBytesToDownload()))
        InstallStatus.DOWNLOADED -> AppUpdateResult.Downloaded
        InstallStatus.INSTALLED -> AppUpdateResult.NotAvailable
        InstallStatus.FAILED -> AppUpdateResult.Failed(UpdateFailureReason.DOWNLOAD_FAILED)
        InstallStatus.CANCELED -> AppUpdateResult.Available
        else -> null                                                    // unknown statuses: no state change
    }

    private fun progressPercent(downloaded: Long, total: Long): Int =
        if (total > 0) ((downloaded * 100) / total).toInt().coerceIn(0, 100) else 0

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { value -> continuation.resume(value) }
        addOnFailureListener { error -> continuation.resumeWithException(error) }
    }

    private companion object {
        const val TAG = "PlayAppUpdateDS"
    }
}
```

### 4.4 DI

```kotlin
// core/di/UpdateModule.kt
package com.quranwords.core.di

import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.quranwords.data.remote.datasource.PlayAppUpdateDataSource
import com.quranwords.domain.repository.AppUpdateRepository
import com.quranwords.ui.update.AppUpdateFlowLauncher
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UpdateProvidesModule {
    @Provides
    @Singleton
    fun provideAppUpdateManager(@ApplicationContext context: Context): AppUpdateManager =
        AppUpdateManagerFactory.create(context)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class UpdateBindingsModule {
    @Binds
    @Singleton
    abstract fun bindAppUpdateRepository(impl: PlayAppUpdateDataSource): AppUpdateRepository

    @Binds
    @Singleton
    abstract fun bindAppUpdateFlowLauncher(impl: PlayAppUpdateDataSource): AppUpdateFlowLauncher
}
```

### 4.5 UI port (UI-owned contract)

```kotlin
// ui/update/AppUpdateFlowLauncher.kt
package com.quranwords.ui.update

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest

/**
 * Contract owned by the UI layer and implemented by the data layer (DIP, Decision #29):
 * the composition owns the registered ActivityResultLauncher, the data layer owns Play
 * knowledge. No Activity context crosses either side.
 */
interface AppUpdateFlowLauncher {
    /** @return true when Play's flexible update sheet was actually started. */
    suspend fun startFlexibleUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean
}
```

### 4.6 UI contract

```kotlin
// ui/update/UpdateContract.kt
package com.quranwords.ui.update

sealed interface UpdateBannerState {
    data object Hidden : UpdateBannerState
    data object Available : UpdateBannerState                 // "update available"
    data class Downloading(val percent: Int = 0) : UpdateBannerState
    data object RestartRequired : UpdateBannerState           // "restart to complete"
    data object Failed : UpdateBannerState
}

data class UpdateUiState(
    val banner: UpdateBannerState = UpdateBannerState.Hidden,
    val isDismissed: Boolean = false,                          // hides only "Available" for the session
)

sealed interface UpdateEvent {
    data object RestartApp : UpdateEvent
    data object Dismiss : UpdateEvent
    data object FlowNotStarted : UpdateEvent                  // startFlexibleUpdate() returned false
    data class UpdateSheetResult(val resultCode: Int) : UpdateEvent
}
```

### 4.7 ViewModel

```kotlin
// ui/update/UpdateViewModel.kt
package com.quranwords.ui.update

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranwords.core.util.Result
import com.quranwords.domain.model.AppUpdateResult
import com.quranwords.domain.repository.AppUpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val repository: AppUpdateRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    /**
     * The UI calls this inside repeatOnLifecycle(STARTED): every resume
     * re-subscribes the cold flow, which re-queries Play fresh — so downloads
     * that finished while backgrounded surface as "Restart to complete".
     * Runs until cancellation; the data layer unregisters its listener on cancel.
     */
    suspend fun observeUpdates(): Unit = coroutineScope {
        repository.checkUpdateAvailability().collect { result ->
            _uiState.update { current -> current.withUpdateResult(result) }
        }
    }

    fun onEvent(event: UpdateEvent) {
        when (event) {
            UpdateEvent.Dismiss -> _uiState.update {
                it.copy(isDismissed = true, banner = it.banner.hideIfAvailable())
            }
            UpdateEvent.RestartApp -> viewModelScope.launch {
                if (repository.completeUpdate() is Result.Error) {
                    _uiState.update { it.copy(banner = UpdateBannerState.Failed) }
                }
            }
            UpdateEvent.FlowNotStarted -> _uiState.update { it.copy(banner = UpdateBannerState.Failed) }
            is UpdateEvent.UpdateSheetResult -> when (event.resultCode) {
                Activity.RESULT_OK -> _uiState.update { state ->        // optimistic until listener refines
                    state.copy(banner = state.banner.optimisticDownloading())
                }
                Activity.RESULT_CANCELED -> Unit                        // banner unchanged; next resume re-checks
                else -> _uiState.update { it.copy(banner = UpdateBannerState.Failed) }
            }
        }
    }

    private fun UpdateUiState.withUpdateResult(result: AppUpdateResult): UpdateUiState {
        val banner: UpdateBannerState = when (result) {
            AppUpdateResult.NotAvailable -> UpdateBannerState.Hidden
            AppUpdateResult.Available -> if (isDismissed) UpdateBannerState.Hidden else UpdateBannerState.Available
            is AppUpdateResult.InProgress -> UpdateBannerState.Downloading(result.percent)
            AppUpdateResult.Downloaded -> UpdateBannerState.RestartRequired
            is AppUpdateResult.Failed -> UpdateBannerState.Failed
        }
        // NotAvailable also clears the session dismissal so future updates are visible again.
        return if (result is AppUpdateResult.NotAvailable) copy(banner = banner, isDismissed = false)
        else copy(banner = banner)
    }

    private fun UpdateBannerState.hideIfAvailable(): UpdateBannerState =
        if (this is UpdateBannerState.Available) UpdateBannerState.Hidden else this

    private fun UpdateBannerState.optimisticDownloading(): UpdateBannerState =
        if (this is UpdateBannerState.Available) UpdateBannerState.Downloading(0) else this
}
```

### 4.8 Compose glue (launcher owned by composition; fresh check per resume)

```kotlin
// ui/update/InAppUpdateHandler.kt
package com.quranwords.ui.update

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

@Composable
fun InAppUpdateHandler(
    updateFlowStarter: AppUpdateFlowLauncher,
    modifier: Modifier = Modifier,
    viewModel: UpdateViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // The ONLY ActivityResult-aware object: created and owned by the composition (review fix #1 & #4).
    val updateSheetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.onEvent(UpdateEvent.UpdateSheetResult(result.resultCode))
    }

    // Fresh availability check on every START => background-completed downloads detected immediately (review fix #3).
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.observeUpdates()
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        when (val banner = uiState.banner) {
            UpdateBannerState.Hidden -> Unit
            UpdateBannerState.Available -> UpdateBanner.UpdateAvailable(
                onUpdateClick = {
                    scope.launch {
                        if (!updateFlowStarter.startFlexibleUpdate(updateSheetLauncher)) {
                            viewModel.onEvent(UpdateEvent.FlowNotStarted)
                        }
                    }
                },
                onDismiss = { viewModel.onEvent(UpdateEvent.Dismiss) },
            )
            is UpdateBannerState.Downloading -> UpdateBanner.Downloading(percent = banner.percent)
            UpdateBannerState.RestartRequired -> UpdateBanner.RestartRequired(
                onRestart = { viewModel.onEvent(UpdateEvent.RestartApp) },
            )
            UpdateBannerState.Failed -> UpdateBanner.Failed(
                onDismiss = { viewModel.onEvent(UpdateEvent.Dismiss) },
            )
        }
    }
}
```

### 4.9 Banner (stateless; theme tokens only; 48dp targets; unified testTags)

```kotlin
// ui/update/UpdateBanner.kt
package com.quranwords.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

internal object UpdateBannerTags {
    const val ROOT = "update_banner_root"
    const val UPDATE_ACTION = "update_banner_action_update"
    const val RESTART_ACTION = "update_banner_action_restart"
    const val DISMISS_ACTION = "update_banner_action_dismiss"
    const val PROGRESS = "update_banner_progress"
}

@Composable
internal fun UpdateBanner.UpdateAvailable(onUpdateClick: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    BannerSurface(modifier = modifier.testTag(UpdateBannerTags.ROOT)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Filled.SystemUpdate, contentDescription = null)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.update_available_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.update_available_message), style = MaterialTheme.typography.bodySmall)
            }
            FilledTonalButton(
                onClick = onUpdateClick,
                modifier = Modifier
                    .height(48.dp)
                    .testTag(UpdateBannerTags.UPDATE_ACTION),
            ) { Text(stringResource(R.string.update_banner_update_now)) }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(UpdateBannerTags.DISMISS_ACTION),
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.update_banner_close_content_description),
                )
            }
        }
    }
}

@Composable
internal fun UpdateBanner.Downloading(percent: Int, modifier: Modifier = Modifier) {
    BannerSurface(modifier = modifier.testTag(UpdateBannerTags.ROOT)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(Modifier.testTag(UpdateBannerTags.PROGRESS))
            Text(stringResource(R.string.update_banner_downloading, percent), style = MaterialTheme.typography.bodyMedium)
            LinearProgressIndicator(
                progress = { percent / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun UpdateBanner.RestartRequired(onRestart: () -> Unit, modifier: Modifier = Modifier) {
    BannerSurface(modifier = modifier.testTag(UpdateBannerTags.ROOT)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.update_banner_restart_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.update_banner_restart_message), style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = onRestart,
                modifier = Modifier
                    .height(48.dp)
                    .testTag(UpdateBannerTags.RESTART_ACTION),
            ) { Text(stringResource(R.string.update_banner_restart_now)) }
        }
    }
}

@Composable
internal fun UpdateBanner.Failed(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    BannerSurface(modifier = modifier.testTag(UpdateBannerTags.ROOT)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.update_banner_failed_message),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(UpdateBannerTags.DISMISS_ACTION),
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.update_banner_close_content_description),
                )
            }
        }
    }
}

@Composable
private fun BannerSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) { Box(Modifier.padding(16.dp)) { content() } }
}
```

### 4.10 Wiring

```kotlin
// MainActivity.kt — add field + pass through
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var updateFlowLauncher: AppUpdateFlowLauncher

    // setContent {
    //     ...
    //     AppNavigation(updateFlowStarter = updateFlowLauncher)
    // }
}
```

```kotlin
// AppNavigation.kt
@Composable
fun AppNavigation(
    updateFlowStarter: AppUpdateFlowLauncher,
    // ...existing params unchanged
) {
    Scaffold(/* unchanged */) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            AppNavHost(/* unchanged */)
            InAppUpdateHandler(
                updateFlowStarter = updateFlowStarter,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}
```

### 4.11 Strings (ar default + en)

```xml
<string name="update_available_title">يتوفر إصدار جديد</string>
<string name="update_available_message">حدِّث التطبيق للحصول على أحدث المزايا والإصلاحات</string>
<string name="update_banner_update_now">تحديث</string>
<string name="update_banner_downloading">جارٍ تنزيل التحديث %1$d%%</string>
<string name="update_banner_restart_title">اكتمل تنزيل التحديث</string>
<string name="update_banner_restart_message">أعد تشغيل التطبيق للبدء بالإصدار الجديد</string>
<string name="update_banner_restart_now">أعد التشغيل الآن</string>
<string name="update_banner_failed_message">تعثّرت عملية التحديث، سنحاول مرة أخرى لاحقاً</string>
<string name="update_banner_close_content_description">إخفاء إشعار التحديث</string>
```

English equivalents under the same keys in `values-en/strings.xml`.

---

## 5) Unit tests — `app/src/test/.../ui/update/UpdateViewModelTest.kt`

Fake (no Android framework needed):

```kotlin
private class RecordingUpdateRepository : AppUpdateRepository {
    val emitted = MutableSharedFlow<AppUpdateResult>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    var completeCalls = 0
    override fun checkUpdateAvailability(): Flow<AppUpdateResult> = emitted
    override suspend fun completeUpdate(): com.quranwords.core.util.Result<Unit> {
        completeCalls++
        return Result.Success(Unit)
    }
}
```

Setup per test: `runTest` → `backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.observeUpdates() }` → emit from the fake → assert `viewModel.uiState.value.banner`.

| # | Case | Expected |
|---|---|---|
| 1 | emit `Available` | `UpdateBannerState.Available` |
| 2 | emit `Downloaded` | `RestartRequired` |
| 3 | emit `NotAvailable` after dismissal | clears `isDismissed`; later `Available` is visible again |
| 4 | `Dismiss`, then `Downloaded` | dismissal hides only `Available`; restart still shows |
| 5 | `UpdateSheetResult(RESULT_OK)` then `InProgress(42)` | optimistic `Downloading(0)` refined to `Downloading(42)` |
| 6 | `RESULT_CANCELED` / `FlowNotStarted` | unchanged / `Failed` |
| 7 | `RestartApp` (success + error paths) | `completeCalls == 1`; error → `Failed` |

## 6) Behavior walkthrough (review fixes in action)

- **Launch:** tap "تحديث" → `startFlexibleUpdate(launcher)` fetches a *fresh* `AppUpdateInfo`, validates `isUpdateTypeAllowed(FLEXIBLE)`, opens Play's sheet through the composition-owned launcher. `RESULT_OK` → optimistic `Downloading(0)`; listener refines with real percentages.
- **Leak-free:** STOP cancels the collection → `awaitClose` unregisters the listener; RESUME re-registers via a brand-new cold flow. At most one listener at any moment.
- **Background completion:** download finishes while backgrounded (process alive → listener fires; process dead or events missed → next START re-queries `appUpdateInfo` → `installStatus == DOWNLOADED`) → `RestartRequired` immediately on resume.
- **Completion:** "أعد التشغيل الآن" → `completeUpdate()` → Play swaps in the new version.
- **Post-update:** fresh check → `UPDATE_NOT_AVAILABLE` → banner hidden; `NotAvailable` also resets the session dismissal.
- `UPDATE_UNAVAILABLE` (sideload / no Play / up-to-date / propagation delay) → nothing renders.

## 7) QA on Play Console (manual)

1. Ship internal-testing track build (versionCode N) via Play → install on an opted-in device (not adb).
2. Upload versionCode N+1 to the same track → wait for propagation (hours) → app start / resume shows the banner.
3. Tap تحديث → accept flexible download → banner shows progress → "أعد التشغيل الآن" → app restarts on N+1.
4. Background the app mid-download; verify the restart prompt appears on return/resume.
5. Verify dismiss-then-disable flow: dismiss hides the availability banner for the session only; future sessions show it again.

## 8) Verification commands (agent build policy — user runs, agent does not)

```bash
./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug
```

Do not claim any check passes without user-provided output.

## 9) Decision Log entry (add to AGENTS.md §20)

| # | Decision | Context | Options considered | Chosen | Reason | Trade-offs | Date |
|---|---|---|---|---|---|---|---|
| 29 | In-app updates: UI-owned launch port implemented by data; cold `callbackFlow`; fresh check per resume | Needed update prompts for Play-distributed installs without leaking Activity context into Data/VM, and without listener leaks | raw manager in Activity / VM-held `startUpdateFlowForResult` / deprecated request-code API | Domain `AppUpdateResult`+`AppUpdateRepository` (pure); `PlayAppUpdateDataSource` implements domain repo + `ui.update.AppUpdateFlowLauncher`; composable owns `rememberLauncherForActivityResult(StartIntentSenderForResult)` + `AppUpdateOptions`; collection via `repeatOnLifecycle(STARTED)` re-queries Play fresh each resume (detects `DOWNLOADED` after backgrounding/play resumed downloads) | No Activity/Context in Data/VM; single listener at a time, unregistered via `awaitClose`; modern launcher API; banner states map to the five domain outcomes | Port interface declared in `ui/` but implemented in `data/` (documented DIP inversion); Play-only installs (GitHub APKs uncovered by design; revisit if needed) | 2026-10-18 |

## 10) Implementation notes / follow-ups

- Verify `libs.versions.toml` version-catalog accessors used by the project (`libs.play.app.update` naming must match existing conventions).
- `LinearProgressIndicator(progress = { ... })` lambda form assumes Compose BOM 1.7+; adapt to the project's version if older.
- `LocalLifecycleOwner`: prefer `androidx.lifecycle.compose.LocalLifecycleOwner`; if the project uses an older lifecycle version, fall back to `androidx.compose.ui.platform.LocalLifecycleOwner`.
- Optional future: `FakeAppUpdateManager` (`app-update-testing`) for DAO-free local checks; GitHub `version.json` check for sideloaded users if coverage becomes a requirement.
- Keep every file under the file-size targets; banner strings localized; no hardcoded colors; RTL-safe layout (Row/Column with logical start/end).

---

## 11) Implementation record (2026-10-19)

Implemented as specified. Final locations/names as written in the code (deviations from this doc are listed here so the record matches the repo):

- Play datasource lives flat in `data/remote/PlayAppUpdateDataSource.kt` (the existing remote layer places datasources directly under `data/remote/`, no `datasource` subfolder).
- Banner composables are top-level internal functions in `ui/update/UpdateBanner.kt`: `UpdateAvailableBanner`, `UpdateDownloadingBanner`, `UpdateRestartBanner`, `UpdateFailedBanner` (+ private `BannerSurface`, `UpdateBannerTags`) — same API/semantics as the planned `UpdateBanner.X` members, matching the project's top-level decomposition style.
- Close-button string follows the project's `cd_` content-description convention: `cd_update_banner_close`.
- `InAppUpdateHandler` adds `statusBarsPadding()` + 16dp horizontal padding per branch for the edge-to-edge shell; hosted in `AppNavigation` inside a `Box(Modifier.fillMaxSize().padding(innerPadding))` after the `NavHost`, aligned `TopCenter`.
- `MainActivity` provides `@Inject lateinit var updateFlowLauncher: AppUpdateFlowLauncher` and passes it through `AppNavigation(updateFlowStarter = ...)`.
- DI: `core/di/UpdateModule.kt` contains both `UpdateProvidesModule` (AppUpdateManager via `@ApplicationContext`) and `UpdateBindingsModule` (repository + launch port bindings).
- Strings added to `values/strings.xml` (English default) and `values-ar/strings.xml` (Arabic) — the project defaults to English with `values-ar` overrides (the doc's earlier "ar default" assumption was flipped).
- Tests at `app/src/test/java/com/quranwords/ui/update/UpdateViewModelTest.kt` (12 cases covering every UiState/EVENT path).
