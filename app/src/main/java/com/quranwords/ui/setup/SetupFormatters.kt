package com.quranwords.ui.setup

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.quranwords.R
@Composable
internal fun downloadErrorMessage(error: SetupError): String = stringResource(
    when (error) {
        SetupError.NETWORK -> R.string.dl_err_download
        SetupError.NOT_ZIP -> R.string.dl_err_not_zip
        SetupError.CHECKSUM_MISMATCH -> R.string.dl_err_checksum
        SetupError.EXTRACT_FAILED -> R.string.dl_err_extract
        SetupError.INVALID_DB -> R.string.dl_err_invalid_db
        SetupError.INCOMPLETE_FILE -> R.string.dl_err_incomplete
        SetupError.INSTALL_FAILED -> R.string.dl_err_install
        SetupError.BAD_PICK -> R.string.dl_err_bad_pick
        SetupError.IMPORT_FAILED -> R.string.dl_err_import
        SetupError.MANIFEST_FAILED -> R.string.dl_err_manifest
        SetupError.UNKNOWN -> R.string.dl_err_unknown
    }
)
