package cn.syphotos.android.ui.gallery

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

/**
 * 解决 PhotoView 与 ViewPager2 的横向手势冲突：
 *
 * - 图片放大且还能左右拖动：交给图片
 * - 图片未放大，或已拖到边缘：交给 ViewPager2 左右翻页
 */
class GalleryNestedScrollableHost @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var initialX = 0f
    private var initialY = 0f

    private val parentViewPager: ViewPager2?
        get() {
            var viewParent = parent
            while (viewParent != null && viewParent !is ViewPager2) {
                viewParent = viewParent.parent
            }
            return viewParent as? ViewPager2
        }

    private val photoView: GalleryZoomPhotoView?
        get() = getChildAt(0) as? GalleryZoomPhotoView

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        handleIntercept(event)
        return super.onInterceptTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        handleIntercept(event)
        return super.onTouchEvent(event)
    }

    private fun handleIntercept(event: MotionEvent) {
        val pager = parentViewPager ?: return
        val child = photoView ?: return

        if (pager.orientation != ViewPager2.ORIENTATION_HORIZONTAL) {
            return
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialX = event.x
                initialY = event.y
                pager.parent?.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_POINTER_UP -> {
                pager.parent?.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - initialX
                val dy = event.y - initialY

                if (abs(dx) <= touchSlop && abs(dy) <= touchSlop) return

                val isHorizontalGesture = abs(dx) > abs(dy)
                if (!isHorizontalGesture) {
                    pager.parent?.requestDisallowInterceptTouchEvent(false)
                    return
                }

                val direction = if (dx > 0) -1 else 1
                val childCanScroll = child.canScrollPhotoHorizontally(direction)
                pager.parent?.requestDisallowInterceptTouchEvent(childCanScroll)
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                pager.parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
    }
}