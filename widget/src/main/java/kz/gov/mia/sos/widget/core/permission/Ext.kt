package kz.gov.mia.sos.widget.core.permission

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

// [BEGIN] isPermissionGranted()

internal fun Context.isPermissionGranted(permission: String): Boolean =
    ContextCompat
        .checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

internal fun Context.isPermissionsGranted(permissions: Array<String>): Boolean =
    permissions.all { isPermissionGranted(it) }

internal fun Context.isPermissionsGranted(permissions: Collection<String>): Boolean =
    permissions.all { isPermissionGranted(it) }

@JvmName("isPermissionsGrantedArgs")
internal fun Context.isPermissionsGranted(vararg permissions: String): Boolean =
    permissions.all { isPermissionGranted(it) }

// [END] isPermissionGranted()
