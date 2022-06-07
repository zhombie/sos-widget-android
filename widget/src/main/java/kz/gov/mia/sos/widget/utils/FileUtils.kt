package kz.gov.mia.sos.widget.utils

import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.util.*

internal object FileUtils {

    fun getMimeType(context: Context, uri: Uri): String? {
        return if (uri.scheme.equals(ContentResolver.SCHEME_CONTENT)) {
            context.contentResolver.getType(uri)
        } else {
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                fileExtension.lowercase(Locale.getDefault())
            )
        }
    }

    fun createFile(context: Context, fileName: String, extension: String? = null): File {
        return if (extension.isNullOrBlank()) {
            File(getRootDirPath(context) + File.separatorChar + fileName)
        } else {
            if (fileName.endsWith(extension)) {
                File(getRootDirPath(context) + File.separatorChar + fileName)
            } else {
                File(getRootDirPath(context) + File.separatorChar + fileName.removeSuffix(".") + extension.removePrefix("."))
            }
        }
    }

    fun getRootDirPath(context: Context): String {
        val path = if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
            var path = context.filesDir.absolutePath

            var files: Array<File?> = ContextCompat.getExternalFilesDirs(context, null)
            if (files.isNullOrEmpty()) {
                files = ContextCompat.getExternalCacheDirs(context)
            }

            if (!files.isNullOrEmpty()) {
                if (files.first() != null) {
                    path = requireNotNull(files.first()).absolutePath
                }
            }

            path
        } else {
            context.filesDir.absolutePath
        }

        return path
    }

    sealed interface OpenFile {
        data class Success constructor(
            val intent: Intent
        ) : OpenFile {
            fun tryToLaunch(context: Context): Boolean {
                return try {
                    context.startActivity(intent)
                    true
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                    false
                }
            }
        }

        sealed interface Error : OpenFile {
            object Unknown : Error
            object FileDoesNotExist : Error
        }
    }

    fun openFile(context: Context, file: File?): OpenFile {
        if (file == null) return OpenFile.Error.FileDoesNotExist
        if (file.exists()) {
            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.applicationContext.packageName}.provider",
                    file
                )
                val mimeType = getMimeType(context, uri)
                return OpenFile.Success(
                    Intent(Intent.ACTION_VIEW)
                        .setDataAndType(uri, mimeType)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                )
            } catch (e: Exception) {
                e.printStackTrace()
                return OpenFile.Error.Unknown
            }
        } else {
            return OpenFile.Error.FileDoesNotExist
        }
    }

}