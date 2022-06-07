package kz.gov.mia.sos.widget.ui.presentation.call.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kz.gov.mia.sos.widget.core.device.Device
import kz.gov.mia.sos.widget.core.error.UnknownViewModelClassException
import kz.gov.mia.sos.widget.domain.repository.ARMRepository
import kz.gov.mia.sos.widget.domain.repository.IceServersRepository
import kz.gov.mia.sos.widget.domain.repository.LocationRepository
import kz.gov.mia.sos.widget.domain.repository.SettingsRepository
import kz.gov.mia.sos.widget.ui.model.L10n
import kz.inqbox.sdk.domain.model.call.type.CallType
import kz.inqbox.sdk.socket.repository.SocketRepository
import kz.inqbox.sdk.webrtc.PeerConnectionClient

internal class CallViewModelFactory constructor(
    private val callType: CallType,
    private val callTopic: String? = null,
    private val l10n: L10n,
    private val device: Device,
    private val armRepository: ARMRepository,
    private val iceServersRepository: IceServersRepository,
    private val locationRepository: LocationRepository,
    private val peerConnectionClient: PeerConnectionClient,
    private val settingsRepository: SettingsRepository,
    private val socketRepository: SocketRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CallViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CallViewModel(
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
            ) as T
        }
        throw UnknownViewModelClassException(modelClass.simpleName)
    }

}