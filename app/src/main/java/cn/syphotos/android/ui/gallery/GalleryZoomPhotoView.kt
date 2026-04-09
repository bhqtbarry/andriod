package cn.syphotos.android.ui.gallery

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.github.chrisbanes.photoview.PhotoView

class GalleryZoomPhotoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : PhotoView(context, attrs) {
    var onZoomStateChanged: ((Boolean) -> Unit)? = null

    init {
        minimumScale = 1f
        mediumScale = 2.2f
        maximumScale = 5f
        scaleType = ScaleType.FIT_CENTER
        runCatching {
            javaClass.getMethod("setAllowParentInterceptOnEdge", Boolean::class.javaPrimitiveType)
                .invoke(this, false)
        }
        setOnScaleChangeListener { _, _, _ ->
            onZoomStateChanged?.invoke(isZoomed())
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE,
            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_POINTER_UP -> {
                parent?.requestDisallowInterceptTouchEvent(isZoomed())
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
