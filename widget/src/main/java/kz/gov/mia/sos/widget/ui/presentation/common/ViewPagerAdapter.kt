package kz.gov.mia.sos.widget.ui.presentation.common

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

internal class ViewPagerAdapter constructor(
    fragmentActivity: FragmentActivity,
    var fragments: Array<Fragment>
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int =
        fragments.size

    override fun createFragment(position: Int): Fragment =
        fragments[position]

}