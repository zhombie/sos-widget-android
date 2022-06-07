package kz.gov.mia.sos.widget.ui.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kz.garage.multimedia.store.model.Content

@Parcelize
open class DownloadState private constructor(
    open val content: Content,
    open val itemPosition: Int
) : Parcelable {

    @Parcelize
    data class Pending constructor(
        override val content: Content,
        override val itemPosition: Int,
        val progress: Float
    ) : DownloadState(content, itemPosition), Parcelable

    @Parcelize
    data class Cancelled constructor(
        override val content: Content,
        override val itemPosition: Int,
        val progress: Float
    ) : DownloadState(content, itemPosition), Parcelable

    @Parcelize
    data class Completed constructor(
        override val content: Content,
        override val itemPosition: Int
    ) : DownloadState(content, itemPosition), Parcelable

}