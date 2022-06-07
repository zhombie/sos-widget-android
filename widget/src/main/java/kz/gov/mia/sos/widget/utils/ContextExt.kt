@file:Suppress("NOTHING_TO_INLINE")

package kz.gov.mia.sos.widget.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

internal inline fun Context.createApplicationSettingsIntent(): Intent = Intent().apply {
    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    data = Uri.fromParts("package", packageName, null)
}

internal inline fun createLocationSettingsIntent(): Intent =
    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
