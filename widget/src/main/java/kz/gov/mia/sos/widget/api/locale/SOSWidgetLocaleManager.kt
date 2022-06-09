package kz.gov.mia.sos.widget.api.locale

import android.app.Activity
import android.content.Context
import kz.garage.locale.LocaleManager
import kz.garage.locale.utils.ActivityRecreationHelper
import kz.inqbox.sdk.domain.model.language.Language
import java.util.*

object SOSWidgetLocaleManager {

    fun initialize(context: Context, locales: List<Locale>) {
        LocaleManager.initialize(context, locales)
    }

    fun onConfigurationChanged() {
        LocaleManager.onConfigurationChanged()
    }

    fun getLocale(): Locale =
        LocaleManager.getLocale() ?: Language.DEFAULT.locale

    fun setLocale(activity: Activity, locale: Locale) {
        LocaleManager.setLocale(locale)
        ActivityRecreationHelper.recreate(activity, true)
    }

}