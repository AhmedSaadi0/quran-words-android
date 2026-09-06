package io.github.ahmedsaadi0.quranwords.ui.viewmodel

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ahmedsaadi0.quranwords.BuildConfig
import io.github.ahmedsaadi0.quranwords.domain.repository.DbUpdateRepository
import io.github.ahmedsaadi0.quranwords.util.MeaningReportContent
import io.github.ahmedsaadi0.quranwords.util.MeaningReportType
import io.github.ahmedsaadi0.quranwords.util.ReportAyahSample
import io.github.ahmedsaadi0.quranwords.util.validateMeaningReport
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * نموذج البلاغ عن الملخص الذكي: حالة خفيفة لحوار واحد (نوع + نصوص).
 * الهدف ثابت دائمًا: المعنى المكتوب بالذكاء الاصطناعي.
 * التحقق وبناء المحتوى دوال خالصة في util لتبقى قابلة للاختبار دون إطار أندرويد.
 */
@HiltViewModel
class ReportMeaningViewModel @Inject constructor(
    private val dbUpdateRepository: DbUpdateRepository
) : ViewModel() {

    companion object {
        const val AI_SUMMARY_SOURCE = "الملخص الذكي"
    }

    private val _reportType = MutableStateFlow(MeaningReportType.INCORRECT)
    val reportType: StateFlow<MeaningReportType> = _reportType.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _suggestion = MutableStateFlow("")
    val suggestion: StateFlow<String> = _suggestion.asStateFlow()

    private val _canSubmit = MutableStateFlow(false)
    val canSubmit: StateFlow<Boolean> = _canSubmit.asStateFlow()

    private val _dbVersionName = MutableStateFlow("")
    private val _dbVersionCode = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            try {
                val installed = dbUpdateRepository.getInstalledVersion()
                _dbVersionName.value = installed.versionName
                _dbVersionCode.value = installed.versionCode
            } catch (_: Exception) {
            }
        }
    }

    fun setReportType(type: MeaningReportType) {
        _reportType.value = type
    }

    fun setDescription(text: String) {
        _description.value = text
        _canSubmit.value = validateMeaningReport(text)
    }

    fun setSuggestion(text: String) {
        _suggestion.value = text
    }

    fun reset() {
        _reportType.value = MeaningReportType.INCORRECT
        _description.value = ""
        _suggestion.value = ""
        _canSubmit.value = false
    }

    fun buildContent(
        rootText: String,
        rootId: Int?,
        aiSummary: String,
        samples: List<ReportAyahSample>
    ): MeaningReportContent {
        return MeaningReportContent(
            rootText = rootText,
            rootId = rootId,
            reportType = _reportType.value,
            targetSource = AI_SUMMARY_SOURCE,
            targetQuote = aiSummary.trim(),
            description = _description.value.trim(),
            suggestion = _suggestion.value.trim(),
            samples = samples,
            appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            dbVersionName = _dbVersionName.value,
            dbVersionCode = _dbVersionCode.value,
            androidRelease = Build.VERSION.RELEASE ?: "",
            locale = try {
                Locale.getDefault().toLanguageTag()
            } catch (_: Exception) {
                ""
            }
        )
    }
}
