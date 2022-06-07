package kz.gov.mia.sos.widget.core

//import android.content.Context
//import android.graphics.Bitmap
//import android.net.Uri
//import android.os.Environment
//import androidx.core.net.toFile
//import androidx.core.net.toUri
//import com.abedelazizshe.lightcompressorlibrary.CompressionListener
//import com.abedelazizshe.lightcompressorlibrary.VideoCompressor
//import com.abedelazizshe.lightcompressorlibrary.VideoQuality
//import com.abedelazizshe.lightcompressorlibrary.config.Configuration
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import kz.garage.kotlin.simpleNameOf
//import kz.garage.multimedia.store.getImageResolution
//import kz.garage.multimedia.store.model.Content
//import kz.garage.multimedia.store.model.Folder
//import kz.garage.multimedia.store.model.Image
//import kz.garage.multimedia.store.model.Video
//import kz.gov.mia.sos.widget.core.logging.Logger
//import kz.gov.mia.sos.widget.utils.ImageCompressor
//import java.io.File
//
//internal class MediaStoreManager {
//
//    companion object {
//        private val TAG = simpleNameOf<MediaStoreManager>()
//
//        fun dispose() {
//            VideoCompressor.cancel()
//        }
//    }
//
//    suspend fun process(
//        context: Context,
//        image: Image?,
//        uri: Uri
//    ): Image? = withContext(Dispatchers.IO) {
//        val compressed = ImageCompressor()
//            .compress(
//                context = context,
//                imageUri = uri,
//                compressFormat = Bitmap.CompressFormat.JPEG,
//                maxWidth = 1280F,
//                maxHeight = 1280F,
//                useMaxScale = true,
//                quality = 75,
//                minWidth = 150F,
//                minHeight = 150F
//            ) ?: return@withContext null
//
//        val file = compressed.toFile()
//
//        var outputImage: Image = image
//            ?: Image(
//                id = Content.generateId(),
//                uri = uri,
//                title = file.name,
//                displayName = null,
//                folder = Folder(Folder.generateId(), file.parent),
//                history = Content.History(modifiedAt = file.lastModified()),
//                resolution = null,
//                properties = Content.Properties(
//                    size = file.length(),
//                    mimeType = context.contentResolver.getType(uri)
//                ),
//                publicFile = Content.PublicFile(compressed),
//                remoteFile = null
//            )
//
//        val resolution = runCatching {
//            val inputStream = context.contentResolver?.openInputStream(compressed) ?: return@runCatching null
//            return@runCatching inputStream.getImageResolution()
//        }.getOrNull()
//
//        outputImage = outputImage.copy(resolution = resolution)
//
//        Logger.debug(TAG, "selected -> image: $outputImage")
//
//        return@withContext outputImage
//    }
//
//    fun process(
//        context: Context,
//        video: Video?,
//        uri: Uri,
//        onProgress: (percent: Float) -> Unit = {},
//        onSuccess: (video: Video?) -> Unit = {}
//    ) {
//        val directory = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
//        val file = File(directory, createVideoFilename(context))
//        VideoCompressor.start(
//            context = context,
//            srcUri = uri,
//            destPath = file.absolutePath,
//            listener = object : CompressionListener {
//                override fun onStart() {
//                    Logger.error(TAG, "onStart()")
//                }
//
//                override fun onProgress(percent: Float) {
//                    Logger.error(TAG, "onProgress() -> $percent")
//                    onProgress(percent)
//                }
//
//                override fun onSuccess() {
//                    Logger.error(TAG, "onSuccess() -> ${file.length()}")
//
//                    val outputVideo = video?.copy(
//                        id = Content.generateId(),
//                        uri = uri,
//                        title = file.name,
//                        folder = Folder(Folder.generateId(), file.parent),
//                        duration = null,
//                        properties = Content.Properties(
//                            size = file.length(),
//                            mimeType = context.contentResolver.getType(uri)
//                        ),
//                        publicFile = Content.PublicFile(file.toUri())
//                    ) ?: Video(
//                        id = Content.generateId(),
//                        uri = uri,
//                        title = file.name,
//                        displayName = null,
//                        folder = Folder(Folder.generateId(), file.parent),
//                        history = Content.History(modifiedAt = file.lastModified()),
//                        duration = null,
//                        resolution = null,
//                        properties = Content.Properties(
//                            size = file.length(),
//                            mimeType = context.contentResolver.getType(uri)
//                        ),
//                        publicFile = Content.PublicFile(file.toUri()),
//                        remoteFile = null
//                    )
//
//                    Logger.error(TAG, "onSuccess() -> $outputVideo")
//
//                    onSuccess(outputVideo)
//                }
//
//                override fun onFailure(failureMessage: String) {
//                    Logger.error(TAG, "onFailure() -> $failureMessage")
//                }
//
//                override fun onCancelled() {
//                    Logger.error(TAG, "onCancelled()")
//                }
//            },
//            configureWith = Configuration(
//                quality = VideoQuality.MEDIUM,
//                frameRate = 24,
//                isMinBitrateCheckEnabled = false
//            )
//        )
//    }
//
//    private fun createVideoFilename(context: Context): String {
//        val timestamp = System.currentTimeMillis()
//        return "VIDEO_${context.packageName}_${timestamp}.mp4"
//    }
//
//}