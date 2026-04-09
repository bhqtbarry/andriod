package cn.syphotos.android.ui.gallery

import android.content.Context
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class WideCropImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AppCompatImageView(context, attrs) {
    private val drawMatrix = Matrix()

    init {
        scaleType = ScaleType.MATRIX
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
        val targetHeight = measuredWidth / 2
        setMeasuredDimension(measuredWidth, targetHeight)
    }

    override fun onSizeChanged(
        w: Int,
        h: Int,
        oldw: Int,
        oldh: Int,
    ) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateImageMatrix()
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        updateImageMatrix()
    }

    private fun updateImageMatrix() {
        val drawable = drawable ?: return
        val viewWidth = width.toFloat().takeIf { it > 0f } ?: return
        val viewHeight = height.toFloat().takeIf { it > 0f } ?: return
        val drawableWidth = drawable.intrinsicWidth.toFloat().takeIf { it > 0f } ?: return
        val drawableHeight = drawable.intrinsicHeight.toFloat().takeIf { it > 0f } ?: return

        val scale = viewWidth / drawableWidth
        val scaledHeight = drawableHeight * scale
        val translateY = (viewHeight - scaledHeight) / 2f

        drawMatrix.reset()
        drawMatrix.setScale(scale, scale)
        drawMatrix.postTranslate(0f, translateY)
        imageMatrix = drawMatrix
    }
}
