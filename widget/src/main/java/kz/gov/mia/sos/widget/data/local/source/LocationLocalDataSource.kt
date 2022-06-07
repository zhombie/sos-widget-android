package kz.gov.mia.sos.widget.data.local.source

import android.content.SharedPreferences
import androidx.core.content.edit
import kz.garage.json.asJSONObject
import kz.garage.json.getStringOrNull
import kz.garage.json.jsonObject
import kz.gov.mia.sos.widget.domain.location.isLocationFixLessThan
import kz.gov.mia.sos.widget.domain.model.LocationConstraints
import kz.gov.mia.sos.widget.domain.repository.LocationRepository
import kz.inqbox.sdk.domain.model.geo.Location

internal class LocationLocalDataSource constructor(
    private val sharedPreferences: SharedPreferences
) : LocationRepository {

    private object Key {
        const val LOCATION = "location"
        const val GEOCODE = "geocode"
    }

    private var temporaryLocation: Location? = null
    private var temporaryGeocode: LocationConstraints.ReverseGeocode? = null

    override fun isLastFoundLocationOutdated(expirationSeconds: Long): Boolean {
        val location = getLastFoundLocation()
        return if (location == null) {
            true
        } else {
            !location.isLocationFixLessThan(seconds = expirationSeconds)
        }
    }

    override fun getLastFoundLocation(): Location? {
        val location = sharedPreferences.getString(Key.LOCATION, null)
        return if (location == null) {
            null
        } else {
            val json = location.asJSONObject ?: return null
            Location(
                provider = json.getString("provider"),
                latitude = json.getDouble("latitude"),
                longitude = json.getDouble("longitude"),
                bearing = json.getDouble("bearing").toFloat(),
                bearingAccuracyDegrees = json.getDouble("bearingAccuracyDegrees").toFloat(),
                xAccuracyMeters = json.getDouble("xAccuracyMeters").toFloat(),
                yAccuracyMeters = json.getDouble("yAccuracyMeters").toFloat(),
                speed = json.getDouble("speed").toFloat(),
                speedAccuracyMetersPerSecond = json.getDouble("speedAccuracyMetersPerSecond").toFloat(),
                elapsedRealtimeNanos = json.getLong("elapsedRealtimeNanos"),
                elapsedRealtimeUncertaintyNanos = json.getDouble("elapsedRealtimeUncertaintyNanos")
            )
        }
    }

    override fun getTemporaryLastFoundLocation(): Location? {
        return temporaryLocation
    }

    override fun setLastFoundLocation(location: Location): Boolean {
        sharedPreferences.edit {
            val json = jsonObject {
                put("provider", location.provider)
                put("latitude", location.latitude)
                put("longitude", location.longitude)
                put("bearing", location.bearing?.toDouble())
                put("bearingAccuracyDegrees", location.bearingAccuracyDegrees?.toDouble())
                put("xAccuracyMeters", location.xAccuracyMeters?.toDouble())
                put("yAccuracyMeters", location.yAccuracyMeters?.toDouble())
                put("speed", location.speed?.toDouble())
                put("speedAccuracyMetersPerSecond", location.speedAccuracyMetersPerSecond?.toDouble())
                put("elapsedRealtimeNanos", location.elapsedRealtimeNanos)
                put("elapsedRealtimeUncertaintyNanos", location.elapsedRealtimeUncertaintyNanos)
            }
            putString(Key.LOCATION, json.toString())
        }
        val lastFoundLocation = getLastFoundLocation()
        return if (lastFoundLocation != null && lastFoundLocation.isEqualsTo(location)) {
            temporaryLocation = lastFoundLocation
            true
        } else {
            false
        }
    }

    override fun removeLastFoundLocation(): Boolean {
        sharedPreferences.edit { remove(Key.LOCATION) }
        return getLastFoundLocation() == null
    }

    override fun getLastFoundGeocode(): LocationConstraints.ReverseGeocode? {
        val geocode = sharedPreferences.getString(Key.GEOCODE, null)
        return if (geocode == null) {
            null
        } else {
            val json = geocode.asJSONObject ?: return null
            val address = json.getJSONObject("address")
            LocationConstraints.ReverseGeocode(
                latitude = json.getDouble("latitude"),
                longitude = json.getDouble("longitude"),
                displayName = json.getString("display_name"),
                address = LocationConstraints.ReverseGeocode.Address(
                    residential = address.getStringOrNull("residential"),
                    cityDistrict = address.getStringOrNull("city_district"),
                    city = address.getStringOrNull("city"),
                    county = address.getStringOrNull("county"),
                    state = address.getStringOrNull("state"),
                    country = address.getStringOrNull("country"),
                    countryCode = address.getStringOrNull("country_code")
                ),
                boundingBox = null
            )
        }
    }

    override fun getTemporaryLastFoundGeocode(): LocationConstraints.ReverseGeocode? {
        return temporaryGeocode
    }

    override fun setLastFoundGeocode(geocode: LocationConstraints.ReverseGeocode): Boolean {
        sharedPreferences.edit {
            val json = jsonObject {
                put("latitude", geocode.latitude)
                put("longitude", geocode.longitude)
                put("display_name", geocode.displayName)
                put("address", jsonObject {
                    put("residential", geocode.address.residential)
                    put("city_district", geocode.address.cityDistrict)
                    put("city", geocode.address.city)
                    put("county", geocode.address.county)
                    put("state", geocode.address.state)
                    put("country", geocode.address.country)
                    put("country_code", geocode.address.countryCode)
                })
            }
            putString(Key.GEOCODE, json.toString())
        }
        val lastFoundGeocode = getLastFoundGeocode()
        return if (lastFoundGeocode != null && lastFoundGeocode.isEqualsTo(geocode)) {
            temporaryGeocode = lastFoundGeocode
            true
        } else {
            false
        }
    }

    override fun removeLastFoundGeocode(): Boolean {
        sharedPreferences.edit { remove(Key.GEOCODE) }
        return getLastFoundGeocode() == null
    }

    override fun clear(): Boolean {
        removeLastFoundLocation()
        temporaryLocation = null
        return getLastFoundLocation() == null && temporaryLocation == null
    }

    private fun Location.isEqualsTo(other: Location): Boolean =
        latitude == other.latitude && longitude == other.longitude

    private fun LocationConstraints.ReverseGeocode.isEqualsTo(
        other: LocationConstraints.ReverseGeocode
    ): Boolean =
        latitude == other.latitude && longitude == other.longitude && displayName == other.displayName

}