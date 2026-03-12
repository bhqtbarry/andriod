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
)

data class CategoryCount(
    val name: String,
    val count: Int,
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

