package kz.gov.mia.sos.widget.data.remote.file

import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.FileAsyncHttpResponseHandler
import com.loopj.android.http.RequestHandle
import cz.msebera.android.httpclient.Header
import kz.gov.mia.sos.widget.data.remote.http.file.DownloadState
import java.io.File

internal fun AsyncHttpClient.download(
    file: File,
    url: String,
    listener: (state: DownloadState) -> Unit
): RequestHandle? {
    return get(url, object : FileAsyncHttpResponseHandler(file) {
        override fun onProgress(bytesWritten: Long, totalSize: Long) {
            super.onProgress(bytesWritten, totalSize)

            val progress = 100F * bytesWritten / totalSize
            listener(DownloadState.Progress(progress))
        }

        override fun onSuccess(statusCode: Int, headers: Array<out Header>?, file: File?) {
            listener(DownloadState.Success)
        }

        override fun onFailure(
            statusCode: Int,
            headers: Array<out Header>?,
            throwable: Throwable?,
            file: File?
        ) {
            listener(DownloadState.Error())
        }
    })
}