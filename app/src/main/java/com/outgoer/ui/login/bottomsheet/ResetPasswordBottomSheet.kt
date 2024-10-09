package com.outgoer.ui.login.bottomsheet

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.outgoer.R
import com.outgoer.api.authentication.model.ResetPasswordRequest
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ResetPasswordBottomsheetBinding
import com.outgoer.ui.login.viewmodel.ResetPasswordViewModel
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class ResetPasswordBottomSheet : BaseBottomSheetDialogFragment() {

    companion object {
        val TAG: String = "OtpVerificationBottomSheet"
        private const val INTENT_EXTRA_EMAIL_ID = "INTENT_EXTRA_EMAIL_ID"

        @JvmStatic
        fun newInstance(email: String): ResetPasswordBottomSheet {
            var resetPasswordBottomSheet = ResetPasswordBottomSheet()

            val args = Bundle()
            args.putString(INTENT_EXTRA_EMAIL_ID, email)

            resetPasswordBottomSheet.arguments = args

            return resetPasswordBottomSheet
        }
    }

    private var resetPasswordClickSubscribe: PublishSubject<String> = PublishSubject.create()
    val resetPasswordClick: Observable<String> = resetPasswordClickSubscribe.hide()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ResetPasswordViewModel>
    private lateinit var resetPasswordViewModel: ResetPasswordViewModel

    private var _binding: ResetPasswordBottomsheetBinding? = null
    private val binding get() = _binding!!

    private var emailId = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)
        OutgoerApplication.component.inject(this)
        resetPasswordViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ResetPasswordBottomsheetBinding.inflate(inflater, container, false)
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
        binding.btnReset.throttleClicks().subscribeAndObserveOnMainThread {
            if (isValidate()) {
                requireActivity().hideKeyboard()
                resetPasswordViewModel.resetPassword(
                    ResetPasswordRequest(
                        email = emailId,
                        password = binding.etPassword.text.toString()
                    )
                )
            }
        }.autoDispose()
    }

    private fun listenToViewModel() {
        resetPasswordViewModel.resetPasswordState.subscribeAndObserveOnMainThread {
            when (it) {
                is ResetPasswordViewModel.ResetPasswordState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is ResetPasswordViewModel.ResetPasswordState.LoadingState -> {
                    buttonVisibility(it.isLoading)
                }
                is ResetPasswordViewModel.ResetPasswordState.SuccessMessage -> {
                    showToast(it.successMessage)
                    dismissBottomSheet()
                    resetPasswordClickSubscribe.onNext(resources.getString(R.string.label_forgot_password_b))
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

    private fun isValidate(): Boolean {
        var isValidate = true
        when {
            binding.etPassword.text.isNullOrEmpty() -> {
                showToast(resources.getString(R.string.empty_password))
                isValidate = false
            }
            binding.etPassword.text.toString().length < 8 -> {
                showToast(resources.getString(R.string.password_minimum_length))
                isValidate = false
            }
            binding.etConfirmPassword.text.isNullOrEmpty() -> {
                showToast(resources.getString(R.string.confirm_your_password))
                isValidate = false
            }
            binding.etPassword.text.toString() != binding.etConfirmPassword.text.toString() -> {
                showToast(resources.getString(R.string.msg_password_should_match))
                isValidate = false
            }
        }
        return isValidate
    }

    private fun buttonVisibility(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.btnReset.visibility = View.INVISIBLE
        } else {
            binding.progressBar.visibility = View.INVISIBLE
            binding.btnReset.visibility = View.VISIBLE
        }
    }

    fun dismissBottomSheet() {
        dismiss()
    }
}