package kz.gov.mia.sos.widget.data.remote.http.file

internal sealed interface DownloadState {
    data class Progress constructor(
        val progress: Float
    ) : DownloadState

    object Success : DownloadState

    data class Error constructor(
        val message: String? = null,
        val cause: Exception? = null
    ) : DownloadState
}