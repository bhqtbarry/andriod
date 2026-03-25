package cn.syphotos.android.ui.viewer

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.github.chrisbanes.photoview.PhotoView

class PagerPhotoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : PhotoView(context, attrs) {

    init {
        runCatching {
            javaClass.getMethod("setAllowParentInterceptOnEdge", Boolean::class.javaPrimitiveType)
                .invoke(this, true)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE,
            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_POINTER_UP -> {
                parent?.requestDisallowInterceptTouchEvent(scale > minimumScale)
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return super.onTouchEvent(event)
    }
}
