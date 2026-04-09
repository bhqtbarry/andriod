package cn.syphotos.android.model

data class GalleryPhotoSource(
    val id: Long,
    val title: String,
    val thumbnailUrl: String,
    val originalUrl: String,
)

fun PhotoItem.asGalleryPhotoSource(state: ViewerPhotoState? = null): GalleryPhotoSource {
    return GalleryPhotoSource(
        id = id,
        title = title,
        thumbnailUrl = state?.thumbUrl?.takeIf { it.isNotBlank() } ?: thumbUrl,
        originalUrl = state?.originalUrl?.takeIf { it.isNotBlank() } ?: originalUrl,
    )
}
