package kz.gov.mia.sos.widget.sample

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import kz.gov.mia.sos.widget.api.SOSWidget
import kz.gov.mia.sos.widget.api.locale.SOSWidgetLocaleManager
import kz.gov.mia.sos.widget.api.ui.component.SOSWidgetExtendedFAB
import java.util.*

class MainActivity : AppCompatActivity() {

    private var changeLocaleButton: MaterialButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        changeLocaleButton = findViewById(R.id.changeLocaleButton)

        changeLocaleButton?.text = "Change locale: " + SOSWidgetLocaleManager.getLocale()

        changeLocaleButton?.setOnClickListener {
            val items = arrayOf(
                "en",
                "kk",
                "ru"
            )

            AlertDialog.Builder(this)
                .setTitle("Change locale")
                .setItems(items) { dialog, which ->
                    dialog.dismiss()

                    SOSWidgetLocaleManager.setLocale(
                        this,
                        Locale(items[which])
                    )
                }
                .setNegativeButton(R.string.sos_widget_cancel) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

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