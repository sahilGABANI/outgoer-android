package com.outgoer.ui.home.newmap.venuemap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import com.outgoer.R
import com.outgoer.base.BaseDialogFragment
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.LocationPermissionDisclousreBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class LocationPermissions : BaseDialogFragment() {

    private val locationStateSubject: PublishSubject<String> = PublishSubject.create()
    val locationState: Observable<String> = locationStateSubject.hide()

    companion object {

        fun newInstance(): LocationPermissions {
            return LocationPermissions()
        }
    }

    private var _binding: LocationPermissionDisclousreBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = LocationPermissionDisclousreBinding.inflate(inflater, container, false)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewEvent()
    }

    private fun listenToViewEvent() {
        binding.cancelAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }.autoDispose()

        binding.okayAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            locationStateSubject.onNext("done")
            dismiss()
        }.autoDispose()

    }
}