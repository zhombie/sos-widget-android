package kz.gov.mia.sos.widget.ui.platform.resource

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import kz.gov.mia.sos.widget.ui.platform.locale.LocaleActivity

abstract class ResourceActivity : LocaleActivity() {

    @ColorInt
    protected fun getColorCompat(@ColorRes resId: Int): Int =
        ContextCompat.getColor(this, resId)

    protected fun getColorStateListCompat(@ColorRes resId: Int): ColorStateList =
        requireNotNull(AppCompatResources.getColorStateList(this, resId))

    protected fun getDrawableCompat(@DrawableRes resId: Int): Drawable =
        requireNotNull(AppCompatResources.getDrawable(this, resId))

}
