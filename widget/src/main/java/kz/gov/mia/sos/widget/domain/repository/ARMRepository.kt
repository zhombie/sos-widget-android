package kz.gov.mia.sos.widget.domain.repository

import kz.inqbox.sdk.socket.model.Card102Status
import kz.inqbox.sdk.socket.model.LocationUpdate

interface ARMRepository {
    fun getLastCard102Status(): Card102Status?
    fun setLastCard102Status(card102Status: Card102Status): Boolean

    fun getLastLocationUpdates(): List<LocationUpdate>
    fun setLastLocationUpdates(locationUpdates: List<LocationUpdate>): Boolean

    fun getLastDurationMillis(): Long?
    fun setLastDurationMillis(durationMillis: Long): Boolean

    fun clear(): Boolean
}