package kz.gov.mia.sos.widget.data.remote.file

import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.JsonHttpResponseHandler
import com.loopj.android.http.RequestHandle
import com.loopj.android.http.RequestParams
import cz.msebera.android.httpclient.Header
import kz.gov.mia.sos.widget.core.logging.Logger
import kz.gov.mia.sos.widget.data.remote.http.file.UploadState
import org.json.JSONArray
import org.json.JSONObject

internal fun AsyncHttpClient.upload(
    url: String?,
    params: RequestParams,
    listener: (state: UploadState) -> Unit
): RequestHandle? {
    if (url.isNullOrBlank()) {
        listener(UploadState.Error(cause = NullPointerException()))
        return null
    }

    params.setUseJsonStreamer(false)
    params.setForceMultipartEntityContentType(true)

    return post(url, params, object : JsonHttpResponseHandler() {
        override fun onProgress(bytesWritten: Long, totalSize: Long) {
            val progress = 100F * bytesWritten / totalSize
            listener(UploadState.Progress(progress))
        }

        override fun onSuccess(
            statusCode: Int,
            headers: Array<out Header>?,
            responseString: String?
        ) {
            Logger.debug("AsyncHttpClient", "responseString: $responseString")
        }

        override fun onSuccess(
            statusCode: Int,
            headers: Array<out Header>?,
            response: JSONObject?
        ) {
            Logger.debug("AsyncHttpClient", "response: $response")

            val hash = response?.optString("hash")
            val title = response?.optString("title")
            val urlPath = response?.optString("url")

            if (hash.isNullOrBlank() || urlPath.isNullOrBlank()) {
                listener(UploadState.Error(cause = NullPointerException()))
            } else {
                listener(UploadState.Success(hash = hash, title = title, urlPath = urlPath))
            }
        }

        override fun onFailure(
            statusCode: Int,
            headers: Array<out Header>?,
            throwable: Throwable?,
            errorResponse: JSONObject?
        ) {
            listener(UploadState.Error(cause = throwable))
        }

        override fun onFailure(
            statusCode: Int,
            headers: Array<out Header>?,
            throwable: Throwable?,
            errorResponse: JSONArray?
        ) {
            listener(UploadState.Error(cause = throwable))
        }

        override fun onFailure(
            statusCode: Int,
            headers: Array<out Header>?,
            responseString: String?,
            throwable: Throwable?
        ) {
            listener(UploadState.Error(cause = throwable))
        }
    })
}