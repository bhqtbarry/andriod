package cn.syphotos.android.data

import cn.syphotos.android.model.CategoryCount
import cn.syphotos.android.model.DeviceSession
import cn.syphotos.android.model.MapCluster
import cn.syphotos.android.model.AuthSession
import cn.syphotos.android.model.AirlineDirectoryItem
import cn.syphotos.android.model.AirlineTreeItem
import cn.syphotos.android.model.PhotoFilter
import cn.syphotos.android.model.PhotoDetail
import cn.syphotos.android.model.PhotoItem
import cn.syphotos.android.model.ReviewItem
import cn.syphotos.android.model.MySummaryStats
import cn.syphotos.android.model.PagedResult
import cn.syphotos.android.model.SearchSuggestion
import cn.syphotos.android.model.UploadConfig
import cn.syphotos.android.model.UploadExifInfo
import cn.syphotos.android.model.UserSummary

interface SyPhotosRepository {
    fun getAuthSession(): AuthSession

    fun login(login: String, password: String): AuthSession

    fun logout()

    fun getPhotos(filter: PhotoFilter = PhotoFilter()): List<PhotoItem>

    fun getPhotosPage(filter: PhotoFilter = PhotoFilter(), page: Int = 1, perPage: Int = 30): PagedResult<PhotoItem>

    fun getPhotoDetail(photoId: Long): PhotoDetail

    fun getCategoryCounts(): Pair<List<CategoryCount>, List<CategoryCount>>

    fun getAirlineDirectory(): List<AirlineDirectoryItem>

    fun getAirlineTypecodes(airline: String): List<AirlineTreeItem>

    fun getAirlineRegistrations(airline: String, typecode: String): List<AirlineTreeItem>

    fun getSuggestions(field: String, query: String, filter: PhotoFilter = PhotoFilter()): List<SearchSuggestion>

    fun getMapClusters(filter: PhotoFilter = PhotoFilter()): List<MapCluster>

    fun getUploadConfig(): UploadConfig

    fun lookupAircraftByRegistration(registration: String): Pair<String, String>?

    fun extractUploadExif(file: java.io.File, originalName: String, mimeType: String): UploadExifInfo

    fun getMySummary(): Pair<UserSummary, MySummaryStats>

    fun getReviewItems(status: String): List<ReviewItem>

    fun deleteMyPhoto(photoId: Long, titleConfirm: String)

    fun getMyLikes(): List<PhotoItem>

    fun getDeviceSessions(): List<DeviceSession>

    fun getUserSummary(): UserSummary
}
