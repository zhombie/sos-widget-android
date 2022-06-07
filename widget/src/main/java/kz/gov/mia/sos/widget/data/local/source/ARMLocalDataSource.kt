package kz.gov.mia.sos.widget.data.local.source

import kz.gov.mia.sos.widget.domain.repository.ARMRepository
import kz.inqbox.sdk.socket.model.Card102Status
import kz.inqbox.sdk.socket.model.LocationUpdate

internal class ARMLocalDataSource : ARMRepository {

    private var card102Status: Card102Status? = null

    private var locationUpdates: List<LocationUpdate>? = null

    private var durationMillis: Long? = null

    override fun getLastCard102Status(): Card102Status? {
        return card102Status
    }

    override fun setLastCard102Status(card102Status: Card102Status): Boolean {
        this.card102Status = card102Status
        return this.card102Status == card102Status
    }

    override fun getLastLocationUpdates(): List<LocationUpdate> {
        return locationUpdates ?: emptyList()
    }

    override fun setLastLocationUpdates(locationUpdates: List<LocationUpdate>): Boolean {
        this.locationUpdates = locationUpdates
        return this.locationUpdates == locationUpdates
    }

    override fun getLastDurationMillis(): Long? {
        return durationMillis
    }

    override fun setLastDurationMillis(durationMillis: Long): Boolean {
        this.durationMillis = durationMillis
        return this.durationMillis == durationMillis
    }

    override fun clear(): Boolean {
        card102Status = null
        locationUpdates = null
        durationMillis = null
        return card102Status == null && locationUpdates == null && durationMillis == null
    }

}