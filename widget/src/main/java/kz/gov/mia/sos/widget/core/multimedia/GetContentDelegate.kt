package kz.gov.mia.sos.widget.core.multimedia

import kz.garage.multimedia.store.model.Content

interface GetContentDelegate {
    sealed interface Result {
        data class Success constructor(
            val content: Content
        ) : Result

        sealed interface Error : Result {
            object Undefined : Error
            object NullableUri : Error
            data class SizeLimitExceeds constructor(
                val maxSize: Int  // in megabytes
            ) : Error
        }
    }

    fun onContentResult(result: Result)
}