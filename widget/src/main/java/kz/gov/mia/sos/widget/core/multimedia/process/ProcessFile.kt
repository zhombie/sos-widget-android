package kz.gov.mia.sos.widget.core.multimedia.process

import java.io.File

internal sealed interface ProcessFile {
    data class Success constructor(
        val file: File,
        val fileName: String
    ) : ProcessFile

    sealed interface Error : ProcessFile {
        object FileNotFound : Error
        object ExtensionNotFound : Error
    }
}