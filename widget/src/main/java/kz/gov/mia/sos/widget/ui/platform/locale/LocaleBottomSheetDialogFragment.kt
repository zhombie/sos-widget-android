package kz.gov.mia.sos.widget.ui.platform.locale

import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kz.gov.mia.sos.widget.ui.platform.BaseActivity
import kz.inqbox.sdk.domain.model.language.Language
import java.util.*

internal abstract class LocaleBottomSheetDialogFragment : BottomSheetDialogFragment() {

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
