package kz.gov.mia.sos.widget.core.multimedia.source

import android.net.Uri

internal sealed class Source constructor(
    open val uri: Uri,
    open val title: String
) {
    data class LocalFile constructor(
        override val uri: Uri,
        override val title: String
    ) : Source(uri, title)

    data class URL constructor(
        override val uri: Uri,
        override val title: String
    ) : Source(uri, title)
}
