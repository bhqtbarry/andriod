package cn.syphotos.android.image

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

class PersistentImageStore(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val rootDir = File(appContext.filesDir, "persistent-photo-cache").apply { mkdirs() }
    private val inflight = ConcurrentHashMap<String, kotlinx.coroutines.Deferred<File?>>()

    init {
        ImageVariant.values().forEach { variant ->
            directoryFor(variant).mkdirs()
        }
    }

    fun rootDirectory(): File = rootDir

    fun directoryFor(variant: ImageVariant): File {
        return File(rootDir, variant.folderName)
    }

    fun fileFor(photoId: Long, variant: ImageVariant): File {
        return File(directoryFor(variant), "${photoId}_${variant.fileSuffix}.img")
    }

    fun localFile(photoId: Long, variant: ImageVariant): File? {
        val file = fileFor(photoId, variant)
        return file.takeIf { it.exists() && it.length() > 0L }
    }

    fun hasLocalFile(photoId: Long, variant: ImageVariant): Boolean {
        return localFile(photoId, variant) != null
    }

    suspend fun getOrFetch(
        photoId: Long,
        remoteUrl: String,
        variant: ImageVariant,
    ): File? {
        if (remoteUrl.isBlank()) return null
        localFile(photoId, variant)?.let { return it }

        val key = cacheKey(photoId, variant)
        val deferred = inflight.getOrPut(key) {
            scope.async {
                downloadToPersistentFile(
                    photoId = photoId,
                    remoteUrl = remoteUrl,
                    variant = variant,
                )
            }
        }

        return try {
            deferred.await()
        } finally {
            inflight.remove(key, deferred)
        }
    }

    fun prefetch(
        photoId: Long,
        remoteUrl: String,
        variant: ImageVariant,
    ) {
        if (remoteUrl.isBlank() || hasLocalFile(photoId, variant)) return
        val key = cacheKey(photoId, variant)
        inflight.getOrPut(key) {
            scope.async {
                try {
                    downloadToPersistentFile(
                        photoId = photoId,
                        remoteUrl = remoteUrl,
                        variant = variant,
                    )
                } finally {
                    inflight.remove(key)
                }
            }
        }
    }

    private suspend fun downloadToPersistentFile(
        photoId: Long,
        remoteUrl: String,
        variant: ImageVariant,
    ): File? = withContext(Dispatchers.IO) {
        localFile(photoId, variant)?.let { return@withContext it }

        val destination = fileFor(photoId, variant)
        destination.parentFile?.mkdirs()
        val tempFile = File(destination.parentFile, "${destination.name}.download")
        if (tempFile.exists()) {
            tempFile.delete()
        }

        val connection = (URL(remoteUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            connectTimeout = 15_000
            readTimeout = 30_000
            doInput = true
        }

        try {
            connection.connect()
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                tempFile.delete()
                return@withContext null
            }

            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            }

            if (tempFile.length() <= 0L) {
                tempFile.delete()
                return@withContext null
            }

            if (destination.exists()) {
                destination.delete()
            }

            if (!tempFile.renameTo(destination)) {
                tempFile.copyTo(destination, overwrite = true)
                tempFile.delete()
            }

            destination.takeIf { it.exists() && it.length() > 0L }
        } catch (_: Throwable) {
            tempFile.delete()
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun cacheKey(
        photoId: Long,
        variant: ImageVariant,
    ): String {
        return "${photoId}_${variant.fileSuffix}"
    }
}
