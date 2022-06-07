package kz.gov.mia.sos.widget.ui.platform.locale

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.LocaleManagerAppCompatDelegate
import kz.garage.locale.LocaleManager
import kz.garage.locale.utils.ActivityRecreationHelper
import kz.inqbox.sdk.domain.model.language.Language
import java.util.*

abstract class LocaleActivity : AppCompatActivity() {

    private var localeManagerAppCompatDelegate: LocaleManagerAppCompatDelegate? = null

    override fun getDelegate(): AppCompatDelegate {
        if (localeManagerAppCompatDelegate == null) {
            localeManagerAppCompatDelegate = LocaleManagerAppCompatDelegate(super.getDelegate())
        }
        return requireNotNull(localeManagerAppCompatDelegate)
    }

    override fun onResume() {
        super.onResume()

        ActivityRecreationHelper.onResume(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        ActivityRecreationHelper.onDestroy(this)
    }

    fun getLocale(): Locale =
        LocaleManager.getLocale() ?: Language.DEFAULT.locale

    fun getLanguage(): Language =
        Language.from(getLocale())

    fun setLocale(locale: Locale) {
        LocaleManager.setLocale(locale)
        onSetLocale(locale)
        ActivityRecreationHelper.recreate(this, true)
    }

    abstract fun onSetLocale(locale: Locale)

}