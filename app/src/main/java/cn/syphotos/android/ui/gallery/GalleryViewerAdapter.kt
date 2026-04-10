package cn.syphotos.android.ui.gallery

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cn.syphotos.android.image.PersistentImageLoader
import cn.syphotos.android.model.GalleryPhotoSource

class GalleryViewerAdapter(
    private val imageLoader: PersistentImageLoader,
    private val onPhotoTap: () -> Unit,
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

        val progressBar = ProgressBar(parent.context, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                2,
                Gravity.BOTTOM,
            )
            visibility = View.GONE
            isIndeterminate = true
            progressDrawable = ContextCompat.getDrawable(context, android.R.color.white)
            indeterminateDrawable?.setTint(Color.WHITE)
            progressDrawable?.setTint(Color.WHITE)
            alpha = 0.92f
        }

        container.addView(photoView)
        container.addView(progressBar)
        return GalleryPageViewHolder(
            container = container,
            photoView = photoView,
            progressBar = progressBar,
            onPhotoTap = onPhotoTap,
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
    ) : RecyclerView.ViewHolder(container) {
        private var currentPhotoId: Long = RecyclerView.NO_ID

        init {
            photoView.setOnPhotoTapListener { _, _, _ -> onPhotoTap() }
            photoView.setOnViewTapListener { _, _, _ -> onPhotoTap() }
        }

        fun bind(
            photo: GalleryPhotoSource,
            imageLoader: PersistentImageLoader,
        ) {
            if (currentPhotoId != photo.id) {
                photoView.resetZoom()
            }
            currentPhotoId = photo.id
            imageLoader.loadViewerImage(photo, photoView) { state ->
                progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                photoView.setZoomEnabled(state.isFullResolutionReady)
            }
        }

        fun recycle(imageLoader: PersistentImageLoader) {
            currentPhotoId = RecyclerView.NO_ID
            progressBar.visibility = View.GONE
            photoView.setZoomEnabled(false)
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