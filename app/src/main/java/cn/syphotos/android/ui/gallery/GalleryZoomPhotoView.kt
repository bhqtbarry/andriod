package cn.syphotos.android.ui.gallery

import android.content.Context
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import com.github.chrisbanes.photoview.PhotoView

class GalleryZoomPhotoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : PhotoView(context, attrs) {
    private val minScaleValue = 1f
    private val mediumScaleValue = 2.2f
    private val maxScaleValue = 5f
    private val disabledMinScaleValue = 1f
    private val disabledMediumScaleValue = 1.1f
    private val disabledMaxScaleValue = 1.2f
    private var zoomEnabled = true

    init {
        applyZoomLevels(
            min = minScaleValue,
            medium = mediumScaleValue,
            max = maxScaleValue,
        )
        scaleType = ScaleType.FIT_CENTER
        runCatching {
            javaClass.getMethod("setAllowParentInterceptOnEdge", Boolean::class.javaPrimitiveType)
                .invoke(this, true)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
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
            applyZoomLevels(
                min = disabledMinScaleValue,
                medium = disabledMediumScaleValue,
                max = disabledMaxScaleValue,
            )
            setScale(disabledMinScaleValue, false)
        } else {
            applyZoomLevels(
                min = minScaleValue,
                medium = mediumScaleValue,
                max = maxScaleValue,
            )
            setScale(minScaleValue, false)
        }
    }

    fun canScrollPhotoHorizontally(direction: Int): Boolean {
        if (!zoomEnabled) return false
        if (scale <= minimumScale + 0.02f) return false

        val rect = currentDisplayRect() ?: return false
        val viewWidth = width.toFloat()
        if (rect.width() <= viewWidth + 1f) return false

        return when {
            direction < 0 -> rect.left < -1f
            direction > 0 -> rect.right > viewWidth + 1f
            else -> false
        }
    }

    private fun applyZoomLevels(
        min: Float,
        medium: Float,
        max: Float,
    ) {
        check(min < medium && medium < max) {
            "Invalid zoom levels: min=$min medium=$medium max=$max"
        }

        if (max >= maximumScale) {
            maximumScale = max
            mediumScale = medium
            minimumScale = min
        } else {
            mediumScale = medium
            minimumScale = min
            maximumScale = max
        }
    }

    private fun currentDisplayRect(): RectF? {
        val drawable = drawable ?: return null
        val rect = RectF(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
        imageMatrix.mapRect(rect)
        return rect
    }
}