package kz.gov.mia.sos.widget.api

import android.content.Context
import android.content.Intent
import kz.gov.mia.sos.widget.api.exception.AuthCredentialsNullOrBlankException
import kz.gov.mia.sos.widget.api.exception.BaseUrlNullOrBlankException
import kz.gov.mia.sos.widget.api.exception.ImageLoaderNullException
import kz.gov.mia.sos.widget.api.image.load.SOSWidgetImageLoader
import kz.gov.mia.sos.widget.ui.presentation.home.HomeActivity

object SOSWidget {

    data class Credentials constructor(
        val username: String,
        val password: String
    )

    var credentials: Credentials? = null
        @Synchronized get
        @Synchronized set

    private var imageLoader: SOSWidgetImageLoader? = null
    private var imageLoaderFactory: SOSWidgetImageLoader.Factory? = null

    @Synchronized
    fun getImageLoader(context: Context?): SOSWidgetImageLoader =
        imageLoader ?: setImageLoaderFactory(context)

    @Synchronized
    fun setImageLoader(loader: SOSWidgetImageLoader) {
        imageLoaderFactory = null
        imageLoader = loader
    }

    @Synchronized
    fun setImageLoader(factory: SOSWidgetImageLoader.Factory) {
        imageLoaderFactory = factory
        imageLoader = null
    }

    @Synchronized
    fun setImageLoaderFactory(context: Context?): SOSWidgetImageLoader {
        imageLoader?.let { return it }

        imageLoader = imageLoaderFactory?.getImageLoader()
            ?: (context?.applicationContext as? SOSWidgetImageLoader.Factory)?.getImageLoader()

        imageLoaderFactory = null

        return imageLoader ?: throw ImageLoaderNullException()
    }

    open class Builder private constructor(private val context: Context) {

        private var baseUrl: String? = null
        private var username: String? = null
        private var password: String? = null
        private var isLoggingEnabled: Boolean = false
        private var callTopic: String? = null

        fun getContext(): Context = context

        fun getBaseUrl(): String? = baseUrl

        fun getUsername(): String? = username

        fun getLoggingEnabled(): Boolean = isLoggingEnabled

        fun getCallTopic(): String? = callTopic

        fun setBaseUrl(baseUrl: String?): Builder {
            this.baseUrl = baseUrl
            return this
        }

        fun setUsername(username: String?): Builder {
            this.username = username
            return this
        }

        fun setPassword(password: String?): Builder {
            this.password = password
            return this
        }

        fun setLoggingEnabled(isEnabled: Boolean): Builder {
            this.isLoggingEnabled = isEnabled
            return this
        }

        fun setCallTopic(callTopic: String?): Builder {
            this.callTopic = callTopic
            return this
        }

        fun build(): Intent =
            HomeActivity.newIntent(
                context = context,
                params = HomeActivity.Params(
                    baseUrl = if (baseUrl.isNullOrBlank()) {
                        throw BaseUrlNullOrBlankException()
                    } else {
                        requireNotNull(baseUrl)
                    },
                    username = if (username.isNullOrBlank()) {
                        throw AuthCredentialsNullOrBlankException()
                    } else {
                        requireNotNull(username)
                    },
                    password = if (password.isNullOrBlank()) {
                        throw AuthCredentialsNullOrBlankException()
                    } else {
                        requireNotNull(password)
                    },
                    isLoggingEnabled = isLoggingEnabled,
                    callTopic = requireNotNull(callTopic)
                )
            )

        fun launch(): Intent {
            val intent = build()
            context.startActivity(intent)
            return intent
        }

        class Default constructor(context: Context) : Builder(context)
    }

}