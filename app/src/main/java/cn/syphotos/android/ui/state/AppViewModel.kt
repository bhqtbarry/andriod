package cn.syphotos.android.ui.state

import android.app.Application
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.syphotos.android.data.SessionStore
import cn.syphotos.android.data.SyPhotosRepository
import cn.syphotos.android.data.WebSyPhotosRepository
import cn.syphotos.android.model.AuthSession
import cn.syphotos.android.model.AirlineDirectoryItem
import cn.syphotos.android.model.AirlineTreeItem
import cn.syphotos.android.model.CategoryCount
import cn.syphotos.android.model.DeviceSession
import cn.syphotos.android.model.MapCluster
import cn.syphotos.android.model.MySummaryStats
import cn.syphotos.android.model.PhotoDetail
import cn.syphotos.android.model.PhotoFilter
import cn.syphotos.android.model.PhotoItem
import cn.syphotos.android.model.ReviewItem
import cn.syphotos.android.model.SearchSuggestion
import cn.syphotos.android.model.UploadConfig
import cn.syphotos.android.model.UploadExifInfo
import cn.syphotos.android.model.UserSummary
import cn.syphotos.android.model.ViewerPhotoState
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val sessionStore = SessionStore(application)
    private val webRepository = WebSyPhotosRepository(sessionStore = sessionStore)

    var uiState by mutableStateOf(AppUiState())
        private set

    init {
        refreshAll()
    }

    fun updateFilter(filter: PhotoFilter) {
        uiState = uiState.copy(
            photoFilter = filter,
            photos = emptyList(),
            feedState = FeedUiState(isLoading = true),
        )
        refreshFeed(filter, reset = true)
        refreshMap(filter)
    }

    fun loadMorePhotos() {
        val feedState = uiState.feedState
        if (feedState.isLoading || feedState.isLoadingMore || !feedState.hasMore) {
            return
        }
        refreshFeed(uiState.photoFilter, reset = false)
    }

    fun login(login: String, password: String) {
        uiState = uiState.copy(myState = uiState.myState.copy(isLoading = true, authErrorMessage = null))
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { webRepository.login(login, password) }
            }.onSuccess { session ->
                uiState = uiState.copy(
                    myState = uiState.myState.copy(
                        authSession = session,
                        isLoading = false,
                        authErrorMessage = null,
                    ),
                )
                refreshMy()
            }.onFailure { error ->
                uiState = uiState.copy(
                    myState = uiState.myState.copy(
                        isLoading = false,
                        authErrorMessage = "Login failed: ${error.message}",
                    ),
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { webRepository.logout() }
            uiState = uiState.copy(
                myState = emptyMyState().copy(
                    authSession = AuthSession(),
                    errorMessage = null,
                    authErrorMessage = null,
                ),
            )
        }
    }

    fun toggleLike(photoId: Long) {
        val currentState = uiState
        val updatedPhotos = currentState.photos.map {
            if (it.id == photoId) it.copy(liked = !it.liked) else it
        }
        val currentDetail = currentState.viewerState.detail
        val updatedViewer = if (currentDetail?.photo?.id == photoId) {
            currentDetail.copy(
                photo = updatedPhotos.firstOrNull { it.id == photoId } ?: currentDetail.photo,
            )
        } else {
            currentDetail
        }
        uiState = currentState.copy(
            photos = updatedPhotos,
            myState = currentState.myState.copy(likedPhotos = updatedPhotos.filter { it.liked }),
            viewerState = currentState.viewerState.copy(
                detail = updatedViewer,
                gallery = currentState.viewerState.gallery.map {
                    if (it.id == photoId) it.copy(liked = !it.liked) else it
                },
            ),
        )
    }

    fun prefetchPhotoDetail(photoId: Long) {
        val existingPhoto = findPhoto(photoId)
        uiState = uiState.copy(
            viewerState = uiState.viewerState.copy(
                detail = existingPhoto?.let { PhotoDetail(photo = it) },
                currentPhotoId = photoId,
                photosById = uiState.viewerState.photosById + (
                    photoId to (
                        uiState.viewerState.photosById[photoId] ?: ViewerPhotoState(
                            photoId = photoId,
                            originalUrl = existingPhoto?.originalUrl.orEmpty(),
                            thumbUrl = existingPhoto?.thumbUrl.orEmpty(),
                        )
                    ).copy(isLoading = true)
                ),
                isLoading = true,
                errorMessage = null,
            ),
        )
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { webRepository.getPhotoDetail(photoId) }
            }.onSuccess { detail ->
                uiState = uiState.copy(
                    viewerState = uiState.viewerState.copy(
                        detail = detail,
                        currentPhotoId = detail.photo.id,
                        photosById = uiState.viewerState.photosById + (
                            detail.photo.id to ViewerPhotoState(
                                photoId = detail.photo.id,
                                originalUrl = detail.originalUrl.ifBlank { detail.photo.originalUrl },
                                thumbUrl = detail.photo.thumbUrl,
                                isLoading = false,
                            )
                        ),
                        isLoading = false,
                        errorMessage = null,
                    ),
                )
            }.onFailure { error ->
                uiState = uiState.copy(
                    viewerState = uiState.viewerState.copy(
                        detail = uiState.viewerState.detail,
                        currentPhotoId = photoId,
                        photosById = uiState.viewerState.photosById + (
                            photoId to (
                                uiState.viewerState.photosById[photoId] ?: ViewerPhotoState(
                                    photoId = photoId,
                                    originalUrl = existingPhoto?.originalUrl.orEmpty(),
                                    thumbUrl = existingPhoto?.thumbUrl.orEmpty(),
                                )
                            ).copy(isLoading = false)
                        ),
                        isLoading = false,
                        errorMessage = "Photo detail unavailable: ${error.message}",
                    ),
                )
            }
        }
    }

    fun openPhotoGallery(photoId: Long, gallery: List<PhotoItem>) {
        uiState = uiState.copy(
            viewerState = ViewerUiState(
                detail = gallery.firstOrNull { it.id == photoId }?.let { PhotoDetail(photo = it) },
                gallery = gallery,
                currentPhotoId = photoId,
                photosById = gallery.associate { photo ->
                    photo.id to ViewerPhotoState(
                        photoId = photo.id,
                        originalUrl = photo.originalUrl,
                        thumbUrl = photo.thumbUrl,
                    )
                },
                isLoading = true,
            ),
        )
        prefetchPhotoDetail(photoId)
    }

    fun prefetchGalleryNeighbors(photoId: Long) {
        val gallery = uiState.viewerState.gallery
        val index = gallery.indexOfFirst { it.id == photoId }
        if (index < 0) return
        listOfNotNull(
            gallery.getOrNull(index - 1),
            gallery.getOrNull(index + 1),
        ).forEach { photo ->
            val cached = uiState.viewerState.photosById[photo.id]
            if (cached != null && (cached.originalUrl.isNotBlank() || cached.isLoading)) {
                return@forEach
            }
            uiState = uiState.copy(
                viewerState = uiState.viewerState.copy(
                    photosById = uiState.viewerState.photosById + (
                        photo.id to ViewerPhotoState(
                            photoId = photo.id,
                            originalUrl = photo.originalUrl,
                            thumbUrl = photo.thumbUrl,
                            isLoading = true,
                        )
                    ),
                ),
            )
            viewModelScope.launch {
                runCatching {
                    withContext(Dispatchers.IO) { webRepository.getPhotoDetail(photo.id) }
                }.onSuccess { detail ->
                    uiState = uiState.copy(
                        viewerState = uiState.viewerState.copy(
                            photosById = uiState.viewerState.photosById + (
                                photo.id to ViewerPhotoState(
                                    photoId = photo.id,
                                    originalUrl = detail.originalUrl.ifBlank { detail.photo.originalUrl },
                                    thumbUrl = detail.photo.thumbUrl,
                                    isLoading = false,
                                )
                            ),
                        ),
                    )
                }.onFailure {
                    uiState = uiState.copy(
                        viewerState = uiState.viewerState.copy(
                            photosById = uiState.viewerState.photosById + (
                                photo.id to ViewerPhotoState(
                                    photoId = photo.id,
                                    originalUrl = photo.originalUrl,
                                    thumbUrl = photo.thumbUrl,
                                    isLoading = false,
                                )
                            ),
                        ),
                    )
                }
            }
        }
    }

    fun requestSuggestions(field: String, query: String) {
        if (query.isBlank()) {
            uiState = uiState.copy(
                suggestionState = uiState.suggestionState.copy(itemsByField = uiState.suggestionState.itemsByField + (field to emptyList())),
            )
            return
        }
        viewModelScope.launch {
            val items = runCatching {
                withContext(Dispatchers.IO) { webRepository.getSuggestions(field, query, uiState.photoFilter) }
            }.getOrElse { emptyList() }
            uiState = uiState.copy(
                suggestionState = uiState.suggestionState.copy(itemsByField = uiState.suggestionState.itemsByField + (field to items)),
            )
        }
    }

    fun clearSuggestions(field: String) {
        uiState = uiState.copy(
            suggestionState = uiState.suggestionState.copy(itemsByField = uiState.suggestionState.itemsByField + (field to emptyList())),
        )
    }

    fun updateUploadSelection(uri: String, fileName: String) {
        uiState = uiState.copy(
            uploadState = uiState.uploadState.copy(
                selectedImageUri = uri,
                fileName = fileName,
                cameraModel = "",
                lensModel = "",
                focalLength = "",
                iso = "",
                aperture = "",
                shutter = "",
                shootingTime = "",
                shootingLocation = "",
                errorMessage = null,
                successMessage = null,
                exifInfo = UploadExifInfo(),
                exifMessage = null,
                registrationLookupMessage = null,
            ),
        )
        fillUploadExif(uri, fileName)
    }

    fun updateUploadDraft(update: (UploadUiState) -> UploadUiState) {
        uiState = uiState.copy(uploadState = update(uiState.uploadState).copy(errorMessage = null, successMessage = null))
    }

    fun updateMySelectedTab(tab: Int) {
        uiState = uiState.copy(myState = uiState.myState.copy(selectedTab = tab))
    }

    fun updateUploadRegistration(value: String) {
        val registration = value.uppercase()
        uiState = uiState.copy(
            uploadState = uiState.uploadState.copy(
                registrationNumber = registration,
                errorMessage = null,
                successMessage = null,
                registrationLookupMessage = null,
            ),
        )
        if (registration.trim().length < 3) {
            return
        }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { webRepository.lookupAircraftByRegistration(registration.trim()) }
            }.onSuccess { result ->
                val current = uiState.uploadState
                uiState = uiState.copy(
                    uploadState = current.copy(
                        aircraftModel = result?.first?.ifBlank { current.aircraftModel } ?: current.aircraftModel,
                        airline = result?.second?.ifBlank { current.airline } ?: current.airline,
                        title = if (result != null && current.title.isBlank()) registration.trim() else current.title,
                        registrationLookupMessage = if (result != null) "已自动填充机型和航司。" else "未找到该注册号对应机型/航司。",
                    ),
                )
            }.onFailure {
                uiState = uiState.copy(
                    uploadState = uiState.uploadState.copy(
                        registrationLookupMessage = "注册号信息获取失败。",
                    ),
                )
            }
        }
    }

    fun submitUpload() {
        val uploadState = uiState.uploadState
        val selectedUri = uploadState.selectedImageUri
        if (!sessionStore.read().isLoggedIn) {
            uiState = uiState.copy(uploadState = uploadState.copy(errorMessage = "请先登录后再上传。"))
            return
        }
        if (selectedUri.isBlank()) {
            uiState = uiState.copy(uploadState = uploadState.copy(errorMessage = "请选择图片。"))
            return
        }
        if (
            uploadState.title.isBlank() ||
            uploadState.registrationNumber.isBlank() ||
            uploadState.aircraftModel.isBlank() ||
            uploadState.airline.isBlank() ||
            uploadState.shootingTime.isBlank() ||
            uploadState.shootingLocation.isBlank()
        ) {
            uiState = uiState.copy(uploadState = uploadState.copy(errorMessage = "请把必填项填写完整。"))
            return
        }
        if (!uploadState.allowUse) {
            uiState = uiState.copy(uploadState = uploadState.copy(errorMessage = "请先同意使用条款。"))
            return
        }

        uiState = uiState.copy(uploadState = uploadState.copy(isLoading = true, errorMessage = null, successMessage = null))
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    val uri = Uri.parse(selectedUri)
                    val mimeType = resolver.getType(uri).orEmpty().ifBlank { "image/jpeg" }
                    val extension = when (mimeType) {
                        "image/png" -> ".png"
                        "image/gif" -> ".gif"
                        else -> ".jpg"
                    }
                    val tempFile = File.createTempFile("syphotos-upload-", extension, getApplication<Application>().cacheDir)
                    try {
                        resolver.openInputStream(uri)?.use { input ->
                            tempFile.outputStream().use { output -> input.copyTo(output) }
                        } ?: throw IllegalStateException("无法读取所选图片。")

                        webRepository.uploadPhoto(
                            file = tempFile,
                            originalName = uploadState.fileName.ifBlank { "upload$extension" },
                            mimeType = mimeType,
                            fields = linkedMapOf(
                                "title" to uploadState.title,
                                "registration_number" to uploadState.registrationNumber.uppercase(),
                                "aircraft_model" to uploadState.aircraftModel,
                                "category" to uploadState.airline,
                                "shooting_time" to uploadState.shootingTime,
                                "shooting_location" to uploadState.shootingLocation.uppercase(),
                                "cameraModel" to uploadState.cameraModel,
                                "lensModel" to uploadState.lensModel,
                                "FocalLength" to uploadState.focalLength,
                                "ISO" to uploadState.iso,
                                "F" to uploadState.aperture,
                                "Shutter" to uploadState.shutter,
                                "watermark_size" to uploadState.watermarkSize.toString(),
                                "watermark_opacity" to uploadState.watermarkOpacity.toString(),
                                "watermark_position" to uploadState.watermarkPosition,
                                "watermark_color" to uploadState.watermarkColor,
                                "watermark_author_style" to uploadState.watermarkAuthorStyle,
                                "allow_use" to if (uploadState.allowUse) "1" else "",
                            ),
                        )
                    } finally {
                        tempFile.delete()
                    }
                }
            }.onSuccess { message ->
                uiState = uiState.copy(
                    uploadState = UploadUiState(
                        config = uiState.uploadState.config,
                        successMessage = message,
                    ),
                )
                refreshMy()
            }.onFailure { error ->
                uiState = uiState.copy(
                    uploadState = uiState.uploadState.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "上传失败",
                    ),
                )
            }
        }
    }

    fun findPhoto(photoId: Long): PhotoItem? = uiState.photos.firstOrNull { it.id == photoId }
        ?: uiState.myState.works.firstOrNull { it.id == photoId }
        ?: uiState.viewerState.gallery.firstOrNull { it.id == photoId }
        ?: uiState.viewerState.detail?.photo?.takeIf { it.id == photoId }

    fun deleteMyPhoto(photo: PhotoItem) {
        uiState = uiState.copy(myState = uiState.myState.copy(isDeleting = true, errorMessage = null, successMessage = null))
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { webRepository.deleteMyPhoto(photo.id, photo.title) }
            }.onSuccess {
                refreshMy(successMessage = "作品已删除。")
            }.onFailure { error ->
                uiState = uiState.copy(
                    myState = uiState.myState.copy(
                        isDeleting = false,
                        errorMessage = error.message ?: "删除失败",
                    ),
                )
            }
        }
    }

    fun refreshMyPage() {
        refreshMy()
    }

    private fun refreshAll() {
        refreshFeed(uiState.photoFilter, reset = true)
        refreshMap(uiState.photoFilter)
        refreshAirlineDirectory()
        refreshUpload()
        refreshMy()
    }

    private fun refreshFeed(filter: PhotoFilter, reset: Boolean) {
        val nextPage = if (reset) 1 else uiState.feedState.page + 1
        uiState = uiState.copy(
            photoFilter = filter,
            feedState = uiState.feedState.copy(
                isLoading = reset,
                isLoadingMore = !reset,
                errorMessage = null,
            ),
        )
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    Pair(webRepository.getPhotosPage(filter, page = nextPage, perPage = 30), webRepository.getCategoryCounts())
                }
            }.onSuccess { (pageResult, categories) ->
                uiState = uiState.copy(
                    photoFilter = filter,
                    photos = if (reset) pageResult.items else uiState.photos + pageResult.items,
                    categoryState = CategoryUiState(
                        airlines = categories.first,
                        models = categories.second,
                        airlineDirectory = uiState.categoryState.airlineDirectory,
                    ),
                    feedState = FeedUiState(
                        page = pageResult.page,
                        hasMore = pageResult.hasMore,
                    ),
                )
            }.onFailure { error ->
                uiState = uiState.copy(
                    photoFilter = filter,
                    photos = if (reset) emptyList() else uiState.photos,
                    categoryState = uiState.categoryState.copy(
                        airlines = if (reset) emptyList() else uiState.categoryState.airlines,
                        models = if (reset) emptyList() else uiState.categoryState.models,
                    ),
                    feedState = FeedUiState(
                        isLoading = false,
                        isLoadingMore = false,
                        page = if (reset) 0 else uiState.feedState.page,
                        hasMore = if (reset) true else uiState.feedState.hasMore,
                        errorMessage = "Photo feed unavailable: ${error.message}",
                    ),
                )
            }
        }
    }

    private fun refreshAirlineDirectory() {
        viewModelScope.launch {
            val airlineDirectory = runCatching {
                withContext(Dispatchers.IO) { webRepository.getAirlineDirectory() }
            }.getOrElse { emptyList() }
            uiState = uiState.copy(
                categoryState = uiState.categoryState.copy(airlineDirectory = airlineDirectory),
            )
        }
    }

    fun loadAirlineTypecodes(airline: String) {
        if (uiState.categoryState.typecodesByAirline.containsKey(airline)) return
        viewModelScope.launch {
            val items = runCatching {
                withContext(Dispatchers.IO) { webRepository.getAirlineTypecodes(airline) }
            }.getOrElse { emptyList() }
            uiState = uiState.copy(
                categoryState = uiState.categoryState.copy(
                    typecodesByAirline = uiState.categoryState.typecodesByAirline + (airline to items),
                ),
            )
        }
    }

    fun loadAirlineRegistrations(airline: String, typecode: String) {
        val key = "$airline|$typecode"
        if (uiState.categoryState.registrationsByType.containsKey(key)) return
        viewModelScope.launch {
            val items = runCatching {
                withContext(Dispatchers.IO) { webRepository.getAirlineRegistrations(airline, typecode) }
            }.getOrElse { emptyList() }
            uiState = uiState.copy(
                categoryState = uiState.categoryState.copy(
                    registrationsByType = uiState.categoryState.registrationsByType + (key to items),
                ),
            )
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
                        clusters = emptyList(),
                        isLoading = false,
                        errorMessage = "Map data unavailable: ${error.message}",
                    ),
                )
            }
        }
    }

    private fun refreshUpload() {
        val config = runCatching { webRepository.getUploadConfig() }.getOrElse { UploadConfig() }
        uiState = uiState.copy(uploadState = uiState.uploadState.copy(isLoading = false, config = config, errorMessage = null))
    }

    private fun refreshMy(successMessage: String? = null) {
        if (!sessionStore.read().isLoggedIn) {
            uiState = uiState.copy(
                myState = emptyMyState().copy(
                    authSession = AuthSession(),
                    isLoading = false,
                    errorMessage = null,
                    authErrorMessage = null,
                    successMessage = successMessage,
                ),
            )
            return
        }
        uiState = uiState.copy(myState = uiState.myState.copy(isLoading = true, errorMessage = null, successMessage = null))
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val summary = webRepository.getMySummary()
                    MyUiState(
                        authSession = webRepository.getAuthSession(),
                        user = summary.first,
                        summary = summary.second,
                        works = openMyWorks(webRepository),
                        likedPhotos = webRepository.getMyLikes(),
                        pending = webRepository.getReviewItems("pending"),
                        rejected = webRepository.getReviewItems("rejected"),
                        sessions = webRepository.getDeviceSessions(),
                        successMessage = successMessage,
                    )
                }
            }.onSuccess { remoteState ->
                uiState = uiState.copy(
                    myState = remoteState.copy(
                        isLoading = false,
                        isDeleting = false,
                        selectedTab = uiState.myState.selectedTab,
                    ),
                )
            }.onFailure { error ->
                if (isAuthFailure(error)) {
                    sessionStore.clear()
                    uiState = uiState.copy(
                        myState = emptyMyState().copy(
                            authSession = AuthSession(),
                            isLoading = false,
                            authErrorMessage = "登录状态已失效，请重新登录。",
                            successMessage = successMessage,
                        ),
                    )
                } else {
                    uiState = uiState.copy(
                        myState = emptyMyState().copy(
                            authSession = sessionStore.read(),
                            isLoading = false,
                            errorMessage = "My page data unavailable: ${error.message}",
                            successMessage = successMessage,
                        ),
                    )
                }
            }
        }
    }

    private fun isAuthFailure(error: Throwable): Boolean {
        val message = error.message.orEmpty()
        return message.contains("401") ||
            message.contains("Missing access token", ignoreCase = true) ||
            message.contains("未登录")
    }

    private fun fillUploadExif(uriString: String, fileName: String) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    val uri = Uri.parse(uriString)
                    val mimeType = resolver.getType(uri).orEmpty().ifBlank { "image/jpeg" }
                    val extension = when (mimeType) {
                        "image/png" -> ".png"
                        "image/gif" -> ".gif"
                        else -> ".jpg"
                    }
                    val tempFile = File.createTempFile("syphotos-exif-", extension, getApplication<Application>().cacheDir)
                    try {
                        resolver.openInputStream(uri)?.use { input ->
                            tempFile.outputStream().use { output -> input.copyTo(output) }
                        } ?: throw IllegalStateException("无法读取所选图片。")
                        val localExif = readLocalExif(tempFile)
                        if (localExif != UploadExifInfo()) {
                            localExif
                        } else {
                            webRepository.extractUploadExif(tempFile, fileName, mimeType)
                        }
                    } finally {
                        tempFile.delete()
                    }
                }
            }.onSuccess { rawExif ->
                val exif = normalizeUploadExif(rawExif)
                val current = uiState.uploadState
                val hasExif = hasExifData(exif)
                uiState = uiState.copy(
                    uploadState = current.copy(
                        exifInfo = exif,
                        exifMessage = if (hasExif) "EXIF 信息读取成功" else "图片无EXIF信息",
                        cameraModel = exif.cameraModel,
                        lensModel = exif.lensModel,
                        focalLength = exif.focalLength,
                        iso = exif.iso,
                        aperture = exif.aperture,
                        shutter = exif.shutterSpeed,
                        shootingLocation = exif.nearestAirport.uppercase(),
                        shootingTime = formatExifDateTime(exif.dateTimeOriginal),
                    ),
                )
            }.onFailure {
                uiState = uiState.copy(
                    uploadState = uiState.uploadState.copy(
                        exifInfo = UploadExifInfo(),
                        exifMessage = "图片无EXIF信息",
                    ),
                )
            }
        }
    }

    private fun formatExifDateTime(value: String): String {
        val parts = value.trim().split(" ")
        if (parts.size != 2) return ""
        val date = parts[0].split(":")
        val time = parts[1].split(":")
        if (date.size != 3 || time.size < 2) return ""
        return "${date[0]}-${date[1]}-${date[2]}T${time[0]}:${time[1]}"
    }

    private fun readLocalExif(file: File): UploadExifInfo {
        val exif = ExifInterface(file)
        return UploadExifInfo(
            cameraModel = exif.getAttribute(ExifInterface.TAG_MODEL).orEmpty(),
            lensModel = exif.getAttribute(ExifInterface.TAG_LENS_MODEL).orEmpty(),
            focalLength = formatRational(exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH).orEmpty()),
            iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY).orEmpty(),
            aperture = formatRational(exif.getAttribute(ExifInterface.TAG_F_NUMBER).orEmpty()),
            shutterSpeed = formatShutterValue(exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME).orEmpty()),
            dateTimeOriginal = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL).orEmpty(),
        )
    }

    private fun formatRational(value: String): String {
        if (value.isBlank()) return ""
        if (!value.contains('/')) return value
        val numerator = value.substringBefore('/').toDoubleOrNull() ?: return value
        val denominator = value.substringAfter('/').toDoubleOrNull()?.takeIf { it != 0.0 } ?: return value
        val result = numerator / denominator
        return if (result % 1.0 == 0.0) result.toInt().toString() else "%.1f".format(result)
    }

    private fun formatShutterValue(value: String): String {
        if (value.isBlank()) return ""
        val normalized = if (value.contains('/')) {
            val numerator = value.substringBefore('/').toDoubleOrNull() ?: return value
            val denominator = value.substringAfter('/').toDoubleOrNull()?.takeIf { it != 0.0 } ?: return value
            numerator / denominator
        } else {
            value.toDoubleOrNull() ?: return value
        }
        if (normalized <= 0.0) return value
        return if (normalized < 1.0) {
            "1/${(1.0 / normalized).roundToInt()}"
        } else {
            "%.1f".format(normalized).trimEnd('0').trimEnd('.')
        }
    }

    private fun normalizeUploadExif(exif: UploadExifInfo): UploadExifInfo {
        return exif.copy(
            focalLength = formatRational(exif.focalLength),
            aperture = formatRational(exif.aperture),
            shutterSpeed = formatShutterValue(exif.shutterSpeed),
        )
    }

    private fun hasExifData(exif: UploadExifInfo): Boolean {
        return exif.cameraModel.isNotBlank() ||
            exif.lensModel.isNotBlank() ||
            exif.focalLength.isNotBlank() ||
            exif.iso.isNotBlank() ||
            exif.aperture.isNotBlank() ||
            exif.shutterSpeed.isNotBlank() ||
            exif.nearestAirport.isNotBlank() ||
            exif.dateTimeOriginal.isNotBlank()
    }

    private fun emptyMyState(): MyUiState = MyUiState(authSession = sessionStore.read())

    private fun openMyWorks(repository: SyPhotosRepository): List<PhotoItem> {
        return repository.getReviewItems("all").map { it.photo }
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
    val suggestionState: SuggestionUiState = SuggestionUiState(),
)

data class FeedUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val page: Int = 0,
    val hasMore: Boolean = true,
    val errorMessage: String? = null,
)

data class CategoryUiState(
    val airlines: List<CategoryCount> = emptyList(),
    val models: List<CategoryCount> = emptyList(),
    val airlineDirectory: List<AirlineDirectoryItem> = emptyList(),
    val typecodesByAirline: Map<String, List<AirlineTreeItem>> = emptyMap(),
    val registrationsByType: Map<String, List<AirlineTreeItem>> = emptyMap(),
)

data class MapUiState(
    val clusters: List<MapCluster> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

data class UploadUiState(
    val selectedImageUri: String = "",
    val fileName: String = "",
    val title: String = "",
    val registrationNumber: String = "",
    val aircraftModel: String = "",
    val airline: String = "",
    val shootingTime: String = "",
    val shootingLocation: String = "",
    val cameraModel: String = "",
    val lensModel: String = "",
    val focalLength: String = "",
    val iso: String = "",
    val aperture: String = "",
    val shutter: String = "",
    val watermarkSize: Int = 15,
    val watermarkOpacity: Int = 80,
    val watermarkPosition: String = "bottom-right",
    val watermarkColor: String = "white",
    val watermarkAuthorStyle: String = "default",
    val allowUse: Boolean = true,
    val config: UploadConfig = UploadConfig(),
    val exifInfo: UploadExifInfo = UploadExifInfo(),
    val exifMessage: String? = null,
    val registrationLookupMessage: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

data class MyUiState(
    val authSession: AuthSession = AuthSession(),
    val user: UserSummary = UserSummary("", "", false),
    val summary: MySummaryStats = MySummaryStats(),
    val works: List<PhotoItem> = emptyList(),
    val likedPhotos: List<PhotoItem> = emptyList(),
    val pending: List<ReviewItem> = emptyList(),
    val rejected: List<ReviewItem> = emptyList(),
    val sessions: List<DeviceSession> = emptyList(),
    val selectedTab: Int = 0,
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null,
    val authErrorMessage: String? = null,
    val successMessage: String? = null,
)

data class ViewerUiState(
    val detail: PhotoDetail? = null,
    val gallery: List<PhotoItem> = emptyList(),
    val currentPhotoId: Long? = null,
    val photosById: Map<Long, ViewerPhotoState> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

data class SuggestionUiState(
    val itemsByField: Map<String, List<SearchSuggestion>> = emptyMap(),
)
