package com.quranwords.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class UiResourcesTest {
    private val resDir: File by lazy {
        val module = File("src/main/res")
        if (module.exists()) module else File("../app/src/main/res")
    }

    @Test
    fun no_emoji_in_user_strings() {
        val emoji = Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF\\u2600-\\u27BF\\uFE0F]")
        resDir.walkTopDown()
            .filter { it.name == "strings.xml" && (it.parentFile?.name ?: "").startsWith("values") }
            .forEach { file ->
                assertFalse(file.path, emoji.containsMatchIn(file.readText()))
            }
    }

    @Test
    fun new_chrome_strings_exist_in_both_locales() {
        val keys = listOf(
            "search_page_suffix", "search_juz_suffix", "search_surah_filter_selected",
            "search_surah_menu_item", "font_decrease", "font_increase",
            "cd_bookmark_ayah", "cd_unbookmark_ayah"
        )
        keys.forEach { key ->
            assertTrue(key, File(resDir, "values/strings.xml").readText().contains("name=\"$key\""))
            assertTrue(key, File(resDir, "values-ar/strings.xml").readText().contains("name=\"$key\""))
        }
    }
}
