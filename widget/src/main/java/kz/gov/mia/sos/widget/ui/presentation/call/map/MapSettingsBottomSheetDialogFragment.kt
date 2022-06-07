package kz.gov.mia.sos.widget.ui.presentation.call.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import kz.garage.kotlin.simpleNameOf
import kz.gov.mia.sos.widget.R
import kz.gov.mia.sos.widget.core.logging.Logger
import kz.gov.mia.sos.widget.ui.platform.BaseBottomSheetDialogFragment

internal class MapSettingsBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {

    companion object {
        private val TAG = simpleNameOf<MapSettingsBottomSheetDialogFragment>()

        fun newInstance(mapType: String?): MapSettingsBottomSheetDialogFragment {
            val fragment = MapSettingsBottomSheetDialogFragment()
            fragment.arguments = bundleOf("map_type" to mapType)
            return fragment
        }
    }

    private var toggleGroup: MaterialButtonToggleGroup? = null
    private var mapButton: MaterialButton? = null
    private var satelliteButton: MaterialButton? = null
    private var hybridButton: MaterialButton? = null

    private var mapType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mapType = arguments?.getString("map_type")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.sos_widget_fragment_map_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toggleGroup = view.findViewById(R.id.toggleGroup)
        mapButton = view.findViewById(R.id.mapButton)
        satelliteButton = view.findViewById(R.id.satelliteButton)
        hybridButton = view.findViewById(R.id.hybridButton)

        setupMapType()
        setupToggleButton()
    }

    private fun setupMapType() {
        when (mapType) {
            "map" -> toggleGroup?.check(mapButton?.id ?: return)
            "satellite" -> toggleGroup?.check(satelliteButton?.id ?: return)
            "hybrid" -> toggleGroup?.check(hybridButton?.id ?: return)
        }
    }

    private fun setupToggleButton() {
        toggleGroup?.addOnButtonCheckedListener { _, checkedId, isChecked ->
            Logger.debug(TAG, "addOnButtonCheckedListener() -> $checkedId, $isChecked")
            if (isChecked) {
                when (checkedId) {
                    mapButton?.id -> {
                        parentFragmentManager.setFragmentResult(
                            "selected_map_type",
                            bundleOf("type" to "map")
                        )
                        dismiss()
                    }
                    satelliteButton?.id -> {
                        parentFragmentManager.setFragmentResult(
                            "selected_map_type",
                            bundleOf("type" to "satellite")
                        )
                        dismiss()
                    }
                    hybridButton?.id -> {
                        parentFragmentManager.setFragmentResult(
                            "selected_map_type",
                            bundleOf("type" to "hybrid")
                        )
                        dismiss()
                    }
                }
            }
        }
    }

}