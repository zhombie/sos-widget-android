package kz.gov.mia.sos.widget.data.remote.http.file

sealed interface UploadState {
    data class Progress constructor(
        val progress: Float
    ) : UploadState

    data class Success constructor(
        val hash: String,
        val title: String?,
        val urlPath: String
    ) : UploadState

    data class Error constructor(
        val message: String? = null,
        val cause: Throwable? = null
    ) : UploadState
}