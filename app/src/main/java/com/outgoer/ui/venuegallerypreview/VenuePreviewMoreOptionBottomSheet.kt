package com.outgoer.ui.venuegallerypreview


import android.app.AlertDialog
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.outgoer.R
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.VenuePreviewMoreOptionBottomSheetBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class VenuePreviewMoreOptionBottomSheet : BaseBottomSheetDialogFragment() {

    private val moreOptionItemClickSubject: PublishSubject<Unit> = PublishSubject.create()
    val moreOptionItemClick: Observable<Unit> = moreOptionItemClickSubject.hide()

    private var _binding: VenuePreviewMoreOptionBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = VenuePreviewMoreOptionBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        listenToViewEvents()
    }

    private fun listenToViewEvents() {
        binding.tvDelete.throttleClicks().subscribeAndObserveOnMainThread {
           openDeletePopup()
            dismissBottomSheet()
        }.autoDispose()

        binding.tvCancel.throttleClicks().subscribeAndObserveOnMainThread {
            dismissBottomSheet()
        }.autoDispose()
    }

    fun dismissBottomSheet() {
        dismiss()
    }
    private fun openDeletePopup() {
        val builder = AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialogCustom))
        builder.setTitle(getString(R.string.label_delete_))
        builder.setMessage(getString(R.string.label_are_you_sure_you_want_to_delete))
        builder.setPositiveButton(getString(R.string.delete)) { dialogInterface, which ->
            moreOptionItemClickSubject.onNext(Unit)
            dialogInterface.dismiss()
        }
        builder.setNeutralButton(getString(R.string.label_cancel)) { dialogInterface, _ ->
            dialogInterface.dismiss()
        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }
}