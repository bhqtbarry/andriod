package cn.syphotos.android.ui.viewer

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cn.syphotos.android.model.PhotoItem
import cn.syphotos.android.model.ViewerPhotoState
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.github.chrisbanes.photoview.PhotoView

class PhotoPagerAdapter(
    private var items: List<PhotoItem>,
    private var photosById: Map<Long, ViewerPhotoState>,
    private val onTap: () -> Unit,
) : RecyclerView.Adapter<PhotoPagerAdapter.PhotoViewHolder>() {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val photoView = PhotoView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            maximumScale = 4f
            mediumScale = 2f
            minimumScale = 1f
            setBackgroundColor(android.graphics.Color.BLACK)
            runCatching {
                javaClass.getMethod("setAllowParentInterceptOnEdge", Boolean::class.javaPrimitiveType)
                    .invoke(this, true)
            }
            setOnPhotoTapListener { _, _, _ -> onTap() }
            setOnViewTapListener { _, _, _ -> onTap() }
        }
        return PhotoViewHolder(photoView)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val item = items[position]
        val cached = photosById[item.id]
        val thumbUrl = cached?.thumbUrl?.takeIf { it.isNotBlank() } ?: item.thumbUrl
        val originalUrl = cached?.originalUrl?.takeIf { it.isNotBlank() } ?: item.originalUrl
        val primaryUrl = originalUrl.ifBlank { thumbUrl }
        val fallbackUrl = thumbUrl.ifBlank { originalUrl }

        holder.photoView.scale = 1f

        val request = Glide.with(holder.photoView)
            .load(primaryUrl)
            .apply(
                RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .fitCenter()
                    .dontAnimate(),
            )

        if (fallbackUrl.isNotBlank() && fallbackUrl != primaryUrl) {
            request.thumbnail(
                Glide.with(holder.photoView)
                    .load(fallbackUrl)
                    .apply(
                        RequestOptions()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .fitCenter()
                            .dontAnimate(),
                    ),
            )
        }

        request.into(holder.photoView)
    }

    override fun getItemId(position: Int): Long = items[position].id

    override fun getItemCount(): Int = items.size

    override fun onViewRecycled(holder: PhotoViewHolder) {
        Glide.with(holder.photoView).clear(holder.photoView)
        holder.photoView.setImageDrawable(null)
        super.onViewRecycled(holder)
    }

    fun updateItems(
        newItems: List<PhotoItem>,
        newPhotosById: Map<Long, ViewerPhotoState>,
    ) {
        val oldItems = items
        val oldPhotosById = photosById
        items = newItems
        photosById = newPhotosById
        if (oldItems.size != newItems.size || oldItems.map { it.id } != newItems.map { it.id }) {
            notifyDataSetChanged()
            return
        }
        newItems.forEachIndexed { index, photo ->
            if (oldItems[index] != photo || oldPhotosById[photo.id] != newPhotosById[photo.id]) {
                notifyItemChanged(index)
            }
        }
    }

    class PhotoViewHolder(
        val photoView: PhotoView,
    ) : RecyclerView.ViewHolder(photoView)
}
