package kz.gov.mia.sos.widget.ui.presentation.call.map

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import kz.garage.kotlin.simpleNameOf
import kz.gov.mia.sos.widget.R
import kz.gov.mia.sos.widget.core.location.asLatLng
import kz.gov.mia.sos.widget.core.logging.Logger
import kz.gov.mia.sos.widget.ui.platform.BaseFragment
import kz.gov.mia.sos.widget.ui.presentation.call.vm.CallViewModel
import kz.gov.mia.sos.widget.utils.bitmapDescriptorFromVector
import kz.gov.mia.sos.widget.utils.location.animateTo
import kz.inqbox.sdk.socket.model.Card102Status
import timerx.Stopwatch
import timerx.StopwatchBuilder
import java.util.concurrent.TimeUnit

class ARMMapFragment : BaseFragment(R.layout.sos_widget_fragment_arm_map) {

    companion object {
        private val TAG = simpleNameOf<ARMMapFragment>()

        private const val ZOOM_THRESHOLD = 14.0F

        fun newInstance() = ARMMapFragment()
    }

    private var cardView: MaterialCardView? = null
    private var contentView: ConstraintLayout? = null
    private var pastTimeValueView: MaterialTextView? = null
    private var infoTextView: MaterialTextView? = null
    private var recyclerView: RecyclerView? = null
    private var expandButton: MaterialButton? = null
    private var mapTypeButton: MaterialButton? = null
    private var selfLocationButton: MaterialButton? = null
    private var overlayView: RelativeLayout? = null

    private val viewModel: CallViewModel by activityViewModels()

    private var googleMap: GoogleMap? = null

    private var selfMarker: Marker? = null
    private var selfCircle: Circle? = null

    private var otherMarkers: MutableList<Pair<Long, Marker>> = mutableListOf()

    private var markersAdapter: MarkersAdapter? = null

    private var stopwatch: Stopwatch? = null

    private var isCardExpanded: Boolean = false
        set(value) {
            field = value

            if (value) {
                recyclerView?.visibility = View.VISIBLE
                expandButton?.setIconResource(R.drawable.sos_widget_ic_caret_up_white)
            } else {
                recyclerView?.visibility = View.GONE
                expandButton?.setIconResource(R.drawable.sos_widget_ic_caret_down_white)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cardView = view.findViewById(R.id.cardView)
        contentView = view.findViewById(R.id.contentView)
        pastTimeValueView = view.findViewById(R.id.pastTimeValueView)
        infoTextView = view.findViewById(R.id.infoTextView)
        recyclerView = view.findViewById(R.id.recyclerView)
        expandButton = view.findViewById(R.id.expandButton)
        mapTypeButton = view.findViewById(R.id.mapTypeButton)
        selfLocationButton = view.findViewById(R.id.selfLocationButton)
        overlayView = view.findViewById(R.id.overlayView)

        setupMapFragment()
        setupInfoView()
        setupCardToggleButton()
        setupMapTypeControlButton()
        setupLocationControlButton()
        setupOverlayView()

        observeLocationUpdatesState()
        observeSelfLocation()
        observeDeviceRotationAngle()
        observeCard102Update()
        observeOtherLocations()
        observeSelectedSelfLocation()
        observeSelectedOtherLocation()
    }

    override fun onPause() {
        super.onPause()

        with(childFragmentManager.findFragmentByTag("map_settings")) {
            if (this is BottomSheetDialogFragment) {
                dismiss()
            }
        }
    }

    override fun onDestroy() {
        childFragmentManager.clearFragmentResultListener("selected_map_type")

        val durationMillis = stopwatch?.getTimeIn(TimeUnit.MILLISECONDS)
        viewModel.onARMMapDestroy(durationMillis)
        releaseStopwatch()

        googleMap?.stopAnimation()
        googleMap?.clear()
        googleMap = null

        super.onDestroy()
    }

    private fun setupMapFragment() {
        (childFragmentManager.findFragmentById(R.id.mapView) as? SupportMapFragment)?.getMapAsync { googleMap ->
            with(googleMap.uiSettings) {
                isCompassEnabled = true
                isMapToolbarEnabled = false
                isMyLocationButtonEnabled = false
                isRotateGesturesEnabled = true
                isScrollGesturesEnabled = true
                isZoomControlsEnabled = false
                isZoomGesturesEnabled = true
            }

            this.googleMap = googleMap

            viewModel.onMapReady()
        }
    }

    private fun setupInfoView(pastTime: Long? = null) {
        cardView?.visibility = View.GONE

        isCardExpanded = false

        if (pastTime == null) {
            pastTimeValueView?.setText(R.string.sos_widget_undefined)
        } else {
            stopwatch?.release()
            stopwatch = null

            stopwatch = StopwatchBuilder()
                .startFormat("HH:MM:SS")
                .onTick { time -> pastTimeValueView?.text = time }
                .build()

            stopwatch?.start()

            pastTimeValueView?.text = stopwatch?.formattedStartTime
        }

        infoTextView?.text = getString(R.string.sos_widget_searching, "...")
    }

    private fun setupCardToggleButton() {
        expandButton?.visibility = View.GONE

        expandButton?.setOnClickListener { isCardExpanded = !isCardExpanded }
    }

    private fun setupMapTypeControlButton() {
        childFragmentManager.setFragmentResultListener("selected_map_type", this) { _, bundle ->
            when (bundle.getString("type")) {
                "map" ->
                    googleMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
                "satellite" ->
                    googleMap?.mapType = GoogleMap.MAP_TYPE_SATELLITE
                "hybrid" ->
                    googleMap?.mapType = GoogleMap.MAP_TYPE_HYBRID
            }
        }

        mapTypeButton?.setOnClickListener {
            bottomSheetDialogFragment?.dismiss()
            bottomSheetDialogFragment = null
            bottomSheetDialogFragment = MapSettingsBottomSheetDialogFragment.newInstance(
                when (googleMap?.mapType) {
                    GoogleMap.MAP_TYPE_NORMAL -> "map"
                    GoogleMap.MAP_TYPE_SATELLITE -> "satellite"
                    GoogleMap.MAP_TYPE_HYBRID -> "hybrid"
                    else -> null
                }
            )
            bottomSheetDialogFragment?.show(childFragmentManager, "map_settings")
        }
    }

    private fun setupLocationControlButton() {
        selfLocationButton?.setOnClickListener { viewModel.onSelfLocationClicked() }
    }

    private fun setupOverlayView() {
        overlayView?.visibility = View.INVISIBLE
    }

    private fun observeLocationUpdatesState() {
//        viewModel.getLocationUpdatesState().observe(viewLifecycleOwner, { isShouldBeSent ->
//            if (isShouldBeSent) {
//                overlayView.visibility = View.INVISIBLE
//            } else {
//                overlayView.visibility = View.VISIBLE
//            }
//        })
    }

    private fun observeSelfLocation() {
        viewModel.getSelfLocation().observe(viewLifecycleOwner, { location ->
            val latLng = location.asLatLng()

            if (location.accuracy < 0) {
                location.accuracy = 0F
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (location.verticalAccuracyMeters < 0) {
                    location.verticalAccuracyMeters = 0F
                }
            }

            if (selfMarker == null) {
                val circleOptions = CircleOptions()
                    .center(latLng)
                    .clickable(false)
                    .fillColor(Color.parseColor("#10FB5252"))
                    .radius(location.accuracy.toDouble())
                    .strokeColor(R.color.sos_widget_transparent)
                    .strokeWidth(0F)
                    .visible(true)
                    .zIndex(0.5F)

                val markerOptions = MarkerOptions()
                markerOptions.alpha(1.0F)
                markerOptions.anchor(0.5F, 0.5F)
                markerOptions.draggable(false)
                markerOptions.flat(true)
                markerOptions.icon(context?.bitmapDescriptorFromVector(R.drawable.sos_widget_ic_red_marker_with_direction))
                markerOptions.position(latLng)
                markerOptions.title(null)
                markerOptions.visible(true)
                markerOptions.zIndex(1.0F)

                selfMarker = googleMap?.addMarker(markerOptions)
                selfCircle = googleMap?.addCircle(circleOptions)

                var zoomLevel = googleMap?.cameraPosition?.zoom ?: ZOOM_THRESHOLD
                if (zoomLevel < ZOOM_THRESHOLD) {
                    zoomLevel = ZOOM_THRESHOLD
                }
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel))
            } else {
                selfMarker?.animateTo(latLng) {
                    selfCircle?.center = latLng
                    selfCircle?.radius = location.accuracy.toDouble()
                }
            }
        })
    }

    private fun observeDeviceRotationAngle() {
        viewModel.getDeviceRotationAngle().observe(viewLifecycleOwner, { angle ->
            selfMarker?.rotation = (angle - 180) % 360
        })
    }

    private fun observeCard102Update() {
        viewModel.getCard102Status().observe(viewLifecycleOwner, { status ->
            Logger.debug(TAG, "getCard102Status() -> status: $status")

            when (status) {
                Card102Status.NEW_CARD102 -> {
                    cardView?.visibility = View.VISIBLE

                    isCardExpanded = false

                    stopwatch?.release()
                    stopwatch = null

                    stopwatch = StopwatchBuilder()
                        .startFormat("HH:MM:SS")
                        .onTick { time -> pastTimeValueView?.text = time }
                        .build()

                    pastTimeValueView?.setText(R.string.sos_widget_undefined)
                }
                Card102Status.ASSIGNED_FORCE -> {
                    stopwatch?.start()

                    isCardExpanded = false

                    infoTextView?.setText(R.string.sos_widget_arm_force_assigned)
                }
                Card102Status.FORCE_ON_SPOT -> {
                    stopwatch?.stop()

                    infoTextView?.setText(R.string.sos_widget_arm_assigned_force_on_spot)
                }
                else -> {
                    stopwatch?.release()
                    stopwatch = null

                    pastTimeValueView?.setText(R.string.sos_widget_undefined)

                    infoTextView?.setText(R.string.sos_widget_arm_operation_completed)
                }
            }
        })
    }

    private fun observeOtherLocations() {
        viewModel.getOtherLocations().observe(viewLifecycleOwner, { locationUpdates ->
            locationUpdates.forEach { locationUpdate ->
                val latLng = LatLng(locationUpdate.y, locationUpdate.x)

                val markerOptions = MarkerOptions()
                markerOptions.alpha(1.0F)
                markerOptions.anchor(0.5F, 0.5F)
                markerOptions.draggable(false)
                markerOptions.flat(true)
                markerOptions.icon(context?.bitmapDescriptorFromVector(R.drawable.sos_widget_ic_police_marker))
                markerOptions.position(latLng)
                markerOptions.title(locationUpdate.gpsCode.toString())
                markerOptions.visible(true)
                markerOptions.zIndex(1.0F)

                googleMap?.let { googleMap ->
                    val otherMarkerIndex =
                        otherMarkers.indexOfFirst { it.first == locationUpdate.gpsCode }
                    if (otherMarkerIndex > -1) {
                        otherMarkers[otherMarkerIndex].second.animateTo(latLng) {}
                    } else {
                        if (markersAdapter == null) {
                            markersAdapter = MarkersAdapter { marker ->
                                isCardExpanded = false
                                viewModel.onGpsCodeClicked(marker)
                            }
//                            recyclerView.setHasFixedSize(true)
                            recyclerView?.itemAnimator = null
                            recyclerView?.setItemViewCacheSize(3)
                            recyclerView?.layoutManager = LinearLayoutManager(
                                requireContext(),
                                LinearLayoutManager.VERTICAL,
                                false
                            )
                            recyclerView?.adapter = markersAdapter
                        }

                        if (markersAdapter?.markers.isNullOrEmpty()) {
                            markersAdapter?.markers = locationUpdates.map { it.gpsCode }
                            expandButton?.visibility = View.VISIBLE
                        }

                        val marker = googleMap.addMarker(markerOptions)
                        if (marker != null) {
                            marker.tag = locationUpdate.gpsCode
                            otherMarkers.add(locationUpdate.gpsCode to marker)
                        }
                    }
                }
            }
        })
    }

    private fun observeSelectedSelfLocation() {
        viewModel.getSelectedSelfLocation().observe(viewLifecycleOwner, { latLng ->
            Logger.debug(TAG, "getSelectedSelfLocation() -> latLng: $latLng")

            if (latLng == null) return@observe

            var zoomLevel = googleMap?.cameraPosition?.zoom ?: ZOOM_THRESHOLD
            Logger.debug(TAG, "getSelectedSelfLocation() -> zoomLevel: $zoomLevel")
            if (zoomLevel < ZOOM_THRESHOLD) {
                zoomLevel = ZOOM_THRESHOLD
            }
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel))
        })
    }

    private fun observeSelectedOtherLocation() {
        viewModel.getSelectedOtherLocation().observe(viewLifecycleOwner, { locationUpdate ->
            Logger.debug(TAG, "getSelectedOtherLocation() -> locationUpdate: $locationUpdate")

            if (locationUpdate == null) return@observe

//            val latLng = LatLng(locationUpdate.y, locationUpdate.x)

            otherMarkers.find { it.second.tag == locationUpdate.gpsCode }?.second?.showInfoWindow()

//            var zoomLevel = googleMap?.cameraPosition?.zoom ?: ZOOM_THRESHOLD
//            if (zoomLevel < ZOOM_THRESHOLD) {
//                zoomLevel = ZOOM_THRESHOLD
//            }
//            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel))
        })
    }

    private fun releaseStopwatch() {
        try {
            stopwatch?.release()
        } catch (outer: Exception) {
            try {
                stopwatch?.stop()
                stopwatch?.reset()
            } catch (inner: Exception) {
                inner.printStackTrace()
            }
        }
        stopwatch = null
    }

}