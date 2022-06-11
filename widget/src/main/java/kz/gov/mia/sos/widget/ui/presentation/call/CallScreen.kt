package kz.gov.mia.sos.widget.ui.presentation.call

import kz.garage.multimedia.store.model.Content

object CallScreen {
    sealed interface State {
        object IDLE : State

        sealed interface Loading : State {
            data class Determinate constructor(
                val progress: Float
            ) : Loading

            object Indeterminate : Loading
        }
    }

    sealed interface Message {
        sealed interface Call : Message {
            object CancelConfirmation : Call
            object HangupConfirmation : Call
        }

        sealed interface Form : Message {
            data class NewTaskCreated constructor(
                val text: String
            ) : Form
        }

        sealed interface Location : Message {
            object Mocked : Location
            object UnableToDetermine : Location
        }

        sealed interface WebSocket : Message {
            object Disconnected : WebSocket
        }

        sealed interface FileUpload : Message {
            data class Failed constructor(
                val reason: String? = null
            ) : FileUpload
        }

        sealed interface FileDownload : Message {
            data class Pending constructor(
                val content: Content,
                val itemPosition: Int,
                val progress: Float
            ) : FileDownload

            object Failed : FileDownload
        }
    }

    sealed interface Route {
        object Back : Route
        object Map : Route
    }
}
