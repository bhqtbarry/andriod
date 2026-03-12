package cn.syphotos.android.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.syphotos.android.data.FakeSyPhotosRepository
import cn.syphotos.android.data.SyPhotosRepository
import cn.syphotos.android.data.WebSyPhotosRepository
import cn.syphotos.android.model.CategoryCount
import cn.syphotos.android.model.DeviceSession
import cn.syphotos.android.model.MapCluster
import cn.syphotos.android.model.PhotoDetail
import cn.syphotos.android.model.PhotoFilter
import cn.syphotos.android.model.PhotoItem
import cn.syphotos.android.model.ReviewItem
import cn.syphotos.android.model.UploadConfig
import cn.syphotos.android.model.UserSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(
    private val fallbackRepository: SyPhotosRepository = FakeSyPhotosRepository(),
    private val webRepository: SyPhotosRepository = WebSyPhotosRepository(),
) : ViewModel() {
    var uiState by mutableStateOf(buildFallbackState())
        private set

    init {
        refreshAll()
    }

    fun updateFilter(filter: PhotoFilter) {
        uiState = uiState.copy(photoFilter = filter, feedState = uiState.feedState.copy(isLoading = true))
        refreshFeed(filter)
        refreshMap(filter)
    }

    fun toggleLike(photoId: Long) {
        val updatedPhotos = uiState.photos.map {
            if (it.id == photoId) it.copy(liked = !it.liked) else it
        }
        val updatedViewer = uiState.viewerState.detail?.takeIf { it.photo.id == photoId }?.copy(
            photo = updatedPhotos.firstOrNull { photo -> photo.id == photoId } ?: uiState.viewerState.detail.photo,
        )
        uiState = uiState.copy(
            photos = updatedPhotos,
            myState = uiState.myState.copy(likedPhotos = updatedPhotos.filter { it.liked }),
            viewerState = uiState.viewerState.copy(detail = updatedViewer),
        )
    }

    fun prefetchPhotoDetail(photoId: Long) {
        uiState = uiState.copy(
            viewerState = uiState.viewerState.copy(
                detail = uiState.viewerState.detail ?: PhotoDetail(photo = findPhoto(photoId)),
                isLoading = true,
                errorMessage = null,
            ),
        )
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { webRepository.getPhotoDetail(photoId) }
            }.onSuccess { detail ->
                uiState = uiState.copy(
                    viewerState = ViewerUiState(detail = detail, isLoading = false),
                )
            }.onFailure { error ->
                uiState = uiState.copy(
                    viewerState = ViewerUiState(
                        detail = uiState.viewerState.detail ?: fallbackRepository.getPhotoDetail(photoId),
                        isLoading = false,
                        errorMessage = "Photo detail unavailable: ${error.message}",
                    ),
                )
            }
        }
    }

    fun findPhoto(photoId: Long): PhotoItem = uiState.photos.firstOrNull { it.id == photoId }
        ?: uiState.viewerState.detail?.photo?.takeIf { it.id == photoId }
        ?: fallbackRepository.getPhotos().first { it.id == photoId }

    private fun refreshAll() {
        refreshFeed(uiState.photoFilter)
        refreshMap(uiState.photoFilter)
        refreshUpload()
        refreshMy()
    }

    private fun refreshFeed(filter: PhotoFilter) {
        uiState = uiState.copy(
            photoFilter = filter,
            feedState = uiState.feedState.copy(isLoading = true, errorMessage = null),
        )
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val photos = webRepository.getPhotos(filter)
                    val categories = fallbackRepository.getCategoryCounts()
                    Pair(photos, categories)
                }
            }.onSuccess { (photos, categories) ->
                uiState = uiState.copy(
                    photoFilter = filter,
                    photos = photos,
                    categoryState = CategoryUiState(
                        airlines = categories.first,
                        models = categories.second,
                    ),
                    feedState = FeedUiState(),
                )
            }.onFailure { error ->
                val fallbackPhotos = fallbackRepository.getPhotos(filter)
                uiState = uiState.copy(
                    photoFilter = filter,
                    photos = fallbackPhotos,
                    feedState = FeedUiState(
                        isLoading = false,
                        errorMessage = "Photo feed unavailable: ${error.message}",
                        usingFallbackData = true,
                    ),
                )
            }
        }
    }

    private fun refreshMap(filter: PhotoFilter) {
        uiState = uiState.copy(mapState = uiState.mapState.copy(isLoading = true, errorMessage = null))
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { webRepository.getMapClusters(filter) }
            }.onSuccess { clusters ->
                uiState = uiState.copy(mapState = MapUiState(clusters = clusters, isLoading = false))
            }.onFailure { error ->
                uiState = uiState.copy(
                    mapState = MapUiState(
                        clusters = fallbackRepository.getMapClusters(filter),
                        isLoading = false,
                        errorMessage = "Map data unavailable: ${error.message}",
                    ),
                )
            }
        }
    }

    private fun refreshUpload() {
        uiState = uiState.copy(uploadState = uiState.uploadState.copy(isLoading = true, errorMessage = null))
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { webRepository.getUploadConfig() }
            }.onSuccess { config ->
                uiState = uiState.copy(
                    uploadState = uiState.uploadState.copy(
                        isLoading = false,
                        config = config,
                        errorMessage = null,
                    ),
                )
            }.onFailure { error ->
                uiState = uiState.copy(
                    uploadState = uiState.uploadState.copy(
                        isLoading = false,
                        config = fallbackRepository.getUploadConfig(),
                        errorMessage = "Upload config unavailable: ${error.message}",
                    ),
                )
            }
        }
    }

    private fun refreshMy() {
        uiState = uiState.copy(myState = uiState.myState.copy(isLoading = true, errorMessage = null))
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    MyUiState(
                        user = webRepository.getUserSummary(),
                        works = webRepository.getPhotos(),
                        likedPhotos = webRepository.getMyLikes(),
                        pending = webRepository.getReviewItems("pending"),
                        rejected = webRepository.getReviewItems("rejected"),
                        sessions = webRepository.getDeviceSessions(),
                    )
                }
            }.onSuccess { remoteState ->
                uiState = uiState.copy(myState = remoteState.copy(isLoading = false))
            }.onFailure { error ->
                uiState = uiState.copy(
                    myState = buildFallbackMyState().copy(
                        isLoading = false,
                        errorMessage = "My page data unavailable: ${error.message}",
                    ),
                )
            }
        }
    }

    private fun buildFallbackState(): AppUiState {
        val photos = fallbackRepository.getPhotos()
        val categories = fallbackRepository.getCategoryCounts()
        return AppUiState(
            photos = photos,
            categoryState = CategoryUiState(
                airlines = categories.first,
                models = categories.second,
            ),
            mapState = MapUiState(clusters = fallbackRepository.getMapClusters()),
            uploadState = UploadUiState(config = fallbackRepository.getUploadConfig()),
            myState = buildFallbackMyState(),
        )
    }

    private fun buildFallbackMyState(): MyUiState {
        return MyUiState(
            user = fallbackRepository.getUserSummary(),
            works = fallbackRepository.getPhotos(),
            likedPhotos = fallbackRepository.getMyLikes(),
            pending = fallbackRepository.getReviewItems("pending"),
            rejected = fallbackRepository.getReviewItems("rejected"),
            sessions = fallbackRepository.getDeviceSessions(),
        )
    }
}

data class AppUiState(
    val photoFilter: PhotoFilter = PhotoFilter(),
    val photos: List<PhotoItem> = emptyList(),
    val feedState: FeedUiState = FeedUiState(),
    val categoryState: CategoryUiState = CategoryUiState(),
    val mapState: MapUiState = MapUiState(),
    val uploadState: UploadUiState = UploadUiState(),
    val myState: MyUiState = MyUiState(),
    val viewerState: ViewerUiState = ViewerUiState(),
)

data class FeedUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val usingFallbackData: Boolean = false,
)

data class CategoryUiState(
    val airlines: List<CategoryCount> = emptyList(),
    val models: List<CategoryCount> = emptyList(),
)

data class MapUiState(
    val clusters: List<MapCluster> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

data class UploadUiState(
    val fileName: String = "",
    val progress: Float = 0.42f,
    val config: UploadConfig = UploadConfig(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

data class MyUiState(
    val user: UserSummary = UserSummary("", "", false),
    val works: List<PhotoItem> = emptyList(),
    val likedPhotos: List<PhotoItem> = emptyList(),
    val pending: List<ReviewItem> = emptyList(),
    val rejected: List<ReviewItem> = emptyList(),
    val sessions: List<DeviceSession> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

data class ViewerUiState(
    val detail: PhotoDetail? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
