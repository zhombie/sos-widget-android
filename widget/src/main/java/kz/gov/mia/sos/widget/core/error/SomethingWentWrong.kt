package kz.gov.mia.sos.widget.core.error

internal class SomethingWentWrong constructor(val text: String? = null) : RuntimeException() {

    override val message: String?
        get() = if (text.isNullOrBlank()) super.message else "${text}. ${super.message}"

}