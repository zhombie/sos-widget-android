package kz.gov.mia.sos.widget.core.error

import kz.gov.mia.sos.widget.R

internal class CannotCreateFileException : BaseException() {
    override val text: Int
        get() = R.string.sos_widget_error_cannot_create_file
}