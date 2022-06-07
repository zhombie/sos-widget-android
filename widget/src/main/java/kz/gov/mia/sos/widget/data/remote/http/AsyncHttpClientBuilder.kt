package kz.gov.mia.sos.widget.data.remote.http

import com.loopj.android.http.AsyncHttpClient
import java.util.concurrent.TimeUnit

class AsyncHttpClientBuilder {

    private var asyncHttpClient: AsyncHttpClient? = null

    private var isLoggingEnabled: Boolean = false
    private var connectionTimeout: Int = TimeUnit.SECONDS.toMillis(15).toInt()
    private var responseTimeout: Int = TimeUnit.SECONDS.toMillis(15).toInt()

    fun setLoggingEnabled(isEnabled: Boolean): AsyncHttpClientBuilder {
        isLoggingEnabled = isEnabled
        return this
    }

    fun setConnectionTimeout(timeout: Int) {
        connectionTimeout = timeout
    }

    fun setResponseTimeout(timeout: Int) {
        responseTimeout = timeout
    }

    fun build(): AsyncHttpClient {
        if (asyncHttpClient == null) {
            asyncHttpClient = AsyncHttpClient()
            asyncHttpClient?.isLoggingEnabled = isLoggingEnabled
            asyncHttpClient?.connectTimeout = connectionTimeout
            asyncHttpClient?.responseTimeout = responseTimeout
        }
        return requireNotNull(asyncHttpClient)
    }

}