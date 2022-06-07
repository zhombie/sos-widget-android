package kz.gov.mia.sos.widget.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

internal fun Context?.bitmapDescriptorFromVector(
    @DrawableRes vectorResId: Int
): BitmapDescriptor? {
    if (this == null) return null
    val vectorDrawable = ContextCompat.getDrawable(this, vectorResId) ?: return null
    vectorDrawable.setBounds(
        0,
        0,
        vectorDrawable.intrinsicWidth,
        vectorDrawable.intrinsicHeight
    )
    val bitmap = Bitmap.createBitmap(
        vectorDrawable.intrinsicWidth,
        vectorDrawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    vectorDrawable.draw(canvas)
    return try {
        BitmapDescriptorFactory.fromBitmap(bitmap)
    } catch (e: Exception) {
        null
    }
}