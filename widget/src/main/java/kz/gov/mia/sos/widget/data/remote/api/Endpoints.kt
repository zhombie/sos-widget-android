package kz.gov.mia.sos.widget.data.remote.api

import kz.gov.mia.sos.widget.core.URLManager

internal enum class Endpoints constructor(val path: String) {
    CONFIGS("/configs"),
    ICE_SERVERS("/ice_servers"),
    LOCATION_CONSTRAINTS("/mobile/location_constraints");

    fun getEndpoint(): String? = URLManager.buildUrl(path)

    override fun toString(): String = path
}