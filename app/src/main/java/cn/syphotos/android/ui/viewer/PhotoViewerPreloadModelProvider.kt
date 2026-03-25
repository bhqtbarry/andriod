package cn.syphotos.android.ui.viewer

import cn.syphotos.android.model.PhotoItem
import cn.syphotos.android.model.ViewerPhotoState
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import com.bumptech.glide.ListPreloader
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions

class PhotoViewerPreloadModelProvider(
    private val requestManager: RequestManager,
    private var items: List<PhotoItem>,
    private var photosById: Map<Long, ViewerPhotoState>,
) : ListPreloader.PreloadModelProvider<String> {

    fun update(
        newItems: List<PhotoItem>,
        newPhotosById: Map<Long, ViewerPhotoState>,
    ) {
        items = newItems
        photosById = newPhotosById
    }

    override fun getPreloadItems(position: Int): List<String> {
        val item = items.getOrNull(position) ?: return emptyList()
        val state = photosById[item.id]
        val originalUrl = state?.originalUrl?.takeIf { it.isNotBlank() } ?: item.originalUrl
        return if (originalUrl.isBlank()) emptyList() else listOf(originalUrl)
    }

    override fun getPreloadRequestBuilder(item: String): RequestBuilder<*> {
        return requestManager
            .load(item)
            .apply(
                RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .fitCenter()
                    .dontAnimate(),
            )
    }
}
