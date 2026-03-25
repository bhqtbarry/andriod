package cn.syphotos.android.ui.viewer

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
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
        val photoView = PagerPhotoView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            maximumScale = 4f
            mediumScale = 2f
            minimumScale = 1f
            setBackgroundColor(android.graphics.Color.BLACK)
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

        if (holder.photoId == item.id && holder.primaryUrl == primaryUrl && holder.fallbackUrl == fallbackUrl) {
            return
        }

        holder.photoId = item.id
        holder.primaryUrl = primaryUrl
        holder.fallbackUrl = fallbackUrl
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
        holder.photoId = RecyclerView.NO_ID
        holder.primaryUrl = ""
        holder.fallbackUrl = ""
        super.onViewRecycled(holder)
    }

    fun updateItems(
        newItems: List<PhotoItem>,
        newPhotosById: Map<Long, ViewerPhotoState>,
    ) {
        val oldEntries = items.map { photo -> PagerEntry(photo = photo, photoState = photosById[photo.id]) }
        val newEntries = newItems.map { photo -> PagerEntry(photo = photo, photoState = newPhotosById[photo.id]) }
        val diffResult = DiffUtil.calculateDiff(
            object : DiffUtil.Callback() {
                override fun getOldListSize(): Int = oldEntries.size

                override fun getNewListSize(): Int = newEntries.size

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return oldEntries[oldItemPosition].photo.id == newEntries[newItemPosition].photo.id
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return oldEntries[oldItemPosition] == newEntries[newItemPosition]
                }
            },
        )
        items = newItems
        photosById = newPhotosById
        diffResult.dispatchUpdatesTo(this)
    }

    class PhotoViewHolder(
        val photoView: PhotoView,
    ) : RecyclerView.ViewHolder(photoView) {
        var photoId: Long = RecyclerView.NO_ID
        var primaryUrl: String = ""
        var fallbackUrl: String = ""
    }

    private data class PagerEntry(
        val photo: PhotoItem,
        val photoState: ViewerPhotoState?,
    )
}
