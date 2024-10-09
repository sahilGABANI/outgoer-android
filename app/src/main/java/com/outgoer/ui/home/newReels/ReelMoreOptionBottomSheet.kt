package com.outgoer.ui.home.newReels


import android.app.AlertDialog
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.outgoer.R
import com.outgoer.api.post.model.PostMoreOption
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ReelMoreOptionBottomSheetBinding
import com.outgoer.ui.home.home.PostMoreOptionBottomSheet
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ReelMoreOptionBottomSheet : BaseBottomSheetDialogFragment() {

    companion object {
        const val SHOW_REPORT = "SHOW_REPORT"

        @JvmStatic
        fun newInstance(showReport: Boolean): ReelMoreOptionBottomSheet {
            var reelMoreOptionBottomSheet = ReelMoreOptionBottomSheet()

            var bundle = Bundle()
            bundle.putBoolean(SHOW_REPORT, showReport)
            reelMoreOptionBottomSheet.arguments = bundle
            return reelMoreOptionBottomSheet
        }
    }

    private val reelMoreOptionClickSubject: PublishSubject<PostMoreOption> = PublishSubject.create()
    val reelMoreOptionClick: Observable<PostMoreOption> = reelMoreOptionClickSubject.hide()

    private var _binding: ReelMoreOptionBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.reel_more_option_bottom_sheet, container, false)
        _binding = ReelMoreOptionBottomSheetBinding.bind(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        arguments?.let {
            if(it.getBoolean(SHOW_REPORT)) {
                binding.tvReport.isVisible = true
                binding.viewReport.isVisible = true
            } else {
                binding.tvDelete.isVisible = true
                binding.viewDelete.isVisible = true

            }
        }

        listenToViewEvents()
    }

    private fun listenToViewEvents() {
        binding.tvDelete.throttleClicks().subscribeAndObserveOnMainThread {
//            reelMoreOptionClickSubject.onNext(Unit)
            openDeletePopup()
            reelMoreOptionClickSubject.onNext(PostMoreOption.DismissClick)
        }.autoDispose()

        binding.tvCancel.throttleClicks().subscribeAndObserveOnMainThread {
            reelMoreOptionClickSubject.onNext(PostMoreOption.DismissClick)
        }.autoDispose()

        binding.tvReport.throttleClicks().subscribeAndObserveOnMainThread {
            reelMoreOptionClickSubject.onNext(PostMoreOption.ReportClick)
        }.autoDispose()
    }
    private fun openDeletePopup() {
//        val builder = AlertDialog.Builder(context)
        val builder = AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialogCustom))
        builder.setTitle(getString(R.string.label_delete_))
        builder.setMessage(getString(R.string.label_are_you_sure_you_want_to_delete_reels))
        builder.setPositiveButton(getString(R.string.delete)) { dialogInterface, which ->
            reelMoreOptionClickSubject.onNext(PostMoreOption.DeleteClick)
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