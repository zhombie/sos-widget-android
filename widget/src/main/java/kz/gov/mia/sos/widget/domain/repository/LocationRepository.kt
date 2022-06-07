package kz.gov.mia.sos.widget.domain.repository

import kz.gov.mia.sos.widget.domain.model.LocationConstraints
import kz.inqbox.sdk.domain.model.geo.Location

interface LocationRepository {
    fun isLastFoundLocationOutdated(expirationSeconds: Long = 5 * 60L): Boolean

    fun getLastFoundLocation(): Location?
    fun getTemporaryLastFoundLocation(): Location?
    fun setLastFoundLocation(location: Location): Boolean
    fun removeLastFoundLocation(): Boolean

    fun getLastFoundGeocode(): LocationConstraints.ReverseGeocode?
    fun getTemporaryLastFoundGeocode(): LocationConstraints.ReverseGeocode?
    fun setLastFoundGeocode(geocode: LocationConstraints.ReverseGeocode): Boolean
    fun removeLastFoundGeocode(): Boolean

    fun clear(): Boolean
}