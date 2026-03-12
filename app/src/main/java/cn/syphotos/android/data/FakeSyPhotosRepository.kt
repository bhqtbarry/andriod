package cn.syphotos.android.data

import cn.syphotos.android.model.CategoryCount
import cn.syphotos.android.model.DeviceSession
import cn.syphotos.android.model.PhotoFilter
import cn.syphotos.android.model.PhotoItem
import cn.syphotos.android.model.ReviewItem
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
        )
    }

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
