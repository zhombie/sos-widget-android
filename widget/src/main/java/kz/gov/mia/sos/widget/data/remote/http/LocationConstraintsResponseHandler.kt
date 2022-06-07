package kz.gov.mia.sos.widget.data.remote.http

import com.loopj.android.http.JsonHttpResponseHandler
import cz.msebera.android.httpclient.Header
import kz.garage.json.getDoubleOrNull
import kz.garage.json.getJSONObjectOrNull
import kz.garage.json.getStringOrNull
import kz.gov.mia.sos.widget.domain.model.LocationConstraints
import org.json.JSONObject

internal class LocationConstraintsResponseHandler constructor(
    private val onSuccess: (locationConstraints: LocationConstraints) -> Unit,
    private val onFailure: (throwable: Throwable?) -> Unit
) : JsonHttpResponseHandler() {

    override fun onSuccess(statusCode: Int, headers: Array<out Header>?, response: JSONObject?) {
        if (response == null) {
            onFailure.invoke(NullPointerException("Response is null!"))
        } else {
            val location = response.getJSONObjectOrNull("result")?.getJSONObjectOrNull("location")

            if (location == null) {
                onFailure.invoke(NullPointerException("Location is null!"))
            } else {
                val address = location.getJSONObject("address")

                onSuccess.invoke(
                    LocationConstraints(
                        reverseGeocode = LocationConstraints.ReverseGeocode(
                            latitude = location.getDoubleOrNull("lat") ?: -1.0,
                            longitude = location.getDoubleOrNull("lon") ?: -1.0,
                            displayName = location.getString("display_name"),
                            address = LocationConstraints.ReverseGeocode.Address(
                                residential = address.getStringOrNull("residential"),
                                cityDistrict = address.getStringOrNull("city_district"),
                                city = address.getStringOrNull("city"),
                                county = address.getStringOrNull("county"),
                                state = address.getStringOrNull("state"),
                                country = address.getStringOrNull("country"),
                                countryCode = address.getStringOrNull("country_code"),
                            ),
                            boundingBox = null  // Not used
                        )
                    )
                )
            }
        }
    }

    override fun onFailure(
        statusCode: Int,
        headers: Array<out Header>?,
        throwable: Throwable?,
        errorResponse: JSONObject?
    ) {
        onFailure.invoke(throwable)
    }

    override fun onFailure(
        statusCode: Int,
        headers: Array<out Header>?,
        responseString: String?,
        throwable: Throwable?
    ) {
        onFailure.invoke(throwable)
    }

}