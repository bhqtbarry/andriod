package cn.syphotos.android.ui.common

import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.appcompat.widget.AppCompatImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

@Composable
fun GlideThumbnailImage(
    url: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            HorizontalFitCropImageView(context).apply {
                this.contentDescription = contentDescription
            }
        },
        update = { imageView ->
            imageView.contentDescription = contentDescription
            Glide.with(imageView)
                .load(url)
                .override(720, 360)
                .fitCenter()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .dontAnimate()
                .into(imageView)
        },
    )
}

@Composable
fun GlideFitWidthImage(
    url: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            AppCompatImageView(context).apply {
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
                this.contentDescription = contentDescription
            }
        },
        update = { imageView ->
            imageView.contentDescription = contentDescription
            Glide.with(imageView)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .dontTransform()
                .dontAnimate()
                .into(imageView)
        },
    )
}

private class HorizontalFitCropImageView(
    context: android.content.Context,
) : AppCompatImageView(context) {
    private val drawMatrix = Matrix()

    init {
        scaleType = ImageView.ScaleType.MATRIX
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateImageMatrix()
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        updateImageMatrix()
    }

    private fun updateImageMatrix() {
        val drawable = drawable ?: return
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
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
