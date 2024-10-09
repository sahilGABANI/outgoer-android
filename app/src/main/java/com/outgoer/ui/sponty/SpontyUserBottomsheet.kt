package com.outgoer.ui.sponty

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.outgoer.R
import com.outgoer.api.sponty.model.SpontyJoins
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.databinding.SpontyUserBottomsheetBinding
import com.outgoer.ui.sponty.view.SpontyUserAdapter

class SpontyUserBottomsheet: BaseBottomSheetDialogFragment() {

    companion object {
        val TAG: String = "SpontyUserBottomsheet"
        val LIST_OF_JOINS: String = "LIST_OF_JOINS"

        @JvmStatic
        fun newInstance(listofjoins: ArrayList<SpontyJoins>): SpontyUserBottomsheet {
            val spontyUserBottomsheet = SpontyUserBottomsheet()
            val bundle = Bundle()
            bundle.putParcelableArrayList(LIST_OF_JOINS, listofjoins)
            spontyUserBottomsheet.arguments = bundle

            return  spontyUserBottomsheet
        }
    }

    private var _binding: SpontyUserBottomsheetBinding? = null
    private val binding get() = _binding!!

    private lateinit var spontyUserAdapter: SpontyUserAdapter

    private var listofspontyusers: ArrayList<SpontyJoins> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SpontyUserBottomsheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        dialog?.apply {
            val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = 0
            behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
        }

        listenToViewEvents()
    }

    private fun listenToViewEvents() {
        spontyUserAdapter = SpontyUserAdapter(requireContext()).apply {
            spontyUserActionState.subscribeAndObserveOnMainThread {

            }
        }

        binding.userRecyclerView.apply {
            adapter = spontyUserAdapter
        }

        arguments?.let {
            listofspontyusers = it.getParcelableArrayList<SpontyJoins>(LIST_OF_JOINS) as ArrayList<SpontyJoins>
            spontyUserAdapter.listOfSpontyUser = listofspontyusers
        }
    }

    private val mBottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismiss()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        }




    fun dismissBottomSheet() {
        dismiss()
    }
}