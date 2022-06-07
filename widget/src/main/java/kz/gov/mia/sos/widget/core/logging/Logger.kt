package kz.gov.mia.sos.widget.core.logging

import android.util.Log

internal object Logger {

    private const val LIMIT = 4000

    var isEnabled = false
        @Synchronized get
        @Synchronized private set

    fun init(isLoggingEnabled: Boolean) {
        isEnabled = isLoggingEnabled
    }

    fun destroy() {
        isEnabled = false
    }

    fun debug(tag: String, message: String) {
        if (isEnabled) {
            if (message.length > LIMIT) {
                Log.d(tag, message.substring(0, LIMIT))
                debug(tag, message.substring(LIMIT))
            } else {
                Log.d(tag, message)
            }
        }
    }

    fun error(tag: String, message: String) {
        if (isEnabled) {
            if (message.length > LIMIT) {
                Log.e(tag, message.substring(0, LIMIT))
                error(tag, message.substring(LIMIT))
            } else {
                Log.e(tag, message)
            }
        }
    }

    fun warn(tag: String, message: String) {
        if (isEnabled) {
            if (message.length > LIMIT) {
                Log.w(tag, message.substring(0, LIMIT))
                warn(tag, message.substring(LIMIT))
            } else {
                Log.w(tag, message)
            }
        }
    }

}