package kz.gov.mia.sos.widget.core.multimedia

import android.content.Context
import android.os.Environment
import kz.garage.multimedia.store.model.Content
import java.io.File

internal class DownloadAssistant {

    companion object {
        fun getDownloadsDirectory(context: Context): File =
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir

        fun getDownloadableFile(context: Context, content: Content): File =
            File(getDownloadsDirectory(context), content.downloadableFilename)
    }

}


internal val Content.downloadableFilename: String
    get() = label ?: "DOWNLOAD_${id}"
