package kz.gov.mia.sos.widget.core.multimedia

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.core.net.toUri
import androidx.core.provider.DocumentsContractCompat
import androidx.fragment.app.Fragment
import kz.garage.kotlin.simpleNameOf
import kz.garage.multimedia.store.getImageResolution
import kz.garage.multimedia.store.model.*
import kz.gov.mia.sos.widget.core.logging.Logger
import java.io.*
import java.lang.ref.WeakReference

internal class StorageAccessFrameworkInteractor private constructor(
    private val fragmentReference: WeakReference<Fragment>,
    private val getContentDelegate: GetContentDelegate
) {

    companion object {
        private val TAG = simpleNameOf<StorageAccessFrameworkInteractor>()
    }

    constructor(
        fragment: Fragment,
        getContentDelegate: GetContentDelegate
    ) : this(WeakReference(fragment), getContentDelegate)

    private val fragment: Fragment?
        get() = fragmentReference.get()

    private val context: Context?
        get() = fragment?.context

    private val contentResolver: ContentResolver?
        get() = context?.contentResolver

    private val launcher = fragment?.registerForActivityResult(GetContentResultContract()) { uri ->
        if (uri == null) {
            return@registerForActivityResult getContentDelegate.onContentResult(
                GetContentDelegate.Result.Error.NullableUri
            )
        }

        contentResolver?.query(
            uri,
            null,
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.parse(uri)
            }
        }
    }

    fun launchSelection(params: GetContentResultContract.Params) {
        launcher?.launch(params)
    }

    fun dispose() {
        launcher?.unregister()
    }

    private fun Cursor.parse(uri: Uri): Boolean {
        val size = getLongOrNull(getColumnIndex(OpenableColumns.SIZE))

        if (size != null) {
            if (size / (1024 * 1024) > 100) {
                getContentDelegate.onContentResult(
                    GetContentDelegate.Result.Error.SizeLimitExceeds(100)
                )
                return false
            }
        }

        val documentId = DocumentsContractCompat.getDocumentId(uri)

        val displayName = getStringOrNull(getColumnIndex(OpenableColumns.DISPLAY_NAME))

        val mimeType = contentResolver?.getType(uri)

        Logger.debug(TAG, "parse() -> mimeType: $mimeType")

        return when {
            mimeType?.startsWith("image") == true -> {
                parseImage(uri, documentId, displayName)
            }
            mimeType?.startsWith("video") == true -> {
                parseVideo(uri, documentId, displayName)
            }
            mimeType?.startsWith("audio") == true -> {
                parseAudio(uri, documentId, displayName)
            }
            else -> {
                val extension = MimeTypeMap.getSingleton()
                    .getExtensionFromMimeType(mimeType)
                parseDocument(uri, documentId, displayName, extension)
            }
        }
    }

    private fun parseImage(
        uri: Uri,
        documentId: String?,
        displayName: String?
    ): Boolean {
        val file = File(
            context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            displayName ?: "IMG_${context?.packageName}_${System.currentTimeMillis()}.jpg"
        )

        if (!file.exists()) {
            file.createNewFile()
        }

        val outputStream = FileOutputStream(file)

        var resolution: Resolution? = null

        contentResolver?.openInputStream(uri).use { inputStream ->
            if (inputStream != null) {
                inputStream.copyTo(outputStream, 8 * 1024)

                resolution = inputStream.getImageResolution()
            }
        }

        try {
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        getContentDelegate.onContentResult(
            GetContentDelegate.Result.Success(
                Image(
                    id = documentId ?: Content.generateId(),
                    uri = uri,
                    title = null,
                    displayName = displayName,
                    folder = Folder(Folder.generateId(), file.parent),
                    history = Content.History(modifiedAt = file.lastModified()),
                    resolution = resolution,
                    properties = Content.Properties(
                        size = file.length(),
                        mimeType = contentResolver?.getType(uri)
                    ),
                    publicFile = Content.PublicFile(file.toUri()),
                    remoteFile = null
                )
            )
        )

        return true
    }

    private fun parseVideo(
        uri: Uri,
        documentId: String?,
        displayName: String?
    ): Boolean {
        val file = File(
            context?.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
            displayName ?: "VIDEO_${context?.packageName}_${System.currentTimeMillis()}.mp4"
        )

        if (!file.exists()) {
            file.createNewFile()
        }

        val outputStream = FileOutputStream(file)

        contentResolver?.openInputStream(uri).use { inputStream ->
            inputStream?.copyTo(outputStream, 8 * 1024)
        }

        try {
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        getContentDelegate.onContentResult(
            GetContentDelegate.Result.Success(
                Video(
                    id = documentId ?: Content.generateId(),
                    uri = uri,
                    title = null,
                    displayName = displayName,
                    folder = Folder(Folder.generateId(), file.parent),
                    history = Content.History(modifiedAt = file.lastModified()),
                    resolution = null,
                    duration = null,
                    properties = Content.Properties(
                        size = file.length(),
                        mimeType = contentResolver?.getType(uri)
                    ),
                    publicFile = Content.PublicFile(file.toUri()),
                    remoteFile = null
                )
            )
        )

        return true
    }

    private fun parseAudio(
        uri: Uri,
        documentId: String?,
        displayName: String?
    ): Boolean {
        val file = File(
            context?.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
            displayName ?: "AUDIO_${context?.packageName}_${System.currentTimeMillis()}.wav"
        )

        if (!file.exists()) {
            file.createNewFile()
        }

        val outputStream = FileOutputStream(file)

        contentResolver?.openInputStream(uri).use { inputStream ->
            inputStream?.copyTo(outputStream, 8 * 1024)
        }

        try {
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        getContentDelegate.onContentResult(
            GetContentDelegate.Result.Success(
                Audio(
                    id = documentId ?: Content.generateId(),
                    uri = uri,
                    title = null,
                    displayName = displayName,
                    folder = Folder(Folder.generateId(), file.parent),
                    history = Content.History(modifiedAt = file.lastModified()),
                    duration = null,
                    properties = Content.Properties(
                        size = file.length(),
                        mimeType = contentResolver?.getType(uri)
                    ),
                    album = null,
                    publicFile = Content.PublicFile(file.toUri()),
                    remoteFile = null
                )
            )
        )

        return true
    }

    private fun parseDocument(
        uri: Uri,
        documentId: String?,
        displayName: String?,
        extension: String?
    ): Boolean {
        if (extension.isNullOrBlank()) return false

        val file = File(
            context?.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            displayName ?: "DOC_${context?.packageName}_${System.currentTimeMillis()}.$extension"
        )

        if (!file.exists()) {
            file.createNewFile()
        }

        val outputStream = FileOutputStream(file)

        contentResolver?.openInputStream(uri).use { inputStream ->
            inputStream?.copyTo(outputStream, 8 * 1024)
        }

        try {
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        getContentDelegate.onContentResult(
            GetContentDelegate.Result.Success(
                Document(
                    id = documentId ?: Content.generateId(),
                    uri = uri,
                    title = null,
                    displayName = displayName,
                    folder = Folder(Folder.generateId(), file.parent),
                    history = Content.History(modifiedAt = file.lastModified()),
                    properties = Content.Properties(
                        size = file.length(),
                        mimeType = contentResolver?.getType(uri)
                    ),
                    publicFile = Content.PublicFile(file.toUri()),
                    remoteFile = null
                )
            )
        )

        return true
    }

}