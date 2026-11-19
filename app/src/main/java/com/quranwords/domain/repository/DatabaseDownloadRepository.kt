package com.quranwords.domain.repository

import com.quranwords.domain.model.DbReleaseInfo
import com.quranwords.domain.model.DownloadState
import kotlinx.coroutines.flow.Flow

/** Domain port for installing the offline corpus (AGENTS §4.1, P1-1). */
interface DatabaseDownloadRepository {
    fun isDatabaseReady(): Boolean
    fun downloadRelease(info: DbReleaseInfo): Flow<DownloadState>
    /** [uri] is a platform URI string; parsing stays in the data implementation. */
    fun importDatabase(uri: String): Flow<DownloadState>
}
