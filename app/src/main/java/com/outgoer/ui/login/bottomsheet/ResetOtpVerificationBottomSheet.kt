package com.outgoer.ui.login.bottomsheet

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.outgoer.R
import com.outgoer.api.authentication.model.ResendCodeRequest
import com.outgoer.api.authentication.model.VerifyUserRequest
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.VerifyResetPasswordBinding
import com.outgoer.ui.login.viewmodel.VerifyResetPasswordViewModel
import com.outgoer.utils.SnackBarUtils
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject

class ResetOtpVerificationBottomSheet : BaseBottomSheetDialogFragment() {

    private var _binding: VerifyResetPasswordBinding? = null
    private val binding get() = _binding!!

    companion object {
        val TAG: String = "OtpVerificationBottomSheet"
        private const val INTENT_EXTRA_EMAIL_ID = "INTENT_EXTRA_EMAIL_ID"

        @JvmStatic
        fun newInstance(email: String): ResetOtpVerificationBottomSheet {
            var otpVerificationBottomSheet = ResetOtpVerificationBottomSheet()

            val args = Bundle()
            args.putString(INTENT_EXTRA_EMAIL_ID, email)

            otpVerificationBottomSheet.arguments = args

            return otpVerificationBottomSheet
        }
    }

    private var otpVerifyClickSubscribe: PublishSubject<String> = PublishSubject.create()
    val otpVerifyClick: Observable<String> = otpVerifyClickSubscribe.hide()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<VerifyResetPasswordViewModel>
    private lateinit var verifyResetPasswordViewModel: VerifyResetPasswordViewModel

    private var emailId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)
        OutgoerApplication.component.inject(this)
        verifyResetPasswordViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = VerifyResetPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        dialog?.apply {
            val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = 0
            behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
        }

        loadDataFromIntent()
        listenToViewModel()
    }

    private val mBottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismiss()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        }


    private fun listenToViewEvents() {

        binding.tvSendAgain.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            verifyResetPasswordViewModel.resendCode(ResendCodeRequest(email = emailId))
        }.autoDispose()

        binding.btnContinue.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            val confirmationCode = binding.otpView.text
            when {
                confirmationCode.isNullOrEmpty() -> {
                    showToast(getString(R.string.msg_enter_otp))
                }
                confirmationCode.length != 4 -> {
                    showToast(getString(R.string.msg_invalid_otp))
                }
                else -> {
                    verifyResetPasswordViewModel.forgotPasswordVerifyCode(
                        VerifyUserRequest(
                            email = emailId,
                            verificationCode = confirmationCode.toString()
                        )
                    )
                }
            }
        }.autoDispose()
    }

    private fun listenToViewModel() {
        verifyResetPasswordViewModel.verifyResetPasswordState.subscribeAndObserveOnMainThread {
            when (it) {
                is VerifyResetPasswordViewModel.VerifyResetPasswordState.ErrorMessage -> {
                    Timber.tag("ErrorMessage").e("VerifyResetPasswordState -> it.errorMessage: ${it.errorMessage}")
                    if (it.errorMessage.startsWith("Unable to resolve host")) {
                        SnackBarUtils.showTopSnackBar(requireView())
                    } else {
                        showToast(it.errorMessage)
                    }
                    clearCode()
                }
                is VerifyResetPasswordViewModel.VerifyResetPasswordState.LoadingState -> {
                    buttonVisibility(it.isLoading)
                }
                is VerifyResetPasswordViewModel.VerifyResetPasswordState.SuccessMessage -> {
                    showToast(it.successMessage)
                    clearCode()
                }
                VerifyResetPasswordViewModel.VerifyResetPasswordState.ResetPasswordPage -> {
                    dismiss()
                    otpVerifyClickSubscribe.onNext(emailId)
                }
            }
        }.autoDispose()
    }

    private fun loadDataFromIntent() {
        arguments?.let {
            if (it.getString(INTENT_EXTRA_EMAIL_ID) != null) {
                val emailId = it.getString(INTENT_EXTRA_EMAIL_ID)
                if (emailId != null && emailId.isNotEmpty()) {
                    this.emailId = emailId
                    listenToViewEvents()
                } else {
                    dismissBottomSheet()
                }
            } else {
                dismissBottomSheet()
            }
        } ?: dismissBottomSheet()
    }

    private fun clearCode() {
        binding.otpView.text?.clear()
        binding.otpView.isCursorVisible = true
    }

    private fun buttonVisibility(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.btnContinue.visibility = View.INVISIBLE
        } else {
            binding.progressBar.visibility = View.INVISIBLE
            binding.btnContinue.visibility = View.VISIBLE
        }
    }

    fun dismissBottomSheet() {
        dismiss()
    }
}