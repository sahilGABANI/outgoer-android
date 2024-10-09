package com.outgoer.ui.post

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.outgoer.R
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.BottomSheetPostCameraBinding
import com.outgoer.databinding.ImagePickerBottomSheetBinding
import com.outgoer.ui.home.search.place.SearchPlacesFragment
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject


class ImagePickerBottomSheet : BaseBottomSheetDialogFragment() {

    private val postCameraItemClicksSubject: PublishSubject<Boolean> = PublishSubject.create()
    val postCameraItemClicks: Observable<Boolean> = postCameraItemClicksSubject.hide()

    private var _binding: ImagePickerBottomSheetBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun getInstance(): ImagePickerBottomSheet {
            return ImagePickerBottomSheet()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.BSDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.image_picker_bottom_sheet, container, false)
        _binding = ImagePickerBottomSheetBinding.bind(view)
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
        binding.tvTakePhoto.throttleClicks().subscribeAndObserveOnMainThread {
            postCameraItemClicksSubject.onNext(true)
        }.autoDispose()

        binding.tvChooseFromGallery.throttleClicks().subscribeAndObserveOnMainThread {
            postCameraItemClicksSubject.onNext(false)
        }.autoDispose()

        binding.tvCancel.throttleClicks().subscribeAndObserveOnMainThread {
            dismissBottomSheet()
        }.autoDispose()
    }

    fun dismissBottomSheet() {
        dismiss()
    }

}