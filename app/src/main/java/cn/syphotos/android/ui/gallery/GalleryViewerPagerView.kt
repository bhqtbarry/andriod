package cn.syphotos.android.ui.gallery

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import cn.syphotos.android.image.PersistentImageLoader
import cn.syphotos.android.image.persistentImageLoader
import cn.syphotos.android.model.GalleryPhotoSource

class GalleryViewerPagerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {
    private val imageLoader: PersistentImageLoader = context.persistentImageLoader()
    private val pagerAdapter: GalleryViewerAdapter = GalleryViewerAdapter(
        imageLoader = imageLoader,
        onPhotoTap = { onPhotoTap() },
        onHorizontalSwipeIntent = ::handleHorizontalSwipeIntent,
    )

    private val viewPager: ViewPager2 = ViewPager2(context).apply {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT,
        )
        orientation = ViewPager2.ORIENTATION_HORIZONTAL
        offscreenPageLimit = 1
        adapter = pagerAdapter
        setBackgroundColor(Color.BLACK)
        (getChildAt(0) as? RecyclerView)?.apply {
            overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            itemAnimator = null
            setItemViewCacheSize(3)
        }
    }

    private var onPageChanged: (Long) -> Unit = {}
    private var onPhotoTap: () -> Unit = {}
    private var currentItems: List<GalleryPhotoSource> = emptyList()

    init {
        setBackgroundColor(Color.BLACK)
        addView(viewPager)
        viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    val item = currentItems.getOrNull(position) ?: return
                    onPageChanged(item.id)
                    prefetchAround(position)
                }
            },
        )
    }

    fun bind(
        items: List<GalleryPhotoSource>,
        currentPhotoId: Long?,
        onPageChanged: (Long) -> Unit,
        onPhotoTap: () -> Unit,
    ) {
        this.onPageChanged = onPageChanged
        this.onPhotoTap = onPhotoTap
        currentItems = items
        pagerAdapter.submitList(items) {
            val targetIndex = items.indexOfFirst { it.id == currentPhotoId }.takeIf { it >= 0 } ?: 0
            if (items.isNotEmpty() && viewPager.currentItem != targetIndex) {
                viewPager.setCurrentItem(targetIndex, false)
            }
            prefetchAround(viewPager.currentItem.coerceIn(0, items.lastIndex.coerceAtLeast(0)))
        }
    }

    private fun handleHorizontalSwipeIntent(position: Int) {
        if (position == viewPager.currentItem) {
            viewPager.beginFakeDrag()
            if (viewPager.isFakeDragging) {
                viewPager.endFakeDrag()
            }
        }
    }

    private fun prefetchAround(position: Int) {
        if (currentItems.isEmpty()) return
        val start = (position - 1).coerceAtLeast(0)
        val end = (position + 1).coerceAtMost(currentItems.lastIndex)
        for (index in start..end) {
            val photo = currentItems[index]
            imageLoader.prefetchThumbnail(photo)
            imageLoader.prefetchOriginal(photo)
        }
    }
}
