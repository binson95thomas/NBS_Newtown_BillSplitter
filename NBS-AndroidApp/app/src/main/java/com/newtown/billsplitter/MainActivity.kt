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
import android.widget.TextView
import com.newtown.billsplitter.utils.HapticUtils
import com.newtown.billsplitter.utils.AnimationUtils

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Add global exception handler
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("MainActivity", "Uncaught exception in thread ${thread.name}", throwable)
        }
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        viewModel.initialize(this)

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

        // Setup Floating Action Button with haptic feedback
        binding.fabAddMember.setOnClickListener {
            HapticUtils.lightTap(it)
            AnimationUtils.bounceButton(it)
            showAddMemberDialog()
        }

        // Observe ViewModel
        viewModel.members.observe(this) { members ->
            // Update FAB visibility based on current tab
            val currentTab = binding.tabLayout.selectedTabPosition
            binding.fabAddMember.visibility = if (currentTab == 0) View.VISIBLE else View.GONE
        }

        // Update FAB visibility when tab changes with haptic feedback
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                // Light haptic feedback for tab selection
                tab?.view?.let { HapticUtils.lightTap(it) }
                binding.fabAddMember.visibility = if (tab?.position == 0) View.VISIBLE else View.GONE
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    fun showAddMemberDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_member, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.memberNameEditText)
        val emojiPreview = dialogView.findViewById<TextView>(R.id.emojiPreview)
        val pickEmojiButton = dialogView.findViewById<View>(R.id.pickEmojiButton)
        var selectedEmoji = "😀"
        emojiPreview.text = selectedEmoji
        pickEmojiButton.setOnClickListener {
            // Simple emoji picker dialog (grid)
            val emojis = listOf("😀","😁","😂","🤣","😃","😄","😅","😆","😉","😊","😋","😎","😍","😘","🥰","😗","😙","😚","🙂","🤗","🤩","🤔","🤨","😐","😑","😶","🙄","😏","😣","😥","😮","🤐","😯","😪","😫","🥱","😴","😌","😛","😜","😝","🤤","😒","😓","😔","😕","🙃","🤑","😲","☹️","🙁","😖","😞","😟","😤","😢","😭","😦","😧","😨","😩","🤯","😬","😰","😱","🥵","🥶","😳","🤪","😵","😡","😠","🤬","😷","🤒","🤕","🤢","🤮","🥴","😇","🥳","🥺","🤠","🤡","🤥","🤫","🤭","🧐","🤓","😈","👿","👹","👺","💀","👻","👽","🤖","💩")
            val emojiGrid = android.widget.GridView(this).apply {
                numColumns = 8
                adapter = object : android.widget.BaseAdapter() {
                    override fun getCount() = emojis.size
                    override fun getItem(position: Int) = emojis[position]
                    override fun getItemId(position: Int) = position.toLong()
                    override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup?): View {
                        val tv = TextView(this@MainActivity)
                        tv.text = emojis[position]
                        tv.textSize = 24f
                        tv.gravity = android.view.Gravity.CENTER
                        tv.setPadding(8, 8, 8, 8)
                        return tv
                    }
                }
                setOnItemClickListener { _, _, pos, _ ->
                    selectedEmoji = emojis[pos]
                    emojiPreview.text = selectedEmoji
                    (it as android.app.AlertDialog).dismiss()
                }
            }
            val dialog = android.app.AlertDialog.Builder(this)
                .setTitle("Pick Emoji")
                .setView(emojiGrid)
                .create()
            emojiGrid.setOnItemClickListener { _, _, pos, _ ->
                selectedEmoji = emojis[pos]
                emojiPreview.text = selectedEmoji
                dialog.dismiss()
            }
            dialog.show()
        }

        AlertDialog.Builder(this)
            .setTitle("Add Member")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = nameEditText.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.addMember(Member(id = System.currentTimeMillis(), name = name, emoji = selectedEmoji))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
} 