package kz.gov.mia.sos.widget.ui.component

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import kz.garage.window.dp2Px
import kz.gov.mia.sos.widget.R
import kotlin.math.roundToInt

internal class ReminderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

    private val contentView: LinearLayout
    private val iconView: ShapeableImageView
    private val titleView: MaterialTextView
    private val textView: MaterialTextView
    private val button: MaterialButton

    init {
        setCardBackgroundColor(ContextCompat.getColor(context, R.color.sos_widget_black_alpha))
        radius = 15F.dp2Px()
        strokeWidth = 0
        cardElevation = 0F
        setContentPadding(
            16F.dp2Px().roundToInt(),
            16F.dp2Px().roundToInt(),
            16F.dp2Px().roundToInt(),
            16F.dp2Px().roundToInt()
        )

        contentView = buildContentView()

        iconView = buildIconView()
        contentView.addView(iconView)

        titleView = buildTitleView()
        contentView.addView(titleView)

        textView = buildTextView()
        contentView.addView(textView)

        button = buildButton()
        contentView.addView(button)

        addView(contentView)
    }

    fun show() {
        visibility = View.VISIBLE
    }

    fun hide() {
        visibility = View.GONE
    }

    fun setIcon(@DrawableRes resId: Int) {
        iconView.setImageResource(resId)
    }

    fun setTitle(@StringRes resId: Int) {
        titleView.setText(resId)
    }

    fun setTitle(text: String) {
        titleView.text = text
    }

    fun setText(@StringRes resId: Int) {
        textView.setText(resId)
    }

    fun setText(text: String) {
        textView.text = text
    }

    fun setButtonText(@StringRes resId: Int) {
        button.setText(resId)
    }

    fun setButtonText(text: String) {
        button.text = text
    }

    fun setOnButtonClickListener(listener: OnClickListener?) {
        button.setOnClickListener(listener)
    }

    private fun buildContentView() = LinearLayout(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        gravity = Gravity.CENTER
        orientation = LinearLayout.VERTICAL
    }

    private fun buildIconView() = ShapeableImageView(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            28F.dp2Px().roundToInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        imageTintList =
            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.sos_widget_white))
    }

    private fun buildTitleView() = MaterialTextView(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 7F.dp2Px().roundToInt(), 0, 0)
        }
        setTextColor(ContextCompat.getColor(context, R.color.sos_widget_white))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 18F)
        setTypeface(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private fun buildTextView() = MaterialTextView(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(
                10F.dp2Px().roundToInt(),
                7F.dp2Px().roundToInt(),
                10F.dp2Px().roundToInt(),
                0
            )
        }
        gravity = Gravity.CENTER
        setLineSpacing(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                0.0f,
                resources.displayMetrics
            ), 1.05f
        )
        setTextColor(ContextCompat.getColor(context, R.color.sos_widget_white))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13F)
    }

    private fun buildButton() =
        MaterialButton(context, null, R.style.SOSWidget_UnelevatedButton).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 26F.dp2Px().roundToInt(), 0, 0)
                setPadding(
                    27F.dp2Px().roundToInt(),
                    11F.dp2Px().roundToInt(),
                    27F.dp2Px().roundToInt(),
                    13F.dp2Px().roundToInt()
                )
            }
            setBackgroundColor(ContextCompat.getColor(context, R.color.sos_widget_transparent))
            setTextColor(ContextCompat.getColor(context, R.color.sos_widget_white))
            cornerRadius = 26F.dp2Px().roundToInt()
            setStrokeColorResource(R.color.sos_widget_white)
            strokeWidth = 1F.dp2Px().roundToInt()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13F)
        }

}