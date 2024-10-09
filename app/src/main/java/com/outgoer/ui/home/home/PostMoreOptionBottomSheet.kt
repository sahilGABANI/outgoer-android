package com.outgoer.ui.home.home

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.outgoer.R
import com.outgoer.api.chat.model.ChatConversationInfo
import com.outgoer.api.post.model.DismissBottomSheet
import com.outgoer.api.post.model.PostMoreOption
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.PostMoreOptionBottomSheetBinding
import com.outgoer.ui.createevent.EventCategoryBottomSheet
import com.outgoer.ui.group.editgroup.EditAdminGroupBottomSheet
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class PostMoreOptionBottomSheet() : BaseBottomSheetDialogFragment() {

    companion object {
        const val SHOW_REPORT = "SHOW_REPORT"
        const val SHOW_PROFILE_OPTIONS = "SHOW_PROFILE_OPTIONS"
        const val HIDE_BLOCK_BUTTON = "HIDE_BLOCK_BUTTON"

        @JvmStatic
        fun newInstance(showReport: Boolean): PostMoreOptionBottomSheet {
            val postMoreOptionBottomSheet = PostMoreOptionBottomSheet()
            val bundle = Bundle()
            bundle.putBoolean(SHOW_REPORT, showReport)
            postMoreOptionBottomSheet.arguments = bundle
            return postMoreOptionBottomSheet
        }

        @JvmStatic
        fun newInstanceWithData(profileOptions: Boolean, isBlockVisible: Boolean = false): PostMoreOptionBottomSheet {
            val postMoreOptionBottomSheet = PostMoreOptionBottomSheet()
            val bundle = Bundle()
            bundle.putBoolean(SHOW_PROFILE_OPTIONS, profileOptions)
            if(isBlockVisible)
                bundle.putBoolean(HIDE_BLOCK_BUTTON, isBlockVisible)
            postMoreOptionBottomSheet.arguments = bundle
            return postMoreOptionBottomSheet
        }
    }

    private val postMoreOptionClickSubject: PublishSubject<PostMoreOption> = PublishSubject.create()
    val postMoreOptionClick: Observable<PostMoreOption> = postMoreOptionClickSubject.hide()

    private var _binding: PostMoreOptionBottomSheetBinding? = null
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
        val view = inflater.inflate(R.layout.post_more_option_bottom_sheet, container, false)
        _binding = PostMoreOptionBottomSheetBinding.bind(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.background = ContextCompat.getDrawable(requireContext(), R.drawable.login_bottom_sheet_background)

        arguments?.let {
            if(it.getBoolean(SHOW_PROFILE_OPTIONS)) {
                binding.moreProfile.isVisible = true
                binding.deleteLinearLayout.isVisible = false
            } else {
                binding.moreProfile.isVisible = false
                binding.deleteLinearLayout.isVisible = true

                if(it.getBoolean(SHOW_REPORT)) {
                    binding.tvReport.isVisible = true
                    binding.viewReport.isVisible = true
                } else {
                    binding.tvDelete.isVisible = true
                    binding.viewDelete.isVisible = true
                }
            }

            if(it.getBoolean(HIDE_BLOCK_BUTTON)) {
                binding.blockAppCompatTextView.isVisible = false
                binding.blockView.isVisible = false
            } else {
                binding.blockAppCompatTextView.isVisible = true
                binding.blockView.isVisible = true
            }
        }

        dialog?.apply {
            val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = 0
            behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
        }

        listenToViewEvents()
    }

    private val mBottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    postMoreOptionClickSubject.onNext(PostMoreOption.DismissClick)
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        }

    private fun listenToViewEvents() {
        binding.blockAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            postMoreOptionClickSubject.onNext(PostMoreOption.BlockClick)
        }

        binding.reportAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            postMoreOptionClickSubject.onNext(PostMoreOption.ReportClick)
        }

        binding.cancelAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            postMoreOptionClickSubject.onNext(PostMoreOption.DismissClick)
        }

        binding.tvDelete.throttleClicks().subscribeAndObserveOnMainThread {
            openDeletePopup()
            postMoreOptionClickSubject.onNext(PostMoreOption.DismissClick)
        }.autoDispose()

        binding.tvCancel.throttleClicks().subscribeAndObserveOnMainThread {
            postMoreOptionClickSubject.onNext(PostMoreOption.DismissClick)
        }.autoDispose()

        binding.tvReport.throttleClicks().subscribeAndObserveOnMainThread {
            postMoreOptionClickSubject.onNext(PostMoreOption.ReportClick)
        }.autoDispose()
    }

    private fun openDeletePopup() {
//        val builder = AlertDialog.Builder(context)
        val builder = AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialogCustom))
        builder.setTitle(getString(R.string.label_delete_))
        builder.setMessage(getString(R.string.label_are_you_sure_you_want_to_delete_post))
        builder.setPositiveButton(getString(R.string.delete)) { dialogInterface, which ->
            postMoreOptionClickSubject.onNext(PostMoreOption.DeleteClick)
            dialogInterface.dismiss()
        }
        builder.setNeutralButton(getString(R.string.label_cancel)) { dialogInterface, _ ->
            dialogInterface.dismiss()
        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    fun dismissBottomSheet() {
        dismiss()
    }
}