package cn.syphotos.android.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import cn.syphotos.android.data.FakeSyPhotosRepository
import cn.syphotos.android.data.SyPhotosRepository
import cn.syphotos.android.data.WebSyPhotosRepository
import androidx.lifecycle.ViewModel
import cn.syphotos.android.model.CategoryCount
import cn.syphotos.android.model.DeviceSession
import cn.syphotos.android.model.PhotoFilter
import cn.syphotos.android.model.PhotoItem
import cn.syphotos.android.model.ReviewItem
import cn.syphotos.android.model.UserSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(
    private val fallbackRepository: SyPhotosRepository = FakeSyPhotosRepository(),
    private val webRepository: WebSyPhotosRepository = WebSyPhotosRepository(),
) : ViewModel() {
    var uiState by mutableStateOf(
        buildState(
            repository = fallbackRepository,
            filter = PhotoFilter(),
            isLoading = true,
        ),
    )
        private set

    init {
        refresh()
    }

    fun updateFilter(filter: PhotoFilter) {
        uiState = uiState.copy(photoFilter = filter)
        refresh(filter)
    }

    fun toggleLike(photoId: Long) {
        val updatedPhotos = uiState.photos.map {
            if (it.id == photoId) it.copy(liked = !it.liked) else it
        }
        uiState = uiState.copy(
            photos = updatedPhotos,
            myState = uiState.myState.copy(likedPhotos = updatedPhotos.filter { it.liked }),
        )
    }

    fun findPhoto(photoId: Long): PhotoItem = uiState.photos.firstOrNull { it.id == photoId }
        ?: fallbackRepository.getPhotos().first { it.id == photoId }

    private fun refresh(filter: PhotoFilter = uiState.photoFilter) {
        uiState = uiState.copy(
            photoFilter = filter,
            isLoading = true,
            errorMessage = null,
        )

        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val photos = webRepository.getPhotos(filter)
                    val categories = webRepository.getCategoryCounts(photos)
                    buildStateFromRemote(
                        filter = filter,
                        photos = photos,
                        categories = categories,
                    )
                }
            }.onSuccess { remoteState ->
                uiState = remoteState.copy(
                    isLoading = false,
                    usingFallbackData = false,
                    errorMessage = null,
                )
            }.onFailure { error ->
                uiState = buildState(
                    repository = fallbackRepository,
                    filter = filter,
                    isLoading = false,
                    errorMessage = "Web service unavailable: ${error.message}",
                    usingFallbackData = true,
                )
            }
        }
    }

    private fun buildState(
        repository: SyPhotosRepository,
        filter: PhotoFilter,
        isLoading: Boolean = false,
        errorMessage: String? = null,
        usingFallbackData: Boolean = false,
    ): AppUiState {
        val photos = repository.getPhotos(filter)
        val categories = repository.getCategoryCounts()
        return AppUiState(
            photoFilter = filter,
            photos = photos,
            isLoading = isLoading,
            errorMessage = errorMessage,
            usingFallbackData = usingFallbackData,
            categoryState = CategoryUiState(
                airlines = categories.first,
                models = categories.second,
            ),
            uploadDraft = UploadDraftUiState(),
            myState = MyUiState(
                user = repository.getUserSummary(),
                works = photos,
                likedPhotos = repository.getMyLikes(),
                pending = repository.getReviewItems("pending"),
                rejected = repository.getReviewItems("rejected"),
                sessions = repository.getDeviceSessions(),
            ),
        )
    }

    private fun buildStateFromRemote(
        filter: PhotoFilter,
        photos: List<PhotoItem>,
        categories: Pair<List<CategoryCount>, List<CategoryCount>>,
    ): AppUiState {
        return AppUiState(
            photoFilter = filter,
            photos = photos,
            categoryState = CategoryUiState(
                airlines = categories.first,
                models = categories.second,
            ),
            uploadDraft = UploadDraftUiState(),
            myState = MyUiState(
                user = fallbackRepository.getUserSummary(),
                works = photos,
                likedPhotos = photos.filter { it.liked },
                pending = fallbackRepository.getReviewItems("pending"),
                rejected = fallbackRepository.getReviewItems("rejected"),
                sessions = fallbackRepository.getDeviceSessions(),
            ),
        )
    }
}

data class AppUiState(
    val photoFilter: PhotoFilter = PhotoFilter(),
    val photos: List<PhotoItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val usingFallbackData: Boolean = false,
    val categoryState: CategoryUiState = CategoryUiState(),
    val uploadDraft: UploadDraftUiState = UploadDraftUiState(),
    val myState: MyUiState = MyUiState(),
)

data class CategoryUiState(
    val airlines: List<CategoryCount> = emptyList(),
    val models: List<CategoryCount> = emptyList(),
)

data class UploadDraftUiState(
    val fileName: String = "",
    val progress: Float = 0.42f,
    val exifEnabled: Boolean = true,
    val watermarkEnabled: Boolean = true,
    val registrationOcrEnabled: Boolean = true,
)

data class MyUiState(
    val user: UserSummary = UserSummary("", "", false),
    val works: List<PhotoItem> = emptyList(),
    val likedPhotos: List<PhotoItem> = emptyList(),
    val pending: List<ReviewItem> = emptyList(),
    val rejected: List<ReviewItem> = emptyList(),
    val sessions: List<DeviceSession> = emptyList(),
)
