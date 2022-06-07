package kz.gov.mia.sos.widget.core.error

import kz.gov.mia.sos.widget.R

internal class InvalidOutputFileException : BaseException() {
    override val text: Int
        get() = R.string.sos_widget_error_invalid_output_file
}