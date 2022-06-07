package kz.gov.mia.sos.widget.domain.location

import android.os.SystemClock
import kz.gov.mia.sos.widget.utils.nanosAsSeconds
import kz.inqbox.sdk.domain.model.geo.Location

fun android.location.Location.asLocation(): Location {
    var location = Location(
        provider = provider,
        latitude = latitude,
        longitude = longitude,
        bearing = bearing,
        xAccuracyMeters = accuracy,
        speed = speed,
        elapsedRealtimeNanos = elapsedRealtimeNanos,
    )

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        location = location.copy(
            bearingAccuracyDegrees = bearingAccuracyDegrees,
            yAccuracyMeters = verticalAccuracyMeters,
            speedAccuracyMetersPerSecond = speedAccuracyMetersPerSecond
        )
    }

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        location = location.copy(elapsedRealtimeUncertaintyNanos = elapsedRealtimeUncertaintyNanos)
    }

    return location
}


fun Location.asLocation(): android.location.Location {
    val location = android.location.Location(provider)

    location.latitude = latitude
    location.longitude = longitude
    location.bearing  = bearing ?: 0.0F
    location.accuracy = xAccuracyMeters ?: 0.0F
    location.speed = speed ?: 0.0F
    location.elapsedRealtimeNanos = elapsedRealtimeNanos ?: 0L

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        location.bearingAccuracyDegrees = bearingAccuracyDegrees ?: 0.0F
        location.speedAccuracyMetersPerSecond = speedAccuracyMetersPerSecond ?: 0.0F
        location.verticalAccuracyMeters = yAccuracyMeters ?: 0.0F
    }

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        location.elapsedRealtimeUncertaintyNanos = elapsedRealtimeUncertaintyNanos ?: 0.0
    }

    return location
}


fun Location.isLocationFixLessThan(seconds: Long): Boolean =
    (SystemClock.elapsedRealtimeNanos() - (elapsedRealtimeNanos ?: 0L)).nanosAsSeconds < seconds
