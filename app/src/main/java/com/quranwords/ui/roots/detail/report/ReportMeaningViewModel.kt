package com.quranwords.ui.roots.detail.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.quranwords.core.util.BuildInfo
import com.quranwords.core.util.Result
import com.quranwords.core.util.runCatchingResult
import com.quranwords.domain.repository.DbUpdateRepository
import com.quranwords.util.MeaningReportContent
import com.quranwords.util.MeaningReportType
import com.quranwords.util.ReportAyahSample
import com.quranwords.util.validateMeaningReport
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * نموذج البلاغ عن الملخص الذكي: حالة خفيفة لحوار واحد (نوع + نصوص).
 * الهدف ثابت دائمًا: المعنى المكتوب بالذكاء الاصطناعي.
 * التحقق وبناء المحتوى دوال خالصة في util لتبقى قابلة للاختبار دون إطار أندرويد.
 * Platform facts come from injected [BuildInfo] — testable without Android.
 */
@HiltViewModel
class ReportMeaningViewModel @Inject constructor(
    private val dbUpdateRepository: DbUpdateRepository,
    private val buildInfo: BuildInfo
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
            when (val result = runCatchingResult { dbUpdateRepository.getInstalledVersion() }) {
                is Result.Success -> {
                    _dbVersionName.value = result.data.versionName
                    _dbVersionCode.value = result.data.versionCode
                }
                // Unknown DB version degrades to empty defaults; not a report blocker.
                is Result.Error -> Unit
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
            appVersion = buildInfo.appVersion,
            dbVersionName = _dbVersionName.value,
            dbVersionCode = _dbVersionCode.value,
            androidRelease = buildInfo.androidRelease,
            locale = buildInfo.locale
        )
    }
}