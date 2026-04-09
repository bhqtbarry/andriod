package cn.syphotos.android.ui.gallery

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import com.github.chrisbanes.photoview.PhotoView
import kotlin.math.abs

class GalleryZoomPhotoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : PhotoView(context, attrs) {
    var onPageSwipeRequested: (() -> Unit)? = null
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val minScaleValue = 1f
    private val mediumScaleValue = 2.2f
    private val maxScaleValue = 5f
    private var downX = 0f
    private var downY = 0f
    private var zoomEnabled = true

    init {
        minimumScale = minScaleValue
        mediumScale = mediumScaleValue
        maximumScale = maxScaleValue
        scaleType = ScaleType.FIT_CENTER
        runCatching {
            javaClass.getMethod("setAllowParentInterceptOnEdge", Boolean::class.javaPrimitiveType)
                .invoke(this, true)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                parent?.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_MOVE -> {
                when {
                    event.pointerCount >= 2 -> {
                        parent?.requestDisallowInterceptTouchEvent(true)
                    }

                    isZoomed() -> {
                        parent?.requestDisallowInterceptTouchEvent(true)
                    }

                    else -> {
                        val dx = event.x - downX
                        val dy = event.y - downY
                        val horizontalSwipe = abs(dx) > touchSlop && abs(dx) > abs(dy)
                        if (horizontalSwipe) {
                            onPageSwipeRequested?.invoke()
                            parent?.requestDisallowInterceptTouchEvent(false)
                        } else {
                            parent?.requestDisallowInterceptTouchEvent(true)
                        }
                    }
                }
            }

            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_POINTER_UP -> {
                parent?.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return super.onTouchEvent(event)
    }

    fun resetZoom() {
        setScale(minimumScale, false)
    }

    fun setZoomEnabled(enabled: Boolean) {
        zoomEnabled = enabled
        if (!enabled) {
            minimumScale = 1f
            mediumScale = 1f
            maximumScale = 1f
            setScale(1f, false)
        } else {
            minimumScale = minScaleValue
            mediumScale = mediumScaleValue
            maximumScale = maxScaleValue
        }
    }

    private fun isZoomed(): Boolean {
        return zoomEnabled && scale > minimumScale + 0.02f
    }
}
