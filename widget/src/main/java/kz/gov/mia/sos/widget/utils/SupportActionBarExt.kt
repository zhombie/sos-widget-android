package kz.gov.mia.sos.widget.utils

import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.WindowDecorActionBar
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar

internal inline fun Fragment.setupActionBar(
    toolbar: MaterialToolbar?,
    isBackButtonEnabled: Boolean = true,
    @StringRes title: Int? = null,
    crossinline onNavigationClick: (view: View) -> Unit
): Boolean {
    if (toolbar == null) return false
    val activity = activity
    if (activity is AppCompatActivity) {
        return activity.setupActionBar(
            toolbar = toolbar,
            isBackButtonEnabled = isBackButtonEnabled,
            title = title,
            onNavigationClickAction = onNavigationClick
        )
    }
    return false
}

internal inline fun AppCompatActivity.setupActionBar(
    toolbar: MaterialToolbar,
    isBackButtonEnabled: Boolean = true,
    @StringRes title: Int? = null,
    crossinline onNavigationClickAction: (view: View) -> Unit
): Boolean {
    if (supportActionBar is WindowDecorActionBar) {
        toolbar.setNavigationOnClickListener {
            onNavigationClickAction.invoke(it)
        }
        return false
    } else {
        setSupportActionBar(toolbar)

        if (isBackButtonEnabled) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        if (title == null) {
            supportActionBar?.setDisplayShowTitleEnabled(false)
        } else {
            supportActionBar?.setTitle(title)
            supportActionBar?.setDisplayShowTitleEnabled(true)
        }

        toolbar.setNavigationOnClickListener {
            onNavigationClickAction.invoke(it)
        }

        return supportActionBar != null
    }
}