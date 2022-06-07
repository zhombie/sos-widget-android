package kz.gov.mia.sos.widget.core.multimedia.process

import kz.garage.file.extension.Extension
import kz.garage.kotlin.enums.findEnumBy
import kz.garage.multimedia.store.model.Content

internal fun Content.processFile(): ProcessFile {
    val file = publicFile?.getFile() ?: return ProcessFile.Error.FileNotFound
    return if (file.exists()) {
        var fileName = if (file.name.isNullOrBlank()) {
            remoteFile?.url?.split("/")?.last() ?: "undefined"
        } else {
            file.name
        }
        val title = title
        if (!title.isNullOrBlank()) {
            fileName = title
        }
        val displayName = displayName
        if (!displayName.isNullOrBlank()) {
            fileName = displayName
        }
        val extension = findEnumBy<Extension> { it.value == file.extension.lowercase() }?.value
        if (extension.isNullOrBlank()) {
            return ProcessFile.Error.ExtensionNotFound
        }
        if (fileName.endsWith(extension)) {
            ProcessFile.Success(file, fileName)
        } else {
            ProcessFile.Success(file, fileName.removeSuffix(".") + "." + extension)
        }
    } else {
        ProcessFile.Error.FileNotFound
    }
}