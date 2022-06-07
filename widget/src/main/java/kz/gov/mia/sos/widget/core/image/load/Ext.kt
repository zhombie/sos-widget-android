package kz.gov.mia.sos.widget.core.image.load

import android.content.Context
import android.net.Uri
import android.widget.ImageView
import kz.gov.mia.sos.widget.api.SOSWidget
import kz.gov.mia.sos.widget.api.image.load.SOSWidgetImageLoader

internal inline val Context.imageLoader: SOSWidgetImageLoader
    get() = SOSWidget.getImageLoader(this)

internal inline fun ImageView.load(
    uri: String?,
    imageLoader: SOSWidgetImageLoader = context.imageLoader,
    builder: SOSWidgetImageLoader.Request.Builder.() -> Unit = {}
): SOSWidgetImageLoader.Disposable = loadAny(uri, imageLoader, builder)

internal inline fun ImageView.load(
    uri: Uri?,
    imageLoader: SOSWidgetImageLoader = context.imageLoader,
    builder: SOSWidgetImageLoader.Request.Builder.() -> Unit = {}
): SOSWidgetImageLoader.Disposable = loadAny(uri, imageLoader, builder)

internal inline fun ImageView.loadAny(
    data: Any?,
    imageLoader: SOSWidgetImageLoader = context.imageLoader,
    builder: SOSWidgetImageLoader.Request.Builder.() -> Unit = {}
): SOSWidgetImageLoader.Disposable =
    imageLoader.enqueue(
        SOSWidgetImageLoader.Request.Builder(context)
            .setData(data)
            .into(this)
            .apply(builder)
            .build()
    )

internal fun ImageView.dispose() {
    context.imageLoader.dispose(this)
}