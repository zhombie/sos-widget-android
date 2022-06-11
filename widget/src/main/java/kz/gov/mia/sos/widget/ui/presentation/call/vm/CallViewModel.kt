package kz.gov.mia.sos.widget.ui.presentation.call.vm

import androidx.lifecycle.*
import com.google.android.gms.maps.model.LatLng
import com.loopj.android.http.RequestParams
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kz.garage.chat.model.Entity
import kz.garage.chat.model.Message
import kz.garage.chat.model.Notification
import kz.garage.chat.model.reply_markup.InlineReplyMarkup
import kz.garage.chat.model.reply_markup.ReplyMarkup
import kz.garage.chat.model.reply_markup.button.Button
import kz.garage.chat.model.reply_markup.button.CallbackButton
import kz.garage.chat.model.reply_markup.button.TextButton
import kz.garage.chat.model.reply_markup.button.URLButton
import kz.garage.kotlin.simpleNameOf
import kz.garage.lifecycle.livedata.SingleLiveEvent
import kz.garage.location.core.isMocked
import kz.garage.multimedia.store.model.Content
import kz.gov.mia.sos.widget.core.URLManager
import kz.gov.mia.sos.widget.core.device.Device
import kz.gov.mia.sos.widget.core.location.setBearing
import kz.gov.mia.sos.widget.core.logging.Logger
import kz.gov.mia.sos.widget.data.remote.file.download
import kz.gov.mia.sos.widget.data.remote.file.upload
import kz.gov.mia.sos.widget.data.remote.http.AsyncHttpClientBuilder
import kz.gov.mia.sos.widget.data.remote.http.file.UploadState
import kz.gov.mia.sos.widget.domain.location.asLocation
import kz.gov.mia.sos.widget.domain.repository.ARMRepository
import kz.gov.mia.sos.widget.domain.repository.IceServersRepository
import kz.gov.mia.sos.widget.domain.repository.LocationRepository
import kz.gov.mia.sos.widget.domain.repository.SettingsRepository
import kz.gov.mia.sos.widget.ui.model.*
import kz.gov.mia.sos.widget.ui.platform.BaseViewModel
import kz.gov.mia.sos.widget.ui.presentation.call.CallScreen
import kz.gov.mia.sos.widget.utils.nowAsSeconds
import kz.inqbox.sdk.domain.model.*
import kz.inqbox.sdk.domain.model.button.RateButton
import kz.inqbox.sdk.domain.model.call.type.CallType
import kz.inqbox.sdk.domain.model.content.asContentType
import kz.inqbox.sdk.domain.model.geo.Location
import kz.inqbox.sdk.domain.model.message.call.CallAction
import kz.inqbox.sdk.domain.model.message.qrtc.QRTCAction
import kz.inqbox.sdk.domain.model.webrtc.*
import kz.inqbox.sdk.socket.listener.*
import kz.inqbox.sdk.socket.model.CallInitialization
import kz.inqbox.sdk.socket.model.Card102Status
import kz.inqbox.sdk.socket.model.Category
import kz.inqbox.sdk.socket.model.LocationUpdate
import kz.inqbox.sdk.socket.repository.SocketRepository
import kz.inqbox.sdk.webrtc.Options
import kz.inqbox.sdk.webrtc.PeerConnectionClient
import kz.inqbox.sdk.webrtc.core.constraints.AudioBooleanConstraints
import kz.inqbox.sdk.webrtc.core.constraints.RTCConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import timerx.Stopwatch
import timerx.StopwatchBuilder
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class CallViewModel constructor(
    val callType: CallType,
    private val callTopic: String?,
    private val l10n: L10n,
    private val device: Device,
    private val armRepository: ARMRepository,
    private val iceServersRepository: IceServersRepository,
    private val locationRepository: LocationRepository,
    val peerConnectionClient: PeerConnectionClient,
    private val settingsRepository: SettingsRepository,
    private val socketRepository: SocketRepository
) : BaseViewModel(),
    PeerConnectionClient.Listener,
    ARMListener,
    SocketStateListener,
    CoreListener,
    ChatBotListener,
    CallListener,
    WebRTCListener {

    companion object {
        private val TAG = simpleNameOf<CallViewModel>()
    }

    private val asyncHttpClient by lazy(LazyThreadSafetyMode.NONE) {
        AsyncHttpClientBuilder()
            .setLoggingEnabled(Logger.isEnabled)
            .build()
    }

    private val state by lazy { MutableLiveData<CallScreen.State>(CallScreen.State.IDLE) }
    fun getState(): LiveData<CallScreen.State> = state

    private val message by lazy { SingleLiveEvent<CallScreen.Message>() }
    fun getMessage(): LiveData<CallScreen.Message> = message

    private val route by lazy { SingleLiveEvent<CallScreen.Route>() }
    fun getRoute(): LiveData<CallScreen.Route> = route

    private val callState by lazy { MutableLiveData<CallState>() }
    fun getCallState(): LiveData<CallState> = callState

    private val callAgent by lazy { MutableLiveData<UICallAgent>() }
    fun getCallAgent(): LiveData<UICallAgent> = callAgent

    private val localAudioEnabled by lazy { MutableLiveData<Boolean>() }
    fun getLocalAudioEnabled(): LiveData<Boolean> = localAudioEnabled

    private val localVideoEnabled by lazy { MutableLiveData<Boolean>() }
    fun getLocalVideoEnabled(): LiveData<Boolean> = localVideoEnabled

    private val modalViewState by lazy { SingleLiveEvent<ModalViewState>() }
    fun getModalViewState(): LiveData<ModalViewState> = modalViewState

    private val newMessage by lazy { MutableLiveData<Entity>() }
    fun getNewMessage(): LiveData<Entity> = newMessage

    fun getChatHistory(): List<Entity> = conversation.history

    private val card102Status by lazy { MutableLiveData<Card102Status>() }
    fun getCard102Status(): LiveData<Card102Status> = card102Status

    private val isLocationTrackerEnabled by lazy { MutableLiveData<Boolean>() }
    fun getLocationTrackerEnabled(): LiveData<Boolean> = isLocationTrackerEnabled

    private val selfLocation by lazy { MutableLiveData<android.location.Location>() }
    fun getSelfLocation(): LiveData<android.location.Location> = selfLocation

    private val otherLocations by lazy { MutableLiveData<List<LocationUpdate>>() }
    fun getOtherLocations(): LiveData<List<LocationUpdate>> = otherLocations

    private val selectedSelfLocation by lazy { MutableLiveData<LatLng?>() }
    fun getSelectedSelfLocation(): LiveData<LatLng?> = selectedSelfLocation

    private val selectedOtherLocation by lazy { MutableLiveData<LocationUpdate?>() }
    fun getSelectedOtherLocation(): LiveData<LocationUpdate?> = selectedOtherLocation

    private val locationFailRetry by lazy { MutableLiveData<Boolean>() }
    fun getLocationFailRetry(): LiveData<Boolean> = locationFailRetry

    private val reminderVisibility by lazy { MutableLiveData(true) }
    fun getReminderVisibility(): LiveData<Boolean> = reminderVisibility

    private val callDuration by lazy { MutableLiveData<CharSequence>() }
    fun getCallDuration(): LiveData<CharSequence> = callDuration

    private val deviceRotationAngle by lazy { MutableLiveData(selfLocation.value?.bearing ?: 0F) }
    fun getDeviceRotationAngle(): LiveData<Float> = deviceRotationAngle

    private val downloadState by lazy { MutableLiveData<DownloadState>() }
    fun getDownloadState(): LiveData<DownloadState> = downloadState

    private val conversation: Conversation = Conversation()

    private var stopwatch: Stopwatch? = null

    private val postponedUserLocations = mutableListOf<Location>()

    private var locationFailRetryCount: Int = 0

    private val otherLocationUpdates = mutableListOf<LocationUpdate>()

    private var mediaUploadJob: Job? = null

    init {
        io.launch {
            initWebSocket()

            conversation.isLocalAudioEnabled =
                callType == CallType.AUDIO || callType == CallType.VIDEO
            conversation.isLocalVideoEnabled = callType == CallType.VIDEO
            conversation.isRemoteAudioEnabled =
                callType == CallType.AUDIO || callType == CallType.VIDEO
            conversation.isRemoteVideoEnabled = false  // User can see only himself
            conversation.isLocalVideoMirrored = true

            releaseStopwatch()

            createPeerConnection()

            withContext(ui) {
                localAudioEnabled.value = conversation.isLocalAudioEnabled
                localVideoEnabled.value = conversation.isLocalVideoEnabled

                isLocationTrackerEnabled.value = true

                callState.value = CallState.Pending

                modalViewState.value = ModalViewState.CREATE
            }
        }
    }

    private suspend fun initWebSocket() = withContext(io) {
        socketRepository.setARMListener(this@CallViewModel)
        socketRepository.setCallListener(this@CallViewModel)
        socketRepository.setChatBotListener(this@CallViewModel)
        socketRepository.setCoreListener(this@CallViewModel)
        socketRepository.setSocketStateListener(this@CallViewModel)
        socketRepository.setWebRTCListener(this@CallViewModel)

        ensureWebSocketConnection()

        socketRepository.registerSocketConnectEventListener()
        socketRepository.registerMessageEventListener()
        socketRepository.registerCallAgentGreetEventListener()
        socketRepository.registerCard102UpdateEventListener()
        socketRepository.registerLocationUpdateEventListener()
        socketRepository.registerUserCallFeedbackEventListener()
        socketRepository.registerSocketDisconnectEventListener()
    }

    private suspend fun ensureWebSocketConnection() = withContext(io) {
        if (!socketRepository.isConnected()) {
            socketRepository.create(URLManager.getSocketUrl())
            socketRepository.connect()
        }
    }

    private suspend fun createPeerConnection(): PeerConnection? = withContext(ui) {
        peerConnectionClient.createPeerConnection(
            options = Options(
                isLocalAudioEnabled = conversation.isLocalAudioEnabled,
                isRemoteAudioEnabled = conversation.isRemoteAudioEnabled,
                isLocalVideoEnabled = conversation.isLocalVideoEnabled,
                isRemoteVideoEnabled = conversation.isRemoteVideoEnabled,
                iceServers = iceServersRepository.getIceServers(),
                videoCodecHwAcceleration = callType == CallType.VIDEO,
                audioBooleanConstraints = RTCConstraints<AudioBooleanConstraints, Boolean>().apply {
                    // Google Hangouts WebRTC
                    // googEchoCancellation:true
                    // googAutoGainControl:true
                    // googNoiseSuppression:true
                    // googHighpassFilter:true
                    // googAudioMirroring:false
                    // googNoiseSuppression2:true
                    // googEchoCancellation2:true
                    // googAutoGainControl2:true
                    // googDucking:false
                    addOptionalConstraint(AudioBooleanConstraints.ECHO_CANCELLATION, true)
                    addOptionalConstraint(AudioBooleanConstraints.ECHO_CANCELLATION_2, true)
                    addOptionalConstraint(AudioBooleanConstraints.AUTO_GAIN_CONTROL, true)
                    addOptionalConstraint(AudioBooleanConstraints.AUTO_GAIN_CONTROL_2, true)
                    addOptionalConstraint(AudioBooleanConstraints.NOISE_SUPPRESSION, true)
                    addOptionalConstraint(AudioBooleanConstraints.NOISE_SUPPRESSION_2, true)
                    addOptionalConstraint(AudioBooleanConstraints.HIGH_PASS_FILTER, true)
                    addOptionalConstraint(AudioBooleanConstraints.AUDIO_MIRRORING, false)
                }
            ),
            listener = this@CallViewModel
        )
    }

    private suspend fun initMediaStreams() = withContext(ui) {
        Logger.debug(TAG, "initMediaStreams()")

        if (callType == CallType.VIDEO) {
            peerConnectionClient.initLocalCameraStream(
                isMirrored = conversation.isLocalVideoMirrored,
                isZOrderMediaOverlay = false
            )
        }

        peerConnectionClient.addLocalStreamToPeer()
    }

    fun onSetupCompleted() {
        io.launch {
            Logger.debug(TAG, "onSetupCompleted()")

            with(locationRepository.getLastFoundLocation()) {
                socketRepository.sendCallInitialization(
                    CallInitialization(
                        callType = callType,
                        userId = null,
                        domain = URLManager.getDomain(),
                        topic = callTopic,
                        location = this,
                        device = CallInitialization.Device(
                            os = device.os,
                            osVersion = device.osVersion,
                            appVersion = device.versionName,
                            name = device.name,
                            mobileOperator = device.operator,
                            battery = CallInitialization.Device.Battery(
                                percentage = device.batteryPercent,
                                isCharging = device.isPhoneCharging,
                                temperature = device.batteryTemperature,
                            )
                        ),
                        language = settingsRepository.getLanguage()
                    )
                )

                if (this != null) {
                    postponedUserLocations.add(this)

                    val location = asLocation()
                    withContext(ui) {
                        selfLocation.value = location
                    }
                }
            }

            initMediaStreams()
        }
    }

    fun onMapReady() {
        io.launch {
            val location = locationRepository.getLastFoundLocation()?.asLocation()
            if (location != null) {
                withContext(ui) {
                    selfLocation.value = location
                }
            }
        }
    }

    fun onResume() {
        ui.launch {
            peerConnectionClient.startLocalVideoCapture()
        }
    }

    fun onPause() {
        ui.launch {
            peerConnectionClient.stopLocalVideoCapture()
        }
    }

    fun onModalViewDragged() {
        Logger.debug(TAG, "onModalViewDragged()")
//        ui.launch {
//            peerConnectionClient.stopLocalVideoCapture()
//        }
    }

    fun onModalViewExpanded() {
        Logger.debug(TAG, "onModalViewExpanded()")
//        ui.launch {
//            peerConnectionClient.startLocalVideoCapture()
//        }
    }

    fun onModalViewCollapsed() {
        Logger.debug(TAG, "onModalViewCollapsed()")
//        ui.launch {
//            peerConnectionClient.startLocalVideoCapture()
//        }
    }

    fun onARMMapDestroy(durationMillis: Long?) {
        io.launch {
            if (durationMillis != null) {
                armRepository.setLastDurationMillis(durationMillis)
            }
        }
    }

    fun onLocationFound(location: android.location.Location) {
        ui.launch {
            if (callState.value is CallState.Disconnected) {
                isLocationTrackerEnabled.value = false
            }

            if (location.isMocked()) {
                message.value = CallScreen.Message.Location.Mocked
            } else {
                val angle = deviceRotationAngle.value

                val newLocation = if (angle == null) {
                    location
                } else {
                    location.setBearing(angle, 100F)
                }.asLocation()

                when (callState.value) {
                    CallState.Pending, CallState.Start, CallState.Ready -> {
                        selfLocation.value = location

                        withContext(io) {
                            postponedUserLocations.add(newLocation)

                            locationRepository.setLastFoundLocation(newLocation)
                        }
                    }
                    CallState.Live -> {
                        selfLocation.value = location

                        withContext(io) {
                            socketRepository.sendUserLocation(
                                id = socketRepository.getId(),
                                location = newLocation
                            )

                            locationRepository.setLastFoundLocation(newLocation)
                        }
                    }
                    else -> {
                    }
                }
            }
        }
    }

    fun onDeviceRotated(angle: Float) {
        deviceRotationAngle.postValue(angle)
    }

    fun onLocationFailed() {
        io.launch {
            if (locationFailRetryCount <= 3) {
                delay(1 * 1000L)

                locationFailRetry.postValue(true)

                locationFailRetryCount++
            }

            withContext(ui) {
                message.value = CallScreen.Message.Location.UnableToDetermine
            }
        }
    }

    fun onSelfLocationClicked() {
        ui.launch {
            selectedOtherLocation.value = null

            val latLng = selfLocation.value?.let { LatLng(it.latitude, it.longitude) }
            if (latLng != null) {
                selectedSelfLocation.value = latLng
            }
        }
    }

    fun onGpsCodeClicked(gpsCode: Long) {
        ui.launch {
            selectedSelfLocation.value = null

            val otherLocationUpdate = otherLocationUpdates.find { gpsCode == it.gpsCode }
            if (otherLocationUpdate != null) {
                selectedOtherLocation.value = otherLocationUpdate
            }
        }
    }

    fun onGoToTextChat() {
        Logger.debug(TAG, "onGoToTextChat()")

        ui.launch {
            modalViewState.value = ModalViewState.HIDE
        }
    }

    fun onGoToMap() {
        Logger.debug(TAG, "onGoToMap()")

        ui.launch {
            modalViewState.value = ModalViewState.HIDE

            route.value = CallScreen.Route.Map
        }
    }

    fun onToggleLocalCamera() {
        ui.launch {
            conversation.isLocalVideoEnabled = !conversation.isLocalVideoEnabled

            peerConnectionClient.setLocalVideoEnabled(conversation.isLocalVideoEnabled)

            localVideoEnabled.value = conversation.isLocalVideoEnabled
        }
    }

    fun onToggleLocalAudio() {
        ui.launch {
            conversation.isLocalAudioEnabled = !conversation.isLocalAudioEnabled

            peerConnectionClient.setLocalAudioEnabled(conversation.isLocalAudioEnabled)

            localAudioEnabled.value = conversation.isLocalAudioEnabled
        }
    }

    fun onToggleLocalCameraSource() {
        ui.launch {
            peerConnectionClient.onSwitchCamera(
                onDone = {},
                onError = {
                    Logger.error(TAG, "onSwitchCamera() -> error -> $it")
                }
            )

            conversation.isLocalVideoMirrored = !conversation.isLocalVideoMirrored
            peerConnectionClient.setLocalVideoStreamMirror(conversation.isLocalVideoMirrored)
        }
    }

    fun onHangupLiveCallPressed() {
        decideCallHangup()
    }

    fun onHangupLiveCall() {
        ui.launch {
            when (callState.value) {
                CallState.Pending -> {
                    Notification.Builder()
                        .setRandomId()
                        .setCreatedAt(nowAsSeconds)
                        .setBody(l10n.userCancelledCall)
                        .build()
                        .addToChat()
                }
                CallState.Start, CallState.Ready, CallState.Live -> {
                    Notification.Builder()
                        .setRandomId()
                        .setCreatedAt(nowAsSeconds)
                        .setBody(l10n.userEndedCall)
                        .build()
                        .addToChat()
                }
                else -> {
                }
            }

            closeLiveCall(true)

            callState.value = CallState.Disconnected(CallState.Disconnected.Initiator.USER)
        }
    }

    fun onBackPressed() {
        decideCallHangup()
    }

    private fun decideCallHangup() {
        ui.launch {
            Logger.debug(TAG, "decideCallHangup() -> callState: ${callState.value}")

            when (callState.value) {
                CallState.Pending -> {
                    message.value = CallScreen.Message.Call.CancelConfirmation
                }
                CallState.Start, CallState.Ready, CallState.Live, CallState.UserRedirected -> {
                    message.value = CallScreen.Message.Call.HangupConfirmation
                }
                else -> {
                    closeLiveCall(true)

                    callState.value = CallState.Finished

                    route.value = CallScreen.Route.Back
                }
            }
        }
    }

    fun onToggleModalView() {
        Logger.debug(TAG, "onToggleModalView()")

        ui.launch {
            modalViewState.value = ModalViewState.FULLSCREEN
        }
    }

    fun onSendMessageButtonClicked(messageText: String) {
        io.launch {
            val trimmedText = messageText.trim()

            if (trimmedText.isNotBlank()) {
                socketRepository.sendUserTextMessage(trimmedText)

                Message.Builder()
                    .setRandomId()
                    .setOutgoingDirection()
                    .setCreatedAt(nowAsSeconds)
                    .setBody(trimmedText)
                    .build()
                    .addToChat()
            }
        }
    }

    fun onUrlInTextClicked(text: String) {
        io.launch {
            val trimmedText = text.trim()

            socketRepository.sendUserTextMessage(trimmedText)

            Message.Builder()
                .setRandomId()
                .setOutgoingDirection()
                .setCreatedAt(nowAsSeconds)
                .setBody(trimmedText)
                .build()
                .addToChat()
        }
    }

    fun onInlineButtonClicked(button: Button) {
        Logger.debug(TAG, "onInlineButtonClicked() -> button: $button")

        io.launch {
            var text: String? = null

            when (button) {
                is CallbackButton -> {
                    text = button.text
                    socketRepository.sendExternal(button.payload)
                }
                is RateButton -> {
                    text = button.text
                    socketRepository.sendUserCallFeedback(button.chatId, button.rating)
                }
                is URLButton -> {
                    text = button.text
                    socketRepository.sendUserTextMessage(button.text)
                }
                is TextButton -> {
                    text = button.text
                    socketRepository.sendUserTextMessage(button.text)
                }
            }

            if (!text.isNullOrBlank()) {
                Message.Builder()
                    .setRandomId()
                    .setOutgoingDirection()
                    .setCreatedAt(nowAsSeconds)
                    .setBody(text)
                    .build()
                    .addToChat()
            }
        }
    }

    fun onMediaSelected(content: Content) {
        mediaUploadJob = ui.launch {
            val params = RequestParams()

            params.put("type", content.asContentType()?.key)

            try {
                @Suppress("BlockingMethodInNonBlockingContext")
                params.put("file", content.publicFile?.getFile())
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                return@launch
            }

            Logger.debug(TAG, "onMediaSelected() -> content: $content")
            Logger.debug(TAG, "onMediaSelected() -> params: $params")

            asyncHttpClient.upload(URLManager.buildUrl("/upload"), params) { uploadState ->
                when (uploadState) {
                    is UploadState.Progress -> {
                        ui.launch {
                            state.value = CallScreen.State.Loading.Determinate(uploadState.progress)
                        }
                    }
                    is UploadState.Success -> {
                        ui.launch {
                            val outputContent = content.clone(
                                remoteFile = Content.RemoteFile(uploadState.urlPath)
                            )

                            socketRepository.sendUserRichMessage(outputContent)

                            Message.Builder()
                                .setRandomId()
                                .setOutgoingDirection()
                                .setCreatedAt(nowAsSeconds)
                                .setContent(outputContent)
                                .build()
                                .addToChat()

                            state.value = CallScreen.State.IDLE
                        }
                    }
                    is UploadState.Error -> {
                        uploadState.cause?.printStackTrace()

                        ui.launch {
                            message.value = CallScreen.Message.FileUpload.Failed(
                                uploadState.cause?.toString() ?: uploadState.message
                            )

                            state.value = CallScreen.State.IDLE
                        }
                    }
                }
            }
        }
    }

    fun onCancelUploadMediaRequest() {
        io.launch {
            mediaUploadJob?.cancelAndJoin()

            withContext(ui) {
                state.value = CallScreen.State.IDLE
            }
        }
    }

    fun onDownloadContent(content: Content, outputFile: File?, itemPosition: Int) {
        ui.launch {
            if (outputFile == null) {
                message.value = CallScreen.Message.FileDownload.Failed
                return@launch
            }

            val url = URLManager.buildUrl(content.remoteFile?.url)

            if (url.isNullOrBlank()) {
                message.value = CallScreen.Message.FileDownload.Failed
                return@launch
            }

            try {
                asyncHttpClient.download(outputFile, url) { downloadState ->
                    ui.launch {
                        when (downloadState) {
                            is kz.gov.mia.sos.widget.data.remote.http.file.DownloadState.Progress -> {
                                this@CallViewModel.downloadState.value = DownloadState.Pending(
                                    content = content,
                                    progress = downloadState.progress,
                                    itemPosition = itemPosition
                                )
                            }
                            is kz.gov.mia.sos.widget.data.remote.http.file.DownloadState.Success -> {
                                this@CallViewModel.downloadState.value = DownloadState.Completed(
                                    content = content,
                                    itemPosition = itemPosition
                                )
                            }
                            is kz.gov.mia.sos.widget.data.remote.http.file.DownloadState.Error -> {
                                message.value = CallScreen.Message.FileDownload.Failed
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                message.value = CallScreen.Message.FileDownload.Failed
            }
        }
    }

    private suspend fun closeLiveCall(isNotifyServerAboutClose: Boolean) {
        withContext(ui) {
            if (isNotifyServerAboutClose) {
                if (callState.value?.isPending == true) {
                    withContext(io) {
                        socketRepository.sendPendingCallCancellation()
                    }
                } else if (callState.value?.isActive == true) {
                    withContext(io) {
                        socketRepository.sendCallAction(action = CallAction.FINISH)
                    }
                }
            }

            postponedUserLocations.clear()

            conversation.unreadMessages = 0

            peerConnectionClient.dispose()

            isLocationTrackerEnabled.value = false

            releaseStopwatch()

            modalViewState.value = ModalViewState.DESTROY
        }
    }

    private suspend fun sendPostponedUserLocations() = withContext(Dispatchers.IO) {
        if (postponedUserLocations.isNotEmpty()) {
            try {
                postponedUserLocations.asFlow()
                    .conflate()
                    .onEach { delay(3 * 1000L) }
                    .onCompletion { postponedUserLocations.clear() }
                    .collect { location ->
                        Logger.debug(TAG, "[FOREACH] -> location: $location")
                        socketRepository.sendUserLocation(
                            id = socketRepository.getId(),
                            location = location
                        )
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                postponedUserLocations.clear()
            }
        }
    }

    /**
     * [PeerConnectionClient.Listener] implementation
     */

    override fun onLocalSessionDescription(sessionDescription: SessionDescription) {
        io.launch {
            socketRepository.sendLocalSessionDescription(sessionDescription)
        }
    }

    override fun onLocalIceCandidate(iceCandidate: IceCandidate) {
        io.launch {
            socketRepository.sendLocalIceCandidate(iceCandidate)
        }
    }

    override fun onIceConnectionChange(iceConnectionState: IceConnectionState) {
        ui.launch {
            when (iceConnectionState) {
                IceConnectionState.CONNECTED -> {
                    callState.value = CallState.Live

                    releaseStopwatch()
                    stopwatch = StopwatchBuilder()
                        .startFormat("MM:SS")
                        .changeFormatWhen(1, TimeUnit.HOURS, "HH:MM:SS")
                        .actionWhen(5, TimeUnit.SECONDS) {
                            reminderVisibility.postValue(false)
                        }
                        .onTick { time ->
                            callDuration.postValue(time)
                        }
                        .build()
                    stopwatch?.start()
                }
                IceConnectionState.CLOSED, IceConnectionState.DISCONNECTED -> {
                    closeLiveCall(false)

                    callState.value =
                        CallState.Disconnected(CallState.Disconnected.Initiator.CALL_AGENT)
                }
                else -> {
                }
            }
        }
    }

    override fun onAddRemoteStream(mediaStream: MediaStream) {
        ui.launch {
            try {
                peerConnectionClient.addRemoteStreamToPeer(mediaStream)
            } catch (e: Exception) {
                // Ignored
            }
        }
    }

    override fun onRemoveStream(mediaStream: MediaStream) {
    }

    override fun onLocalVideoCapturerCreateError(e: Exception) {
        e.printStackTrace()
    }

    override fun onPeerConnectionError(errorMessage: String) {
        Logger.error(TAG, "onPeerConnectionError() -> errorMessage: $errorMessage")
    }

    override fun onRenegotiationNeeded() {
        // Ignored
    }

    /**
     * [SocketStateListener] implementation
     */

    override fun onSocketConnect() {
        io.launch {
            socketRepository.sendUserLanguage(settingsRepository.getLanguage())
        }
    }

    override fun onSocketDisconnect() {
        ui.launch {
            // Socket can be closed unexpectedly

            if (callState.value == CallState.Pending ||
                callState.value == CallState.Start ||
                callState.value == CallState.Ready ||
                callState.value == CallState.Live
            ) {
                Notification.Builder()
                    .setRandomId()
                    .setCreatedAt(nowAsSeconds)
                    .setBody(l10n.unknownNetworkError)
                    .build()
                    .addToChat()
            }

            closeLiveCall(true)

            releaseStopwatch()

            callState.value = CallState.Disconnected(CallState.Disconnected.Initiator.USER)
        }
    }

    /**
     * [WebRTCListener] implementation
     */

    override fun onCallAccept() {
        io.launch {
            socketRepository.sendQRTCAction(action = QRTCAction.PREPARE)

            withContext(ui) {
                callState.value = CallState.Start
            }
        }
    }

    override fun onCallRedirect() {
        io.launch {
            Logger.debug(TAG, "onCallRedirect()")

            createPeerConnection()

            withContext(ui) {
                callState.value = CallState.Pending

                isLocationTrackerEnabled.value = true
            }

            initMediaStreams()

            socketRepository.sendQRTCAction(action = QRTCAction.PREPARE)

            sendPostponedUserLocations()
        }
    }

    override fun onCallRedial() {
    }

    override fun onCallPrepare() {
        io.launch {
            socketRepository.sendQRTCAction(action = QRTCAction.READY)
        }
    }

    override fun onCallReady() {
        peerConnectionClient.createOffer()

        ui.launch {
            callState.value = CallState.Ready
        }
    }

    override fun onCallAnswer(sessionDescription: SessionDescription) {
        peerConnectionClient.setRemoteDescription(sessionDescription)
    }

    override fun onCallOffer(sessionDescription: SessionDescription) {
        if (sessionDescription.description.isBlank()) {
            Logger.error(TAG, "SessionDescription is empty")
        }

        peerConnectionClient.setRemoteDescription(sessionDescription)

        peerConnectionClient.createAnswer()
    }

    override fun onRemoteIceCandidate(iceCandidate: IceCandidate) {
        peerConnectionClient.addRemoteIceCandidate(iceCandidate)
    }

    override fun onPeerHangupCall() {
        io.launch {
            closeLiveCall(false)

            withContext(ui) {
                callState.value =
                    CallState.Disconnected(CallState.Disconnected.Initiator.CALL_AGENT)
            }
        }
    }

    /**
     * [ChatBotListener] implementation
     */

    override fun onFuzzyTaskOffered(text: String, timestamp: Long): Boolean {
        return true
    }

    override fun onNoResultsFound(text: String, timestamp: Long): Boolean {
        io.launch {
            Message.Builder()
                .setRandomId()
                .setIncomingDirection()
                .setCreatedAt(timestamp)
                .setBody(text)
                .build()
                .addToChat()
        }
        return true
    }

    override fun onMessage(message: Message) {
        io.launch {
            message.addToChat()
        }
    }

    override fun onCategories(categories: List<Category>) {
    }

    /**
     * [CallListener] implementation
     */

    override fun onPendingUsersQueueCount(text: String?, count: Int) {
        Logger.debug(TAG, "onPendingUsersQueueCount() -> text: $text, count: $count")
    }

    override fun onNoOnlineCallAgents(text: String?): Boolean {
        ui.launch {
            Message.Builder()
                .setRandomId()
                .setIncomingDirection()
                .setCreatedAt(nowAsSeconds)
                .setBody(text)
                .build()
                .addToChat()

            closeLiveCall(false)

            callState.value = CallState.Disconnected(CallState.Disconnected.Initiator.CALL_AGENT)
        }
        return true
    }

    override fun onCallAgentGreet(fullName: String, photoUrl: String?, text: String) {
        ui.launch {
            callAgent.value = UICallAgent(fullName, photoUrl)

            Message.Builder()
                .setRandomId()
                .setIncomingDirection()
                .setCreatedAt(nowAsSeconds)
                .setBody(text.replace("{}", fullName))
                .build()
                .addToChat()

            sendPostponedUserLocations()
        }
    }

    override fun onCallFeedback(text: String, rateButtons: List<RateButton>?) {
        io.launch {
            val rows = mutableListOf<ReplyMarkup.Row>()
            if (!rateButtons.isNullOrEmpty()) {
                var row = mutableListOf<RateButton>()
                for (button in rateButtons) {
                    if (row.size == 2) {
                        rows.add(ReplyMarkup.Row(row))
                        row = mutableListOf()
                    }

                    row.add(button)
                }
                if (!row.isNullOrEmpty()) {
                    rows.add(ReplyMarkup.Row(row))
                }
            }

            Message.Builder()
                .setRandomId()
                .setIncomingDirection()
                .setCreatedAt(nowAsSeconds)
                .setBody(text)
                .setReplyMarkup(
                    if (rows.isNullOrEmpty()) {
                        null
                    } else {
                        InlineReplyMarkup(rows)
                    }
                )
                .build()
                .addToChat()
        }
    }

    override fun onLiveChatTimeout(text: String?, timestamp: Long): Boolean {
        ui.launch {
            Notification.Builder()
                .setRandomId()
                .setCreatedAt(timestamp)
                .setBody(text)
                .build()
                .addToChat()

            closeLiveCall(false)

            callState.value = CallState.Disconnected(CallState.Disconnected.Initiator.USER)
        }
        return true
    }

    override fun onUserRedirected(text: String?, timestamp: Long): Boolean {
        ui.launch {
            Notification.Builder()
                .setRandomId()
                .setCreatedAt(timestamp)
                .setBody(text)
                .build()
                .addToChat()

            closeLiveCall(false)

            callState.value = CallState.UserRedirected
        }
        return true
    }

    override fun onCallAgentDisconnected(text: String?, timestamp: Long): Boolean {
        ui.launch {
            Notification.Builder()
                .setRandomId()
                .setCreatedAt(timestamp)
                .setBody(text)
                .build()
                .addToChat()

            closeLiveCall(false)

            callState.value = CallState.Disconnected(CallState.Disconnected.Initiator.CALL_AGENT)
        }
        return true
    }

    /**
     * [ARMListener] implementation
     */

    override fun onCard102Update(card102Status: Card102Status) {
        io.launch {
            when (card102Status) {
                // First step (new card is created)
                Card102Status.NEW_CARD102 -> {
                    socketRepository.sendLocationUpdateSubscription()
                }
                // Second step (for example, police force is assigned & location updates will be sent)
                Card102Status.ASSIGNED_FORCE -> {
                }
                // Third step (for example, when police force on spot & ready to help)
                Card102Status.FORCE_ON_SPOT -> {
                }
                Card102Status.COMPLETED_OPERATION -> {
                }
                else -> {
                }
            }

            armRepository.setLastCard102Status(card102Status)

            withContext(ui) {
                this@CallViewModel.card102Status.value = card102Status
            }
        }
    }

    override fun onLocationUpdate(locationUpdates: List<LocationUpdate>) {
        io.launch {
            locationUpdates.forEach { locationUpdate ->
                val otherLocationUpdateIndex = otherLocationUpdates.indexOfFirst {
                    it.gpsCode == locationUpdate.gpsCode
                }
                if (otherLocationUpdateIndex < 0) {
                    otherLocationUpdates.add(locationUpdate)
                } else {
                    otherLocationUpdates[otherLocationUpdateIndex] = locationUpdate

                    withContext(ui) {
                        if (locationUpdate.gpsCode == selectedOtherLocation.value?.gpsCode) {
                            selectedSelfLocation.value = null
                            selectedOtherLocation.value = locationUpdate
                        }
                    }
                }
            }

            armRepository.setLastLocationUpdates(locationUpdates)

            otherLocations.postValue(locationUpdates)
        }
    }

    override fun onCoroutineException(context: CoroutineContext, exception: Throwable) {
        Logger.error(TAG, "onCoroutineException() -> $context: $exception")
    }

    private suspend fun Entity.addToChat() = withContext(ui) {
        Logger.debug(TAG, "addToChat() -> entity: ${this@addToChat}")
        when (this@addToChat) {
            is Message -> {
                if (!isEmpty()) {
                    conversation.history.add(this@addToChat)
                    newMessage.value = this@addToChat
                }
            }
            is Notification -> {
                conversation.history.add(this@addToChat)
                newMessage.value = this@addToChat
            }
            else -> {
            }
        }
    }

    private fun releaseStopwatch() {
        if (stopwatch == null) return
        try {
            stopwatch?.release()
        } catch (outer: Exception) {
            try {
                stopwatch?.stop()
                stopwatch?.reset()
            } catch (inner: Exception) {
                inner.printStackTrace()
            }
        }
        stopwatch = null
    }

    override fun onCleared() {
        io.launch {
            closeLiveCall(true)

            callState.value = CallState.Finished
        }

        peerConnectionClient.removeListeners()

        socketRepository.unregisterAllEventListeners()
        socketRepository.removeAllListeners()

        super.onCleared()
    }

}