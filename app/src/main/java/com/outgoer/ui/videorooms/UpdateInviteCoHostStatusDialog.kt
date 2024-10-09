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
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseDialogFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showLongToast
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.DialogLiveStreamAccpetRejectBinding
import com.outgoer.ui.videorooms.viewmodel.UpdateInviteCoHostStatusViewModel
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class UpdateInviteCoHostStatusDialog(
    private val liveEventInfo: LiveEventInfo
) : BaseDialogFragment() {

    private var _binding: DialogLiveStreamAccpetRejectBinding? = null
    private val binding get() = _binding!!

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<UpdateInviteCoHostStatusViewModel>
    private lateinit var updateInviteCoHostStatusViewModel: UpdateInviteCoHostStatusViewModel

    private val inviteCoHostStatusSubject: PublishSubject<Boolean> = PublishSubject.create()
    val inviteCoHostStatus: Observable<Boolean> = inviteCoHostStatusSubject.hide()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        OutgoerApplication.component.inject(this)
        updateInviteCoHostStatusViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogLiveStreamAccpetRejectBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewEvents()
        listenToViewModel()
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
            .placeholder(R.drawable.ic_chat_user_placeholder)
            .into(binding.ivUserProfile)

        binding.tvUsername.text = liveEventInfo.userName ?: ""

        binding.btnAccept.throttleClicks().subscribeAndObserveOnMainThread {
            inviteCoHostStatusSubject.onNext(true)
        }.autoDispose()

        binding.btnReject.throttleClicks().subscribeAndObserveOnMainThread {
            updateInviteCoHostStatusViewModel.rejectAsCoHost(liveEventInfo.channelId)
        }.autoDispose()
    }


    private fun listenToViewModel() {
        updateInviteCoHostStatusViewModel.updateInviteCoHostStatusStates.subscribeAndObserveOnMainThread {
            when (it) {
                is UpdateInviteCoHostStatusViewModel.UpdateInviteCoHostStatus.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is UpdateInviteCoHostStatusViewModel.UpdateInviteCoHostStatus.LoadingSettingState -> {
                    rejectButtonVisibility(it.isLoading)
                }
                UpdateInviteCoHostStatusViewModel.UpdateInviteCoHostStatus.RejectedCoHostRequest -> {
                    inviteCoHostStatusSubject.onNext(false)
                }
                is UpdateInviteCoHostStatusViewModel.UpdateInviteCoHostStatus.SuccessMessage -> {

                }
            }
        }.autoDispose()
    }

    private fun rejectButtonVisibility(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.llAcceptRejectButtons.visibility = View.INVISIBLE
        } else {
            binding.llAcceptRejectButtons.visibility = View.INVISIBLE
            binding.progressBar.visibility = View.VISIBLE
        }
    }

    fun dismissDialog() {
        dismiss()
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}