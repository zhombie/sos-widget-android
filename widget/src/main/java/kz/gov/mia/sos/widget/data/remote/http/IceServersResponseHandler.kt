package kz.gov.mia.sos.widget.data.remote.http

import com.loopj.android.http.JsonHttpResponseHandler
import cz.msebera.android.httpclient.Header
import kz.garage.json.getStringOrNull
import kz.garage.json.mapNotNull
import kz.inqbox.sdk.domain.model.webrtc.IceServer
import org.json.JSONObject

internal class IceServersResponseHandler constructor(
    private val onSuccess: (iceServers: List<IceServer>) -> Unit,
    private val onFailure: (throwable: Throwable?) -> Unit
) : JsonHttpResponseHandler() {

    override fun onSuccess(statusCode: Int, headers: Array<out Header>?, response: JSONObject?) {
        if (response == null) {
            onFailure.invoke(NullPointerException())
        } else {
            onSuccess.invoke(
                response.optJSONArray("ice_servers")?.mapNotNull<JSONObject, IceServer> {
                    IceServer(
                        url = it.getStringOrNull("url") ?: "",
                        urls = it.getStringOrNull("urls") ?: "",
                        username = it.getStringOrNull("username"),
                        credential = it.getStringOrNull("credential")
                    )
                } ?: emptyList()
            )
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

}