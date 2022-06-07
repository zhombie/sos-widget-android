package kz.gov.mia.sos.widget.ui.platform.alert

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.TypedValue
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kz.gov.mia.sos.widget.R

internal sealed interface ThemedAlertDialog {

    open class Builder constructor(
        context: Context
    ) : MaterialAlertDialogBuilder(context, R.style.SOSWidget_AlertDialogTheme)

    fun build(
        negative: (() -> Unit)? = null,
        positive: (() -> Unit)? = null,
    ): MaterialAlertDialogBuilder

    class CallHangupConfirmation constructor(
        private val context: Context
    ) : ThemedAlertDialog {

        override fun build(
            negative: (() -> Unit)?,
            positive: (() -> Unit)?
        ): MaterialAlertDialogBuilder {
            return Builder(context)
                .setTitle(R.string.sos_widget_attention)
                .setMessage(R.string.sos_widget_user_confirmation_end_call)
                .setNegativeButton(R.string.sos_widget_no) { dialog, _ ->
                    dialog.dismiss()
                    negative?.invoke()
                }
                .setPositiveButton(R.string.sos_widget_yes) { dialog, _ ->
                    dialog.dismiss()
                    positive?.invoke()
                }
        }

    }

    class OpenLinkConfirmationConfirmation constructor(
        private val context: Context,
        private val url: String
    ) : ThemedAlertDialog {

        override fun build(
            negative: (() -> Unit)?,
            positive: (() -> Unit)?
        ): MaterialAlertDialogBuilder {
            val messageView = FrameLayout(context)

            val textView = TextView(context)

            textView.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.sos_widget_very_dark_gray
                )
            )

            val colorStateList = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_pressed),
                    intArrayOf(android.R.attr.state_selected),
                    intArrayOf()
                ),
                intArrayOf(
                    ContextCompat.getColor(context, R.color.sos_widget_very_light_blue),
                    ContextCompat.getColor(context, R.color.sos_widget_very_light_blue),
                    ContextCompat.getColor(context, R.color.sos_widget_light_blue)
                )
            )

            textView.highlightColor = Color.TRANSPARENT

            textView.setLinkTextColor(colorStateList)

            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)

            textView.linksClickable = true

            textView.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                val margin =
                    context.resources.getDimensionPixelOffset(R.dimen.sos_widget_dialog_horizontal_padding)
                setMargins(margin, margin, margin, margin)
            }

            val spannable =
                SpannableString(context.getString(R.string.sos_widget_go_to_link_confirmation, url))
            textView.autoLinkMask = Linkify.WEB_URLS
            textView.text = spannable
            textView.movementMethod = LinkMovementMethod.getInstance()

            messageView.addView(textView)

            return Builder(context)
                .setTitle(R.string.sos_widget_open_link)
                .setView(messageView)
                .setNegativeButton(R.string.sos_widget_no) { dialog, _ ->
                    dialog.dismiss()
                    negative?.invoke()
                }
                .setPositiveButton(R.string.sos_widget_yes) { dialog, _ ->
                    dialog.dismiss()
                    positive?.invoke()
                }
        }

    }

    class FakeLocationUsage constructor(
        private val context: Context
    ) : ThemedAlertDialog {

        override fun build(
            negative: (() -> Unit)?,
            positive: (() -> Unit)?
        ): MaterialAlertDialogBuilder {
            return Builder(context)
                .setTitle(R.string.sos_widget_attention)
                .setMessage(R.string.sos_widget_error_mocked_location)
                .setPositiveButton(R.string.sos_widget_ok) { dialog, _ ->
                    dialog.dismiss()
                    positive?.invoke()
                }
        }

    }

}