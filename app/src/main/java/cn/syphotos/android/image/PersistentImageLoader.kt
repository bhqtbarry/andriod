package cn.syphotos.android.image

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.ColorDrawable
import android.widget.ImageView
import cn.syphotos.android.R
import cn.syphotos.android.model.GalleryPhotoSource
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import java.io.File
import java.util.WeakHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PersistentImageLoader(
    context: Context,
    private val imageStore: PersistentImageStore,
) {
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val jobs = WeakHashMap<ImageView, Job>()
    private val fallbackDrawable = ColorDrawable(Color.parseColor("#121212"))

    data class ViewerLoadState(
        val isLoading: Boolean,
        val isFullResolutionReady: Boolean,
    )

    fun clear(target: ImageView) {
        synchronized(jobs) {
            jobs.remove(target)?.cancel()
        }
        Glide.with(target).clear(target)
    }

    fun loadThumbnail(
        photo: GalleryPhotoSource,
        target: ImageView,
    ) {
        clear(target)
        val requestToken = System.nanoTime()
        target.setTag(R.id.tag_persistent_image_request, requestToken)

        imageStore.localFile(photo.id, ImageVariant.THUMBNAIL)?.let { file ->
            loadLocalFile(
                target = target,
                file = file,
                centerCrop = true,
            )
            return
        }

        target.setImageDrawable(fallbackDrawable)

        val job = mainScope.launch {
            val file = imageStore.getOrFetch(
                photoId = photo.id,
                remoteUrl = photo.thumbnailUrl,
                variant = ImageVariant.THUMBNAIL,
            )
            if (!isLatestRequest(target, requestToken) || file == null) return@launch
            loadLocalFile(
                target = target,
                file = file,
                centerCrop = true,
            )
        }

        synchronized(jobs) {
            jobs[target] = job
        }
    }

    fun loadViewerImage(
        photo: GalleryPhotoSource,
        target: ImageView,
        onStateChanged: (ViewerLoadState) -> Unit,
    ) {
        clear(target)
        val requestToken = System.nanoTime()
        target.setTag(R.id.tag_persistent_image_request, requestToken)

        val job = mainScope.launch {
            val originalFile = withContext(Dispatchers.IO) {
                imageStore.localFile(photo.id, ImageVariant.ORIGINAL)
            }

            if (originalFile != null) {
                onStateChanged(ViewerLoadState(isLoading = false, isFullResolutionReady = true))
                loadLocalFile(
                    target = target,
                    file = originalFile,
                    centerCrop = false,
                    loadFullResolution = true,
                )
                return@launch
            }

            onStateChanged(ViewerLoadState(isLoading = true, isFullResolutionReady = false))

            val thumbFile = withContext(Dispatchers.IO) {
                imageStore.localFile(photo.id, ImageVariant.THUMBNAIL)
            } ?: imageStore.getOrFetch(
                photoId = photo.id,
                remoteUrl = photo.thumbnailUrl,
                variant = ImageVariant.THUMBNAIL,
            )

            if (isLatestRequest(target, requestToken) && thumbFile != null) {
                loadLocalFile(
                    target = target,
                    file = thumbFile,
                    centerCrop = false,
                    loadFullResolution = false,
                    onDisplayed = {
                        if (isLatestRequest(target, requestToken)) {
                            onStateChanged(ViewerLoadState(isLoading = true, isFullResolutionReady = false))
                        }
                    },
                )
            } else if (isLatestRequest(target, requestToken)) {
                target.setImageDrawable(fallbackDrawable)
            }

            val fetchedOriginal = imageStore.getOrFetch(
                photoId = photo.id,
                remoteUrl = photo.originalUrl,
                variant = ImageVariant.ORIGINAL,
            )

            if (!isLatestRequest(target, requestToken)) return@launch

            if (fetchedOriginal != null) {
                loadLocalFile(
                    target = target,
                    file = fetchedOriginal,
                    centerCrop = false,
                    loadFullResolution = true,
                    onDisplayed = {
                        if (isLatestRequest(target, requestToken)) {
                            onStateChanged(ViewerLoadState(isLoading = false, isFullResolutionReady = true))
                        }
                    },
                )
            } else {
                onStateChanged(ViewerLoadState(isLoading = false, isFullResolutionReady = false))
            }
        }

        synchronized(jobs) {
            jobs[target] = job
        }
    }

    fun prefetchThumbnail(photo: GalleryPhotoSource) {
        imageStore.prefetch(
            photoId = photo.id,
            remoteUrl = photo.thumbnailUrl,
            variant = ImageVariant.THUMBNAIL,
        )
    }

    fun prefetchOriginal(photo: GalleryPhotoSource) {
        imageStore.prefetch(
            photoId = photo.id,
            remoteUrl = photo.originalUrl,
            variant = ImageVariant.ORIGINAL,
        )
    }

    private fun loadLocalFile(
        target: ImageView,
        file: File,
        centerCrop: Boolean,
        loadFullResolution: Boolean = false,
        onDisplayed: (() -> Unit)? = null,
    ) {
        Glide.with(target)
            .load(file)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .dontAnimate()
            .listener(
                object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean,
                    ): Boolean {
                        onDisplayed?.invoke()
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean,
                    ): Boolean {
                        onDisplayed?.invoke()
                        return false
                    }
                },
            )
            .also { builder ->
                if (loadFullResolution) {
                    builder.override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                }
                if (centerCrop) {
                    builder.centerCrop()
                } else {
                    builder.fitCenter()
                }
            }
            .into(target)
    }

    private fun isLatestRequest(
        target: ImageView,
        token: Long,
    ): Boolean {
        return (target.getTag(R.id.tag_persistent_image_request) as? Long) == token
    }
}
