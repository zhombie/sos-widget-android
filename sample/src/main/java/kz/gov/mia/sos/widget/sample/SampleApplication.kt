package kz.gov.mia.sos.widget.sample

import android.app.Application
import android.content.res.Configuration
import kz.gov.mia.sos.widget.api.image.load.SOSWidgetImageLoader
import kz.gov.mia.sos.widget.api.locale.SOSWidgetLocaleManager
import kz.gov.mia.sos.widget.coil.CoilImageLoader
import kz.inqbox.sdk.domain.model.language.Language

class SampleApplication : Application(), SOSWidgetImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()

        SOSWidgetLocaleManager.initialize(
            this,
            listOf(
                Language.ENGLISH.locale,
                Language.KAZAKH.locale,
                Language.RUSSIAN.locale,
            )
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        SOSWidgetLocaleManager.onConfigurationChanged()
    }

    override fun getImageLoader(): SOSWidgetImageLoader {
        return CoilImageLoader(this, BuildConfig.LOG)
    }

}