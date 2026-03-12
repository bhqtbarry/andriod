package cn.syphotos.android.data

import cn.syphotos.android.model.CategoryCount
import cn.syphotos.android.model.DeviceSession
import cn.syphotos.android.model.MapCluster
import cn.syphotos.android.model.AuthSession
import cn.syphotos.android.model.PhotoFilter
import cn.syphotos.android.model.PhotoDetail
import cn.syphotos.android.model.PhotoItem
import cn.syphotos.android.model.ReviewItem
import cn.syphotos.android.model.SearchSuggestion
import cn.syphotos.android.model.MySummaryStats
import cn.syphotos.android.model.UploadConfig
import cn.syphotos.android.model.UserSummary

class FakeSyPhotosRepository : SyPhotosRepository {
    private val photos = List(18) { index ->
        PhotoItem(
            id = index + 1L,
            title = "B-6${100 + index} at Jakarta",
            author = if (index % 2 == 0) "Barry" else "Alex",
            airline = if (index % 3 == 0) "Garuda Indonesia" else "Lion Air",
            aircraftModel = if (index % 2 == 0) "Boeing 737-800" else "Airbus A320",
            registration = "PK-L${index + 10}",
            location = if (index % 2 == 0) "CGK" else "SUB",
            camera = "Sony A7 IV",
            lens = "70-200mm F2.8",
            createdAt = "2026-03-${(index % 9) + 1}",
            liked = index % 4 == 0,
            thumbUrl = "https://picsum.photos/seed/syphotos-thumb-${index + 1}/600/300",
            originalUrl = "https://picsum.photos/seed/syphotos-original-${index + 1}/1600/800",
            detailUrl = "https://www.syphotos.cn/photo_detail.php?id=${index + 1}",
            shareUrl = "https://www.syphotos.cn/photo_detail.php?id=${index + 1}",
        )
    }

    override fun getAuthSession(): AuthSession = AuthSession(
        accessToken = "fake-access",
        refreshToken = "fake-refresh",
        username = "barry",
        email = "barry@syphotos.cn",
    )

    override fun login(login: String, password: String): AuthSession = getAuthSession()

    override fun logout() = Unit

    override fun getPhotos(filter: PhotoFilter): List<PhotoItem> {
        return photos.filter { photo ->
            listOf(
                filter.keyword to listOf(photo.title, photo.location),
                filter.author to listOf(photo.author),
                filter.airline to listOf(photo.airline),
                filter.aircraftModel to listOf(photo.aircraftModel),
                filter.camera to listOf(photo.camera),
                filter.lens to listOf(photo.lens),
                filter.registration to listOf(photo.registration),
                filter.locationCode to listOf(photo.location),
            ).all { (query, fields) ->
                query.isBlank() || fields.any { it.contains(query, ignoreCase = true) }
            }
        }
    }

    override fun getPhotoDetail(photoId: Long): PhotoDetail {
        val photo = photos.first { it.id == photoId }
        return PhotoDetail(
            photo = photo,
            originalUrl = photo.originalUrl,
            shareUrl = photo.shareUrl,
            description = "${photo.airline} ${photo.aircraftModel} at ${photo.location}",
        )
    }

    override fun getCategoryCounts(): Pair<List<CategoryCount>, List<CategoryCount>> {
        return Pair(
            listOf(
                CategoryCount("Garuda Indonesia", 42),
                CategoryCount("Lion Air", 30),
                CategoryCount("Batik Air", 16),
            ),
            listOf(
                CategoryCount("Boeing 737-800", 48),
                CategoryCount("Airbus A320", 27),
                CategoryCount("ATR 72-600", 13),
            ),
        )
    }

    override fun getSuggestions(field: String, query: String, filter: PhotoFilter): List<SearchSuggestion> {
        val candidates = when (field) {
            "userid" -> getPhotos(filter).groupBy { it.author }
            "airline" -> getPhotos(filter).groupBy { it.airline }
            "aircraft_model" -> getPhotos(filter).groupBy { it.aircraftModel }
            "cam" -> getPhotos(filter).groupBy { it.camera }
            "lens" -> getPhotos(filter).groupBy { it.lens }
            "registration_number" -> getPhotos(filter).groupBy { it.registration }
            "iatacode" -> getPhotos(filter).groupBy { it.location }
            else -> emptyMap()
        }
        return candidates.entries
            .map { (label, items) -> SearchSuggestion(value = label, label = label, count = items.size) }
            .filter { it.label.contains(query, ignoreCase = true) }
            .take(5)
    }

    override fun getMapClusters(filter: PhotoFilter): List<MapCluster> {
        return getPhotos(filter)
            .groupBy { it.location }
            .map { (location, items) ->
                MapCluster(
                    id = location,
                    name = location,
                    level = "airport",
                    photoCount = items.size,
                    locationCode = location,
                    latitude = when (location) {
                        "CGK" -> -6.1256
                        "SUB" -> -7.3798
                        else -> 0.0
                    },
                    longitude = when (location) {
                        "CGK" -> 106.6558
                        "SUB" -> 112.7878
                        else -> 0.0
                    },
                )
            }
            .sortedByDescending { it.photoCount }
    }

    override fun getUploadConfig(): UploadConfig = UploadConfig()

    override fun getMySummary(): Pair<UserSummary, MySummaryStats> {
        return Pair(
            getUserSummary(),
            MySummaryStats(
                allPhotos = photos.size,
                approvedPhotos = photos.size - 2,
                pendingPhotos = 1,
                rejectedPhotos = 1,
                likedPhotos = photos.count { it.liked },
                unreadNotifications = 0,
            ),
        )
    }

    override fun getReviewItems(status: String): List<ReviewItem> {
        return listOf(
            ReviewItem(
                photo = photos.first(),
                status = status,
                rejectionReason = if (status == "rejected") "Horizon tilt" else null,
                adminComment = if (status == "rejected") "Please level the frame and reupload." else null,
            ),
        )
    }

    override fun getMyLikes(): List<PhotoItem> = photos.filter { it.liked }

    override fun getDeviceSessions(): List<DeviceSession> {
        return listOf(
            DeviceSession("current", "Pixel 8 Pro", "2026-03-12 10:45", "36.85.12.1", "Android 15", true),
            DeviceSession("old-1", "Galaxy S23", "2026-03-10 20:15", "36.90.11.2", "Android 14", false),
        )
    }

    override fun getUserSummary(): UserSummary {
        return UserSummary(
            username = "barry",
            email = "barry@syphotos.cn",
            emailVerified = true,
        )
    }
}
