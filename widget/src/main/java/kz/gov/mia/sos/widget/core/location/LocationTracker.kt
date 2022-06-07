package kz.gov.mia.sos.widget.core.location

import android.Manifest
import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import kz.garage.context.system.service.locationManager
import kz.garage.kotlin.simpleNameOf
import kz.garage.location.compass.Compass
import kz.garage.location.core.createLocationListener
import kz.garage.location.core.isMocked
import kz.garage.location.core.provider.LocationProvider
import kz.garage.location.core.provider.isProviderEnabled
import kz.garage.location.gms.createLocationCallback
import kz.gov.mia.sos.widget.core.logging.Logger
import kz.gov.mia.sos.widget.utils.secondsAsMillis
import java.lang.ref.WeakReference
import java.util.*

internal fun locationTracker(
    activity: FragmentActivity,
    init: LocationTracker.Builder.() -> Unit
): LocationTracker {
    val builder = LocationTracker.Builder(activity)
    builder.init()
    return builder.build()
}


/**
 * Build a LocationTracker
 * @param fastestInterval The minimum time interval between location updates, in milliseconds
 * @param distanceInterval The minimum distance between location updates in meters
 * @param providers Specifies if tracker should use the provider
 */
class LocationTracker private constructor(
    private val activityReference: WeakReference<FragmentActivity>,

    val fastestInterval: Long,
    val interval: Long,
    val distanceInterval: Float,
    val maxWaitTime: Long,
    val isLastKnownLocationIgnored: Boolean,

    val providers: List<LocationProvider>,

    val locale: Locale
) : DefaultLifecycleObserver {

    companion object {
        private val TAG = simpleNameOf<LocationTracker>()
    }

    enum class Error {
        LOCATION_MANAGER_ABSENT,
        NO_PERMISSION,
        GOOGLE_PLAY_SERVICES_SETTINGS_DENIED,
        SETTINGS_CHANGE_UNAVAILABLE,
        FAKE_LOCATION
    }

    class Builder internal constructor(private val activity: FragmentActivity) {
        var fastestInterval: Long = 10L.secondsAsMillis
        var interval: Long = 20L.secondsAsMillis
        var distanceInterval: Float = 10F
        var maxWaitTime: Long = 20L.secondsAsMillis
        var isLastKnownLocationIgnored: Boolean = false

        var providers: List<LocationProvider> = emptyList()

        var locale: Locale = Locale.getDefault()

        fun setupDefault() {
            fastestInterval = 3L.secondsAsMillis
            interval = 3L.secondsAsMillis
            distanceInterval = 0F
            maxWaitTime = 20L.secondsAsMillis

            providers = listOf(LocationProvider.FUSED, LocationProvider.GPS)
        }

        fun build(): LocationTracker {
            return LocationTracker(
                activityReference = WeakReference(activity),
                fastestInterval = fastestInterval,
                interval = interval,
                distanceInterval = distanceInterval,
                maxWaitTime = maxWaitTime,
                isLastKnownLocationIgnored = isLastKnownLocationIgnored,
                providers = providers,
                locale = locale
            )
        }
    }

    private val activity: FragmentActivity?
        get() = activityReference.get()

    private val context: Context?
        get() = activity

    // Android LocationManager
    private var locationManager: LocationManager? = null

    private var fusedLocationProviderClient: FusedLocationProviderClient? = null

    private val locationRequest: LocationRequest by lazy {
        LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setFastestInterval(fastestInterval)
            .setInterval(interval)
//            .setSmallestDisplacement(distanceInterval)
            .setMaxWaitTime(maxWaitTime)
    }

    // Last known location
    private var currentLocation: Location? = null

    // Listener for the LocationManager
    private val locationListener = context?.createLocationListener(
        onLocationChanged = { location ->
            onLocationFound(Location(location))
        },
        onStatusChanged = { provider, status, extras ->
            behaviorListener.forEach {
                it.onStatusChanged(LocationProvider.from(provider), status, extras)
            }
        },
        onProviderEnabled = { provider ->
            behaviorListener.forEach {
                it.onProviderEnabled(LocationProvider.from(provider))
            }
        },
        onProviderDisabled = { provider ->
            behaviorListener.forEach {
                it.onProviderDisabled(LocationProvider.from(provider))
            }
        }
    )

    private val locationCallback = context?.createLocationCallback(
        onLocationResult = { locationResult ->
            Logger.debug(TAG, "onLocationResult() -> $locationResult")

            val location = locationResult.lastLocation
            onLocationFound(Location(location))
        },
        onLocationAvailability = { locationAvailability ->
            Logger.debug(TAG, "onLocationAvailability() -> $locationAvailability")
            if (locationAvailability.isLocationAvailable) {
                behaviorListener.forEach { it.onProviderEnabled(LocationProvider.FUSED) }
            } else {
                behaviorListener.forEach { it.onProviderDisabled(LocationProvider.FUSED) }
            }
        }
    )

    private fun onLocationFound(location: Location) {
        Logger.debug(TAG, "onLocationFound() -> location: $location")

        if (location.isMocked()) {
            listeners.forEach { it.onError(Error.FAKE_LOCATION) }
        } else {
            currentLocation = location
            listeners.forEach { it.onLocationFound(location) }
        }
    }

    // List used to register the listeners to notify
    private val listeners: MutableSet<Listener> = mutableSetOf()

    // List used to register the behavior listeners to notify
    private val behaviorListener: MutableSet<BehaviorListener> = mutableSetOf()

    /**
     * Indicates if [LocationTracker] is listening for updates or not
     */
    var isListening = false
        private set

    private val criteria: Criteria by lazy {
        Criteria().apply {
            accuracy = Criteria.ACCURACY_FINE
            powerRequirement = Criteria.POWER_MEDIUM
            isAltitudeRequired = false
            isSpeedRequired = true
            isCostAllowed = true
            isBearingRequired = true
            horizontalAccuracy = Criteria.ACCURACY_HIGH
            verticalAccuracy = Criteria.ACCURACY_MEDIUM
            bearingAccuracy = Criteria.ACCURACY_MEDIUM
            speedAccuracy = Criteria.ACCURACY_MEDIUM
        }
    }

    var bestProvider: String? = null
        get() = locationManager?.getBestProvider(criteria, true)
        private set

    private var compass: Compass? = null

    /**
     * Add a listener to the stack so it will be notified once a new location is found
     * @param listener the listener to add to the list.
     * @return true if the listener has been added, false otherwise
     */
    fun addListener(listener: Listener): Boolean = listeners.add(listener)

    /**
     * Remove a listener from the stack
     * @param listener the listener to remove from the list.
     * @return true if the listener has been removed, false otherwise
     */
    fun removeListener(listener: Listener): Boolean = listeners.remove(listener)

    /**
     * Add a behavior listener to the stack so it will be notified when a provider is updated
     * @param listener the listener to add to the list.
     * @return true if the listener has been added, false otherwise
     */
    fun addBehaviorListener(listener: BehaviorListener): Boolean = behaviorListener.add(listener)

    /**
     * Remove a behavior listener from the stack
     * @param listener the listener to remove from the list.
     * @return true if the listener has been removed, false otherwise
     */
    fun removeBehaviorListener(listener: BehaviorListener): Boolean =
        behaviorListener.remove(listener)

    private val compassAngleObserver = Observer<Float> { angle ->
        listeners.forEach { it.onDeviceRotated(angle) }
    }

    /**
     * Make the tracker listening for location updates
     */
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    fun startListening() {
        Logger.debug(TAG, "startListening()")

        val activity = activity ?: throw IllegalStateException("Activity is not attached!")
        val context = context ?: throw IllegalStateException("Context is not attached!")

        stopListening(false)

        if (providers.isEmpty()) {
            throw IllegalStateException("All location providers are disabled, please, enable at least one of them")
        }

        if (compass == null) {
            compass = Compass.newInstance(activity)
        }
        if (compass?.getAngle()?.hasObservers() == true) {
            compass?.getAngle()?.removeObserver(compassAngleObserver)
        }
        compass?.getAngle()?.observe(activity, compassAngleObserver)

        compass?.stop()
        compass?.start()

        LocationInteractor.checkLocationSettings(
            context = context,
            locationRequest = locationRequest,
            onSuccess = { locationSettingsStates ->
                Logger.debug(TAG, "requestSingleLocationUpdate() -> states: $locationSettingsStates")

                init()

                if (!isListening) {
                    if (LocationProvider.FUSED in providers) {
                        registerForLocationUpdates(LocationProvider.FUSED)
                    }
                    if (LocationProvider.GPS in providers) {
                        registerForLocationUpdates(LocationProvider.GPS)
                    }
                    if (LocationProvider.NETWORK in providers) {
                        registerForLocationUpdates(LocationProvider.NETWORK)
                    }
                    if (LocationProvider.PASSIVE in providers) {
                        registerForLocationUpdates(LocationProvider.PASSIVE)
                    }
                    isListening = true
                }
            },
            onResolutionRequired = { exception ->
                Logger.debug(TAG, "requestSingleLocationUpdate() -> onResolutionRequired()")

                listeners.forEach { it.onResolutionRequired(exception) }
            },
            onSettingsChangeUnavailable = {
                Logger.debug(TAG, "requestSingleLocationUpdate() -> onSettingsChangeUnavailable()")

                listeners.forEach { it.onError(Error.SETTINGS_CHANGE_UNAVAILABLE) }
            },
            onFailure = {
                Logger.debug(TAG, "requestSingleLocationUpdate() -> onFailure()")

                listeners.forEach { it.onError(Error.GOOGLE_PLAY_SERVICES_SETTINGS_DENIED) }
            }
        )
    }

    /**
     * Make the tracker stops listening for location updates
     * @param clearListeners optional (default false) drop all the listeners if set to true
     */
    fun stopListening(clearListeners: Boolean = false) {
        Logger.debug(
            TAG, "stopListening() -> " +
                "isListening: $isListening, clearListeners: $clearListeners")
        compass?.stop()
        if (locationListener != null) {
            locationManager?.removeUpdates(locationListener)
        }
        if (locationCallback != null) {
            fusedLocationProviderClient?.removeLocationUpdates(locationCallback)
        }
        isListening = false
        if (clearListeners) {
            listeners.clear()
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    private fun init() {
        Logger.debug(TAG, "init()")

        val activity = activity ?: throw IllegalStateException("Activity is not attached!")

        if (LocationInteractor.hasLocationRequiredPermissions(activity)) {
            // Init the manager
            locationManager = activity.applicationContext.locationManager

            if (locationManager == null) {
                listeners.forEach { it.onError(Error.LOCATION_MANAGER_ABSENT) }
                return
            }

            if (LocationProvider.FUSED in providers) {
                fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)
            }

            if (!isLastKnownLocationIgnored) {
                if (currentLocation == null) {
                    // Update the lastKnownLocation
                    if (LocationProvider.FUSED in providers) {
                        fusedLocationProviderClient?.lastLocation
                            ?.addOnSuccessListener { currentLocation = it }
                    }
                    if (LocationProvider.GPS in providers) {
                        currentLocation =
                            locationManager?.getLastKnownLocation(LocationProvider.GPS.value)
                    }
                    if (LocationProvider.NETWORK in providers) {
                        currentLocation =
                            locationManager?.getLastKnownLocation(LocationProvider.NETWORK.value)
                    }
                    if (LocationProvider.PASSIVE in providers) {
                        currentLocation =
                            locationManager?.getLastKnownLocation(LocationProvider.PASSIVE.value)
                    }
                }
            }
        } else {
            Logger.error(TAG, "Application has no location group permissions")

            listeners.forEach { it.onError(Error.NO_PERMISSION) }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    private fun registerForLocationUpdates(provider: LocationProvider) {
        Logger.debug(TAG, "registerForLocationUpdates() -> provider: $provider")

        if (LocationInteractor.hasLocationRequiredPermissions(context)) {
            if (provider == LocationProvider.FUSED) {
                if (locationCallback != null) {
                    fusedLocationProviderClient?.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        Looper.getMainLooper()
                    )?.addOnSuccessListener {
                        Logger.debug(TAG, "$LocationInteractor.FUSED_PROVIDER is registered")
                    }
                }
            }
        }

        if (locationManager?.isProviderEnabled(provider) == true) {
            if (LocationInteractor.hasLocationRequiredPermissions(context)) {
                if (locationListener != null) {
                    locationManager?.requestLocationUpdates(
                        provider.get(),
                        fastestInterval,
                        distanceInterval,
//                        criteria,
                        locationListener,
                        Looper.getMainLooper()
                    )
                    Logger.debug(TAG, "$provider is registered")
                }
            } else {
                Logger.error(TAG, "Application has no location group permissions")

                listeners.forEach { it.onError(Error.NO_PERMISSION) }
            }
        } else {
            listeners.forEach {
                if (provider == LocationProvider.FUSED) return@forEach
                it.onProviderError(provider)
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)

        stopListening(true)
        locationManager = null
        fusedLocationProviderClient = null
        compass = null
    }

    interface Listener {
        fun onDeviceRotated(angle: Float)

        /**
         * Called when the tracker had found a location
         *
         * @param location the found location
         */
        fun onLocationFound(location: Location)

        fun onResolutionRequired(exception: ResolvableApiException)

        fun onProviderError(provider: LocationProvider)

        fun onError(error: Error)
    }

    interface BehaviorListener {
        /**
         * See [android.location.LocationListener.onProviderDisabled]
         */
        fun onProviderDisabled(provider: LocationProvider?)

        /**
         * See [android.location.LocationListener.onProviderEnabled]
         */
        fun onProviderEnabled(provider: LocationProvider?)

        /**
         * See [android.location.LocationListener.onStatusChanged]
         */
        fun onStatusChanged(provider: LocationProvider?, status: Int, extras: Bundle?)
    }

}