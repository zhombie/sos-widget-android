package kz.gov.mia.sos.widget.core

import android.content.Context
import android.content.SharedPreferences

internal object Preferences {
    private const val DEFAULT_NAME = "sos.widget.preferences"

    fun getSharedPreferences(context: Context, name: String = DEFAULT_NAME): SharedPreferences {
        return context.getSharedPreferences(name, Context.MODE_PRIVATE)
    }
}