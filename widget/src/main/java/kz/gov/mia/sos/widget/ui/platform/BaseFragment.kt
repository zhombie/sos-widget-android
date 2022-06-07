package kz.gov.mia.sos.widget.ui.platform

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import kz.garage.kotlin.simpleNameOf
import kz.gov.mia.sos.widget.core.logging.Logger
import kz.gov.mia.sos.widget.ui.platform.resource.ResourceFragment

abstract class BaseFragment constructor(
    @LayoutRes contentLayoutId: Int
) : ResourceFragment(contentLayoutId) {

    companion object {
        private val TAG = simpleNameOf<BaseFragment>()
    }

    constructor() : this(0)

//    val injection: Injection
//        get() = Injection.getInstance(requireContext())

    // Menu
    protected var menu: Menu? = null

    // UI dialogs
    protected var alertDialog: AlertDialog? = null
    protected var bottomSheetDialogFragment: BottomSheetDialogFragment? = null
    protected var snackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        logLifecycleEvent("onCreate")
    }

    override fun onStart() {
        super.onStart()

        logLifecycleEvent("onStart")
    }

    override fun onResume() {
        super.onResume()

        logLifecycleEvent("onResume")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        logLifecycleEvent("onCreateView")
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logLifecycleEvent("onViewCreated")
    }

    override fun onPause() {
        super.onPause()

        logLifecycleEvent("onPause")
    }

    override fun onDestroyView() {
        menu = null

        try {
            alertDialog?.dismiss()
        } catch (e: IllegalStateException) {
        } finally {
            alertDialog = null
        }

        try {
            bottomSheetDialogFragment?.dismiss()
        } catch (e: Exception) {
        } finally {
            bottomSheetDialogFragment = null
        }

        snackbar?.dismiss()
        snackbar = null

        super.onDestroyView()

        logLifecycleEvent("onDestroyView")
    }

    override fun onDestroy() {
        super.onDestroy()

        logLifecycleEvent("onDestroy")
    }

    private fun logLifecycleEvent(event: String) {
        Logger.debug(TAG, "$event()")
    }

}