package com.outgoer.ui.home.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.outgoer.R
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.AddToStoryBottomSheetBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class AddToYourStoryBottomSheet : BaseBottomSheetDialogFragment() {

    companion object {
        const val SHOW_REPORT = "SHOW_REPORT"

        @JvmStatic
        fun newInstance(): AddToYourStoryBottomSheet = AddToYourStoryBottomSheet()
    }

    private val addToStoryOptionClickSubject: PublishSubject<String> = PublishSubject.create()
    val addToStoryOptionClick: Observable<String> = addToStoryOptionClickSubject.hide()


    private var _binding: AddToStoryBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.add_to_story_bottom_sheet, container, false)
        _binding = AddToStoryBottomSheetBinding.bind(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.apply {
            val bottomSheetDialog = this as BottomSheetDialog
            val bottomSheet =
                bottomSheetDialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = 0
                behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
            }
        }

        listenToViewEvents()
    }


    private val mBottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    addToStoryOptionClickSubject.onNext("")
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        }

    private fun listenToViewEvents() {

        binding.tvCancel.throttleClicks().subscribeAndObserveOnMainThread {
            dismissBottomSheet()
        }.autoDispose()

        binding.addToStoryAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            addToStoryOptionClickSubject.onNext("")

        }.autoDispose()
    }

    fun dismissBottomSheet() {
        dismiss()
    }
}