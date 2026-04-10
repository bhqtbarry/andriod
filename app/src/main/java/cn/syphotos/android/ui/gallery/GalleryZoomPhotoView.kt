package cn.syphotos.android.ui.gallery

import android.content.Context
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import com.github.chrisbanes.photoview.PhotoView
import kotlin.math.abs

class GalleryZoomPhotoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : PhotoView(context, attrs) {
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val minScaleValue = 1f
    private val mediumScaleValue = 2.2f
    private val maxScaleValue = 5f
    private val disabledMinScaleValue = 1f
    private val disabledMediumScaleValue = 1.1f
    private val disabledMaxScaleValue = 1.2f
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
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
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                lastX = event.x
                parent?.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_MOVE -> {
                val dxFromDown = event.x - downX
                val dyFromDown = event.y - downY
                val dxStep = event.x - lastX
                lastX = event.x

                when {
                    event.pointerCount >= 2 -> {
                        parent?.requestDisallowInterceptTouchEvent(true)
                    }

                    !isHorizontalGesture(dxFromDown, dyFromDown) -> {
                        parent?.requestDisallowInterceptTouchEvent(true)
                    }

                    !isZoomed() -> {
                        parent?.requestDisallowInterceptTouchEvent(false)
                    }

                    shouldAllowParentIntercept(dxStep) -> {
                        parent?.requestDisallowInterceptTouchEvent(false)
                    }

                    else -> {
                        parent?.requestDisallowInterceptTouchEvent(true)
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

    private fun applyZoomLevels(
        min: Float,
        medium: Float,
        max: Float,
    ) {
        check(min < medium && medium < max) {
            "Invalid zoom levels: min=$min medium=$medium max=$max"
        }

        // PhotoView validates the full min/medium/max triple on each setter call,
        // so we must keep the invariant true through every intermediate step.
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

    private fun isZoomed(): Boolean {
        return zoomEnabled && scale > minimumScale + 0.02f
    }

    private fun isHorizontalGesture(dx: Float, dy: Float): Boolean {
        return abs(dx) > touchSlop && abs(dx) > abs(dy)
    }

    private fun shouldAllowParentIntercept(dxStep: Float): Boolean {
        if (dxStep == 0f) return false
        val rect = currentDisplayRect() ?: return true
        val atLeftEdge = rect.left >= -1f
        val atRightEdge = rect.right <= width + 1f
        return (dxStep > 0f && atLeftEdge) || (dxStep < 0f && atRightEdge)
    }

    private fun currentDisplayRect(): RectF? {
        val drawable = drawable ?: return null
        val rect = RectF(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
        imageMatrix.mapRect(rect)
        return rect
    }
}
