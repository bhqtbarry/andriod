package cn.syphotos.android.ui.gallery

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import kotlin.math.abs
import com.github.chrisbanes.photoview.PhotoView

class GalleryZoomPhotoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : PhotoView(context, attrs) {
    var onZoomStateChanged: ((Boolean) -> Unit)? = null
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f

    init {
        minimumScale = 1f
        mediumScale = 2.2f
        maximumScale = 5f
        scaleType = ScaleType.FIT_CENTER
        runCatching {
            javaClass.getMethod("setAllowParentInterceptOnEdge", Boolean::class.javaPrimitiveType)
                .invoke(this, true)
        }
        setOnScaleChangeListener { _, _, _ ->
            onZoomStateChanged?.invoke(isZoomed())
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                parent?.requestDisallowInterceptTouchEvent(false)
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isZoomed() && event.pointerCount == 1) {
                    val dx = event.x - downX
                    val dy = event.y - downY
                    val horizontalSwipe = abs(dx) > touchSlop && abs(dx) > abs(dy)
                    if (horizontalSwipe) {
                        parent?.requestDisallowInterceptTouchEvent(false)
                    }
                }
            }

            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_POINTER_UP -> {
                parent?.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(isZoomed())
            }
        }
        return super.onTouchEvent(event)
    }

    fun resetZoom() {
        setScale(minimumScale, false)
        onZoomStateChanged?.invoke(false)
    }

    private fun isZoomed(): Boolean {
        return scale > minimumScale + 0.02f
    }
}
