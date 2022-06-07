package kz.gov.mia.sos.widget.api.ui.component

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import kz.gov.mia.sos.widget.R

class SOSWidgetExtendedFAB @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.extendedFloatingActionButtonStyle
) : ExtendedFloatingActionButton(context, attrs, defStyleAttr) {

    init {
        backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.sos_widget_white))
        setIconResource(R.drawable.sos_widget_ic_logo)
        iconSize = context.resources.getDimensionPixelOffset(R.dimen.sos_widget_extended_fab_icon_size)
        iconTint = null
        text = null
//        setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.sos_widget_black)))
    }

}