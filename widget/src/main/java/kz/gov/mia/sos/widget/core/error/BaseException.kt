package kz.gov.mia.sos.widget.core.error

import androidx.annotation.StringRes

internal abstract class BaseException : Exception() {
    abstract val text: Int
        @StringRes get
}