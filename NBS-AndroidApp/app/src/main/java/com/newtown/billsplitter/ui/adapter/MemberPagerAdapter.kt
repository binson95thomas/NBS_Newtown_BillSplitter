package com.newtown.billsplitter.ui.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.newtown.billsplitter.viewmodel.MainViewModel
import com.newtown.billsplitter.ui.fragments.MemberBreakdownPageFragment

class MemberPagerAdapter(
    fragment: Fragment,
    private var breakdowns: List<MainViewModel.MemberBreakdown>
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = breakdowns.size

    override fun createFragment(position: Int): Fragment {
        val index = position % breakdowns.size
        val b = breakdowns[index]
        val itemsList = b.items.joinToString(", ") { "${it.name} (£%.2f)".format(it.price) }
        return MemberBreakdownPageFragment.newInstance(
            memberName = b.memberName,
            subtotal = b.subtotal,
            discount = b.discountShare,
            finalAmount = b.finalAmount,
            itemsText = itemsList
        )
    }

    fun update(newBreakdowns: List<MainViewModel.MemberBreakdown>) {
        breakdowns = newBreakdowns
        notifyDataSetChanged()
    }
}

