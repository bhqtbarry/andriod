package cn.syphotos.android.data

import cn.syphotos.android.model.CategoryCount
import cn.syphotos.android.model.DeviceSession
import cn.syphotos.android.model.MapCluster
import cn.syphotos.android.model.AuthSession
import cn.syphotos.android.model.PhotoFilter
import cn.syphotos.android.model.PhotoDetail
import cn.syphotos.android.model.PhotoItem
import cn.syphotos.android.model.ReviewItem
import cn.syphotos.android.model.MySummaryStats
import cn.syphotos.android.model.UploadConfig
import cn.syphotos.android.model.UserSummary

interface SyPhotosRepository {
    fun getAuthSession(): AuthSession

    fun login(login: String, password: String): AuthSession

    fun logout()

    fun getPhotos(filter: PhotoFilter = PhotoFilter()): List<PhotoItem>

    fun getPhotoDetail(photoId: Long): PhotoDetail

    fun getCategoryCounts(): Pair<List<CategoryCount>, List<CategoryCount>>

    fun getMapClusters(filter: PhotoFilter = PhotoFilter()): List<MapCluster>

    fun getUploadConfig(): UploadConfig

    fun getMySummary(): Pair<UserSummary, MySummaryStats>

    fun getReviewItems(status: String): List<ReviewItem>

    fun getMyLikes(): List<PhotoItem>

    fun getDeviceSessions(): List<DeviceSession>

    fun getUserSummary(): UserSummary
}
