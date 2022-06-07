package kz.gov.mia.sos.widget.core.error

internal class ViewHolderViewTypeException constructor(private val viewType: Int) : RuntimeException() {

    override val message: String
        get() = "There is no ViewHolder for viewType: $viewType"

}