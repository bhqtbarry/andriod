package cn.syphotos.android.ui.gallery

import android.graphics.Color
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cn.syphotos.android.image.PersistentImageLoader
import cn.syphotos.android.model.GalleryPhotoSource

class PhotoGridAdapter(
    private val imageLoader: PersistentImageLoader,
    private val onPhotoClick: (GalleryPhotoSource) -> Unit,
) : ListAdapter<GalleryPhotoSource, PhotoGridAdapter.PhotoGridViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): PhotoGridViewHolder {
        val container = FrameLayout(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            setBackgroundColor(Color.BLACK)
        }

        val imageView = WideCropImageView(parent.context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setBackgroundColor(Color.parseColor("#111111"))
        }

        container.addView(imageView)
        return PhotoGridViewHolder(container, imageView, onPhotoClick)
    }

    override fun onBindViewHolder(
        holder: PhotoGridViewHolder,
        position: Int,
    ) {
        holder.bind(getItem(position), imageLoader)
    }

    override fun onViewRecycled(holder: PhotoGridViewHolder) {
        imageLoader.clear(holder.imageView)
        super.onViewRecycled(holder)
    }

    class PhotoGridViewHolder(
        container: FrameLayout,
        val imageView: WideCropImageView,
        private val onPhotoClick: (GalleryPhotoSource) -> Unit,
    ) : RecyclerView.ViewHolder(container) {
        private var currentPhoto: GalleryPhotoSource? = null

        init {
            container.setOnClickListener {
                currentPhoto?.let(onPhotoClick)
            }
        }

        fun bind(
            photo: GalleryPhotoSource,
            imageLoader: PersistentImageLoader,
        ) {
            currentPhoto = photo
            imageLoader.loadThumbnail(photo, imageView)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<GalleryPhotoSource>() {
        override fun areItemsTheSame(
            oldItem: GalleryPhotoSource,
            newItem: GalleryPhotoSource,
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: GalleryPhotoSource,
            newItem: GalleryPhotoSource,
        ): Boolean = oldItem == newItem
    }
}
