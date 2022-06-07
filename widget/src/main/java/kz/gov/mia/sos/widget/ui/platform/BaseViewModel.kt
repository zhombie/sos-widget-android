package kz.gov.mia.sos.widget.ui.platform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

abstract class BaseViewModel : ViewModel() {

    protected val scope: CoroutineScope
        get() = viewModelScope

    // [BEGIN] Coroutines

    protected val default: CoroutineContext
        get() = Dispatchers.Default

    protected val io: CoroutineContext
        get() = Dispatchers.IO

    protected val ui: CoroutineContext
        get() = Dispatchers.Main

    protected val exceptionHandler = CoroutineExceptionHandler { context, exception ->
        onCoroutineException(context, exception)
    }

    open fun onCoroutineException(context: CoroutineContext, exception: Throwable) {}

    protected fun CoroutineContext.launch(
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ): Job = scope.launch(context = this, start = start, block = block)

    // [END] Coroutines

}