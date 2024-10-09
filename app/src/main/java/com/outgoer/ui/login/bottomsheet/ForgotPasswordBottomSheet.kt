package com.outgoer.ui.login.bottomsheet

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.outgoer.R
import com.outgoer.api.authentication.model.ForgotPasswordRequest
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ForgotPasswordBottomsheetBinding
import com.outgoer.ui.login.viewmodel.ForgotPasswordViewModel
import com.outgoer.utils.SnackBarUtils
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject

class ForgotPasswordBottomSheet : BaseBottomSheetDialogFragment() {

    private var _binding: ForgotPasswordBottomsheetBinding? = null
    private val binding get() = _binding!!


    companion object {
        val TAG: String = "ForgotPasswordBottomSheet"

        @JvmStatic
        fun newInstance(): ForgotPasswordBottomSheet {
            return ForgotPasswordBottomSheet()
        }
    }


    private var forgotPasswordClickSubscribe: PublishSubject<String> = PublishSubject.create()
    val forgotPasswordClick: Observable<String> = forgotPasswordClickSubscribe.hide()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ForgotPasswordViewModel>
    private lateinit var forgotPasswordViewModel: ForgotPasswordViewModel

    private val emailPattern = Patterns.EMAIL_ADDRESS


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)
        forgotPasswordViewModel = getViewModelFromFactory(viewModelFactory)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ForgotPasswordBottomsheetBinding.inflate(inflater, container, false)
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

        listenToViewEvents()
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

        binding.btnSend.throttleClicks().subscribeAndObserveOnMainThread {
            if (isValidate()) {
                requireActivity().hideKeyboard()
                forgotPasswordViewModel.forgotPassword(ForgotPasswordRequest(email = binding.etEmailId.text.toString()))
            }
        }.autoDispose()
    }

    private fun listenToViewModel() {
        forgotPasswordViewModel.forgotPasswordState.subscribeAndObserveOnMainThread {
            when (it) {
                is ForgotPasswordViewModel.ForgotPasswordState.ErrorMessage -> {
                    Timber.tag("ErrorMessage").e("ForgotPasswordState -> it.errorMessage: ${it.errorMessage}")
                    if (it.errorMessage.startsWith("Unable to resolve host")) {
                        SnackBarUtils.showTopSnackBar(requireView())
                    } else {
                        showToast(it.errorMessage)
                    }
                }
                is ForgotPasswordViewModel.ForgotPasswordState.LoadingState -> {
                    buttonVisibility(it.isLoading)
                }
                is ForgotPasswordViewModel.ForgotPasswordState.SuccessMessage -> {
                    showToast(it.successMessage)
                    dismiss()
                    forgotPasswordClickSubscribe.onNext(binding.etEmailId.text.toString())

                }
            }
        }.autoDispose()
    }

    private fun isValidate(): Boolean {
        var isValidate = true
        if (binding.etEmailId.text.toString().isEmpty()) {
            showToast(resources.getString(R.string.empty_email))
            isValidate = false
        } else if (!emailPattern.matcher(binding.etEmailId.text.toString()).matches()) {
            showToast(resources.getString(R.string.invalid_email))
            isValidate = false
        }
        return isValidate
    }

    private fun buttonVisibility(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.btnSend.visibility = View.INVISIBLE
        } else {
            binding.progressBar.visibility = View.INVISIBLE
            binding.btnSend.visibility = View.VISIBLE
        }
    }
}