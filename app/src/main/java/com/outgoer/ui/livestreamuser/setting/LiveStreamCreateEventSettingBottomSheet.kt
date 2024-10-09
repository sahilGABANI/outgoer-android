package com.outgoer.ui.livestreamuser.setting

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding3.widget.checkedChanges
import com.outgoer.R
import com.outgoer.api.live.model.CreateLiveEventRequest
import com.outgoer.api.live.model.LiveEventInfo
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showToast
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.BottomSheetLiveStreamCreateEventSettingBinding
import com.outgoer.ui.livestreamuser.setting.viewmodel.LiveStreamCreateEventSettingState
import com.outgoer.ui.livestreamuser.setting.viewmodel.LiveStreamCreateEventSettingViewModel
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class LiveStreamCreateEventSettingBottomSheet : BaseBottomSheetDialogFragment() {

    private val liveNowSuccessSubject: PublishSubject<LiveEventInfo> = PublishSubject.create()
    val liveNowSuccess: Observable<LiveEventInfo> = liveNowSuccessSubject.hide()

    private val closeIconClickSubject: PublishSubject<Unit> = PublishSubject.create()
    val closeIconClick: Observable<Unit> = closeIconClickSubject.hide()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<LiveStreamCreateEventSettingViewModel>
    private lateinit var liveStreamCreateEventSettingViewModel: LiveStreamCreateEventSettingViewModel

    private var _binding: BottomSheetLiveStreamCreateEventSettingBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)

        OutgoerApplication.component.inject(this)
        liveStreamCreateEventSettingViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetLiveStreamCreateEventSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        listenToViewModel()
        listenToViewEvent()

        dialog?.apply {
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            setOnKeyListener { _, p1, _ -> p1 == KeyEvent.KEYCODE_BACK }
        }
    }

    private fun listenToViewModel() {
        liveStreamCreateEventSettingViewModel.liveStreamCreateEventSettingStates.subscribeAndObserveOnMainThread {
            when (it) {
                is LiveStreamCreateEventSettingState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is LiveStreamCreateEventSettingState.LoadCreateEventInfo -> {
                    liveNowSuccessSubject.onNext(it.liveEventInfo)
                }
                is LiveStreamCreateEventSettingState.LoadingSettingState -> {
                    buttonVisibility(it.isLoading)
                }
                is LiveStreamCreateEventSettingState.SuccessMessage -> {

                }
            }
        }.autoDispose()
    }

    private fun listenToViewEvent() {
        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            closeIconClickSubject.onNext(Unit)
        }.autoDispose()

        binding.switchMakePrivate.checkedChanges().subscribeAndObserveOnMainThread {
            if (it) {
                binding.rlPassword.visibility = View.VISIBLE
            } else {
                binding.rlPassword.visibility = View.GONE
            }
        }.autoDispose()

        binding.btnCreate.throttleClicks().subscribeAndObserveOnMainThread {
            if (isValidate()) {
                createLiveEvent()
            }
        }.autoDispose()

        binding.btnCancel.throttleClicks().subscribeAndObserveOnMainThread {
            closeIconClickSubject.onNext(Unit)
        }.autoDispose()
    }

    private fun isValidate(): Boolean {
        var isValidate = true
        if (binding.etEventName.text.toString().isEmpty()) {
            showToast(resources.getString(R.string.msg_enter_event_name))
            isValidate = false
        } else if (binding.switchMakePrivate.isChecked) {
            if (binding.etPassword.text.toString().isEmpty()) {
                showToast(resources.getString(R.string.empty_password))
                isValidate = false
            }
        }
        return isValidate
    }

    private fun createLiveEvent() {
        val isLock = if (binding.switchMakePrivate.isChecked) {
            1
        } else {
            0
        }
        liveStreamCreateEventSettingViewModel.createLiveEvent(
            CreateLiveEventRequest(
                eventName = binding.etEventName.text.toString(),
                isLock = isLock,
                password = binding.etPassword.text.toString(),
                inviteIds = ""
            )
        )
    }

    private fun buttonVisibility(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.llBtnCreateCancel.visibility = View.INVISIBLE
        } else {
            binding.progressBar.visibility = View.INVISIBLE
            binding.llBtnCreateCancel.visibility = View.VISIBLE
        }
    }

    fun dismissBottomSheet() {
        dismiss()
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}