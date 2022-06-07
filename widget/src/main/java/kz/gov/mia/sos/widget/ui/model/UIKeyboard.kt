package kz.gov.mia.sos.widget.ui.model

internal data class UIKeyboard constructor(
    val buttons: List<List<UIInlineButton>> = emptyList()
) {

    data class UIInlineButton constructor(
        val text: String,
        val payload: String? = null
    ) : Pressable()

    abstract class Pressable {
        var isPressed: Boolean = false
    }

    fun flatten(): List<UIInlineButton> = buttons.flatten()

}