package kz.gov.mia.sos.widget.api.exception

import kz.garage.kotlin.simpleNameOf
import kz.gov.mia.sos.widget.api.SOSWidget
import kz.gov.mia.sos.widget.api.image.load.SOSWidgetImageLoader

class ImageLoaderNullException : IllegalStateException() {

    override val message: String
        get() = "${simpleNameOf<SOSWidgetImageLoader>()} not initialized at ${simpleNameOf<SOSWidget>()}"

}