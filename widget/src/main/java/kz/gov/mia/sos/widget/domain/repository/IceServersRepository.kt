package kz.gov.mia.sos.widget.domain.repository

import kz.inqbox.sdk.domain.model.webrtc.IceServer

interface IceServersRepository {
    suspend fun getIceServers(): List<IceServer>
    suspend fun setIceServers(iceServers: List<IceServer>?): Boolean

    suspend fun clear(): Boolean
}