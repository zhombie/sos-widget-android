package kz.gov.mia.sos.widget.di

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModelStoreOwner
import kz.garage.kotlin.simpleNameOf
import kz.garage.lifecycle.viewmodel.factory.createViewModel
import kz.gov.mia.sos.widget.core.Preferences
import kz.gov.mia.sos.widget.core.device.Device
import kz.gov.mia.sos.widget.core.logging.Logger
import kz.gov.mia.sos.widget.data.local.source.ARMLocalDataSource
import kz.gov.mia.sos.widget.data.local.source.IceServersLocalDataSource
import kz.gov.mia.sos.widget.data.local.source.LocationLocalDataSource
import kz.gov.mia.sos.widget.data.local.source.SharedPreferencesSettingsDataSource
import kz.gov.mia.sos.widget.domain.repository.ARMRepository
import kz.gov.mia.sos.widget.domain.repository.IceServersRepository
import kz.gov.mia.sos.widget.domain.repository.LocationRepository
import kz.gov.mia.sos.widget.domain.repository.SettingsRepository
import kz.gov.mia.sos.widget.ui.model.L10n
import kz.gov.mia.sos.widget.ui.presentation.call.vm.CallViewModel
import kz.gov.mia.sos.widget.ui.presentation.call.vm.CallViewModelFactory
import kz.gov.mia.sos.widget.ui.presentation.home.vm.HomeViewModel
import kz.gov.mia.sos.widget.ui.presentation.home.vm.HomeViewModelFactory
import kz.inqbox.sdk.domain.model.call.type.CallType
import kz.inqbox.sdk.socket.SocketClient
import kz.inqbox.sdk.socket.repository.SocketRepository
import kz.inqbox.sdk.webrtc.PeerConnectionClient

internal class Injection private constructor(context: Context) {

    companion object {
        private val TAG = simpleNameOf<Injection>()

        @Volatile
        private var INSTANCE: Injection? = null

        fun getInstance(context: Context): Injection =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Injection(context.applicationContext).also { INSTANCE = it }
            }
    }

    private val socketRepository: SocketRepository
        get() = SocketClient.getInstance()

    private val device: Device by lazy { Device(context) }

    init {
        Logger.debug(TAG, "created")
    }

    private val iceServersRepository: IceServersRepository by lazy {
        IceServersLocalDataSource()
    }

    private val locationRepository: LocationRepository by lazy {
        LocationLocalDataSource(provideSharedPreferences(context))
    }

    private val armRepository: ARMRepository by lazy {
        ARMLocalDataSource()
    }

    val settingsRepository: SettingsRepository by lazy {
        SharedPreferencesSettingsDataSource(context)
    }

    fun provideSharedPreferences(context: Context): SharedPreferences =
        Preferences.getSharedPreferences(context)

    fun providePeerConnectionClient(context: Context): PeerConnectionClient =
        PeerConnectionClient(context.applicationContext)

    fun provideHomeViewModel(
        owner: ViewModelStoreOwner,
        callTopic: String?
    ): HomeViewModel =
        HomeViewModelFactory(
            callTopic = callTopic,
            iceServersRepository = iceServersRepository,
            locationRepository = locationRepository,
            settingsRepository = settingsRepository,
            socketRepository = socketRepository
        ).createViewModel(owner)

    fun provideCallViewModel(
        owner: ViewModelStoreOwner,
        callType: CallType,
        callTopic: String? = null,
        l10n: L10n,
        peerConnectionClient: PeerConnectionClient
    ): CallViewModel =
        CallViewModelFactory(
            callType = callType,
            callTopic = callTopic,
            l10n = l10n,
            device = device,
            armRepository = armRepository,
            iceServersRepository = iceServersRepository,
            locationRepository = locationRepository,
            peerConnectionClient = peerConnectionClient,
            settingsRepository = settingsRepository,
            socketRepository = socketRepository
        ).createViewModel(owner)

    fun destroy() {
        INSTANCE = null
    }

}