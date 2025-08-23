package com.newtown.billsplitter.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.newtown.billsplitter.model.BillItem
import com.newtown.billsplitter.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.appcompat.app.AlertDialog
import android.text.method.ScrollingMovementMethod
import android.content.ClipData
import android.content.ClipboardManager

/**
 * Utility class for sharing bill breakdowns to any app with clean formatting
 */
object UniversalShareUtils {
    
    /**
     * Share bill breakdown to any app with formatted message
     */
    fun shareBillToAnyApp(
        context: Context,
        billItems: List<BillItem>,
        memberBreakdowns: List<MainViewModel.MemberBreakdown>,
        discountPercentage: Double,
        subtotal: Double,
        discountAmount: Double,
        finalTotal: Double
    ) {
        try {
            val formattedMessage = formatBillMessage(
                billItems, memberBreakdowns, discountPercentage, 
                subtotal, discountAmount, finalTotal
            )
            
            // Use universal share intent
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, formattedMessage)
                putExtra(Intent.EXTRA_SUBJECT, "Bill Split Breakdown")
            }
            
            context.startActivity(Intent.createChooser(intent, "Share Bill Breakdown"))
            
            // Add haptic feedback for share action
            HapticUtils.successPattern(context)
            
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share bill: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Format the bill breakdown into a clean message without advertising
     */
    private fun formatBillMessage(
        billItems: List<BillItem>,
        memberBreakdowns: List<MainViewModel.MemberBreakdown>,
        discountPercentage: Double,
        subtotal: Double,
        discountAmount: Double,
        finalTotal: Double
    ): String {
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        
        return buildString {
            // Header
            appendLine("💰 BILL SPLIT BREAKDOWN")
            appendLine("📅 $currentDate")
            appendLine()
            
            // Bill Items Section
            appendLine("📋 BILL ITEMS")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━")
            
            val sortedItems = billItems.sortedByDescending { it.price }
            sortedItems.forEachIndexed { index, item ->
                val priceStr = "£%.2f".format(item.price)
                val itemLine = "${index + 1}. ${item.name}"
                
                // Add special formatting for deals/discounts
                when (item.itemType) {
                    "deal" -> appendLine("🎯 $itemLine - $priceStr")
                    "discount" -> appendLine("💫 $itemLine - $priceStr")
                    else -> appendLine("🍽️ $itemLine - $priceStr")
                }
            }
            
            appendLine()
            
            // Bill Summary Section
            appendLine("💵 BILL SUMMARY")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Subtotal: £%.2f".format(subtotal))
            
            if (discountPercentage > 0) {
                appendLine("Discount (%.1f%%): -£%.2f".format(discountPercentage, discountAmount))
            }
            
            appendLine("Final Total: £%.2f".format(finalTotal))
            appendLine()
            
            // Member Breakdown Section
            appendLine("👥 MEMBER BREAKDOWN")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━")
            
            memberBreakdowns.forEachIndexed { index, breakdown ->
                appendLine()
                appendLine("${getEmojiForIndex(index)} ${breakdown.memberName.uppercase()}")
                appendLine("└─ Amount: £%.2f".format(breakdown.finalAmount))
                
                if (breakdown.items.isNotEmpty()) {
                    appendLine("└─ Items:")
                    breakdown.items.forEach { item ->
                        val itemPrice = "£%.2f".format(item.price)
                        appendLine("   • ${item.name} - $itemPrice")
                    }
                }
            }
            
            appendLine()
            
            // Payment Summary
            appendLine("💳 PAYMENT SUMMARY")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━")
            memberBreakdowns.forEach { breakdown ->
                appendLine("${breakdown.memberName}: £%.2f".format(breakdown.finalAmount))
            }
            
            appendLine()
            appendLine("✅ Total Verified: £%.2f".format(memberBreakdowns.sumOf { it.finalAmount }))
        }
    }
    
    /**
     * Get emoji for member index to make breakdown more visual
     */
    private fun getEmojiForIndex(index: Int): String {
        return when (index % 8) {
            0 -> "🟦"
            1 -> "🟨"
            2 -> "🟩"
            3 -> "🟪"
            4 -> "🟫"
            5 -> "🟧"
            6 -> "⬜"
            7 -> "🟥"
            else -> "⚪"
        }
    }
    
    /**
     * Generate a compact version of the message for SMS or other character-limited platforms
     */
    fun generateCompactMessage(
        memberBreakdowns: List<MainViewModel.MemberBreakdown>,
        finalTotal: Double
    ): String {
        return buildString {
            appendLine("💰 Bill Split (£%.2f total)".format(finalTotal))
            memberBreakdowns.forEach { breakdown ->
                appendLine("${breakdown.memberName}: £%.2f".format(breakdown.finalAmount))
            }
        }
    }
    
    /**
     * Show a preview of the message before sharing
     */
    fun showMessagePreview(
        context: Context,
        billItems: List<BillItem>,
        memberBreakdowns: List<MainViewModel.MemberBreakdown>,
        discountPercentage: Double,
        subtotal: Double,
        discountAmount: Double,
        finalTotal: Double,
        onConfirm: () -> Unit
    ) {
        val formattedMessage = formatBillMessage(
            billItems, memberBreakdowns, discountPercentage,
            subtotal, discountAmount, finalTotal
        )
        
        val dialog = AlertDialog.Builder(context)
            .setTitle("📤 Message Preview")
            .setMessage(formattedMessage)
            .setPositiveButton("Share") { _, _ ->
                onConfirm()
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Copy Message") { _, _ ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Bill Breakdown", formattedMessage)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Message copied to clipboard!", Toast.LENGTH_SHORT).show()
                HapticUtils.successPattern(context)
            }
            .create()
        
        dialog.show()
        
        // Make the message scrollable if it's long
        dialog.findViewById<android.widget.TextView>(android.R.id.message)?.apply {
            movementMethod = ScrollingMovementMethod()
            maxHeight = 600 // Limit height so it doesn't take full screen
        }
    }
}
