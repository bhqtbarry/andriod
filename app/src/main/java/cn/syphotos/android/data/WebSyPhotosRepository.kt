package cn.syphotos.android.data

import android.net.Uri
import cn.syphotos.android.BuildConfig
import cn.syphotos.android.model.CategoryCount
import cn.syphotos.android.model.MapCluster
import cn.syphotos.android.model.PhotoFilter
import cn.syphotos.android.model.PhotoDetail
import cn.syphotos.android.model.PhotoItem
import cn.syphotos.android.model.ReviewItem
import cn.syphotos.android.model.UploadConfig
import cn.syphotos.android.model.DeviceSession
import cn.syphotos.android.model.UserSummary
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL

class WebSyPhotosRepository(
    private val baseUrl: String = BuildConfig.SY_PHOTOS_BASE_URL,
) : SyPhotosRepository {
    override fun getPhotos(filter: PhotoFilter): List<PhotoItem> {
        val uri = apiUri("photos").buildUpon().apply {
            appendIfNotBlank("keyword", filter.keyword)
            appendIfNotBlank("author", filter.author)
            appendIfNotBlank("airline", filter.airline)
            appendIfNotBlank("aircraft_model", filter.aircraftModel)
            appendIfNotBlank("camera", filter.camera)
            appendIfNotBlank("lens", filter.lens)
            appendIfNotBlank("registration", filter.registration)
            appendIfNotBlank("location_code", filter.locationCode)
        }.build()

        return openJsonArray(uri.toString()).toPhotoItems()
    }

    override fun getPhotoDetail(photoId: Long): PhotoDetail {
        val item = openJson(apiUri("photos/$photoId").toString())
        val photo = item.toPhotoItem()
        return PhotoDetail(
            photo = photo,
            originalUrl = item.optString("originalUrl").ifBlank { item.optString("original_url") },
            shareUrl = item.optString("shareUrl").ifBlank { item.optString("share_url") }.ifBlank { "https://www.syphotos.cn/photo/$photoId" },
            description = item.optString("description"),
        )
    }

    override fun getCategoryCounts(): Pair<List<CategoryCount>, List<CategoryCount>> {
        val photos = getPhotos()
        return getCategoryCounts(photos)
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

    override fun getMapClusters(filter: PhotoFilter): List<MapCluster> {
        val uri = apiUri("map/clusters").buildUpon().apply {
            appendIfNotBlank("location_code", filter.locationCode)
            appendIfNotBlank("keyword", filter.keyword)
        }.build()
        return openJsonArray(uri.toString()).toMapClusters()
    }

    override fun getUploadConfig(): UploadConfig {
        val item = openJson(apiUri("upload/config").toString())
        return UploadConfig(
            maxFileSizeMb = item.optInt("maxFileSizeMb", item.optInt("max_file_size_mb", 40)),
            minAspectRatio = item.optString("minAspectRatio").ifBlank { item.optString("min_aspect_ratio") }.ifBlank { "1:2" },
            maxAspectRatio = item.optString("maxAspectRatio").ifBlank { item.optString("max_aspect_ratio") }.ifBlank { "2:1" },
            exifEnabled = item.optBoolean("exifEnabled", item.optBoolean("exif_enabled", true)),
            watermarkEnabled = item.optBoolean("watermarkEnabled", item.optBoolean("watermark_enabled", true)),
            registrationOcrEnabled = item.optBoolean("registrationOcrEnabled", item.optBoolean("registration_ocr_enabled", true)),
            uploadUrl = item.optString("uploadUrl").ifBlank { item.optString("upload_url") },
        )
    }

    override fun getReviewItems(status: String): List<ReviewItem> {
        val items = openJsonArray(apiUri("me/$status").toString())
        return items.toReviewItems(status)
    }

    override fun getMyLikes(): List<PhotoItem> {
        return openJsonArray(apiUri("me/likes").toString()).toPhotoItems()
    }

    override fun getDeviceSessions(): List<DeviceSession> {
        val items = openJsonArray(apiUri("me/devices").toString())
        return buildList(items.length()) {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                add(
                    DeviceSession(
                        id = item.optString("id"),
                        deviceName = item.optString("deviceName").ifBlank { item.optString("device_name") },
                        loginTime = item.optString("loginTime").ifBlank { item.optString("login_time") },
                        ipAddress = item.optString("ipAddress").ifBlank { item.optString("ip_address") },
                        systemVersion = item.optString("systemVersion").ifBlank { item.optString("system_version") },
                        isCurrent = item.optBoolean("isCurrent", item.optBoolean("is_current", false)),
                    ),
                )
            }
        }
    }

    override fun getUserSummary(): UserSummary {
        val item = openJson(apiUri("me").toString())
        return UserSummary(
            username = item.optString("username"),
            email = item.optString("email"),
            emailVerified = item.optBoolean("emailVerified", item.optBoolean("email_verified", false)),
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

    private fun openJsonArray(url: String): JSONArray {
        val root = openJson(url)
        return when {
            root.has("items") -> root.getJSONArray("items")
            root.has("data") -> root.getJSONArray("data")
            else -> throw IllegalStateException("Unsupported response from $url")
        }
    }

    private fun JSONObject.toPhotoItem(): PhotoItem {
        return PhotoItem(
            id = optLong("id"),
            title = optString("title"),
            author = optString("author"),
            airline = optString("airline"),
            aircraftModel = optString("aircraftModel").ifBlank { optString("aircraft_model") },
            registration = optString("registration"),
            location = optString("location").ifBlank { optString("locationCode") }.ifBlank { optString("location_code") },
            camera = optString("camera"),
            lens = optString("lens"),
            createdAt = optString("createdAt").ifBlank { optString("created_at") },
            liked = optBoolean("liked", false),
        )
    }

    private fun JSONArray.toPhotoItems(): List<PhotoItem> = buildList(length()) {
        for (index in 0 until length()) {
            add(getJSONObject(index).toPhotoItem())
        }
    }

    private fun JSONArray.toMapClusters(): List<MapCluster> = buildList(length()) {
        for (index in 0 until length()) {
            val item = getJSONObject(index)
            add(
                MapCluster(
                    id = item.optString("id").ifBlank { item.optString("locationCode").ifBlank { item.optString("location_code") } },
                    name = item.optString("name").ifBlank { item.optString("label") },
                    level = item.optString("level").ifBlank { "airport" },
                    photoCount = item.optInt("photoCount", item.optInt("photo_count", 0)),
                    locationCode = item.optString("locationCode").ifBlank { item.optString("location_code") },
                ),
            )
        }
    }

    private fun JSONArray.toReviewItems(status: String): List<ReviewItem> = buildList(length()) {
        for (index in 0 until length()) {
            val item = getJSONObject(index)
            val photoJson = item.optJSONObject("photo") ?: item
            add(
                ReviewItem(
                    photo = photoJson.toPhotoItem(),
                    status = item.optString("status").ifBlank { status },
                    rejectionReason = item.optString("rejectionReason").ifBlank { item.optString("rejection_reason") }.blankToNull(),
                    adminComment = item.optString("adminComment").ifBlank { item.optString("admin_comment") }.blankToNull(),
                ),
            )
        }
    }

    private fun Uri.Builder.appendIfNotBlank(key: String, value: String) {
        if (value.isNotBlank()) {
            appendQueryParameter(key, value)
        }
    }

    private fun apiUri(path: String): Uri = Uri.parse(baseUrl.trimEnd('/') + "/" + path.trimStart('/'))

    private fun String.blankToNull(): String? = if (isBlank()) null else this

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
