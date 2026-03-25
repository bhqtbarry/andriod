package cn.syphotos.android.ui.viewer

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import cn.syphotos.android.model.PhotoItem
import cn.syphotos.android.model.ViewerPhotoState
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
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
        val container = FrameLayout(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setBackgroundColor(Color.BLACK)
        }
        val photoView = PhotoView(parent.context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            minimumScale = 1f
            mediumScale = 2f
            maximumScale = 4f
            setBackgroundColor(Color.BLACK)
        }
        val loadingIndicator = ProgressBar(parent.context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER,
            )
            visibility = View.GONE
            alpha = 0.9f
        }
        container.addView(photoView)
        container.addView(loadingIndicator)
        return PhotoViewHolder(container, photoView, loadingIndicator, onTap)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val item = items[position]
        val cached = photosById[item.id]
        val thumbUrl = cached?.thumbUrl?.takeIf { it.isNotBlank() } ?: item.thumbUrl
        val originalUrl = cached?.originalUrl?.takeIf { it.isNotBlank() } ?: item.originalUrl
        val primaryUrl = originalUrl.ifBlank { thumbUrl }
        val fallbackUrl = thumbUrl.ifBlank { originalUrl }

        holder.bind(
            photoId = item.id,
            primaryUrl = primaryUrl,
            fallbackUrl = fallbackUrl,
        )
    }

    override fun getItemId(position: Int): Long = items[position].id

    override fun getItemCount(): Int = items.size

    override fun onViewRecycled(holder: PhotoViewHolder) {
        holder.resetInteractionState()
        Glide.with(holder.photoView).clear(holder.photoView)
        holder.photoView.setImageDrawable(null)
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
        containerView: View,
        val photoView: PhotoView,
        private val loadingIndicator: ProgressBar,
        private val onTap: () -> Unit,
    ) : RecyclerView.ViewHolder(containerView) {
        private var viewPager: ViewPager2? = null
        private var photoId: Long = RecyclerView.NO_ID
        private var primaryUrl: String = ""
        private var fallbackUrl: String = ""

        init {
            photoView.setOnPhotoTapListener { _, _, _ -> onTap() }
            photoView.setOnViewTapListener { _, _, _ -> onTap() }
            photoView.setOnScaleChangeListener { _, _, _ ->
                syncPagerScrollable()
            }
            photoView.setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        view.parent?.requestDisallowInterceptTouchEvent(true)
                        syncPagerScrollable()
                    }

                    MotionEvent.ACTION_MOVE,
                    MotionEvent.ACTION_POINTER_DOWN,
                    MotionEvent.ACTION_POINTER_UP -> {
                        view.parent?.requestDisallowInterceptTouchEvent(true)
                        syncPagerScrollable()
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        syncPagerScrollable()
                        if (photoView.scale <= 1f) {
                            view.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                }
                false
            }
        }

        fun bind(
            photoId: Long,
            primaryUrl: String,
            fallbackUrl: String,
        ) {
            viewPager = findViewPager2(photoView)

            if (this.photoId == photoId && this.primaryUrl == primaryUrl && this.fallbackUrl == fallbackUrl) {
                syncPagerScrollable()
                return
            }

            this.photoId = photoId
            this.primaryUrl = primaryUrl
            this.fallbackUrl = fallbackUrl

            photoView.scale = 1f
            syncPagerScrollable()
            loadingIndicator.visibility =
                if (fallbackUrl.isNotBlank() && fallbackUrl != primaryUrl) View.VISIBLE else View.GONE

            val request = Glide.with(photoView)
                .load(primaryUrl)
                .apply(
                    RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .fitCenter()
                        .dontAnimate(),
                )
                .listener(
                    object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean,
                        ): Boolean {
                            if (this@PhotoViewHolder.photoId == photoId && this@PhotoViewHolder.primaryUrl == primaryUrl) {
                                loadingIndicator.visibility = View.GONE
                            }
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>?,
                            dataSource: DataSource,
                            isFirstResource: Boolean,
                        ): Boolean {
                            if (this@PhotoViewHolder.photoId == photoId && this@PhotoViewHolder.primaryUrl == primaryUrl) {
                                loadingIndicator.visibility = View.GONE
                            }
                            return false
                        }
                    },
                )

            if (fallbackUrl.isNotBlank() && fallbackUrl != primaryUrl) {
                request.thumbnail(
                    Glide.with(photoView)
                        .load(fallbackUrl)
                        .apply(
                            RequestOptions()
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .fitCenter()
                                .dontAnimate(),
                        ),
                )
            }

            request.into(photoView)
        }

        fun resetInteractionState() {
            photoView.scale = 1f
            viewPager?.isUserInputEnabled = true
            photoView.parent?.requestDisallowInterceptTouchEvent(false)
            loadingIndicator.visibility = View.GONE
            photoId = RecyclerView.NO_ID
            primaryUrl = ""
            fallbackUrl = ""
        }

        private fun syncPagerScrollable() {
            val zoomed = photoView.scale > 1f
            viewPager?.isUserInputEnabled = !zoomed
            if (!zoomed) {
                photoView.parent?.requestDisallowInterceptTouchEvent(false)
            }
        }

        private fun findViewPager2(view: View): ViewPager2? {
            var current = view.parent
            while (current != null) {
                if (current is ViewPager2) {
                    return current
                }
                current = current.parent
            }
            return null
        }
    }

    private data class PagerEntry(
        val photo: PhotoItem,
        val photoState: ViewerPhotoState?,
    )
}
