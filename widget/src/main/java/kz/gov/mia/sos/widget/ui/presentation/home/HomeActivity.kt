package kz.gov.mia.sos.widget.ui.presentation.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.widget.LinearLayout
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.LocationRequest
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import kz.garage.activity.toast.toast
import kz.garage.activity.view.bind
import kz.garage.animation.scale.setScaleAnimationOnClick
import kz.garage.context.system.service.clipboardManager
import kz.garage.kotlin.simpleNameOf
import kz.gov.mia.sos.widget.R
import kz.gov.mia.sos.widget.api.SOSWidget
import kz.gov.mia.sos.widget.api.exception.AuthCredentialsNullOrBlankException
import kz.gov.mia.sos.widget.core.URLManager
import kz.gov.mia.sos.widget.core.location.LocationInteractor
import kz.gov.mia.sos.widget.core.location.settings.asString
import kz.gov.mia.sos.widget.core.logging.Logger
import kz.gov.mia.sos.widget.core.permission.isPermissionGranted
import kz.gov.mia.sos.widget.ui.component.SOSWidgetProgressView
import kz.gov.mia.sos.widget.ui.platform.BaseActivity
import kz.gov.mia.sos.widget.ui.platform.alert.ThemedAlertDialog
import kz.gov.mia.sos.widget.ui.presentation.call.CallActivity
import kz.gov.mia.sos.widget.ui.presentation.home.vm.HomeViewModel
import kz.gov.mia.sos.widget.utils.createApplicationSettingsIntent
import kz.gov.mia.sos.widget.utils.createLocationSettingsIntent
import kz.gov.mia.sos.widget.utils.location.getDisplayElapsedTime
import kz.gov.mia.sos.widget.utils.setupActionBar
import kz.inqbox.sdk.socket.Socket
import kz.inqbox.sdk.webrtc.WebRTC

class HomeActivity : BaseActivity() {

    companion object {
        private val TAG = simpleNameOf<HomeActivity>()

        private val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        fun newIntent(context: Context, params: Params) =
            Intent(context, HomeActivity::class.java)
                .putExtra("base_url", params.baseUrl)
                .putExtra("username", params.username)
                .putExtra("password", params.password)
                .putExtra("is_logging_enabled", params.isLoggingEnabled)
                .putExtra("call_topic", params.callTopic)
    }

    data class Params constructor(
        val baseUrl: String,
        val username: String,
        val password: String,
        val isLoggingEnabled: Boolean,
        val callTopic: String
    )

    private val toolbar by bind<MaterialToolbar>(R.id.toolbar)
    private val locationView by bind<LinearLayout>(R.id.locationView)
    private val locationValueView by bind<MaterialTextView>(R.id.locationValueView)
    private val sosButton by bind<ShapeableImageView>(R.id.sosButton)
    private val progressView by bind<SOSWidgetProgressView>(R.id.progressView)

    private var viewModel: HomeViewModel? = null

    private var locationPermissionsAlertDialog: androidx.appcompat.app.AlertDialog? = null

    private var resolutionResolveCallback: ((location: Location?) -> Unit)? = null

    private val locationPermissionsRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            Logger.debug(TAG, "locationPermissionsRequest -> permissions: $permissions")

            if (permissions.all { it.value }) {
                start()
            } else {
                locationPermissionsAlertDialog?.cancel()
                locationPermissionsAlertDialog = null
                locationPermissionsAlertDialog = ThemedAlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.sos_widget_permission_settings)
                    .setMessage(
                        getString(
                            R.string.sos_widget_info_permissions_necessity,
                            getString(R.string.sos_widget_location)
                        )
                    )
                    .setNegativeButton(R.string.sos_widget_cancel) { dialog, _ ->
                        locationPermissionsAlertDialog = null
                        dialog.dismiss()

                        finish()
                    }
                    .setPositiveButton(R.string.sos_widget_to_settings) { dialog, _ ->
                        locationPermissionsAlertDialog = null
                        dialog.dismiss()

                        startActivity(createApplicationSettingsIntent())
                    }
                    .show()
            }
        }

    private val locationSettingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Logger.debug(TAG, "locationSettingsLauncher -> result: $result")
        }

    // Location Settings Issue
    @SuppressLint("MissingPermission")
    private val resolutionResolveLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            Logger.debug(TAG, "resolutionResolveLauncher -> result: $result")

            if (result.resultCode != Activity.RESULT_OK) {
                resolutionResolveCallback =
                    null  // Code flow will continue with onResume() lifecycle event
                return@registerForActivityResult
            }

            if (LOCATION_PERMISSIONS.all { isPermissionGranted(it) }) {
                LocationInteractor.getFusedSingleLocation(
                    context = this,
                    onResult = { location ->
                        resolutionResolveCallback?.invoke(location)
                        resolutionResolveCallback = null
                    },
                    onFailure = {
                        it.printStackTrace()

                        LocationInteractor.getSingleLocation(
                            context = this,
                            onResult = { location ->
                                resolutionResolveCallback?.invoke(location)
                                resolutionResolveCallback = null
                            },
                            onFailure = { exception ->
                                exception.printStackTrace()
                                if (exception is IllegalStateException) {
                                    showGPSDisabledErrorAlert()
                                } else {
                                    resolutionResolveCallback?.invoke(null)
                                }
                                resolutionResolveCallback = null
                            }
                        )
                    }
                )
            } else {
                resolutionResolveCallback = null
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sos_widget_activity_home)

        init()

        if (SOSWidget.credentials == null) {
            val username = intent.getStringExtra("username")
            val password = intent.getStringExtra("password")
            if (username.isNullOrBlank() || password.isNullOrBlank()) {
                throw AuthCredentialsNullOrBlankException()
            }
            SOSWidget.credentials = SOSWidget.Credentials(username, password)
        }

        if (viewModel == null) {
            viewModel = injection.provideHomeViewModel(
                this,
                intent.getStringExtra("call_topic")
            )
        }

        setupBaseUrl()
        setupActionBar()
        setupLocationView()
        setupSOSButton()

        observeState()
        observeCommand()
        observeMessage()
        observeRoute()
        observeGeocode()
    }

    override fun onResume() {
        super.onResume()

        if (locationPermissionsAlertDialog == null) {
            if (resolutionResolveCallback == null) {
                if (LOCATION_PERMISSIONS.all { isPermissionGranted(it) }) {
                    start()
                } else {
                    locationPermissionsRequest.launch(LOCATION_PERMISSIONS)
                }
            } else {
                Logger.error(TAG, "resolutionResolveCallback is not null!")
            }
        } else {
            Logger.error(TAG, "locationPermissionsAlertDialog is not null!")
        }
    }

    private fun init() {
        val isLoggingEnabled = intent.getBooleanExtra("is_logging_enabled", false)

        Logger.init(isLoggingEnabled = isLoggingEnabled)

        Socket.init(isLoggingEnabled = isLoggingEnabled, language = getLanguage())

        WebRTC.init(isLoggingEnabled = isLoggingEnabled)
    }

    @SuppressLint("MissingPermission")
    private fun start() {
        checkLocationProviderStatus(
            onLocationEnabled = {
                viewModel?.onLocationProviderEnabled()
            },
            onRejectedProceed = {
                finish()
            },
            onAgreedToProceed = {
                if (LOCATION_PERMISSIONS.all { isPermissionGranted(it) }) {
                    requestSingleLocationUpdate { location ->
                        viewModel?.onLocationFound(location)
                    }
                } else {
                    locationPermissionsRequest.launch(LOCATION_PERMISSIONS)
                }
            }
        )
    }

    private fun setupBaseUrl() {
        if (URLManager.isBaseUrlNullOrBlank()) {
            val baseUrl = intent.getStringExtra("base_url")
            if (!URLManager.setBaseUrl(baseUrl)) {
                finish()
            }
        }
    }

    private fun setupActionBar() {
        setupActionBar(toolbar) { finish() }
    }

    private fun setupLocationView() {
        locationView.setOnClickListener {
            viewModel?.onLocationShow()
        }
    }

    private fun setupSOSButton() {
        sosButton.setScaleAnimationOnClick(90F) {
            viewModel?.onSOSButtonPressed()
        }
    }

    private fun observeState() {
        viewModel?.getState()?.observe(this, { state ->
            when (state) {
                HomeScreen.State.IDLE -> {
                    progressView.hide()
                }
                is HomeScreen.State.Loading -> {
                    when (state.phase) {
                        HomeScreen.State.Phase.WEB_SOCKET_CONNECTION -> {
                            progressView.setText("(${state.phase.displayIndex}/${state.phase.size}) WebSocket connection")
                            progressView.show()
                        }
                        HomeScreen.State.Phase.ICE_SERVERS_REQUEST -> {
                            progressView.setText("(${state.phase.displayIndex}/${state.phase.size}) Ice servers request")
                            progressView.show()
                        }
                    }
                }
                is HomeScreen.State.Error -> {
                    when (state.phase) {
                        HomeScreen.State.Phase.WEB_SOCKET_CONNECTION -> {
                            toast("WEB_SOCKET_CONNECTION")
                        }
                        HomeScreen.State.Phase.ICE_SERVERS_REQUEST -> {
                            toast("ICE_SERVERS_REQUEST")
                        }
                    }
                }
                HomeScreen.State.Content -> {
                    progressView.hide()
                }
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun observeCommand() {
        viewModel?.getCommand()?.observe(this, { command ->
            when (command) {
                HomeScreen.Command.RequestLocation -> {
                    if (LOCATION_PERMISSIONS.all { isPermissionGranted(it) }) {
                        requestSingleLocationUpdate { location ->
                            viewModel?.onLocationFound(location)
                        }
                    } else {
                        locationPermissionsRequest.launch(LOCATION_PERMISSIONS)
                    }
                }
                else -> {
                }
            }
        })
    }

    private fun observeMessage() {
        viewModel?.getMessage()?.observe(this, { message ->
            when (message) {
                HomeScreen.Message.WebSocket.ConnectionFailed -> {
                    toast("WebSocket connection failed!")
                }
                else -> {
                }
            }
        })
    }

    private fun observeRoute() {
        viewModel?.getRoute()?.observe(this, { route ->
            when (route) {
                HomeScreen.Route.Back -> {
                    super.onBackPressed()
                }
                is HomeScreen.Route.LocationDetails -> {
                    alertDialog?.dismiss()
                    alertDialog = null

                    var text = route.displayAddress

                    text += "\n\n" + getString(
                        R.string.sos_widget_latitude_and_longitude,
                        route.location.latitude,
                        route.location.longitude
                    )

                    if (route.location.xAccuracyMeters != null) {
                        text += "\n" + getString(
                            R.string.sos_widget_location_accuracy,
                            route.location.xAccuracyMeters
                        )
                    }

                    text += "\n" + getString(
                        R.string.sos_widget_location_elapsed_time,
                        route.location.getDisplayElapsedTime()
                    )

                    alertDialog = ThemedAlertDialog.Builder(this)
                        .setCancelable(true)
                        .setTitle(R.string.sos_widget_my_geolocation)
                        .setMessage(text)
                        .setNeutralButton(R.string.sos_widget_close) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .setNegativeButton(R.string.sos_widget_refresh) { dialog, _ ->
                            dialog.dismiss()

                            viewModel?.onRefreshLocationDataRequested()
                        }
                        .setPositiveButton(R.string.sos_widget_copy) { dialog, _ ->
                            dialog.dismiss()

                            clipboardManager?.setPrimaryClip(
                                ClipData
                                    .newPlainText(
                                        getString(R.string.sos_widget_message),
                                        route.displayAddress
                                    )
                            )
                            toast(R.string.sos_widget_copied)
                        }
                        .show()
                }
                is HomeScreen.Route.Call -> {
                    startActivity(
                        CallActivity.newIntent(
                            context = this,
                            callType = route.callType,
                            callTopic = route.callTopic
                        )
                    )
                }
                else -> {
                }
            }
        })
    }

    private fun observeGeocode() {
        viewModel?.getGeocode()?.observe(this) { geocode ->
            if (geocode == null) {
                locationValueView.text = getString(R.string.sos_widget_searching, "...")
            } else {
                locationValueView.text = geocode.getDisplayAddress()
            }
        }
    }

    @RequiresPermission(
        allOf = [
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        ]
    )
    fun requestSingleLocationUpdate(callback: (location: Location?) -> Unit) {
        Logger.debug(TAG, "requestSingleLocationUpdate() -> callback: $callback")

        fun getAlternativeSingleLocation() {
            LocationInteractor.getSingleLocation(
                context = this,
                onResult = { location ->
                    callback(location)
                },
                onFailure = { exception ->
                    exception.printStackTrace()
                    if (exception is IllegalStateException) {
                        showGPSDisabledErrorAlert()
                    } else {
                        callback(null)
                    }
                }
            )
        }

        LocationInteractor.checkLocationSettings(
            context = this,
            locationRequest = LocationRequest
                .create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setWaitForAccurateLocation(true),
            onSuccess = { states ->
                Logger.debug(TAG, "requestSingleLocationUpdate() -> onSuccess()")

                if (states == null) {
                    Logger.error(
                        TAG,
                        "requestSingleLocationUpdate() -> onSuccess() -> locationSettingsStates is null!"
                    )
                } else {
                    Logger.debug(
                        TAG,
                        "requestSingleLocationUpdate() -> onSuccess() -> locationSettingsStates: ${states.asString()}"
                    )
                }

                LocationInteractor.getFusedSingleLocation(
                    context = this,
                    onResult = { location ->
                        callback(location)
                    },
                    onFailure = {
                        it.printStackTrace()
                        getAlternativeSingleLocation()
                    }
                )
            },
            onResolutionRequired = {
                Logger.debug(TAG, "requestSingleLocationUpdate() -> onResolutionRequired()")

                it.printStackTrace()

                try {
                    resolutionResolveCallback = callback
                    resolutionResolveLauncher.launch(
                        IntentSenderRequest.Builder(it.resolution)
                            .build()
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    resolutionResolveCallback = null
                    getAlternativeSingleLocation()
                }
            },
            onSettingsChangeUnavailable = {
                Logger.debug(TAG, "requestSingleLocationUpdate() -> onSettingsChangeUnavailable()")
                getAlternativeSingleLocation()
            },
            onFailure = {
                Logger.debug(TAG, "requestSingleLocationUpdate() -> onFailure()")
                it.printStackTrace()
                getAlternativeSingleLocation()
            }
        )
    }

    private fun showGPSDisabledErrorAlert() {
        alertDialog?.dismiss()
        alertDialog = null

        alertDialog = ThemedAlertDialog.Builder(this)
            .setCancelable(false)
            .setTitle(R.string.sos_widget_attention)
            .setMessage(R.string.sos_widget_request_welcome_gps_enable)
            .setPositiveButton(R.string.sos_widget_ok) { dialog, _ ->
                dialog.dismiss()

                locationSettingsLauncher.launch(createLocationSettingsIntent())
            }
            .show()
    }

}