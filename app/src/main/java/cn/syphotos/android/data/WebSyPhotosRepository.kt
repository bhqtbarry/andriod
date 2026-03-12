package cn.syphotos.android.data

import android.net.Uri
import cn.syphotos.android.BuildConfig
import cn.syphotos.android.model.CategoryCount
import cn.syphotos.android.model.PhotoFilter
import cn.syphotos.android.model.PhotoItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL

class WebSyPhotosRepository(
    private val baseUrl: String = BuildConfig.SY_PHOTOS_BASE_URL,
) {
    fun getPhotos(filter: PhotoFilter = PhotoFilter()): List<PhotoItem> {
        val uri = Uri.parse(baseUrl.trimEnd('/') + "/photos").buildUpon().apply {
            appendIfNotBlank("keyword", filter.keyword)
            appendIfNotBlank("author", filter.author)
            appendIfNotBlank("airline", filter.airline)
            appendIfNotBlank("aircraft_model", filter.aircraftModel)
            appendIfNotBlank("camera", filter.camera)
            appendIfNotBlank("lens", filter.lens)
            appendIfNotBlank("registration", filter.registration)
            appendIfNotBlank("location_code", filter.locationCode)
        }.build()

        return openJson(uri.toString()).let { root ->
            when {
                root.has("items") -> root.getJSONArray("items").toPhotoItems()
                root.has("data") -> root.getJSONArray("data").toPhotoItems()
                else -> throw IllegalStateException("Unsupported response from $uri")
            }
        }
    }

    fun getCategoryCounts(photos: List<PhotoItem>): Pair<List<CategoryCount>, List<CategoryCount>> {
        return Pair(
            photos.groupingBy { it.airline }.eachCount().entries
                .sortedByDescending { it.value }
                .map { CategoryCount(it.key, it.value) },
            photos.groupingBy { it.aircraftModel }.eachCount().entries
                .sortedByDescending { it.value }
                .map { CategoryCount(it.key, it.value) },
        )
    }

    private fun openJson(url: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("Accept", "application/json")
        }

        return connection.useResponse { statusCode, body ->
            if (statusCode !in 200..299) {
                throw IllegalStateException("HTTP $statusCode from $url")
            }
            JSONObject(body)
        }
    }

    private fun JSONArray.toPhotoItems(): List<PhotoItem> = buildList(length()) {
        for (index in 0 until length()) {
            val item = getJSONObject(index)
            add(
                PhotoItem(
                    id = item.optLong("id"),
                    title = item.optString("title"),
                    author = item.optString("author"),
                    airline = item.optString("airline"),
                    aircraftModel = item.optString("aircraftModel").ifBlank { item.optString("aircraft_model") },
                    registration = item.optString("registration"),
                    location = item.optString("location").ifBlank { item.optString("locationCode") }.ifBlank { item.optString("location_code") },
                    camera = item.optString("camera"),
                    lens = item.optString("lens"),
                    createdAt = item.optString("createdAt").ifBlank { item.optString("created_at") },
                    liked = item.optBoolean("liked", false),
                ),
            )
        }
    }

    private fun Uri.Builder.appendIfNotBlank(key: String, value: String) {
        if (value.isNotBlank()) {
            appendQueryParameter(key, value)
        }
    }

    private inline fun <T> HttpURLConnection.useResponse(block: (Int, String) -> T): T {
        return try {
            val stream = if (responseCode in 200..299) inputStream else errorStream
            val body = stream?.use { BufferedInputStream(it).reader().readText() }.orEmpty()
            block(responseCode, body)
        } finally {
            disconnect()
        }
    }
}
