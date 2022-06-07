package kz.gov.mia.sos.widget.ui.platform

import com.google.android.material.snackbar.Snackbar
import kz.garage.fragment.keyboard.hideKeyboard
import kz.gov.mia.sos.widget.R
import kz.gov.mia.sos.widget.di.Injection
import kz.gov.mia.sos.widget.ui.platform.resource.ResourceBottomSheetDialogFragment

internal abstract class BaseBottomSheetDialogFragment : ResourceBottomSheetDialogFragment() {

    val injection: Injection
        get() = Injection.getInstance(requireContext())

    // UI dialogs
    protected var snackbar: Snackbar? = null

    override fun getTheme(): Int = R.style.SOSWidget_BottomSheetDialog

    override fun onPause() {
        super.onPause()

        hideKeyboard()
    }

    override fun onDestroy() {
        snackbar?.dismiss()
        snackbar = null

        super.onDestroy()
    }

}