package kz.gov.mia.sos.widget.ui.presentation.home

internal class Initialization constructor(
    private var listener: EventListener? = null
) {

    var isConnectedToWebSocket: Boolean = false
        @Synchronized get
        @Synchronized set(value) {
            if (field != value) {
                field = value
                listener?.onWebSocketConnectionState(value)
            }
        }

    var isIceServersFetched: Boolean = false
        @Synchronized get
        @Synchronized set(value) {
            if (field != value) {
                field = value
                listener?.onIceServersFetchState(value)
            }
        }

    fun dispose() {
        listener = null

        isConnectedToWebSocket = false
        isIceServersFetched = false
    }

    interface EventListener {
        fun onIceServersFetchState(isFetched: Boolean)
        fun onWebSocketConnectionState(isConnected: Boolean)
    }

}