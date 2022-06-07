package kz.gov.mia.sos.widget.core.location

import android.Manifest
import android.content.Context
import android.location.Location
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import kz.garage.context.system.service.locationManager
import kz.garage.kotlin.simpleNameOf
import kz.garage.location.core.provider.LocationProvider
import kz.gov.mia.sos.widget.core.permission.isPermissionGranted

class LocationInteractor {

    companion object {
        private val TAG = simpleNameOf<LocationInteractor>()

        fun hasLocationRequiredPermissions(context: Context?): Boolean =
            hasFineLocationPermission(context) && hasCoarseLocationPermission(context)

        private fun hasFineLocationPermission(context: Context?): Boolean =
            context?.isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION) == true

        private fun hasCoarseLocationPermission(context: Context?): Boolean =
            context?.isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION) == true

        fun checkLocationSettings(
            context: Context,
            locationRequest: LocationRequest,
            onSuccess: (locationSettingsStates: LocationSettingsStates?) -> Unit = {},
            onResolutionRequired: (exception: ResolvableApiException) -> Unit = {},
            onSettingsChangeUnavailable: () -> Unit = {},
            onFailure: (exception: Exception) -> Unit = {}
        ) {
            val executor = ContextCompat.getMainExecutor(context)

            LocationServices.getSettingsClient(context.applicationContext)
                .checkLocationSettings(
                    LocationSettingsRequest.Builder()
                        .addLocationRequest(locationRequest)
                        // Reference: http://stackoverflow.com/questions/29824408/google-play-services-locationservices-api-new-option-never
                        .setAlwaysShow(true)
                        .build()
                )
                .addOnSuccessListener(executor) {
                    onSuccess(it.locationSettingsStates)
                }
                .addOnFailureListener(executor) {
                    if (it is ApiException) {
                        when (it.statusCode) {
                            LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                                if (it is ResolvableApiException) {
                                    onResolutionRequired(it)
                                } else {
                                    onFailure(it)
                                }
                            }
                            LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE ->
                                onSettingsChangeUnavailable()
                            else -> {
                                onFailure(it)
                            }
                        }
                    } else {
                        onFailure(it)
                    }
                }
        }

        @RequiresPermission(
            allOf = [
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ]
        )
        fun getFusedSingleLocation(
            context: Context,
            onResult: (location: Location?) -> Unit = {},
            onFailure: (exception: Exception) -> Unit = {}
        ): CancellationTokenSource {
            val executor = ContextCompat.getMainExecutor(context)
            val cancellationTokenSource = CancellationTokenSource()
            with(LocationServices.getFusedLocationProviderClient(context.applicationContext)) {
                getCurrentLocation(
                    LocationRequest.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                )
                    .addOnSuccessListener(executor) { location ->
                        onResult(location)
                    }
                    .addOnFailureListener(executor) {
                        lastLocation
                            .addOnSuccessListener(executor) { location ->
                                onResult(location)
                            }
                            .addOnFailureListener(executor) {
                                onFailure(it)
                            }
                    }
            }
            return cancellationTokenSource
        }

        @RequiresPermission(
            allOf = [
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ]
        )
        fun getSingleLocation(
            context: Context,
            onResult: (location: Location?) -> Unit = {},
            onFailure: (exception: Exception) -> Unit = {}
        ): CancellationSignal? {
            val locationManager = context.locationManager

            if (locationManager == null) {
                onFailure(NullPointerException("LocationManager is null!"))
                return null
            }

            var cancellationSignal: CancellationSignal? = null

            val executor = ContextCompat.getMainExecutor(context)

            listOf(
                LocationProvider.GPS,
                LocationProvider.NETWORK,
                LocationProvider.PASSIVE
            ).forEach { provider ->
                if (locationManager.isProviderEnabled(provider.value)) {
                    if (cancellationSignal == null) {
                        cancellationSignal = CancellationSignal()
                    }
                    LocationManagerCompat.getCurrentLocation(
                        locationManager,
                        provider.value,
                        cancellationSignal,
                        executor
                    ) { location ->
                        onResult(location)
                    }
                    return cancellationSignal
                }
            }

            onFailure(IllegalStateException("Location providers are disabled!"))

            return null
        }
    }

}