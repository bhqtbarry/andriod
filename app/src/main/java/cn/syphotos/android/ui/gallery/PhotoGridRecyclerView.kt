package cn.syphotos.android.ui.gallery

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.syphotos.android.image.persistentImageLoader
import cn.syphotos.android.model.GalleryPhotoSource

class PhotoGridRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {
    private val imageLoader = context.persistentImageLoader()
    private val gridLayoutManager = GridLayoutManager(context, 3)
    private val adapter = PhotoGridAdapter(
        imageLoader = imageLoader,
        onPhotoClick = { photo -> onPhotoClick(photo) },
    )

    private val recyclerView = RecyclerView(context).apply {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT,
        )
        layoutManager = gridLayoutManager
        overScrollMode = RecyclerView.OVER_SCROLL_ALWAYS
        itemAnimator = null
        setHasFixedSize(true)
        adapter = this@PhotoGridRecyclerView.adapter
        setItemViewCacheSize(24)
        addItemDecoration(PhotoGridSpacingDecoration(spanCount = 3, spacingPx = dp(2)))
    }

    private var hasMore: Boolean = true
    private var isLoading: Boolean = false
    private var isLoadingMore: Boolean = false
    private var onLoadMore: () -> Unit = {}
    private var onPhotoClick: (GalleryPhotoSource) -> Unit = {}

    init {
        addView(recyclerView)
        recyclerView.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(
                    recyclerView: RecyclerView,
                    dx: Int,
                    dy: Int,
                ) {
                    if (dy != 0) {
                        prefetchVisibleWindow()
                        maybeLoadMore()
                    }
                }
            },
        )
    }

    fun bind(
        photos: List<GalleryPhotoSource>,
        hasMore: Boolean,
        isLoading: Boolean,
        isLoadingMore: Boolean,
        onLoadMore: () -> Unit,
        onPhotoClick: (GalleryPhotoSource) -> Unit,
    ) {
        this.hasMore = hasMore
        this.isLoading = isLoading
        this.isLoadingMore = isLoadingMore
        this.onLoadMore = onLoadMore
        this.onPhotoClick = onPhotoClick
        adapter.submitList(photos) {
            post {
                prefetchVisibleWindow()
                maybeLoadMore()
            }
        }
    }

    private fun maybeLoadMore() {
        val lastVisible = gridLayoutManager.findLastVisibleItemPosition()
        val totalCount = adapter.itemCount
        if (totalCount == 0) return
        if (hasMore && !isLoading && !isLoadingMore && lastVisible >= totalCount - 12) {
            onLoadMore()
        }
    }

    private fun prefetchVisibleWindow() {
        val list = adapter.currentList
        if (list.isEmpty()) return
        val firstVisible = gridLayoutManager.findFirstVisibleItemPosition().coerceAtLeast(0)
        val lastVisible = gridLayoutManager.findLastVisibleItemPosition().coerceAtLeast(firstVisible)
        val prefetchEnd = (lastVisible + 18).coerceAtMost(list.lastIndex)
        for (index in firstVisible..prefetchEnd) {
            imageLoader.prefetchThumbnail(list[index])
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
