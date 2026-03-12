package cn.syphotos.android.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import cn.syphotos.android.data.FakeSyPhotosRepository
import cn.syphotos.android.model.CategoryCount
import cn.syphotos.android.model.DeviceSession
import cn.syphotos.android.model.PhotoFilter
import cn.syphotos.android.model.PhotoItem
import cn.syphotos.android.model.ReviewItem
import cn.syphotos.android.model.UserSummary

class AppViewModel(
    private val repository: FakeSyPhotosRepository = FakeSyPhotosRepository(),
) : ViewModel() {
    var uiState by mutableStateOf(buildState(PhotoFilter()))
        private set

    fun updateFilter(filter: PhotoFilter) {
        uiState = buildState(filter)
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
        ?: repository.getPhotos().first { it.id == photoId }

    private fun buildState(filter: PhotoFilter): AppUiState {
        val photos = repository.getPhotos(filter)
        val categories = repository.getCategoryCounts()
        return AppUiState(
            photoFilter = filter,
            photos = photos,
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
}

data class AppUiState(
    val photoFilter: PhotoFilter = PhotoFilter(),
    val photos: List<PhotoItem> = emptyList(),
    val categoryState: CategoryUiState = CategoryUiState(),
    val uploadDraft: UploadDraftUiState = UploadDraftUiState(),
    val myState: MyUiState = MyUiState(),
)

data class CategoryUiState(
    val airlines: List<CategoryCount> = emptyList(),
    val models: List<CategoryCount> = emptyList(),
)

data class UploadDraftUiState(
    val fileName: String = "Select one image <= 40 MB",
    val ratioRule: String = "Accepted ratio: 1:2 to 2:1",
    val progress: Float = 0.42f,
    val exifStatus: String = "EXIF auto extraction enabled",
    val watermarkStatus: String = "Website watermark rules will be reused",
    val registrationStatus: String = "Aircraft registration OCR placeholder",
)

data class MyUiState(
    val user: UserSummary = UserSummary("", "", false),
    val works: List<PhotoItem> = emptyList(),
    val likedPhotos: List<PhotoItem> = emptyList(),
    val pending: List<ReviewItem> = emptyList(),
    val rejected: List<ReviewItem> = emptyList(),
    val sessions: List<DeviceSession> = emptyList(),
)
