package cn.syphotos.android.ui.gallery

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cn.syphotos.android.image.PersistentImageLoader
import cn.syphotos.android.model.GalleryPhotoSource

class GalleryViewerAdapter(
    private val imageLoader: PersistentImageLoader,
    private val onPhotoTap: () -> Unit,
    private val onZoomStateChanged: (position: Int, zoomed: Boolean) -> Unit,
) : ListAdapter<GalleryPhotoSource, GalleryViewerAdapter.GalleryPageViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): GalleryPageViewHolder {
        val container = FrameLayout(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setBackgroundColor(Color.BLACK)
        }

        val photoView = GalleryZoomPhotoView(parent.context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setBackgroundColor(Color.BLACK)
        }

        val progressBar = ProgressBar(parent.context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER,
            )
            visibility = View.GONE
        }

        container.addView(photoView)
        container.addView(progressBar)
        return GalleryPageViewHolder(
            container = container,
            photoView = photoView,
            progressBar = progressBar,
            onPhotoTap = onPhotoTap,
            onZoomStateChanged = onZoomStateChanged,
        )
    }

    override fun onBindViewHolder(
        holder: GalleryPageViewHolder,
        position: Int,
    ) {
        holder.bind(
            photo = getItem(position),
            imageLoader = imageLoader,
        )
    }

    override fun onViewRecycled(holder: GalleryPageViewHolder) {
        holder.recycle(imageLoader)
        super.onViewRecycled(holder)
    }

    class GalleryPageViewHolder(
        container: FrameLayout,
        private val photoView: GalleryZoomPhotoView,
        private val progressBar: ProgressBar,
        private val onPhotoTap: () -> Unit,
        private val onZoomStateChanged: (position: Int, zoomed: Boolean) -> Unit,
    ) : RecyclerView.ViewHolder(container) {
        private var currentPhotoId: Long = RecyclerView.NO_ID

        init {
            photoView.setOnPhotoTapListener { _, _, _ -> onPhotoTap() }
            photoView.setOnViewTapListener { _, _, _ -> onPhotoTap() }
            photoView.onZoomStateChanged = { zoomed ->
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onZoomStateChanged(position, zoomed)
                }
            }
        }

        fun bind(
            photo: GalleryPhotoSource,
            imageLoader: PersistentImageLoader,
        ) {
            if (currentPhotoId != photo.id) {
                photoView.resetZoom()
            }
            currentPhotoId = photo.id
            imageLoader.loadViewerImage(photo, photoView) { isLoading ->
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        fun recycle(imageLoader: PersistentImageLoader) {
            currentPhotoId = RecyclerView.NO_ID
            progressBar.visibility = View.GONE
            photoView.resetZoom()
            imageLoader.clear(photoView)
            photoView.setImageDrawable(null)
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
