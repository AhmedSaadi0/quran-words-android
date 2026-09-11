package io.github.ahmedsaadi0.quranwords.ui.appstate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ahmedsaadi0.quranwords.domain.model.DbReleaseInfo
import io.github.ahmedsaadi0.quranwords.domain.model.DbUpdateState
import io.github.ahmedsaadi0.quranwords.domain.repository.DbCheckResult
import io.github.ahmedsaadi0.quranwords.domain.repository.DbUpdateRepository
import io.github.ahmedsaadi0.quranwords.domain.usecase.CheckDbUpdateUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * App-level DB-update banner state (Decision 14): shown on Home when a newer
 * database release is available.
 */
@HiltViewModel
class DbUpdateViewModel @Inject constructor(
    private val checkUpdate: CheckDbUpdateUseCase,
    private val dbUpdateRepository: DbUpdateRepository
) : ViewModel() {

    private val _state = MutableStateFlow<DbUpdateState>(DbUpdateState.Unknown)
    val state: StateFlow<DbUpdateState> = _state.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    fun checkOnce() {
        if (_isChecking.value) return
        viewModelScope.launch {
            _isChecking.value = true
            try {
                when (val r = checkUpdate()) {
                    is DbCheckResult.Success -> _state.value = r.data
                    is DbCheckResult.Error -> _state.value = DbUpdateState.Unknown
                }
            } finally {
                _isChecking.value = false
            }
        }
    }

    fun dismiss(info: DbReleaseInfo) {
        viewModelScope.launch {
            dbUpdateRepository.setDismissedVersionCode(info.versionCode)
            _state.value = DbUpdateState.UpToDate
        }
    }
}