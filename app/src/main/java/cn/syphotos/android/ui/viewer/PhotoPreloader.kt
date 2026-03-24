package cn.syphotos.android.ui.viewer

import android.content.Context
import cn.syphotos.android.model.PhotoItem
import cn.syphotos.android.model.ViewerPhotoState
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions

class PhotoPreloader(
    private val context: Context,
    private val items: List<PhotoItem>,
    private val photoStateProvider: (Long) -> ViewerPhotoState?,
) {
    fun preloadAround(position: Int) {
        preload(position)
        preload(position - 1)
        preload(position + 1)
    }

    private fun preload(position: Int) {
        val item = items.getOrNull(position) ?: return
        val cached = photoStateProvider(item.id)
        val originalUrl = cached?.originalUrl?.takeIf { it.isNotBlank() } ?: item.originalUrl
        if (originalUrl.isBlank()) return

        Glide.with(context)
            .load(originalUrl)
            .apply(
                RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .fitCenter()
                    .format(DecodeFormat.PREFER_RGB_565)
                    .dontAnimate(),
            )
            .preload()
    }
}
