package cn.syphotos.android.ui.viewer

import android.content.Context
import cn.syphotos.android.model.PhotoItem
import cn.syphotos.android.model.ViewerPhotoState
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions

class PhotoPreloader(
    private val context: Context,
    private var items: List<PhotoItem>,
    private var photosById: Map<Long, ViewerPhotoState>,
) {
    fun update(
        newItems: List<PhotoItem>,
        newPhotosById: Map<Long, ViewerPhotoState>,
    ) {
        items = newItems
        photosById = newPhotosById
    }

    fun preloadAround(position: Int) {
        for (offset in -2..2) {
            if (offset != 0) preload(position + offset)
        }
    }

    fun preloadCurrent(position: Int) {
        preload(position)
    }

    private fun preload(position: Int) {
        val item = items.getOrNull(position) ?: return
        val cached = photosById[item.id]
        val originalUrl = cached?.originalUrl?.takeIf { it.isNotBlank() } ?: item.originalUrl
        if (originalUrl.isBlank()) return

        Glide.with(context)
            .load(originalUrl)
            .apply(
                RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .fitCenter()
                    .dontAnimate(),
            )
            .preload()
    }
}
