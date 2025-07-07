package com.newtown.billsplitter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.newtown.billsplitter.databinding.ActivityMainBinding
import com.newtown.billsplitter.ui.adapter.MainPagerAdapter
import com.newtown.billsplitter.viewmodel.MainViewModel
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.app.AlertDialog
import com.newtown.billsplitter.model.Member

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        viewModel.initialize(this)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        // Setup ViewPager and TabLayout
        val pagerAdapter = MainPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Members"
                1 -> "Upload"
                2 -> "Items"
                3 -> "Totals"
                else -> ""
            }
        }.attach()

        // Setup Floating Action Button
        binding.fabAddMember.setOnClickListener {
            showAddMemberDialog()
        }

        // Observe ViewModel
        viewModel.members.observe(this) { members ->
            // Update FAB visibility based on current tab
            val currentTab = binding.tabLayout.selectedTabPosition
            binding.fabAddMember.visibility = if (currentTab == 0) View.VISIBLE else View.GONE
        }

        // Update FAB visibility when tab changes
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                binding.fabAddMember.visibility = if (tab?.position == 0) View.VISIBLE else View.GONE
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    fun showAddMemberDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_member, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.memberNameEditText)

        AlertDialog.Builder(this)
            .setTitle("Add Member")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = nameEditText.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.addMember(Member(name = name))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
} 