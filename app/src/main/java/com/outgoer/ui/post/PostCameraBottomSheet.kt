package com.outgoer.ui.post

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.outgoer.R
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.BottomSheetPostCameraBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class PostCameraBottomSheet : BaseBottomSheetDialogFragment() {

    private val postCameraItemClicksSubject: PublishSubject<Boolean> = PublishSubject.create()
    val postCameraItemClicks: Observable<Boolean> = postCameraItemClicksSubject.hide()

    private var _binding: BottomSheetPostCameraBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun create(): PostCameraBottomSheet {
            return PostCameraBottomSheet()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_post_camera, container, false)
        _binding = BottomSheetPostCameraBinding.bind(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewEvents()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val bottomSheet = (view?.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun listenToViewEvents() {
        binding.tvCapturePhoto.throttleClicks().subscribeAndObserveOnMainThread {
            postCameraItemClicksSubject.onNext(true)
            dismiss()
        }.autoDispose()

        binding.tvRecordVideo.throttleClicks().subscribeAndObserveOnMainThread {
            postCameraItemClicksSubject.onNext(false)
            dismiss()
        }.autoDispose()

        binding.tvCancel.throttleClicks().subscribeAndObserveOnMainThread {
            dismissBottomSheet()
        }.autoDispose()
    }

    fun dismissBottomSheet() {
        dismiss()
    }
}