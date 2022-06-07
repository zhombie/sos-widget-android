package kz.gov.mia.sos.widget.core

import android.net.Uri
import kz.gov.mia.sos.widget.api.exception.BaseUrlNullOrBlankException

internal object URLManager {

    private var BASE_URL: String? = null

    private const val SOCKET_URL_PATH = "/user"

    fun isBaseUrlNullOrBlank(): Boolean = BASE_URL.isNullOrBlank()

    fun getBaseUrl(): String? = BASE_URL

    fun requireBaseUrl(): String {
        val baseUrl = BASE_URL
        if (baseUrl.isNullOrBlank()) throw BaseUrlNullOrBlankException()
        return baseUrl
    }

    fun setBaseUrl(baseUrl: String?): Boolean {
        BASE_URL = if (baseUrl.isNullOrBlank()) null else baseUrl
        return BASE_URL == baseUrl
    }

    fun getDomain(): String? {
        var baseUrl = BASE_URL
        if (baseUrl.isNullOrBlank()) return null
        baseUrl = baseUrl.removeSuffix("/")
        return when {
            baseUrl.startsWith("http://") -> {
                baseUrl.removePrefix("http://").removePrefix("www.")
            }
            baseUrl.startsWith("https://") -> {
                baseUrl.removePrefix("https://").removePrefix("www.")
            }
            else -> {
                baseUrl
            }
        }
    }

    fun getSocketUrl(): String {
        val baseUrl = BASE_URL
        if (baseUrl.isNullOrBlank()) throw BaseUrlNullOrBlankException()
        return "$BASE_URL$SOCKET_URL_PATH"
    }

    fun build(uri: Uri?): Uri? =
        build(uri?.path)

    fun build(path: String?): Uri? {
        val url = buildUrl(path)
        return if (url.isNullOrBlank()) null else Uri.parse(url)
    }

    fun buildUrl(uri: Uri?): String? =
        buildUrl(uri?.path)

    fun buildUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        val baseUrl = BASE_URL
        if (baseUrl.isNullOrBlank()) return null
        if (path.startsWith(baseUrl)) return path
        val clearBaseUrl = baseUrl.dropLastWhile { it == '/' }
        if (path.startsWith('/')) return clearBaseUrl + path
        return "$clearBaseUrl/$path"
    }

}