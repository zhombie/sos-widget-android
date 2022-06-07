package kz.gov.mia.sos.widget.api.exception

class AuthCredentialsNullOrBlankException : RuntimeException() {

    override val message: String
        get() = "Username or password is null or blank!"

}