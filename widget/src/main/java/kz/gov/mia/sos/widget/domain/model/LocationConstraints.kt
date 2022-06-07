package kz.gov.mia.sos.widget.domain.model

data class LocationConstraints constructor(
    val reverseGeocode: ReverseGeocode
) {

    data class ReverseGeocode constructor(
        val latitude: Double,
        val longitude: Double,
        val displayName: String,
        val address: Address,
        val boundingBox: List<String>?
    ) {

        data class Address constructor(
            val residential: String?,
            val cityDistrict: String?,
            val city: String?,
            val county: String?,
            val state: String?,
            val country: String?,
            val countryCode: String?
        )

        fun getDisplayAddress(): String =
            if (address.country.isNullOrBlank()) {
                displayName
            } else {
                when {
                    !address.city.isNullOrBlank() ->
                        "${address.city}, ${address.country}"
                    !address.county.isNullOrBlank() ->
                        "${address.county}, ${address.country}"
                    !address.state.isNullOrBlank() ->
                        "${address.state}, ${address.country}"
                    !address.residential.isNullOrBlank() ->
                        "${address.residential}, ${address.country}"
                    !address.cityDistrict.isNullOrBlank() ->
                        "${address.cityDistrict}, ${address.country}"
                    else ->
                        address.country
                }
            }

    }

}