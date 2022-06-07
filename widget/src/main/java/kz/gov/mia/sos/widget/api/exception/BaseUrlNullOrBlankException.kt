package kz.gov.mia.sos.widget.api.exception

class BaseUrlNullOrBlankException : RuntimeException() {

    override val message: String
        get() = "Base url is null or blank!"

}