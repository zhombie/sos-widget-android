package kz.gov.mia.sos.widget.ui.presentation.home.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kz.gov.mia.sos.widget.core.error.UnknownViewModelClassException
import kz.gov.mia.sos.widget.domain.repository.IceServersRepository
import kz.gov.mia.sos.widget.domain.repository.LocationRepository
import kz.gov.mia.sos.widget.domain.repository.SettingsRepository
import kz.inqbox.sdk.socket.repository.SocketRepository

internal class HomeViewModelFactory constructor(
    private val callTopic: String?,
    private val iceServersRepository: IceServersRepository,
    private val locationRepository: LocationRepository,
    private val settingsRepository: SettingsRepository,
    private val socketRepository: SocketRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(
                callTopic = callTopic,
                iceServersRepository = iceServersRepository,
                locationRepository = locationRepository,
                settingsRepository = settingsRepository,
                socketRepository = socketRepository
            ) as T
        }
        throw UnknownViewModelClassException(modelClass.simpleName)
    }

}