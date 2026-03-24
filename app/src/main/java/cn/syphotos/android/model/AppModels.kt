package cn.syphotos.android.model

data class PhotoFilter(
    val keyword: String = "",
    val author: String = "",
    val airline: String = "",
    val aircraftModel: String = "",
    val camera: String = "",
    val lens: String = "",
    val registration: String = "",
    val locationCode: String = "",
)

data class PhotoItem(
    val id: Long,
    val title: String,
    val author: String,
    val airline: String,
    val aircraftModel: String,
    val registration: String,
    val location: String,
    val camera: String,
    val lens: String,
    val createdAt: String,
    val liked: Boolean,
    val thumbUrl: String = "",
    val originalUrl: String = "",
    val detailUrl: String = "",
    val shareUrl: String = "",
    val status: String = "",
    val rejectionReason: String? = null,
    val adminComment: String? = null,
)

data class PhotoDetail(
    val photo: PhotoItem,
    val originalUrl: String = "",
    val shareUrl: String = "",
    val description: String = "",
    val shootingTime: String = "",
    val focalLength: String = "",
    val iso: String = "",
    val aperture: String = "",
    val shutter: String = "",
    val score: String = "",
)

data class CategoryCount(
    val name: String,
    val count: Int,
)

data class AirlineDirectoryItem(
    val label: String,
    val aircraftCount: Int,
    val photoCount: Int,
    val href: String,
    val photoStatus: String,
)

data class AirlineTreeItem(
    val label: String,
    val aircraftCount: Int = 0,
    val photoCount: Int = 0,
    val level: String,
    val airline: String = "",
    val typecode: String = "",
    val registration: String = "",
    val photoStatus: String = "",
)

data class MapCluster(
    val id: String,
    val name: String,
    val level: String,
    val photoCount: Int,
    val locationCode: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

data class SearchSuggestion(
    val value: String,
    val label: String,
    val count: Int,
)

data class PagedResult<T>(
    val items: List<T>,
    val page: Int,
    val perPage: Int,
    val total: Int,
    val hasMore: Boolean,
)

data class ReviewItem(
    val photo: PhotoItem,
    val status: String,
    val rejectionReason: String? = null,
    val adminComment: String? = null,
)

data class DeviceSession(
    val id: String,
    val deviceName: String,
    val loginTime: String,
    val ipAddress: String,
    val systemVersion: String,
    val isCurrent: Boolean,
)

data class UserSummary(
    val username: String,
    val email: String,
    val emailVerified: Boolean,
)

data class AuthSession(
    val accessToken: String = "",
    val refreshToken: String = "",
    val accessTokenExpiresAt: String = "",
    val refreshTokenExpiresAt: String = "",
    val username: String = "",
    val email: String = "",
) {
    val isLoggedIn: Boolean get() = accessToken.isNotBlank() && refreshToken.isNotBlank()
}

data class MySummaryStats(
    val allPhotos: Int = 0,
    val approvedPhotos: Int = 0,
    val pendingPhotos: Int = 0,
    val rejectedPhotos: Int = 0,
    val likedPhotos: Int = 0,
    val unreadNotifications: Int = 0,
)

data class ViewerPhotoState(
    val photoId: Long,
    val originalUrl: String = "",
    val thumbUrl: String = "",
    val isLoading: Boolean = false,
)

data class UploadConfig(
    val maxFileSizeMb: Int = 40,
    val minAspectRatio: String = "1:2",
    val maxAspectRatio: String = "2:1",
    val exifEnabled: Boolean = true,
    val watermarkEnabled: Boolean = true,
    val registrationOcrEnabled: Boolean = true,
    val uploadUrl: String = "",
)

data class UploadExifInfo(
    val cameraModel: String = "",
    val lensModel: String = "",
    val focalLength: String = "",
    val iso: String = "",
    val aperture: String = "",
    val shutterSpeed: String = "",
    val nearestAirport: String = "",
    val dateTimeOriginal: String = "",
)
