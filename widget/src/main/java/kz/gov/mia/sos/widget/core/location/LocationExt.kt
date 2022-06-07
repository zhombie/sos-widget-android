package kz.gov.mia.sos.widget.core.location

import android.location.Location
import com.google.android.gms.maps.model.LatLng

internal fun Location.asLatLng(): LatLng =
    LatLng(latitude, longitude)


internal fun Location.setBearing(
    bearing: Float,
    bearingAccuracyDegrees: Float
): Location {
    this.bearing = bearing
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        this.bearingAccuracyDegrees = bearingAccuracyDegrees
    }
    return this
}