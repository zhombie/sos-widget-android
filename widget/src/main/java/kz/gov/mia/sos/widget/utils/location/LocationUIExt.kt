package kz.gov.mia.sos.widget.utils.location

import android.animation.Animator
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import kotlin.math.abs
import kotlin.math.sign

internal fun interface LatLngInterpolator {
    fun interpolate(fraction: Float, a: LatLng, b: LatLng): LatLng

    class LinearFixed : LatLngInterpolator {
        override fun interpolate(fraction: Float, a: LatLng, b: LatLng): LatLng {
            val lat = (b.latitude - a.latitude) * fraction + a.latitude
            var lngDelta = b.longitude - a.longitude
            // Take the shortest path across the 180th meridian.
            if (abs(lngDelta) > 180) {
                lngDelta -= sign(lngDelta) * 360
            }
            val lng = lngDelta * fraction + a.longitude
            return LatLng(lat, lng)
        }
    }
}


internal fun Marker.animateTo(endPosition: LatLng, callback: () -> Unit) {
    val startPosition = position
    val interpolator = LatLngInterpolator.LinearFixed()
    val valueAnimator = ValueAnimator.ofFloat(0F, 1F)
    valueAnimator.duration = 750  // duration 1 second
    valueAnimator.interpolator = LinearInterpolator()
    valueAnimator.addListener(object : Animator.AnimatorListener {
        override fun onAnimationStart(animation: Animator?) {
        }

        override fun onAnimationCancel(animation: Animator?) {
            callback()
        }

        override fun onAnimationEnd(animation: Animator?) {
            callback()
        }

        override fun onAnimationRepeat(animation: Animator?) {
        }
    })
    valueAnimator.addUpdateListener { animation ->
        try {
            val fraction = animation.animatedFraction
            position = interpolator.interpolate(fraction, startPosition, endPosition)
        } catch (e: Exception) {
            // Ignored
        }
    }
    valueAnimator.start()
}