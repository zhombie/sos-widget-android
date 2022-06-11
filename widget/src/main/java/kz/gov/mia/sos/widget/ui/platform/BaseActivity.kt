package kz.gov.mia.sos.widget.ui.platform

import android.annotation.SuppressLint
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import kz.garage.context.location.manager.isLocationEnabled
import kz.garage.kotlin.simpleNameOf
import kz.gov.mia.sos.widget.R
import kz.gov.mia.sos.widget.core.logging.Logger
import kz.gov.mia.sos.widget.di.Injection
import kz.gov.mia.sos.widget.ui.platform.alert.ThemedAlertDialog
import kz.gov.mia.sos.widget.ui.platform.resource.ResourceActivity
import kz.inqbox.sdk.domain.model.language.Language
import java.util.*

abstract class BaseActivity : ResourceActivity() {

    companion object {
        private val TAG = simpleNameOf<BaseActivity>()
    }

    internal val injection: Injection
        get() = Injection.getInstance(this)

    // UI dialogs
    protected var alertDialog: androidx.appcompat.app.AlertDialog? = null
    protected var bottomSheetDialogFragment: BottomSheetDialogFragment? = null
    protected var snackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        logLifecycleEvent("onCreate")

        injection.settingsRepository
            .setLanguage(language = getLanguage())
    }

    override fun onResume() {
        super.onResume()

        logLifecycleEvent("onResume")
    }

    override fun onBackPressed() {
        super.onBackPressed()

        logLifecycleEvent("onBackPressed")
    }

    override fun onPause() {
        try {
            alertDialog?.dismiss()
        } catch (e: IllegalStateException) {
        } finally {
            alertDialog = null
        }

        try {
            bottomSheetDialogFragment?.dismiss()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            bottomSheetDialogFragment = null
        }

        snackbar?.dismiss()
        snackbar = null

        super.onPause()

        logLifecycleEvent("onPause")
    }

    override fun onDestroy() {
        super.onDestroy()

        logLifecycleEvent("onDestroy")
    }

    override fun onSetLocale(locale: Locale) {
        injection.settingsRepository
            .setLanguage(language = Language.from(locale))
    }

    @SuppressLint("MissingPermission")
    protected fun checkLocationProviderStatus(
        onLocationEnabled: () -> Unit,
        onRejectedProceed: () -> Unit,
        onAgreedToProceed: () -> Unit
    ) {
        if (isLocationEnabled()) {
            onLocationEnabled.invoke()
        } else {
            alertDialog?.dismiss()
            alertDialog = null

            alertDialog = ThemedAlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.sos_widget_attention)
                .setMessage(R.string.sos_widget_request_gps_enable)
                .setNegativeButton(R.string.sos_widget_cancel) { dialog, _ ->
                    alertDialog = null
                    dialog.dismiss()

                    onRejectedProceed.invoke()
                }
                .setPositiveButton(R.string.sos_widget_ok) { dialog, _ ->
                    alertDialog = null
                    dialog.dismiss()

                    onAgreedToProceed.invoke()
                }
                .show()
        }
    }

    private fun logLifecycleEvent(event: String) {
        Logger.debug(TAG, "$event()")
    }

}