package cn.syphotos.android.data

import cn.syphotos.android.model.CategoryCount
import cn.syphotos.android.model.DeviceSession
import cn.syphotos.android.model.PhotoFilter
import cn.syphotos.android.model.PhotoItem
import cn.syphotos.android.model.ReviewItem
import cn.syphotos.android.model.UserSummary

interface SyPhotosRepository {
    fun getPhotos(filter: PhotoFilter = PhotoFilter()): List<PhotoItem>

    fun getCategoryCounts(): Pair<List<CategoryCount>, List<CategoryCount>>

    fun getReviewItems(status: String): List<ReviewItem>

    fun getMyLikes(): List<PhotoItem>

    fun getDeviceSessions(): List<DeviceSession>

    fun getUserSummary(): UserSummary
}
