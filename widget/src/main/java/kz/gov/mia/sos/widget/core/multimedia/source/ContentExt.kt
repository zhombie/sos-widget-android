package kz.gov.mia.sos.widget.core.multimedia.source

import android.net.Uri
import kz.garage.multimedia.store.model.Content
import kz.gov.mia.sos.widget.core.URLManager

internal val Content.source: Source?
    get() {
        val file = publicFile?.getFile()
        return if (file?.exists() == true) {
            Source.LocalFile(
                uri = Uri.fromFile(file),
                title = label ?: file.name
            )
        } else {
            val url = URLManager.buildUrl(remoteFile?.uri)
            if (url.isNullOrBlank()) {
                null
            } else {
                Source.RemoteFile(
                    uri = Uri.parse(url),
                    title = label ?: url.split("/").last()
                )
            }
        }
    }

internal val Content.sourceUri: Uri?
    get() = when (val source = source) {
        is Source.LocalFile, is Source.RemoteFile ->
            source.uri
        else ->
            null
    }
