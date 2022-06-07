package kz.gov.mia.sos.widget.utils

import android.view.WindowManager
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import kz.gov.mia.sos.widget.R

internal fun Fragment.setStatusBarTransparent() = with(activity) {
    if (this is AppCompatActivity) {
        setStatusBarTransparent()
    }
}


internal fun AppCompatActivity.setStatusBarTransparent() = with(window) {
    addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

    WindowCompat.setDecorFitsSystemWindows(this, false)

    statusBarColor = ContextCompat.getColor(this@setStatusBarTransparent, R.color.sos_widget_transparent)
}


internal fun Fragment.setStatusBarColor(@ColorRes id: Int) = with(activity) {
    if (this is AppCompatActivity) {
        setStatusBarColor(id)
    }
}


internal fun AppCompatActivity.setStatusBarColor(@ColorRes id: Int) = with(window) {
    addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

    statusBarColor = ContextCompat.getColor(this@setStatusBarColor, id)
}