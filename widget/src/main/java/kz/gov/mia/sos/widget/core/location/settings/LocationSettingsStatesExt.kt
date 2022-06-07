package kz.gov.mia.sos.widget.core.location.settings

import com.google.android.gms.location.LocationSettingsStates

internal fun LocationSettingsStates.asString(): String =
    "LocationSettingsStates(" +
            "isGpsUsable=$isGpsUsable," +
            "isGpsPresent=$isGpsPresent," +
            "isNetworkLocationUsable=$isNetworkLocationUsable," +
            "isNetworkLocationPresent=$isNetworkLocationPresent," +
            "isLocationUsable=$isLocationUsable," +
            "isLocationPresent=$isLocationPresent," +
            "isBleUsable=$isBleUsable," +
            "isBlePresent=$isBlePresent)"