package kz.gov.mia.sos.widget.ui.model

import kz.garage.chat.model.Entity

internal class Conversation {

    companion object {
        const val MAX_UNREAD_MESSAGES_COUNT = 9
    }

    val history = mutableListOf<Entity>()

    var unreadMessages: Int = 0
        set(value) {
            if (value < 0) {
                return
            }
            if (field >= MAX_UNREAD_MESSAGES_COUNT) {
                return
            } else {
                field = value
            }
        }

    var isLocalAudioEnabled: Boolean = false
    var isLocalVideoEnabled: Boolean = false
    var isRemoteAudioEnabled: Boolean = false
    var isRemoteVideoEnabled: Boolean = false

    var remoteVideoScaleType: ScaleType? = null

    var isLocalVideoMirrored: Boolean = false
}