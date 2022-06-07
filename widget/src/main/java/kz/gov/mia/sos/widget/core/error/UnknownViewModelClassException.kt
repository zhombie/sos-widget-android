package kz.gov.mia.sos.widget.core.error

internal class UnknownViewModelClassException constructor(
    private val className: String? = null
) : IllegalStateException() {

    override val message: String
        get() {
            val text = "Unknown ViewModel class"
            return if (className.isNullOrBlank()) {
                text
            } else {
                "$text. $className"
            }
        }

}