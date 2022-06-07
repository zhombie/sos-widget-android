package kz.gov.mia.sos.widget.ui.component.chat

import android.content.Context
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.internal.TextWatcherAdapter
import com.google.android.material.textfield.TextInputLayout
import kz.gov.mia.sos.widget.R

internal class ChatFooterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val mediaSelectionButton: MaterialButton
    private val inputView: TextInputLayout
    private val sendMessageButton: MaterialButton

    var isMediaSelectionButtonEnabled: Boolean = false
        private set

    var isSendMessageActionRestricted: Boolean = false
        private set

    var callback: Callback? = null

    private val messageMaxLength: Int

//    private var inputViewTooltip: Tooltip? = null

    init {
        val view = inflate(context, R.layout.sos_widget_view_chat_footer, this)

        mediaSelectionButton = view.findViewById(R.id.mediaSelectionButton)
        inputView = view.findViewById(R.id.inputView)
        sendMessageButton = view.findViewById(R.id.sendMessageButton)

        mediaSelectionButton.setOnClickListener { callback?.onMediaSelectionButtonClicked() }

        sendMessageButton.setOnClickListener {
            if (isSendMessageActionRestricted) {
                return@setOnClickListener
            } else {
                callback?.onSendMessageButtonClicked(
                    inputView.editText?.text?.toString() ?: return@setOnClickListener
                )
            }
        }

        messageMaxLength = context.resources.getInteger(R.integer.sos_widget_message_max_length)
    }

//    override fun onDetachedFromWindow() {
//        super.onDetachedFromWindow()
//
//        inputViewTooltip?.close()
//        inputViewTooltip = null
//    }

    fun clearInputViewText() {
        inputView.editText?.text?.clear()
    }

    fun enableSendMessageButton() {
        setSendMessageButtonEnabled(true)
    }

    fun disableSendMessageButton() {
        setSendMessageButtonEnabled(false)
    }

    fun setSendMessageButtonClickRestriction(isRestricted: Boolean) {
        isSendMessageActionRestricted = isRestricted

        if (isRestricted) {
            sendMessageButton.isEnabled = false
        } else {
            sendMessageButton.isEnabled = (inputView.editText?.text?.length ?: 0) > 0
        }
    }

    private fun setSendMessageButtonEnabled(isEnabled: Boolean) {
        sendMessageButton.isEnabled = if (isSendMessageActionRestricted) {
            false
        } else {
            isEnabled
        }
    }

    fun enableMediaSelectionButton(): Boolean = setMediaSelectionButtonEnabled(true)

    fun disableMediaSelectionButton(): Boolean = setMediaSelectionButtonEnabled(false)

    private fun setMediaSelectionButtonEnabled(isEnabled: Boolean): Boolean {
        isMediaSelectionButtonEnabled = isEnabled
        return isMediaSelectionButtonEnabled == isEnabled
    }

    fun setOnTextChangedListener(
        callback: (s: CharSequence?, start: Int, before: Int, count: Int) -> Unit
    ) {
        inputView.editText?.addTextChangedListener(object : TextWatcherAdapter() {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                callback(s, start, before, count)

//                if (s?.length == messageMaxLength) {
//                    inputViewTooltip?.close()
//                    inputViewTooltip = null
//                    inputViewTooltip = inputView.showTooltip(
//                        text = context.getString(
//                            R.string.sos_widget_info_text_message_exceeds_max_limit,
//                            context.resources.getInteger(R.integer.message_max_length)
//                        ),
//                        position = Position.TOP
//                    )
//                }
            }
        })
    }

    fun setImeSendByEnter() {
        inputView.editText?.imeOptions = EditorInfo.IME_ACTION_DONE
        inputView.editText?.inputType =
            EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES or EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE
        inputView.editText?.setHorizontallyScrolling(false)
        inputView.editText?.maxLines = 5
        inputView.editText?.isSingleLine = false
    }

    fun setImeNewLineByEnter() {
        inputView.editText?.imeOptions = EditorInfo.IME_ACTION_NEXT
        inputView.editText?.inputType = EditorInfo.TYPE_CLASS_TEXT
        inputView.editText?.setHorizontallyScrolling(true)
        inputView.editText?.maxLines = 5
        inputView.editText?.isSingleLine = false
    }

    fun setOnInputViewActionListener(
        onActionIds: Array<Int>,
        onKeycode: Int,
        callback: (text: String) -> Unit
    ) {
        inputView.editText?.setOnEditorActionListener { _, actionId, event ->
            if (isSendMessageActionRestricted) {
                return@setOnEditorActionListener false
            } else {
                if (onActionIds.any { it == actionId } || onKeycode == event.keyCode) {
                    val text = inputView.editText?.text?.toString()
                    if (text.isNullOrBlank()) {
                        return@setOnEditorActionListener false
                    } else {
                        callback(text)
                        return@setOnEditorActionListener true
                    }
                }
                return@setOnEditorActionListener false
            }
        }
    }

    fun resetOnInputViewActionDoneClick() {
        inputView.editText?.setOnEditorActionListener(null)
    }

    interface Callback {
        fun onMediaSelectionButtonClicked(): Boolean
        fun onSendMessageButtonClicked(messageText: String)
    }

}