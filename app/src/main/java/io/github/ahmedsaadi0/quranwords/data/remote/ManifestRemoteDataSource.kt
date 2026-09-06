package io.github.ahmedsaadi0.quranwords.data.remote

import io.github.ahmedsaadi0.quranwords.core.util.DatabaseConstants
import io.github.ahmedsaadi0.quranwords.di.IoDispatcher
import io.github.ahmedsaadi0.quranwords.domain.model.DbReleaseInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject

class ManifestRemoteDataSource @Inject constructor(
    private val client: OkHttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun fetchLatest(manifestUrl: String = DatabaseConstants.MANIFEST_URL): DbReleaseInfo =
        withContext(ioDispatcher) {
            val request = Request.Builder()
                .url(manifestUrl)
                .header("User-Agent", "QuranWordsApp")
                .header("Cache-Control", "no-cache")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("manifest http ${response.code}")
                }
                val body = response.body?.string()
                    ?: throw IllegalStateException("empty manifest")
                parseManifest(body)
            }
        }

    fun parseManifest(json: String): DbReleaseInfo {
        val o = JSONObject(json)
        val code = o.optInt("latestVersionCode", 0)
        if (code <= 0) throw IllegalArgumentException("invalid versionCode")
        val url = o.optString("downloadUrl", "")
        if (url.isBlank() || !url.startsWith("https://")) {
            throw IllegalArgumentException("invalid downloadUrl")
        }
        return DbReleaseInfo(
            versionCode = code,
            versionName = o.optString("latestVersionName", "v$code"),
            downloadUrl = url,
            compressedSize = o.optLong("compressedSize", 0L),
            uncompressedSize = o.optLong("uncompressedSize", 0L),
            sha256 = o.optString("sha256", "").takeIf { it.length == 64 },
            publishedAt = o.optString("publishedAt", "").takeIf { it.isNotBlank() },
            releaseNotesAr = parseReleaseNotes(o),
            releasePageUrl = o.optString("releasePageUrl", "").takeIf { it.isNotBlank() },
            minAppVersionCode = o.optInt("minAppVersionCode", 0)
        )
    }

    /**
     * يقبل الشكل الجديد (مصفوفة نصوص) والشكل القديم (نص واحد متعدد الأسطر)
     * ويُرجع نصًا واحدًا مفصولًا بـ "\n" ليتوافق مع واجهات العرض الحالية.
     */
    fun parseReleaseNotes(o: JSONObject): String {
        val raw = o.opt("releaseNotesAr")
        if (raw is org.json.JSONArray) {
            val items = mutableListOf<String>()
            for (i in 0 until raw.length()) {
                val s = raw.optString(i, "").trim()
                if (s.isNotBlank()) items.add(s)
            }
            return items.joinToString("\n")
        }
        return (raw as? String)?.trim() ?: ""
    }
}
