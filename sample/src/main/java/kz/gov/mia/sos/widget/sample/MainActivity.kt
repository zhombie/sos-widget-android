package kz.gov.mia.sos.widget.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kz.gov.mia.sos.widget.api.SOSWidget
import kz.gov.mia.sos.widget.api.ui.component.SOSWidgetExtendedFAB

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<SOSWidgetExtendedFAB>(R.id.launchWidgetButton).setOnClickListener {
            launchWidget()
        }
    }

    private fun launchWidget() {
        SOSWidget.Builder.Default(this)
            .setBaseUrl(BuildConfig.BASE_URL)
            .setUsername("test")
            .setPassword("test")
            .setCallTopic("sos")
            .setLoggingEnabled(true)
            .launch()
    }

}