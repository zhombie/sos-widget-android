package kz.gov.mia.sos.widget.ui.presentation.home

import kz.inqbox.sdk.domain.model.call.type.CallType

object HomeScreen {

    sealed interface State {
        object IDLE : State

        data class Loading constructor(
            val phase: Phase
        ) : State

        data class Error constructor(
            val phase: Phase
        ) : State

        object Content : State

        enum class Phase {
            WEB_SOCKET_CONNECTION,
            ICE_SERVERS_REQUEST;

            val index: Int
                get() = when (this) {
                    WEB_SOCKET_CONNECTION -> 0
                    ICE_SERVERS_REQUEST -> 1
                }

            val size: Int
                get() = this::class.java.enumConstants.size

            val displayIndex: Int
                get() = index + 1
        }
    }

    sealed interface Command {
        object RequestLocation : Command
    }

    sealed interface Message {
        sealed interface Location : Message {
            object Mocked : Location
            object UnableToDetermine : Location
        }

        sealed interface WebSocket : Message {
            object ConnectionFailed : WebSocket
        }
    }

    sealed interface Route {
        object Back : Route

        data class Call constructor(
            val callType: CallType,
            val callTopic: String?
        ) : Route
    }

}