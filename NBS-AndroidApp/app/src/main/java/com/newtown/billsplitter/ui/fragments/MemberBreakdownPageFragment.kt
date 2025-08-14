package com.newtown.billsplitter.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.newtown.billsplitter.databinding.ItemMemberBreakdownBinding

class MemberBreakdownPageFragment : Fragment() {
    private var _binding: ItemMemberBreakdownBinding? = null
    private val binding get() = _binding!!

    private lateinit var memberName: String
    private var subtotal: Double = 0.0
    private var discount: Double = 0.0
    private var finalAmount: Double = 0.0
    private lateinit var itemsText: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            memberName = args.getString("memberName").orEmpty()
            subtotal = args.getDouble("subtotal")
            discount = args.getDouble("discount")
            finalAmount = args.getDouble("finalAmount")
            itemsText = args.getString("itemsText").orEmpty()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ItemMemberBreakdownBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.memberName.text = memberName
        binding.subtotalText.text = "Subtotal: £%.2f".format(subtotal)
        binding.discountText.text = "Discount: -£%.2f".format(discount)
        binding.finalAmountText.text = "Final: £%.2f".format(finalAmount)
        binding.itemsText.text = "Items: $itemsText"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(
            memberName: String,
            subtotal: Double,
            discount: Double,
            finalAmount: Double,
            itemsText: String
        ): MemberBreakdownPageFragment {
            val f = MemberBreakdownPageFragment()
            val b = Bundle()
            b.putString("memberName", memberName)
            b.putDouble("subtotal", subtotal)
            b.putDouble("discount", discount)
            b.putDouble("finalAmount", finalAmount)
            b.putString("itemsText", itemsText)
            f.arguments = b
            return f
        }
    }
}

