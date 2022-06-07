package kz.gov.mia.sos.widget.ui.presentation.common.chat.adapter.vh

import android.view.View
import kz.gov.mia.sos.widget.R
import kz.gov.mia.sos.widget.ui.presentation.common.chat.adapter.ChatMessagesAdapter
import kz.gov.mia.sos.widget.ui.presentation.common.chat.adapter.vh.base.BaseTextMessageViewHolder

internal class IncomingTextMessageViewHolder constructor(
    view: View,
    override val callback: ChatMessagesAdapter.Callback? = null
) : BaseTextMessageViewHolder(view, callback) {

    companion object : LayoutResourceProvider() {
        override fun getLayoutId(): Int = R.layout.sos_widget_cell_incoming_text_message
    }

}