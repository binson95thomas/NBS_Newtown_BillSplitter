package com.newtown.billsplitter.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.newtown.billsplitter.ui.fragments.BillUploadFragment
import com.newtown.billsplitter.ui.fragments.ItemsFragment
import com.newtown.billsplitter.ui.fragments.MembersFragment
import com.newtown.billsplitter.ui.fragments.TotalsFragment

class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    
    override fun getItemCount(): Int = 4
    
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MembersFragment()
            1 -> BillUploadFragment()
            2 -> ItemsFragment()
            3 -> TotalsFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
} 