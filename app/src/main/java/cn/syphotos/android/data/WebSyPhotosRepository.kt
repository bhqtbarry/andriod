package cn.syphotos.android.data

import android.net.Uri
import cn.syphotos.android.BuildConfig
import cn.syphotos.android.model.CategoryCount
import cn.syphotos.android.model.MapCluster
import cn.syphotos.android.model.MySummaryStats
import cn.syphotos.android.model.AuthSession
import cn.syphotos.android.model.PhotoFilter
import cn.syphotos.android.model.PhotoDetail
import cn.syphotos.android.model.PhotoItem
import cn.syphotos.android.model.ReviewItem
import cn.syphotos.android.model.SearchSuggestion
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
    private val sessionStore: SessionStore,
) : SyPhotosRepository {
    override fun getAuthSession(): AuthSession = sessionStore.read()

    override fun login(login: String, password: String): AuthSession {
        val json = openJson(
            url = apiUri("auth/login.php").toString(),
            method = "POST",
            formBody = mapOf(
                "login" to login,
                "password" to password,
                "platform" to "android",
                "device_name" to "Android App",
                "app_version" to "1.0.0",
            ),
        )
        val user = json.optJSONObject("user") ?: JSONObject()
        val auth = json.optJSONObject("auth") ?: throw IllegalStateException("Missing auth payload")
        val session = AuthSession(
            accessToken = auth.optString("access_token"),
            refreshToken = auth.optString("refresh_token"),
            accessTokenExpiresAt = auth.optString("access_token_expires_at"),
            refreshTokenExpiresAt = auth.optString("refresh_token_expires_at"),
            username = user.optString("username"),
            email = user.optString("email"),
        )
        sessionStore.write(session)
        return session
    }

    override fun logout() {
        runCatching {
            openJson(
                url = apiUri("auth/logout.php").toString(),
                method = "POST",
                requiresAuth = true,
                formBody = emptyMap(),
            )
        }
        sessionStore.clear()
    }

    override fun getPhotos(filter: PhotoFilter): List<PhotoItem> {
        val uri = apiUri("photos/feed.php").buildUpon().apply {
            appendQueryParameter("page", "1")
            appendQueryParameter("per_page", "30")
            appendIfNotBlank("keyword", mergeKeyword(filter.keyword, filter.author))
            filter.author.toLongOrNull()?.let { appendQueryParameter("userid", it.toString()) }
            appendIfNotBlank("airline", filter.airline)
            appendIfNotBlank("aircraft_model", filter.aircraftModel)
            appendIfNotBlank("cam", filter.camera)
            appendIfNotBlank("lens", filter.lens)
            appendIfNotBlank("registration_number", filter.registration.uppercase())
            appendIfNotBlank("iatacode", filter.locationCode.uppercase())
        }.build()

        return openJsonArray(uri.toString()).toPhotoItems()
    }

    override fun getPhotoDetail(photoId: Long): PhotoDetail {
        val root = openJson(apiUri("photos/detail.php").buildUpon().appendQueryParameter("id", photoId.toString()).build().toString())
        val item = root.optJSONObject("item") ?: throw IllegalStateException("Missing item in photo detail response")
        val photo = item.toPhotoItem()
        return PhotoDetail(
            photo = photo,
            originalUrl = photo.originalUrl.ifBlank {
                normalizeUrl(item.optString("originalUrl").ifBlank { item.optString("original_url") })
            },
            shareUrl = photo.shareUrl.ifBlank {
                normalizeUrl(
                    item.optString("shareUrl")
                        .ifBlank { item.optString("share_url") }
                        .ifBlank { "https://www.syphotos.cn/photo_detail.php?id=$photoId" },
                )
            },
            description = item.optString("description"),
        )
    }

    override fun getCategoryCounts(): Pair<List<CategoryCount>, List<CategoryCount>> {
        val photos = getPhotos(PhotoFilter())
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

    override fun getSuggestions(field: String, query: String, filter: PhotoFilter): List<SearchSuggestion> {
        val uri = apiUri("search/suggestions.php").buildUpon().apply {
            appendQueryParameter("field", field)
            appendQueryParameter("q", query)
            appendIfNotBlank("keyword", mergeKeyword(filter.keyword, filter.author))
            filter.author.toLongOrNull()?.let { appendQueryParameter("userid", it.toString()) }
            appendIfNotBlank("airline", filter.airline)
            appendIfNotBlank("aircraft_model", filter.aircraftModel)
            appendIfNotBlank("cam", filter.camera)
            appendIfNotBlank("lens", filter.lens)
            appendIfNotBlank("registration_number", filter.registration.uppercase())
            appendIfNotBlank("iatacode", filter.locationCode.uppercase())
        }.build()
        return buildList {
            val items = openJsonArray(uri.toString())
            for (index in 0 until minOf(items.length(), 5)) {
                val item = items.getJSONObject(index)
                add(
                    SearchSuggestion(
                        value = item.optString("value"),
                        label = item.optString("label").ifBlank { item.optString("value") },
                        count = item.optInt("count", 0),
                    ),
                )
            }
        }
    }

    override fun getMapClusters(filter: PhotoFilter): List<MapCluster> {
        val uri = apiUri("map/clusters.php").buildUpon().apply {
            appendQueryParameter("level", "country")
            appendIfNotBlank("iatacode", filter.locationCode.uppercase())
            appendIfNotBlank("keyword", mergeKeyword(filter.keyword, filter.author))
            appendIfNotBlank("airline", filter.airline)
            appendIfNotBlank("aircraft_model", filter.aircraftModel)
            appendIfNotBlank("cam", filter.camera)
            appendIfNotBlank("lens", filter.lens)
            appendIfNotBlank("registration_number", filter.registration.uppercase())
        }.build()
        return openJsonArray(uri.toString()).toMapClusters()
    }

    override fun getUploadConfig(): UploadConfig {
        throw UnsupportedOperationException("android_api_reference_zh: no app upload API is implemented yet")
    }

    override fun getMySummary(): Pair<UserSummary, MySummaryStats> {
        val item = openJson(apiUri("me/summary.php").toString(), requiresAuth = true)
        val user = item.optJSONObject("user") ?: JSONObject()
        val stats = item.optJSONObject("stats") ?: JSONObject()
        return Pair(
            UserSummary(
                username = user.optString("username"),
                email = user.optString("email"),
                emailVerified = user.optBoolean("email_verified", false),
            ),
            MySummaryStats(
                allPhotos = stats.optInt("all_photos", 0),
                approvedPhotos = stats.optInt("approved_photos", 0),
                pendingPhotos = stats.optInt("pending_photos", 0),
                rejectedPhotos = stats.optInt("rejected_photos", 0),
                likedPhotos = stats.optInt("liked_photos", 0),
                unreadNotifications = stats.optInt("unread_notifications", 0),
            ),
        )
    }

    override fun getReviewItems(status: String): List<ReviewItem> {
        val items = openJsonArray(
            apiUri("photos/my.php")
                .buildUpon()
                .appendQueryParameter("status", status)
                .appendQueryParameter("page", "1")
                .appendQueryParameter("per_page", "30")
                .build()
                .toString(),
            requiresAuth = true,
        )
        return items.toReviewItems(status)
    }

    override fun getMyLikes(): List<PhotoItem> {
        return openJsonArray(
            apiUri("photos/likes.php")
                .buildUpon()
                .appendQueryParameter("page", "1")
                .appendQueryParameter("per_page", "30")
                .build()
                .toString(),
            requiresAuth = true,
        ).toPhotoItems()
    }

    override fun getDeviceSessions(): List<DeviceSession> {
        val items = openJsonArray(apiUri("auth/devices.php").toString(), requiresAuth = true)
        return buildList(items.length()) {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                add(
                    DeviceSession(
                        id = item.optString("id"),
                        deviceName = item.optString("deviceName").ifBlank { item.optString("device_name") },
                        loginTime = item.optString("createdAt").ifBlank { item.optString("created_at") },
                        ipAddress = item.optString("ipAddress").ifBlank { item.optString("ip_address") },
                        systemVersion = buildString {
                            append(item.optString("platform"))
                            val version = item.optString("systemVersion").ifBlank { item.optString("system_version") }
                            if (version.isNotBlank()) {
                                if (isNotEmpty()) append(" ")
                                append(version)
                            }
                        },
                        isCurrent = item.optBoolean("isCurrent", item.optBoolean("is_current", false)),
                    ),
                )
            }
        }
    }

    override fun getUserSummary(): UserSummary {
        return getMySummary().first
    }

    private fun openJson(
        url: String,
        method: String = "GET",
        requiresAuth: Boolean = false,
        formBody: Map<String, String> = emptyMap(),
        retryAfterRefresh: Boolean = true,
    ): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("Accept", "application/json")
            if (requiresAuth) {
                val token = sessionStore.read().accessToken
                if (token.isBlank()) {
                    throw IllegalStateException("Missing access token")
                }
                setRequestProperty("Authorization", "Bearer $token")
            }
            if (method == "POST") {
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                outputStream.use { stream ->
                    stream.write(formBody.toFormBody().toByteArray())
                }
            }
        }

        return connection.useResponse { statusCode, body ->
            if (requiresAuth && statusCode == 401 && retryAfterRefresh && refreshSession()) {
                return@useResponse openJson(url, method, requiresAuth, formBody, retryAfterRefresh = false)
            }
            if (statusCode !in 200..299) {
                throw IllegalStateException("HTTP $statusCode from $url")
            }
            val json = JSONObject(body)
            if (json.has("success") && !json.optBoolean("success", false)) {
                throw IllegalStateException(json.optString("error", "Request failed"))
            }
            json
        }
    }

    private fun openJsonArray(url: String, requiresAuth: Boolean = false): JSONArray {
        val root = openJson(url, requiresAuth = requiresAuth)
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
            author = optString("username").ifBlank { optString("author_name") }.ifBlank { optString("author") },
            airline = optString("airline"),
            aircraftModel = optString("aircraftModel").ifBlank { optString("aircraft_model") },
            registration = optString("registration").ifBlank { optString("registration_number") },
            location = optString("location").ifBlank { optString("shooting_location") }.ifBlank { optString("locationCode") }.ifBlank { optString("location_code") },
            camera = optString("camera").ifBlank { optString("cam") },
            lens = optString("lens").ifBlank { optString("lens_model") },
            createdAt = optString("createdAt").ifBlank { optString("created_at") },
            liked = optBoolean("liked", false),
            thumbUrl = normalizeUrl(optString("thumbUrl").ifBlank { optString("thumb_url") }),
            originalUrl = normalizeUrl(optString("originalUrl").ifBlank { optString("original_url") }),
            detailUrl = normalizeUrl(optString("detailUrl").ifBlank { optString("detail_url") }),
            shareUrl = normalizeUrl(optString("shareUrl").ifBlank { optString("share_url") }),
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
                    id = item.optString("key").ifBlank { item.optString("id") },
                    name = item.optString("name").ifBlank { item.optString("label") },
                    level = item.optString("level").ifBlank { "airport" },
                    photoCount = item.optInt("photoCount", item.optInt("photo_count", 0)),
                    locationCode = item.optString("key").ifBlank { item.optString("locationCode").ifBlank { item.optString("location_code") } },
                    latitude = item.optDouble("latitude").takeUnless { it.isNaN() },
                    longitude = item.optDouble("longitude").takeUnless { it.isNaN() },
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

    private fun mergeKeyword(keyword: String, author: String): String {
        if (author.isBlank() || author.toLongOrNull() != null) return keyword
        return listOf(keyword, author).filter { it.isNotBlank() }.joinToString(" ")
    }

    private fun apiUri(path: String): Uri = Uri.parse(baseUrl.trimEnd('/') + "/" + path.trimStart('/'))

    private fun String.blankToNull(): String? = if (isBlank()) null else this

    private fun normalizeUrl(url: String): String {
        if (url.isBlank()) return url
        return if (url.startsWith("http://") || url.startsWith("https://")) url else "https://www.syphotos.cn/${url.trimStart('/')}"
    }

    private fun Map<String, String>.toFormBody(): String {
        return entries.joinToString("&") { (key, value) ->
            "${Uri.encode(key)}=${Uri.encode(value)}"
        }
    }

    private fun refreshSession(): Boolean {
        val current = sessionStore.read()
        if (current.refreshToken.isBlank()) return false
        return runCatching {
            val json = openJson(
                url = apiUri("auth/refresh.php").toString(),
                method = "POST",
                formBody = mapOf("refresh_token" to current.refreshToken),
                retryAfterRefresh = false,
            )
            val user = json.optJSONObject("user") ?: JSONObject()
            val auth = json.optJSONObject("auth") ?: JSONObject()
            sessionStore.write(
                AuthSession(
                    accessToken = auth.optString("access_token"),
                    refreshToken = auth.optString("refresh_token"),
                    accessTokenExpiresAt = auth.optString("access_token_expires_at"),
                    refreshTokenExpiresAt = auth.optString("refresh_token_expires_at"),
                    username = user.optString("username"),
                    email = user.optString("email"),
                ),
            )
        }.isSuccess
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
