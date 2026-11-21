package com.quranwords.core

import com.quranwords.core.util.FileSize
import com.quranwords.core.util.SizeUnit
import com.quranwords.core.util.formatFileSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FileSizeFormatterTest {
    @Test
    fun boundaries_and_zero() {
        assertNull(formatFileSize(0))
        assertNull(formatFileSize(-1))
        assertEquals(FileSize(0, SizeUnit.KB), formatFileSize(1))
        assertEquals(FileSize(1, SizeUnit.KB), formatFileSize(1024))
        assertEquals(FileSize(1023, SizeUnit.KB), formatFileSize(1024L * 1024 - 1))
        assertEquals(FileSize(1, SizeUnit.MB), formatFileSize(1024L * 1024))
        assertEquals(FileSize(118, SizeUnit.MB), formatFileSize(118L * 1024 * 1024))
    }
}
