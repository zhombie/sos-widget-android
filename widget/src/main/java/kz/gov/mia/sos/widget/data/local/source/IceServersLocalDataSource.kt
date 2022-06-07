package kz.gov.mia.sos.widget.data.local.source

import kz.gov.mia.sos.widget.domain.repository.IceServersRepository
import kz.inqbox.sdk.domain.model.webrtc.IceServer

internal class IceServersLocalDataSource : IceServersRepository {

    private var iceServers: List<IceServer>? = null

    override suspend fun getIceServers(): List<IceServer> = iceServers ?: emptyList()

    override suspend fun setIceServers(iceServers: List<IceServer>?): Boolean {
        this.iceServers = iceServers
        return this.iceServers == iceServers
    }

    override suspend fun clear(): Boolean {
        iceServers = null
        return iceServers == null
    }

}