package kz.gov.mia.sos.widget.api.locale

import android.content.Context
import kz.garage.locale.LocaleManager
import java.util.*

object SOSWidgetLocaleManager {

    fun initialize(context: Context, locales: List<Locale>) {
        LocaleManager.initialize(context, locales)
    }

    fun onConfigurationChanged() {
        LocaleManager.onConfigurationChanged()
    }

}