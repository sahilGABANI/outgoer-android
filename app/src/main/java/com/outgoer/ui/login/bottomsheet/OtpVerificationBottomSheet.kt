package com.outgoer.ui.login.bottomsheet

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.authentication.model.ResendCodeRequest
import com.outgoer.api.authentication.model.VerifyUserRequest
import com.outgoer.api.post.model.PostMoreOption
import com.outgoer.api.post.model.VerificationSuccess
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.OtpVerificationBottomsheetBinding
import com.outgoer.ui.suggested.SuggestedUsersActivity
import com.outgoer.ui.verification.viewmodel.VerificationViewModel
import com.outgoer.utils.SnackBarUtils.showTopSnackBar
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject

class OtpVerificationBottomSheet : BaseBottomSheetDialogFragment() {

    companion object {
        val TAG: String = "OtpVerificationBottomSheet"
        private const val INTENT_EXTRA_EMAIL_ID = "INTENT_EXTRA_EMAIL_ID"

        @JvmStatic
        fun newInstance(email: String): OtpVerificationBottomSheet {
            var otpVerificationBottomSheet = OtpVerificationBottomSheet()

            val args = Bundle()
            args.putString(INTENT_EXTRA_EMAIL_ID, email)

            otpVerificationBottomSheet.arguments = args

            return otpVerificationBottomSheet
        }
    }

    private var _binding: OtpVerificationBottomsheetBinding? = null
    private val binding get() = _binding!!

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<VerificationViewModel>
    private lateinit var verificationViewModel: VerificationViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var emailId = ""

    private val otpVerificationSuccessSubject: PublishSubject<VerificationSuccess> = PublishSubject.create()
    val otpVerificationSuccessClick: Observable<VerificationSuccess> = otpVerificationSuccessSubject.hide()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)
        OutgoerApplication.component.inject(this)
        verificationViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = OtpVerificationBottomsheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        dialog?.apply {
//            isCancelable = false
//            setCanceledOnTouchOutside(false)
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

        binding.tvResendOTP.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            verificationViewModel.resendCode(ResendCodeRequest(email = emailId))
        }.autoDispose()

        binding.btnVerify.throttleClicks().subscribeAndObserveOnMainThread {
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
                    loggedInUserCache.setVenueRequest(null)
                    verificationViewModel.verifyEmail(
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
        verificationViewModel.verificationState.subscribeAndObserveOnMainThread {
            when (it) {
                is VerificationViewModel.VerificationViewState.ErrorMessage -> {
                    Timber.tag("ErrorMessage").e("VerificationViewState -> it.errorMessage: ${it.errorMessage}")
                    if (it.errorMessage.startsWith("Unable to resolve host")) {
                        showTopSnackBar(requireView())
                    } else {
                        showLongToast(it.errorMessage)
                    }
                    clearCode()
                }
                VerificationViewModel.VerificationViewState.HomePageNavigation -> {
                    otpVerificationSuccessSubject.onNext(VerificationSuccess)

                }
                is VerificationViewModel.VerificationViewState.LoadingState -> {
                    buttonVisibility(it.isLoading)
                }
                is VerificationViewModel.VerificationViewState.SuccessMessage -> {
                    showLongToast(it.successMessage)
                    clearCode()
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
                    binding.tvEmailId.text = emailId
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
            binding.btnVerify.visibility = View.INVISIBLE
        } else {
            binding.progressBar.visibility = View.INVISIBLE
            binding.btnVerify.visibility = View.VISIBLE
        }
    }


    fun dismissBottomSheet() {
        dismiss()
    }
}