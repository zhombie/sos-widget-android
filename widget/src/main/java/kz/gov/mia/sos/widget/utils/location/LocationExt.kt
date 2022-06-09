package kz.gov.mia.sos.widget.utils.location

import android.os.SystemClock
import kz.inqbox.sdk.domain.model.geo.Location
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.nanoseconds

internal fun Location.getDisplayElapsedTime(): String {
    val elapsedRealtimeNanos = (elapsedRealtimeNanos ?: SystemClock.elapsedRealtimeNanos())

    val difference = SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos

    val duration = difference.nanoseconds

    fun formatInterval(millis: Long): String {
        return try {
            val hour = TimeUnit.MILLISECONDS.toHours(millis)
            val minute = TimeUnit.MILLISECONDS.toMinutes(millis - TimeUnit.HOURS.toMillis(hour))
            val seconds = TimeUnit.MILLISECONDS.toSeconds(millis - TimeUnit.HOURS.toMillis(hour) - TimeUnit.MINUTES.toMillis(minute))
            String.format("%02d:%02d:%02d", hour, minute, seconds)
        } catch (e: Exception) {
            e.printStackTrace()
            String.format("%02d:%02d:%02d", 0, 0, 0)
        }
    }

    return formatInterval(duration.inWholeMilliseconds)
}
