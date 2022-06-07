package kz.gov.mia.sos.widget.utils

import android.os.Build
import java.time.Clock
import java.util.concurrent.TimeUnit

internal val Long.secondsAsMillis: Long
    get() = if (this in 0..60) {
        TimeUnit.SECONDS.toMillis(this)
    } else {
        throw IllegalStateException("Unable to convert.")
    }

internal val Long.millisAsSeconds: Long
    get() = TimeUnit.MILLISECONDS.toSeconds(this)


internal val Long.nanosAsMillis: Long
    get() = TimeUnit.NANOSECONDS.toMillis(this)


internal val Long.nanosAsSeconds: Long
    get() = TimeUnit.NANOSECONDS.toSeconds(this)


internal val nowAsSeconds: Long
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Clock.systemUTC().instant().epochSecond
    } else {
        TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
    }
