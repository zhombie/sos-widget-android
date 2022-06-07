package kz.gov.mia.sos.widget.ui.model

sealed interface CallState {
    object Pending : CallState

    object Start : CallState

//    object Preparation : CallState

    object Ready : CallState

    object Live : CallState

    object UserRedirected : CallState

    data class Disconnected constructor(
        val initiator: Initiator
    ) : CallState {

        enum class Initiator {
            USER,
            CALL_AGENT
        }

        fun isUser(): Boolean = initiator == Initiator.USER

        fun isCallAgent(): Boolean = initiator == Initiator.CALL_AGENT

    }

    object Finished : CallState

    // ------------------------------------

    val isPending: Boolean
        get() = this is Pending

    val isStarted: Boolean
        get() = this is Pending || isActive

    val isActive: Boolean
        get() = this is Start || this is Ready || this is Live

    val isCompleted: Boolean
        get() = this is Disconnected || this is Finished

}