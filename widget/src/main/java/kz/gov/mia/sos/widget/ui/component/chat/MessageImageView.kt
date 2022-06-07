package kz.gov.mia.sos.widget.ui.component.chat

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.ShapeAppearanceModel
import kz.gov.mia.sos.widget.R

internal class MessageImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ShapeableImageView(context, attrs, defStyleAttr) {

    init {
        setupBounds()
        setupScaleType()
        setupShapeAppearance()
        setClickable()
    }

    private fun setupBounds() {
        adjustViewBounds = true

        minimumWidth = context.resources.getDimensionPixelOffset(R.dimen.sos_widget_message_image_min_width)
        minimumHeight = context.resources.getDimensionPixelOffset(R.dimen.sos_widget_message_image_min_height)
        maxWidth = context.resources.getDimensionPixelOffset(R.dimen.sos_widget_message_image_max_width)
        maxHeight = context.resources.getDimensionPixelOffset(R.dimen.sos_widget_message_image_max_height)
    }

    private fun setupScaleType() {
        scaleType = ScaleType.CENTER_CROP
    }

    private fun setupShapeAppearance() {
        shapeAppearanceModel = ShapeAppearanceModel
            .builder()
            .setAllCornerSizes(context.resources.getDimension(R.dimen.sos_widget_message_background_corner_radius))
            .build()
    }

    fun setClickable() {
        isClickable = true
    }

    fun showStroke(
        @ColorRes strokeColorResourceId: Int = R.color.sos_widget_light_gray,
        @DimenRes strokeWidthResourceId: Int = R.dimen.sos_widget_border_stroke_width
    ) {
        setStrokeColorResource(strokeColorResourceId)
        setStrokeWidthResource(strokeWidthResourceId)
    }

}