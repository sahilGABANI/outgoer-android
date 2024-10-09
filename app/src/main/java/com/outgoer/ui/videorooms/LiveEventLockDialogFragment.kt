package com.outgoer.ui.videorooms

import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.live.model.LiveEventInfo
import com.outgoer.api.live.model.LiveEventVerifyRequest
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseDialogFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.DialogLiveStreamEnterPasswordBinding
import com.outgoer.ui.videorooms.viewmodel.LiveEventVerifyViewModel
import com.outgoer.utils.UiUtils
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class LiveEventLockDialogFragment(
    private val liveEventInfo: LiveEventInfo
) : BaseDialogFragment() {

    private var _binding: DialogLiveStreamEnterPasswordBinding? = null
    private val binding get() = _binding!!

    private val verifySubject: PublishSubject<Unit> = PublishSubject.create()
    val verify: Observable<Unit> = verifySubject.hide()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<LiveEventVerifyViewModel>
    private lateinit var liveEventVerifyViewModel: LiveEventVerifyViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        OutgoerApplication.component.inject(this)
        liveEventVerifyViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogLiveStreamEnterPasswordBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewEvents()
        listenToViewModel()
    }

    private fun listenToViewModel() {
        liveEventVerifyViewModel.liveEventVerifyStates.subscribeAndObserveOnMainThread {
            when (it) {
                is LiveEventVerifyViewModel.LiveEventVerifyState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is LiveEventVerifyViewModel.LiveEventVerifyState.LoadingState -> {
                    buttonVisibility(it.isLoading)
                }
                is LiveEventVerifyViewModel.LiveEventVerifyState.SuccessMessage -> {
                    verifySubject.onNext(Unit)
                }
            }
        }.autoDispose()
    }

    private fun buttonVisibility(isLoading: Boolean) {
        binding.apply {
            if (isLoading) {
                progressBar.visibility = View.VISIBLE
                btnAccept.visibility = View.GONE
            } else {
                progressBar.visibility = View.GONE
                btnAccept.visibility = View.VISIBLE
            }
        }
    }

    private fun DialogFragment.setWidthHeightPercent(percentageWidthInt: Int) {
        val percentageWidth = percentageWidthInt.toFloat() / 100
        val dm = resources.displayMetrics
        val rect = dm.run { Rect(0, 0, widthPixels, heightPixels) }
        val percentWidth = rect.width() * percentageWidth
        dialog?.window?.setLayout(percentWidth.toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setWidthHeightPercent(90)
    }

    private fun listenToViewEvents() {
        Glide.with(requireContext())
            .load(liveEventInfo.profileUrl ?: "")
            .placeholder(R.drawable.venue_placeholder)
            .into(binding.ivUserProfile)

        binding.tvUsername.text = liveEventInfo.userName ?: ""

        binding.btnAccept.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.etPassword.text.toString().isNotEmpty()) {
                UiUtils.hideKeyboard(requireContext())
                liveEventVerifyViewModel.verifyEvent(
                    LiveEventVerifyRequest(
                        password = binding.etPassword.text.toString(),
                        channelId = liveEventInfo.channelId
                    )
                )
            } else {
                showToast(getString(R.string.empty_password))
            }
        }.autoDispose()
    }
}