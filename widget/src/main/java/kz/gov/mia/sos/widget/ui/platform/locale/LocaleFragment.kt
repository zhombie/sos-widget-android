package kz.gov.mia.sos.widget.ui.platform.locale

import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import kz.gov.mia.sos.widget.ui.platform.BaseActivity
import kz.inqbox.sdk.domain.model.language.Language
import java.util.*

abstract class LocaleFragment constructor(
    @LayoutRes contentLayoutId: Int
) : Fragment(contentLayoutId) {

    constructor() : this(0)

    protected fun getLocale(): Locale? {
        val activity = activity
        return if (activity is BaseActivity) {
            activity.getLocale()
        } else {
            null
        }
    }

    protected fun getLanguage(): Language {
        val activity = activity
        return if (activity is BaseActivity) {
            activity.getLanguage()
        } else {
            null
        } ?: Language.DEFAULT
    }

}
