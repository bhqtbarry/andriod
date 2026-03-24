package cn.syphotos.android.data

import android.net.Uri
import cn.syphotos.android.BuildConfig
import cn.syphotos.android.model.CategoryCount
import cn.syphotos.android.model.MapCluster
import cn.syphotos.android.model.MySummaryStats
import cn.syphotos.android.model.AuthSession
import cn.syphotos.android.model.AirlineDirectoryItem
import cn.syphotos.android.model.AirlineTreeItem
import cn.syphotos.android.model.PagedResult
import cn.syphotos.android.model.PhotoFilter
import cn.syphotos.android.model.PhotoDetail
import cn.syphotos.android.model.PhotoItem
import cn.syphotos.android.model.ReviewItem
import cn.syphotos.android.model.SearchSuggestion
import cn.syphotos.android.model.UploadConfig
import cn.syphotos.android.model.UploadExifInfo
import cn.syphotos.android.model.DeviceSession
import cn.syphotos.android.model.UserSummary
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedWriter
import java.io.File
import java.io.OutputStreamWriter
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
        return getPhotosPage(filter = filter).items
    }

    override fun getPhotosPage(filter: PhotoFilter, page: Int, perPage: Int): PagedResult<PhotoItem> {
        val uri = apiUri("photos/feed.php").buildUpon().apply {
            appendQueryParameter("page", page.toString())
            appendQueryParameter("per_page", perPage.toString())
            appendIfNotBlank("keyword", mergeKeyword(filter.keyword, filter.author))
            filter.author.toLongOrNull()?.let { appendQueryParameter("userid", it.toString()) }
            appendIfNotBlank("airline", filter.airline)
            appendIfNotBlank("aircraft_model", filter.aircraftModel)
            appendIfNotBlank("cam", filter.camera)
            appendIfNotBlank("lens", filter.lens)
            appendIfNotBlank("registration_number", filter.registration.uppercase())
            appendIfNotBlank("iatacode", filter.locationCode.uppercase())
        }.build()
        val root = openJson(uri.toString())
        val items = when {
            root.has("items") -> root.getJSONArray("items")
            root.has("data") -> root.getJSONArray("data")
            else -> JSONArray()
        }
        return PagedResult(
            items = items.toPhotoItems(),
            page = root.optInt("page", page),
            perPage = root.optInt("per_page", perPage),
            total = root.optInt("total", items.length()),
            hasMore = root.optBoolean("has_more", false),
        )
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
            shootingTime = item.optString("shootingTime").ifBlank { item.optString("shooting_time") },
            focalLength = item.optString("focalLength").ifBlank { item.optString("focal_length") },
            iso = item.optString("iso"),
            aperture = item.optString("aperture"),
            shutter = item.optString("shutter"),
            score = item.optString("score"),
        )
    }

    override fun getCategoryCounts(): Pair<List<CategoryCount>, List<CategoryCount>> {
        val photos = getPhotos(PhotoFilter())
        return getCategoryCounts(photos)
    }

    override fun getAirlineDirectory(): List<AirlineDirectoryItem> {
        val items = openJsonArray(apiUri("categories/airlines.php").toString())
        return buildList(items.length()) {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                add(
                    AirlineDirectoryItem(
                        label = item.optString("label"),
                        aircraftCount = item.optInt("aircraft_count", 0),
                        photoCount = item.optInt("photo_count", 0),
                        href = normalizeUrl(item.optString("href")),
                        photoStatus = item.optString("photo_status"),
                    ),
                )
            }
        }
    }

    override fun getAirlineTypecodes(airline: String): List<AirlineTreeItem> {
        val root = openJson(
            webUri("airline.php").buildUpon()
                .appendQueryParameter("action", "children")
                .appendQueryParameter("level", "airline")
                .appendQueryParameter("airline", airline)
                .build()
                .toString(),
        )
        val items = root.optJSONArray("items") ?: JSONArray()
        return buildList(items.length()) {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                add(
                    AirlineTreeItem(
                        label = item.optString("label"),
                        aircraftCount = item.optInt("aircraft_count", 0),
                        photoCount = item.optInt("photo_count", 0),
                        level = item.optString("level").ifBlank { "typecode" },
                        airline = item.optString("airline").ifBlank { airline },
                        typecode = item.optString("label"),
                        photoStatus = item.optString("photo_status"),
                    ),
                )
            }
        }
    }

    override fun getAirlineRegistrations(airline: String, typecode: String): List<AirlineTreeItem> {
        val root = openJson(
            webUri("airline.php").buildUpon()
                .appendQueryParameter("action", "children")
                .appendQueryParameter("level", "typecode")
                .appendQueryParameter("airline", airline)
                .appendQueryParameter("typecode", typecode)
                .build()
                .toString(),
        )
        val items = root.optJSONArray("items") ?: JSONArray()
        return buildList(items.length()) {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                add(
                    AirlineTreeItem(
                        label = item.optString("label"),
                        photoCount = item.optInt("photo_count", 0),
                        level = item.optString("level").ifBlank { "registration" },
                        airline = airline,
                        typecode = typecode,
                        registration = item.optString("registration").ifBlank { item.optString("label") },
                        photoStatus = item.optString("photo_status"),
                    ),
                )
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
            appendQueryParameter("level", "airport")
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
        return UploadConfig(
            maxFileSizeMb = 45,
            minAspectRatio = "1:2",
            maxAspectRatio = "2:1",
            exifEnabled = true,
            watermarkEnabled = true,
            registrationOcrEnabled = true,
            uploadUrl = webUri("upload.php").toString(),
        )
    }

    override fun lookupAircraftByRegistration(registration: String): Pair<String, String>? {
        val root = openJson(
            webUri("api/plane-info.php").buildUpon()
                .appendQueryParameter("registration", registration.uppercase())
                .build()
                .toString(),
        )
        if (root.optString("status") != "success") {
            return null
        }
        val data = root.optJSONObject("data") ?: return null
        val model = data.optString("机型")
        val airline = data.optString("运营机构")
        if (model.isBlank() && airline.isBlank()) {
            return null
        }
        return model to airline
    }

    override fun extractUploadExif(file: File, originalName: String, mimeType: String): UploadExifInfo {
        val boundary = "----SyPhotosExif${System.currentTimeMillis()}"
        val url = webUri("srv/exif-service/public/").toString()
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            val cookieHeader = sessionStore.readCookieHeader()
            if (cookieHeader.isNotBlank()) {
                setRequestProperty("Cookie", cookieHeader)
            }
        }

        connection.outputStream.use { output ->
            val writer = BufferedWriter(OutputStreamWriter(output, Charsets.UTF_8))
            writer.append("--").append(boundary).append("\r\n")
            writer.append("""Content-Disposition: form-data; name="file"; filename="$originalName"""").append("\r\n")
            writer.append("Content-Type: ").append(mimeType).append("\r\n\r\n")
            writer.flush()
            file.inputStream().use { input -> input.copyTo(output) }
            output.write("\r\n".toByteArray())
            writer.append("--").append(boundary).append("--").append("\r\n")
            writer.flush()
        }

        return connection.useResponse { statusCode, body, _ ->
            if (statusCode !in 200..299) {
                throw IllegalStateException("HTTP $statusCode from $url")
            }
            val payloadObject = runCatching { JSONObject(body) }.getOrNull()
            val payloadArray = if (payloadObject == null) runCatching { JSONArray(body) }.getOrNull() else null
            if (payloadObject?.has("error") == true) {
                throw IllegalStateException(payloadObject.optString("error"))
            }
            val data: JSONObject = when {
                payloadObject?.optJSONArray("data") != null ->
                    payloadObject.optJSONArray("data")?.optJSONObject(0)
                payloadObject?.optJSONArray("items") != null ->
                    payloadObject.optJSONArray("items")?.optJSONObject(0)
                payloadArray != null ->
                    payloadArray.optJSONObject(0)
                else -> payloadObject
            } ?: throw IllegalStateException("EXIF 服务返回格式无效")

            if (data.optString("error").isNotBlank()) {
                throw IllegalStateException(data.optString("error"))
            }

            UploadExifInfo(
                cameraModel = data.optString("Model"),
                lensModel = data.optString("LensID"),
                focalLength = data.optString("FocalLength"),
                iso = data.optString("ISO"),
                aperture = data.optString("Aperture"),
                shutterSpeed = normalizeShutter(data.optString("ShutterSpeed")),
                nearestAirport = data.optString("NearestAirport"),
                dateTimeOriginal = data.optString("DateTimeOriginal"),
            )
        }
    }

    fun uploadPhoto(
        file: File,
        originalName: String,
        mimeType: String,
        fields: Map<String, String>,
    ): String {
        val response = submitMultipartJson(
            url = apiUri("photos/upload.php").toString(),
            fields = fields,
            fileField = "photo",
            file = file,
            originalName = originalName,
            mimeType = mimeType,
            requiresAuth = true,
        )
        if (response.has("success") && !response.optBoolean("success", false)) {
            throw IllegalStateException(response.optString("error", "Upload failed"))
        }
        return response.optString("message").ifBlank { "图片上传成功，等待审核" }
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
                .appendQueryParameter("per_page", "100")
                .build()
                .toString(),
            requiresAuth = true,
        )
        return items.toReviewItems(status)
    }

    override fun deleteMyPhoto(photoId: Long, titleConfirm: String) {
        openJson(
            url = apiUri("photos/delete.php").toString(),
            method = "POST",
            requiresAuth = true,
            formBody = mapOf(
                "photo_id" to photoId.toString(),
                "title_confirm" to titleConfirm,
            ),
        )
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
                var token = sessionStore.read().accessToken
                if (token.isBlank() && retryAfterRefresh && refreshSession()) {
                    token = sessionStore.read().accessToken
                }
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

        return connection.useResponse { statusCode, body, _ ->
            if (requiresAuth && statusCode == 401 && retryAfterRefresh && refreshSession()) {
                return@useResponse openJson(url, method, requiresAuth, formBody, retryAfterRefresh = false)
            }
            if (statusCode !in 200..299) {
                throw IllegalStateException(extractErrorMessage(body) ?: "HTTP $statusCode from $url")
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
            status = optString("status"),
            rejectionReason = optString("rejectionReason").ifBlank { optString("rejection_reason") }.blankToNull(),
            adminComment = optString("adminComment").ifBlank { optString("admin_comment") }.blankToNull(),
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

    private fun webUri(path: String): Uri {
        val root = baseUrl.substringBefore("/api/app/v1").trimEnd('/')
        return Uri.parse(root + "/" + path.trimStart('/'))
    }

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

    private fun extractErrorMessage(body: String): String? {
        if (body.isBlank()) return null
        return runCatching {
            val json = JSONObject(body)
            json.optString("error")
                .ifBlank { json.optString("message") }
                .ifBlank { null }
        }.getOrNull()
    }

    private fun normalizeShutter(value: String): String {
        if (value.isBlank()) return ""
        if (value.contains('/')) return value
        val numeric = value.toDoubleOrNull() ?: return value
        if (numeric <= 0.0) return value
        return if (numeric < 1.0) {
            "1/${kotlin.math.round(1.0 / numeric).toInt()}"
        } else {
            "%.1f".format(numeric).trimEnd('0').trimEnd('.')
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

    private fun submitMultipartJson(
        url: String,
        fields: Map<String, String>,
        fileField: String,
        file: File,
        originalName: String,
        mimeType: String,
        requiresAuth: Boolean = false,
    ): JSONObject {
        val boundary = "----SyPhotosBoundary${System.currentTimeMillis()}"
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            if (requiresAuth) {
                var token = sessionStore.read().accessToken
                if (token.isBlank() && refreshSession()) {
                    token = sessionStore.read().accessToken
                }
                if (token.isBlank()) {
                    throw IllegalStateException("Missing access token")
                }
                setRequestProperty("Authorization", "Bearer $token")
            }
        }

        connection.outputStream.use { output ->
            val writer = BufferedWriter(OutputStreamWriter(output, Charsets.UTF_8))
            fields.forEach { (name, value) ->
                writer.append("--").append(boundary).append("\r\n")
                writer.append("""Content-Disposition: form-data; name="$name"""").append("\r\n\r\n")
                writer.append(value).append("\r\n")
            }
            writer.append("--").append(boundary).append("\r\n")
            writer.append(
                """Content-Disposition: form-data; name="$fileField"; filename="$originalName"""",
            ).append("\r\n")
            writer.append("Content-Type: ").append(mimeType).append("\r\n\r\n")
            writer.flush()
            file.inputStream().use { input -> input.copyTo(output) }
            output.write("\r\n".toByteArray())
            writer.append("--").append(boundary).append("--").append("\r\n")
            writer.flush()
        }

        return connection.useResponse { statusCode, body, _ ->
            if (statusCode !in 200..299) {
                throw IllegalStateException(extractErrorMessage(body) ?: "HTTP $statusCode from $url")
            }
            JSONObject(body)
        }
    }

    private inline fun <T> HttpURLConnection.useResponse(block: (Int, String, Map<String, List<String>>) -> T): T {
        return try {
            val stream = if (responseCode in 200..299) inputStream else errorStream
            val body = stream?.use { BufferedInputStream(it).reader().readText() }.orEmpty()
            headerFields["Set-Cookie"]
                ?.map { it.substringBefore(";").trim() }
                ?.filter { it.contains("=") }
                ?.takeIf { it.isNotEmpty() }
                ?.joinToString("; ")
                ?.let(sessionStore::mergeCookieHeader)
            block(responseCode, body, headerFields)
        } finally {
            disconnect()
        }
    }
}
