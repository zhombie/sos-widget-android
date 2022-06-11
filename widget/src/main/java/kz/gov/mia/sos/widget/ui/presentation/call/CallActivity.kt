package kz.gov.mia.sos.widget.ui.presentation.call

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Location
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationSettingsStates
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textview.MaterialTextView
import kz.garage.activity.keyboard.hideKeyboard
import kz.garage.activity.toast.toast
import kz.garage.activity.view.bind
import kz.garage.chat.model.Message
import kz.garage.context.system.service.audioManager
import kz.garage.kotlin.simpleNameOf
import kz.garage.location.core.provider.LocationProvider
import kz.gov.mia.sos.widget.R
import kz.gov.mia.sos.widget.api.image.load.SOSWidgetImageLoader
import kz.gov.mia.sos.widget.core.URLManager
import kz.gov.mia.sos.widget.core.image.load.load
import kz.gov.mia.sos.widget.core.image.preview.ImagePreviewDialogFragment
import kz.gov.mia.sos.widget.core.location.LocationTracker
import kz.gov.mia.sos.widget.core.location.locationTracker
import kz.gov.mia.sos.widget.core.location.settings.asString
import kz.gov.mia.sos.widget.core.logging.Logger
import kz.gov.mia.sos.widget.core.permission.isPermissionGranted
import kz.gov.mia.sos.widget.ui.component.ReminderView
import kz.gov.mia.sos.widget.ui.model.CallState
import kz.gov.mia.sos.widget.ui.model.L10n
import kz.gov.mia.sos.widget.ui.model.ModalViewState
import kz.gov.mia.sos.widget.ui.platform.BaseActivity
import kz.gov.mia.sos.widget.ui.platform.alert.ThemedAlertDialog
import kz.gov.mia.sos.widget.ui.presentation.call.chat.TextChatFragment
import kz.gov.mia.sos.widget.ui.presentation.call.map.ARMMapFragment
import kz.gov.mia.sos.widget.ui.presentation.call.vm.CallViewModel
import kz.gov.mia.sos.widget.ui.presentation.common.ViewPagerAdapter
import kz.gov.mia.sos.widget.utils.*
import kz.inqbox.sdk.domain.model.call.type.CallType
import kz.inqbox.sdk.socket.model.Card102Status
import kz.inqbox.sdk.webrtc.core.ui.SurfaceViewRenderer
import java.io.IOException
import kotlin.math.roundToInt
import kotlin.properties.Delegates

class CallActivity : BaseActivity() {

    companion object {
        private val TAG = simpleNameOf<CallActivity>()

        private val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        private val VIDEO_CALL_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.BLUETOOTH
        )

        fun newIntent(
            context: Context,
            callType: CallType,
            callTopic: String? = null
        ): Intent =
            Intent(context, CallActivity::class.java)
                .putExtra(IntentKey.CALL_TYPE, callType.value)
                .putExtra(IntentKey.CALL_TOPIC, callTopic)
    }

    private object IntentKey {
        const val CALL_TYPE = "call_type"
        const val CALL_TOPIC = "call_topic"
    }

    class CallMenuItem constructor(
        val isEnabled: Boolean,
        @StringRes val title: Int,
    )

    private val rootLayout by bind<CoordinatorLayout>(R.id.rootLayout)
    private val toolbar by bind<MaterialToolbar>(R.id.toolbar)
    private val smallImageView by bind<ShapeableImageView>(R.id.smallImageView)
    private val largeImageView by bind<ShapeableImageView>(R.id.largeImageView)
    private val titleView by bind<MaterialTextView>(R.id.titleView)
    private val subtitleView by bind<MaterialTextView>(R.id.subtitleView)
    private val tabLayout by bind<TabLayout>(R.id.tabLayout)
    private val toggleButton by bind<MaterialButton>(R.id.toggleButton)
    private val viewPager by bind<ViewPager2>(R.id.viewPager)
    private val videoCallView by bind<FrameLayout>(R.id.videoCallView)
    private val minimizeButton by bind<MaterialButton>(R.id.minimizeButton)
    private val reminderButton by bind<MaterialButton>(R.id.reminderButton)
    private val mapButton by bind<MaterialButton>(R.id.mapButton)
    private val surfaceViewRenderer by bind<SurfaceViewRenderer>(R.id.surfaceViewRenderer)
    private val textView by bind<MaterialTextView>(R.id.textView)
    private val statusView by bind<MaterialTextView>(R.id.statusView)
    private val toggleCameraButton by bind<MaterialButton>(R.id.toggleCameraButton)
    private val toggleAudioButton by bind<MaterialButton>(R.id.toggleAudioButton)
    private val toggleCameraSourceButton by bind<MaterialButton>(R.id.toggleCameraSourceButton)
    private val hangupButton by bind<MaterialButton>(R.id.hangupButton)
    private val reminderLayout by bind<FrameLayout>(R.id.reminderLayout)
    private val reminderView by bind<ReminderView>(R.id.reminderView)

    private var viewModel: CallViewModel? = null

    // ViewPager
    private var adapter: ViewPagerAdapter? = null

    // BottomSheet
    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null

    private val bottomSheetBehaviorCallback by lazy(LazyThreadSafetyMode.NONE) {
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_DRAGGING -> {
                        viewModel?.onModalViewDragged()
                    }
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        viewModel?.onModalViewExpanded()
                    }
                    BottomSheetBehavior.STATE_COLLAPSED,
                    BottomSheetBehavior.STATE_HIDDEN -> {
                        viewModel?.onModalViewCollapsed()
                    }
                    else -> {
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }
        }
    }

    private var locationTracker: LocationTracker? = null

    private var mediaPlayer: MediaPlayer? = null

    @SuppressLint("MissingPermission")
    private val locationPermissionsRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            Logger.debug(TAG, "locationPermissionsRequest -> permissions: $permissions")

            if (permissions.all { it.value }) {
                locationTracker?.startListening()
            } else {
                alertDialog?.cancel()
                alertDialog = null
                alertDialog = ThemedAlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.sos_widget_permission_settings)
                    .setMessage(
                        getString(
                            R.string.sos_widget_info_permissions_necessity,
                            getString(R.string.sos_widget_location)
                        )
                    )
                    .setNegativeButton(R.string.sos_widget_cancel) { dialog, _ ->
                        alertDialog = null
                        dialog.dismiss()

                        finish()
                    }
                    .setPositiveButton(R.string.sos_widget_to_settings) { dialog, _ ->
                        alertDialog = null
                        dialog.dismiss()

                        startActivity(createApplicationSettingsIntent())
                    }
                    .show()
            }
        }

    private val videoCallPermissionsRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            Logger.debug(TAG, "videoCallPermissionsRequest -> permissions: $permissions")

            if (permissions.all { it.value }) {
                start()
            } else {
                alertDialog?.cancel()
                alertDialog = null
                alertDialog = ThemedAlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.sos_widget_permission_settings)
                    .setMessage(
                        getString(
                            R.string.sos_widget_info_permissions_necessity,
                            formatEachWithIndex(
                                getString(R.string.sos_widget_camera),
                                getString(R.string.sos_widget_microphone)
                            )
                        )
                    )
                    .setNegativeButton(R.string.sos_widget_cancel) { dialog, _ ->
                        alertDialog = null
                        dialog.dismiss()

                        finish()
                    }
                    .setPositiveButton(R.string.sos_widget_to_settings) { dialog, _ ->
                        alertDialog = null
                        dialog.dismiss()

                        startActivity(createApplicationSettingsIntent())
                    }
                    .show()
            }
        }

    @SuppressLint("MissingPermission")
    private val resolutionResolveLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data

                if (data != null) {
                    val locationSettingsStates = LocationSettingsStates.fromIntent(data)
                    if (locationSettingsStates == null) {
                        Logger.error(TAG, "Location Settings States is null!")
                    } else {
                        Logger.debug(TAG, locationSettingsStates.asString())
                    }
                }

                if (LOCATION_PERMISSIONS.all { isPermissionGranted(it) }) {
                    locationTracker?.startListening()
                }
            } else {
                checkLocationProviderStatus(
                    onLocationEnabled = {
                        locationTracker?.startListening()
                    },
                    onRejectedProceed = {
                        finish()
                    },
                    onAgreedToProceed = {
                        if (LOCATION_PERMISSIONS.all { isPermissionGranted(it) }) {
                            locationTracker?.startListening()
                        }
                    }
                )
            }
        }

    private var menuItem by Delegates.observable(
        CallMenuItem(
            title = R.string.sos_widget_cancel_pending_call,
            isEnabled = true
        )
    ) { _, old, new ->
        if (new != old) invalidateOptionsMenu()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sos_widget_activity_call)

        setupLocationTracker()

        checkPlayServicesAvailability()

        setupWindowFlags()
        setupStatusBar()
        setupActionBar()
        setupCallAgent()
        setupTabLayout()
        setupToggleButton()
        setupViewPager()
        setupHeaderButtons()
        setupControlButtons()
        setupReminderView()
    }

    override fun onResume() {
        super.onResume()

        if (VIDEO_CALL_PERMISSIONS.all { isPermissionGranted(it) }) {
            start()
        } else {
            return videoCallPermissionsRequest.launch(VIDEO_CALL_PERMISSIONS)
        }

        if (locationTracker?.isListening == false) {
            locationPermissionsRequest.launch(LOCATION_PERMISSIONS)
        }

        viewModel?.onResume()
    }

    override fun onBackPressed() {
        Logger.debug(TAG, "onBackPressed()")

        if (viewModel == null) {
            return super.onBackPressed()
        }

        if (bottomSheetBehavior?.state != BottomSheetBehavior.STATE_COLLAPSED) {
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        } else {
            val fragments = supportFragmentManager.fragments

            val imagePreviewDialogFragments =
                fragments.filterIsInstance<ImagePreviewDialogFragment>()

            if (imagePreviewDialogFragments.isNotEmpty()) {
                imagePreviewDialogFragments.forEach {
                    it.dismiss()
                    supportFragmentManager.fragments.remove(it)
                }
            } else {
                viewModel?.onBackPressed()
            }
        }
    }

    override fun onPause() {
        super.onPause()

        if (locationTracker?.isListening == true) {
            locationTracker?.stopListening()
        }

        viewModel?.onPause()
    }

    override fun onStop() {
        super.onStop()

        releaseMediaPlayer()
    }

    override fun onDestroy() {
        revertWindowFlags()

        locationTracker?.stopListening(clearListeners = true)
        locationTracker?.let { lifecycle.removeObserver(it) }
        locationTracker = null

        bottomSheetBehavior?.removeBottomSheetCallback(bottomSheetBehaviorCallback)
        bottomSheetBehavior = null

        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Logger.debug(TAG, "onCreateOptionsMenu() -> $menu")
        menuInflater.inflate(R.menu.sos_widget_call, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        Logger.debug(TAG, "onPrepareOptionsMenu() -> $menu")
        menu?.findItem(R.id.hangupCall)?.let {
            it.isEnabled = menuItem.isEnabled
            it.setTitle(menuItem.title)
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Logger.debug(TAG, "onOptionsItemSelected() -> $item")
        return when (item.itemId) {
            R.id.hangupCall -> {
                viewModel?.onHangupLiveCallPressed()
                true
            }
            else ->
                super.onOptionsItemSelected(item)
        }
    }

    private fun start() {
        Logger.debug(TAG, "start()")

        if (viewModel == null) {
            val callType = requireNotNull(
                CallType.from(
                    requireNotNull(intent.getStringExtra(IntentKey.CALL_TYPE))
                )
            )

            val peerConnectionClient = injection.providePeerConnectionClient(this)
            peerConnectionClient.setLocalSurfaceView(surfaceViewRenderer)

            viewModel = injection.provideCallViewModel(
                owner = this,
                callType = callType,
                callTopic = intent.getStringExtra(IntentKey.CALL_TOPIC),
                l10n = L10n(
                    userCancelledCall = getString(R.string.sos_widget_user_cancelled_call),
                    userEndedCall = getString(R.string.sos_widget_user_ended_call),
                    unknownNetworkError = getString(R.string.sos_widget_info_connection_interrupted),
                ),
                peerConnectionClient = peerConnectionClient
            )

            setupToggleButtonText()

            observeMessage()
            observeRoute()
            observeLocationTrackerEnabled()
            observeReminderVisibility()
            observeCallAgent()
            observeCallDuration()
            observeCallState()
            observeLocalAudioEnabled()
            observeLocalVideoEnabled()
            observeModalViewState()
            observeNewMessage()
            observeCard102Status()

//            setupSurfaceViewRenderer()

            if (adapter?.fragments.isNullOrEmpty()) {
                adapter?.fragments = arrayOf(
                    TextChatFragment.newInstance(),
                    ARMMapFragment.newInstance()
                )
                adapter?.notifyItemRangeInserted(0, adapter?.fragments?.size ?: 0)
            }

            checkAudioVolume()

            viewModel?.onSetupCompleted()
        }
    }

    private fun setupLocationTracker() {
        locationTracker = locationTracker(this) { setupDefault() }.also {
            lifecycle.addObserver(it)
        }

        locationTracker?.addListener(object : LocationTracker.Listener {
            override fun onLocationFound(location: Location) {
                viewModel?.onLocationFound(location)
            }

            override fun onDeviceRotated(angle: Float) {
                viewModel?.onDeviceRotated(angle)
            }

            override fun onResolutionRequired(exception: ResolvableApiException) {
                try {
                    resolutionResolveLauncher.launch(
                        IntentSenderRequest.Builder(exception.resolution)
                            .build()
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onError(error: LocationTracker.Error) {
                Logger.error(TAG, "onError() -> error: $error")

                when (error) {
                    LocationTracker.Error.NO_PERMISSION -> {
                        locationPermissionsRequest.launch(LOCATION_PERMISSIONS)
                    }
                    LocationTracker.Error.FAKE_LOCATION -> {
                        Logger.error(TAG, "[FINISH] onError() -> $error")
                        finish()
                    }
                    else -> {
                    }
                }
            }

            override fun onProviderError(provider: LocationProvider) {
                Logger.error(TAG, "onProviderError() -> error: $provider")

                viewModel?.onLocationFailed()
            }
        })
    }

    private fun checkPlayServicesAvailability() {
        with(GoogleApiAvailability.getInstance()) {
            val status = isGooglePlayServicesAvailable(this@CallActivity)

            if (status != ConnectionResult.SUCCESS) {
                if (isUserResolvableError(status)) {
                    getErrorDialog(this@CallActivity, status, 777)  // Magic number
                        ?.show()
                }
            }
        }
    }

    private fun setupWindowFlags() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun revertWindowFlags() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun setupStatusBar() {
        setStatusBarTransparent()
    }

    private fun setupActionBar() {
        setupActionBar(toolbar) { viewModel?.onBackPressed() }
    }

    private fun setupCallAgent() {
        // Header
        smallImageView.setImageDrawable(getDrawableCompat(R.drawable.sos_widget_ic_no_photo))

        titleView.setText(R.string.sos_widget_chat_with_call_agent)

        if (subtitleView.visibility != View.GONE) {
            subtitleView.visibility = View.GONE
        }

        subtitleView.text = null

        // Video
        largeImageView.setImageDrawable(getDrawableCompat(R.drawable.sos_widget_ic_no_photo))

        textView.setText(R.string.sos_widget_chat_with_call_agent)
    }

    private fun setupTabLayout() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab == null) return

                hideKeyboard()

                viewPager.setCurrentItem(tab.position, true)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })
    }

    private fun setupToggleButton() {
        if (toggleButton.visibility != View.GONE) {
            toggleButton.visibility = View.GONE
        }

        toggleButton.setOnClickListener {
            viewModel?.onToggleModalView()
        }
    }

    private fun setupToggleButtonText() {
        when (viewModel?.callType) {
            CallType.AUDIO ->
                toggleButton.setText(R.string.sos_widget_return_to_audio_call)
            CallType.VIDEO ->
                toggleButton.setText(R.string.sos_widget_return_to_video_call)
            else ->
                toggleButton.text = null
        }
    }

    private fun setupViewPager() {
        if (adapter == null) {
            adapter = ViewPagerAdapter(
                this,
                emptyArray()
            )
            viewPager.adapter = adapter
            viewPager.isUserInputEnabled = false
            viewPager.offscreenPageLimit = if (adapter?.fragments.isNullOrEmpty()) {
                2
            } else {
                adapter?.fragments?.size ?: 2
            }
        }
    }

    private fun setupSurfaceViewRenderer() {
        viewModel?.peerConnectionClient?.setLocalSurfaceView(surfaceViewRenderer)
    }

    private fun setupHeaderButtons() {
        minimizeButton.setOnClickListener { viewModel?.onGoToTextChat() }

        reminderButton.setOnClickListener { toggleReminderView() }

        mapButton.setOnClickListener { viewModel?.onGoToMap() }
    }

    private fun setupControlButtons() {
        toggleCameraButton.setOnClickListener { viewModel?.onToggleLocalCamera() }

        toggleAudioButton.setOnClickListener { viewModel?.onToggleLocalAudio() }

        toggleCameraSourceButton.setOnClickListener { viewModel?.onToggleLocalCameraSource() }

        hangupButton.setOnClickListener {
            ThemedAlertDialog.CallHangupConfirmation(this)
                .build(
                    positive = {
                        viewModel?.onHangupLiveCall()
                    }
                )
                .show()
        }
    }

    private fun setupReminderView() {
        reminderView.setIcon(R.drawable.sos_widget_ic_info_filled)
        reminderView.setTitle(R.string.sos_widget_attention)
        reminderView.setText(R.string.sos_widget_info_video_call_terms)
        reminderView.setButtonText(R.string.sos_widget_ok)
        reminderView.setOnButtonClickListener {
            toggleReminderView()
        }
    }

    private fun toggleReminderView() {
        if (reminderLayout.visibility == View.VISIBLE) {
            hideReminderView()
        } else {
            showReminderView()
        }
    }

    private fun showReminderView() {
        if (reminderLayout.visibility != View.VISIBLE) {
            reminderLayout.visibility = View.VISIBLE
        }
    }

    private fun hideReminderView() {
        if (reminderLayout.visibility != View.GONE) {
            reminderLayout.visibility = View.GONE
        }
    }

    private fun observeMessage() {
        viewModel?.getMessage()?.observe(this, { message ->
            when (message) {
                is CallScreen.Message.Call.CancelConfirmation -> {
                    ThemedAlertDialog.CallCancellationConfirmation(this)
                        .build(
                            positive = {
                                viewModel?.onHangupLiveCall()
                            }
                        )
                        .show()
                }
                is CallScreen.Message.Call.HangupConfirmation -> {
                    ThemedAlertDialog.CallHangupConfirmation(this)
                        .build(
                            positive = {
                                viewModel?.onHangupLiveCall()
                            }
                        )
                        .show()
                }
                is CallScreen.Message.Form.NewTaskCreated -> {
                    alertDialog?.dismiss()
                    alertDialog = null

                    alertDialog = ThemedAlertDialog.Builder(this)
                        .setTitle(R.string.sos_widget_attention)
                        .setMessage(message.text)
                        .setPositiveButton(R.string.sos_widget_ok) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
                is CallScreen.Message.Location.Mocked -> {
                    ThemedAlertDialog.FakeLocationUsage(this)
                        .build(
                            positive = {
                                viewModel?.onHangupLiveCall()
                            }
                        )
                        .show()
                }
                is CallScreen.Message.Location.UnableToDetermine -> {
                    toast(R.string.sos_widget_error_detect_user_location)
                }
                is CallScreen.Message.WebSocket.Disconnected ->
                    toast(R.string.sos_widget_info_connection_interrupted)
                is CallScreen.Message.FileUpload.Failed ->
                    toast(R.string.sos_widget_file_upload_error, Toast.LENGTH_LONG)
                else -> {
                }
            }
        })
    }

    private fun observeRoute() {
        viewModel?.getRoute()?.observe(this, { route ->
            Logger.debug(TAG, "getRoute() -> route: $route")

            when (route) {
                is CallScreen.Route.Back -> {
                    super.onBackPressed()
                }
                is CallScreen.Route.Map -> {
                    tabLayout.selectTab(tabLayout.getTabAt(1), true)
                }
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun observeLocationTrackerEnabled() {
        viewModel?.getLocationTrackerEnabled()?.observe(this, { isShouldBeSent ->
            if (isShouldBeSent) {
                if (LOCATION_PERMISSIONS.all { isPermissionGranted(it) }) {
                    locationTracker?.startListening()
                }
            } else {
                locationTracker?.stopListening()
            }
        })
    }

    private fun observeReminderVisibility() {
        viewModel?.getReminderVisibility()?.observe(this, { isVisible ->
            if (isVisible) {
                showReminderView()
            } else {
                hideReminderView()
            }
        })
    }

    private fun observeCallAgent() {
        viewModel?.getCallAgent()?.observe(this, { callAgent ->
            Logger.debug(TAG, "getCallAgent() -> $callAgent")

            val fullUrl = URLManager.buildUrl(callAgent.photoUrl)

            // Header
            if (fullUrl.isNullOrBlank()) {
                smallImageView.setImageDrawable(getDrawableCompat(R.drawable.sos_widget_ic_no_photo))
            } else {
                smallImageView.load(fullUrl) {
                    setCrossfade(true)
                    setErrorDrawable(R.drawable.sos_widget_ic_no_photo)
                    setPlaceholderDrawable(R.drawable.sos_widget_widget_shape_gradient_gray)
                    setScale(SOSWidgetImageLoader.Request.Scale.FIT)
                }
            }

            titleView.text = callAgent.fullName

            subtitleView.setText(R.string.sos_widget_call_agent)

            if (subtitleView.visibility != View.VISIBLE) {
                subtitleView.visibility = View.VISIBLE
            }

            // Video
            if (fullUrl.isNullOrBlank()) {
                largeImageView.setImageDrawable(getDrawableCompat(R.drawable.sos_widget_ic_no_photo))
            } else {
                largeImageView.load(fullUrl) {
                    setCrossfade(true)
                    setErrorDrawable(R.drawable.sos_widget_ic_no_photo)
                    setPlaceholderDrawable(R.drawable.sos_widget_widget_shape_gradient_gray)
                    setScale(SOSWidgetImageLoader.Request.Scale.FIT)
                }
            }

            textView.text = callAgent.fullName
        })
    }

    private fun observeCallDuration() {
        viewModel?.getCallDuration()?.observe(this, { time ->
            statusView.text = time

            if (statusView.tag != "is_time_set") {
                statusView.tag = "is_time_set"
            }

            when (viewModel?.callType) {
                CallType.AUDIO ->
                    toggleButton.text =
                        getString(R.string.sos_widget_return_to_audio_call) + " • " + time
                CallType.VIDEO ->
                    toggleButton.text =
                        getString(R.string.sos_widget_return_to_video_call) + " • " + time
                else ->
                    toggleButton.text = null
            }
        })
    }

    private fun observeCallState() {
        Logger.debug(TAG, "observeCallState()")

        viewModel?.getCallState()?.observe(this, { callState ->
            Logger.debug(TAG, "getCallState() -> callState: $callState")

            if (callState.isStarted) {
                toggleButton.visibility = View.VISIBLE
            } else {
                toggleButton.visibility = View.GONE
            }

            when (callState) {
                is CallState.Pending -> {
                    // Menu
                    menuItem = CallMenuItem(
                        isEnabled = true,
                        title = R.string.sos_widget_cancel_pending_call
                    )

                    // Call status
                    statusView.setText(R.string.sos_widget_connection_in_progress)

                    // Device volume control
                    volumeControlStream = AudioManager.STREAM_VOICE_CALL

                    // Ringtone
                    playSound(R.raw.sos_widget_dial_tone)
                }
                is CallState.Start -> {
                    // Call status
                    statusView.setText(R.string.sos_widget_waiting_for_call_agent)
                }
                is CallState.Ready -> {
                    releaseMediaPlayer()
                }
                is CallState.Live -> {
                    // Menu
                    menuItem = CallMenuItem(
                        isEnabled = true,
                        title = R.string.sos_widget_hangup_call
                    )

                    // Call status
                    if (statusView.tag != "is_time_set") {
                        statusView.setText(R.string.sos_widget_connected)
                    }

                    // Release media player once, in order to handle media player,
                    // if it is not released before
                    releaseMediaPlayer()
                }
                is CallState.UserRedirected -> {
                    menuItem = CallMenuItem(
                        isEnabled = true,
                        title = R.string.sos_widget_hangup_call
                    )

                    releaseMediaPlayer()
                }
                is CallState.Disconnected -> {
                    menuItem = CallMenuItem(
                        isEnabled = false,
                        title = R.string.sos_widget_hangup_call
                    )

                    releaseMediaPlayer()
                }
                is CallState.Finished -> {
                    menuItem = CallMenuItem(
                        isEnabled = false,
                        title = R.string.sos_widget_hangup_call
                    )

                    // Device volume control
                    volumeControlStream = AudioManager.USE_DEFAULT_STREAM_TYPE

                    releaseMediaPlayer()
                }
                else -> {
                }
            }
        })
    }

    private fun observeLocalAudioEnabled() {
        viewModel?.getLocalAudioEnabled()?.observe(this, { isEnabled ->
            if (isEnabled) {
                toggleAudioButton.backgroundTintList =
                    getColorStateListCompat(R.color.sos_widget_button_bg_white_to_purple_blue)
                toggleAudioButton.icon =
                    getDrawableCompat(R.drawable.sos_widget_ic_mic_on_stroke_blue)
                toggleAudioButton.iconTint =
                    getColorStateListCompat(R.color.sos_widget_button_text_purple_blue_to_white)
            } else {
                toggleAudioButton.backgroundTintList =
                    getColorStateListCompat(R.color.sos_widget_button_bg_alpha_black_to_white)
                toggleAudioButton.icon =
                    getDrawableCompat(R.drawable.sos_widget_ic_mic_off_stroke_white)
                toggleAudioButton.iconTint =
                    getColorStateListCompat(R.color.sos_widget_button_text_white_to_purple_blue_2)
            }
        })
    }

    private fun observeLocalVideoEnabled() {
        viewModel?.getLocalVideoEnabled()?.observe(this, { isEnabled ->
            if (isEnabled) {
                toggleCameraButton.backgroundTintList =
                    getColorStateListCompat(R.color.sos_widget_button_bg_white_to_purple_blue)
                toggleCameraButton.icon =
                    getDrawableCompat(R.drawable.sos_widget_ic_camera_on_stroke_blue)
                toggleCameraButton.iconTint =
                    getColorStateListCompat(R.color.sos_widget_button_text_purple_blue_to_white)
            } else {
                toggleCameraButton.backgroundTintList =
                    getColorStateListCompat(R.color.sos_widget_button_bg_alpha_black_to_white)
                toggleCameraButton.icon =
                    getDrawableCompat(R.drawable.sos_widget_ic_camera_off_stroke_white)
                toggleCameraButton.iconTint =
                    getColorStateListCompat(R.color.sos_widget_button_text_white_to_purple_blue)
            }
        })
    }

    private fun observeModalViewState() {
        viewModel?.getModalViewState()?.observe(this, { viewState ->
            Logger.debug(TAG, "getModalViewState() -> $viewState")

            when (viewState) {
                ModalViewState.CREATE -> {
                    hideKeyboard()

                    if (bottomSheetBehavior == null) {
                        bottomSheetBehavior = BottomSheetBehavior.from(videoCallView)
                        bottomSheetBehavior?.expandedOffset = 0
                        bottomSheetBehavior?.isDraggable = false
                        bottomSheetBehavior?.isFitToContents = true
                        bottomSheetBehavior?.isHideable = true
                        bottomSheetBehavior?.setPeekHeight(0, false)
                        bottomSheetBehavior?.addBottomSheetCallback(bottomSheetBehaviorCallback)
                    }

                    if (bottomSheetBehavior?.state != BottomSheetBehavior.STATE_EXPANDED) {
                        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                    }
                }
                ModalViewState.FULLSCREEN -> {
                    hideKeyboard()

                    if (bottomSheetBehavior?.state != BottomSheetBehavior.STATE_EXPANDED) {
                        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                    }
                }
                ModalViewState.HIDE -> {
                    if (bottomSheetBehavior?.state != BottomSheetBehavior.STATE_COLLAPSED) {
                        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
                    }
                }
                ModalViewState.DESTROY -> {
                    if (bottomSheetBehavior?.state != BottomSheetBehavior.STATE_COLLAPSED) {
                        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
                    }
                }
                else -> {
                }
            }
        })
    }

    @SuppressLint("ShowToast")
    private fun observeNewMessage() {
        viewModel?.getNewMessage()?.observe(this, { message ->
            if (message is Message) {
                if (bottomSheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED) {
                    snackbar?.dismiss()
                    snackbar = null
                    snackbar = Snackbar.make(
                        this,
                        rootLayout,
                        message.body ?: getString(R.string.sos_widget_new_message),
                        Snackbar.LENGTH_LONG
                    ).apply {
                        if (toggleCameraButton.visibility == View.VISIBLE) {
                            anchorView = toggleCameraButton
                            animationMode = BaseTransientBottomBar.ANIMATION_MODE_FADE
                        } else {
                            animationMode = BaseTransientBottomBar.ANIMATION_MODE_SLIDE
                        }
                        setBackgroundTint(getColorCompat(R.color.sos_widget_black_alpha))
                        setActionTextColor(getColorCompat(R.color.sos_widget_white_alpha))
                        setAction(R.string.sos_widget_to_chat) { viewModel?.onGoToTextChat() }
                    }
                    snackbar?.show()
                }
            }
        })
    }

    @SuppressLint("ShowToast")
    private fun observeCard102Status() {
        viewModel?.getCard102Status()?.observe(this, { status ->
            if (bottomSheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED) {
                snackbar?.dismiss()
                snackbar = null
                snackbar = Snackbar.make(
                    this,
                    rootLayout,
                    when (status) {
                        Card102Status.NEW_CARD102 ->
                            getString(R.string.sos_widget_arm_new_card_created)
                        Card102Status.ASSIGNED_FORCE ->
                            getString(R.string.sos_widget_arm_force_assigned)
                        Card102Status.FORCE_ON_SPOT ->
                            getString(R.string.sos_widget_arm_assigned_force_on_spot)
                        Card102Status.COMPLETED_OPERATION ->
                            getString(R.string.sos_widget_arm_operation_completed)
                        else -> return@observe
                    },
                    Snackbar.LENGTH_LONG
                ).apply {
                    if (toggleCameraButton.visibility == View.VISIBLE) {
                        anchorView = toggleCameraButton
                        animationMode = BaseTransientBottomBar.ANIMATION_MODE_FADE
                    } else {
                        animationMode = BaseTransientBottomBar.ANIMATION_MODE_SLIDE
                    }
                    setBackgroundTint(getColorCompat(R.color.sos_widget_black_alpha))
                    setActionTextColor(getColorCompat(R.color.sos_widget_white_alpha))
                    setAction(R.string.sos_widget_to_map) { viewModel?.onGoToMap() }
                }
                snackbar?.show()
            }
        })
    }

    private fun checkAudioVolume() {
        if (viewModel?.getCallState()?.value?.isStarted == true) {
            audioManager?.let {
                val volumeLevel = it.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
                val maxVolumeLevel = it.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
                val volumePercent = (volumeLevel / maxVolumeLevel * 100F).roundToInt()
                if (volumePercent < 20) {
                    toast(R.string.sos_widget_info_call_low_speaker_volume)
                }
            }
        }
    }

    private fun playSound(@RawRes soundId: Int) {
        try {
            mediaPlayer = MediaPlayer()
            resources.openRawResourceFd(soundId).use {
                mediaPlayer?.setDataSource(
                    it.fileDescriptor,
                    it.startOffset,
                    it.length
                )
            }
            mediaPlayer?.isLooping = true
            mediaPlayer?.prepare()
            mediaPlayer?.start()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

}