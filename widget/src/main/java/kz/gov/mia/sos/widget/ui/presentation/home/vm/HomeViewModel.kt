package kz.gov.mia.sos.widget.ui.presentation.home.vm

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.loopj.android.http.RequestParams
import kotlinx.coroutines.withContext
import kz.garage.kotlin.simpleNameOf
import kz.garage.lifecycle.livedata.SingleLiveEvent
import kz.garage.location.core.isMocked
import kz.gov.mia.sos.widget.api.SOSWidget
import kz.gov.mia.sos.widget.core.URLManager
import kz.gov.mia.sos.widget.core.logging.Logger
import kz.gov.mia.sos.widget.data.remote.api.Endpoints
import kz.gov.mia.sos.widget.data.remote.http.AsyncHttpClientBuilder
import kz.gov.mia.sos.widget.data.remote.http.IceServersResponseHandler
import kz.gov.mia.sos.widget.data.remote.http.LocationConstraintsResponseHandler
import kz.gov.mia.sos.widget.domain.location.asLocation
import kz.gov.mia.sos.widget.domain.model.LocationConstraints
import kz.gov.mia.sos.widget.domain.repository.IceServersRepository
import kz.gov.mia.sos.widget.domain.repository.LocationRepository
import kz.gov.mia.sos.widget.domain.repository.SettingsRepository
import kz.gov.mia.sos.widget.ui.platform.BaseViewModel
import kz.gov.mia.sos.widget.ui.presentation.home.HomeScreen
import kz.gov.mia.sos.widget.ui.presentation.home.Initialization
import kz.inqbox.sdk.domain.model.call.type.CallType
import kz.inqbox.sdk.domain.model.webrtc.IceServer
import kz.inqbox.sdk.socket.listener.SocketStateListener
import kz.inqbox.sdk.socket.repository.SocketRepository

class HomeViewModel constructor(
    private val callType: CallType = CallType.VIDEO,
    private val callTopic: String?,
    private val iceServersRepository: IceServersRepository,
    private val locationRepository: LocationRepository,
    private val settingsRepository: SettingsRepository,
    private val socketRepository: SocketRepository
) : BaseViewModel(),
    SocketStateListener,
    Initialization.EventListener {

    companion object {
        private val TAG = simpleNameOf<HomeViewModel>()
    }

    private val initialization = Initialization(this)

    private val asyncHttpClient by lazy(LazyThreadSafetyMode.NONE) {
        AsyncHttpClientBuilder()
            .setLoggingEnabled(Logger.isEnabled)
            .build()
    }

    private val state by lazy { MutableLiveData<HomeScreen.State>(HomeScreen.State.IDLE) }
    fun getState(): LiveData<HomeScreen.State> = state

    private val command by lazy { SingleLiveEvent<HomeScreen.Command>() }
    fun getCommand(): LiveData<HomeScreen.Command> = command

    private val message by lazy { SingleLiveEvent<HomeScreen.Message>() }
    fun getMessage(): LiveData<HomeScreen.Message> = message

    private val route by lazy { SingleLiveEvent<HomeScreen.Route>() }
    fun getRoute(): LiveData<HomeScreen.Route> = route

    private val geocode by lazy { MutableLiveData<LocationConstraints.ReverseGeocode?>(null) }
    fun getGeocode(): LiveData<LocationConstraints.ReverseGeocode?> = geocode

    init {
        initWebSocket()
    }

    private fun initWebSocket() {
        io.launch {
            withContext(ui) {
                state.value = HomeScreen.State.Loading(
                    phase = HomeScreen.State.Phase.WEB_SOCKET_CONNECTION
                )
            }

            socketRepository.setSocketStateListener(this@HomeViewModel)
            ensureWebSocketConnection()
            socketRepository.registerSocketConnectEventListener()
            socketRepository.registerSocketDisconnectEventListener()
        }
    }

    private suspend fun ensureWebSocketConnection() = withContext(io) {
        if (!socketRepository.isConnected()) {
            socketRepository.create(URLManager.getSocketUrl())
            socketRepository.connect()
        }
    }

    // TODO: Should be IO, but AsyncHttpClient requires UI thread
    private suspend fun loadIceServers(
        onSuccess: (iceServers: List<IceServer>) -> Unit,
        onFailure: (throwable: Throwable?) -> Unit
    ) = withContext(ui) {
        val requestParams = RequestParams()
        requestParams.put("username", SOSWidget.credentials?.username)
        requestParams.put("password", SOSWidget.credentials?.password)

        asyncHttpClient.get(
            Endpoints.ICE_SERVERS.getEndpoint(),
            requestParams,
            IceServersResponseHandler(
                onSuccess = { iceServers ->
                    onSuccess(iceServers)
                },
                onFailure = { throwable ->
                    onFailure(throwable)
                }
            )
        )
    }

    // TODO: Should be IO, but AsyncHttpClient requires UI thread
    private suspend fun loadLocationConstraints(
        vararg params: Pair<String, Any>,
        onSuccess: (reverseGeocode: LocationConstraints.ReverseGeocode) -> Unit,
        onFailure: (throwable: Throwable?) -> Unit
    ) = withContext(ui) {
        val requestParams = RequestParams()
        requestParams.setUseJsonStreamer(true)
        requestParams.put("username", SOSWidget.credentials?.username)
        requestParams.put("password", SOSWidget.credentials?.password)
        params.forEach {
            requestParams.put(it.first, it.second)
        }

        asyncHttpClient.post(
            Endpoints.LOCATION_CONSTRAINTS.getEndpoint(),
            requestParams,
            LocationConstraintsResponseHandler(
                onSuccess = { locationConstraints ->
                    onSuccess(locationConstraints.reverseGeocode)
                },
                onFailure = { throwable ->
                    onFailure(throwable)
                }
            )
        )
    }

    /**
     * [Initialization.EventListener] implementation
     */

    override fun onWebSocketConnectionState(isConnected: Boolean) {
        ui.launch {
            if (isConnected) {
                state.value = HomeScreen.State.Loading(
                    phase = HomeScreen.State.Phase.ICE_SERVERS_REQUEST
                )
            } else {
                state.value = HomeScreen.State.Error(
                    phase = HomeScreen.State.Phase.WEB_SOCKET_CONNECTION
                )
            }

            if (isConnected) {
                loadIceServers(
                    onSuccess = {
                        io.launch {
                            initialization.isIceServersFetched = true
                            iceServersRepository.setIceServers(it)
                        }
                    },
                    onFailure = {
                        it?.printStackTrace()

                        io.launch {
                            // Continue no matter on result (success or failure)
                            initialization.isIceServersFetched = true
                            iceServersRepository.setIceServers(null)
                        }
                    }
                )
            }
        }
    }

    override fun onIceServersFetchState(isFetched: Boolean) {
        ui.launch {
            state.value = HomeScreen.State.Content
        }
    }

    /**
     * [SocketStateListener] implementation
     */

    override fun onSocketConnect() {
        io.launch {
            initialization.isConnectedToWebSocket = true

            socketRepository.sendUserLanguage(settingsRepository.getLanguage())
        }
    }

    override fun onSocketDisconnect() {
        io.launch {
            initialization.isConnectedToWebSocket = false
        }
    }

    // [BEGIN] User interactions

    fun onLocationProviderEnabled() {
        Logger.debug(TAG, "onLocationProviderEnabled()")

        io.launch {
            val lastFoundGeocode = locationRepository.getLastFoundGeocode()
                ?: locationRepository.getTemporaryLastFoundGeocode()

//            if (lastFoundGeocode == null || locationRepository.isLastFoundLocationOutdated(3 * 60L)) {
            if (lastFoundGeocode == null) {
                withContext(ui) {
                    geocode.value = lastFoundGeocode
                    command.value = HomeScreen.Command.RequestLocation
                }
            } else {
                withContext(ui) {
                    geocode.value = lastFoundGeocode
                }
            }
        }
    }

    fun onLocationFound(location: Location?) {
        Logger.debug(TAG, "location: $location")

        io.launch {
            when {
                location == null -> {
                    withContext(ui) {
                        message.value = HomeScreen.Message.Location.UnableToDetermine
                    }
                }
                location.isMocked() -> {
                    withContext(ui) {
                        message.value = HomeScreen.Message.Location.Mocked
                    }
                }
                else -> {
                    loadLocationConstraints(
                        params = arrayOf(
                            "lat" to location.latitude,
                            "lon" to location.longitude,
                            "language" to settingsRepository.getLanguage().key
                        ),
                        onSuccess = {
                            io.launch {
                                locationRepository.setLastFoundLocation(location.asLocation())
                                locationRepository.setLastFoundGeocode(it)

                                withContext(ui) {
                                    geocode.value = it
                                }
                            }
                        },
                        onFailure = {
                            it?.printStackTrace()

                            ui.launch {
                                message.value = HomeScreen.Message.Location.UnableToDetermine
                            }
                        }
                    )
                }
            }
        }
    }

    fun onSOSButtonPressed() {
        io.launch {
            if (socketRepository.isConnected()) {
                withContext(ui) {
                    route.value = HomeScreen.Route.Call(callType, callTopic)
                }
            } else {
                withContext(ui) {
                    message.value = HomeScreen.Message.WebSocket.ConnectionFailed
                }
            }
        }
    }

    // [END] User interactions

    /**
     * [BaseViewModel.onCleared] implementation
     */

    override fun onCleared() {
        super.onCleared()

        initialization.dispose()

        asyncHttpClient.cancelAllRequests(true)

        socketRepository.unregisterAllEventListeners()
        socketRepository.removeAllListeners()
        socketRepository.release()
    }

}