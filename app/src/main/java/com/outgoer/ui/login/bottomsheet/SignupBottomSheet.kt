package com.outgoer.ui.login.bottomsheet

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.util.Linkify
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.jakewharton.rxbinding3.widget.textChanges
import com.outgoer.R
import com.outgoer.api.authentication.model.RegisterRequest
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.hideKeyboard
import com.outgoer.base.extension.showLongToast
import com.outgoer.base.extension.showToast
import com.outgoer.base.extension.startActivityWithDefaultAnimation
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.SignUpBottomsheetBinding
import com.outgoer.mediapicker.constants.BaseConstants
import com.outgoer.ui.activateaccount.ActivateAccountActivity
import com.outgoer.ui.register.viewmodel.RegisterViewModel
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.inject.Inject

class SignupBottomSheet : BaseBottomSheetDialogFragment() {

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<RegisterViewModel>
    private lateinit var registerViewModel: RegisterViewModel

    private var _binding: SignUpBottomsheetBinding? = null
    private val binding get() = _binding!!

    private val emailPattern = Patterns.EMAIL_ADDRESS
    private var isValidUsername = true

    private var loginClickSubscribe: PublishSubject<String> = PublishSubject.create()
    val loginClicks: Observable<String> = loginClickSubscribe.hide()
    private var fusedLocationClient: FusedLocationProviderClient? = null

    companion object {
        val TAG: String = "SignupBottomSheet"

        @JvmStatic
        fun newInstance(): SignupBottomSheet {
            return SignupBottomSheet()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)
        registerViewModel = getViewModelFromFactory(viewModelFactory)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SignUpBottomsheetBinding.inflate(inflater, container, false)
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


        val termsConditionsText = getString(R.string.text_terms_conditions)
        val privacyPolicyText = getString(R.string.text_privacy_policy)
        val userAgreementText = getString(R.string.text_user_agreement)
        val legalText = getString(R.string.account_create_info, userAgreementText, privacyPolicyText, termsConditionsText)
        binding.agreeTermsCheckBox.setText(legalText)
        Linkify.addLinks(
            binding.agreeTermsCheckBox,
            Pattern.compile(userAgreementText),
            null,
            null,
            { match, url -> BaseConstants.USER_AGREEMENT }
        )

        Linkify.addLinks(
            binding.agreeTermsCheckBox,
            Pattern.compile(termsConditionsText),
            null,
            null,
            { match, url -> BaseConstants.TERMS_N_CONDITIONS }
        )
        Linkify.addLinks(
            binding.agreeTermsCheckBox,
            Pattern.compile(privacyPolicyText),
            null,
            null,
            { match, url -> BaseConstants.PRIVACY_POLICY }
        )

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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        binding.etUsername.textChanges()
            .skipInitialValue()
            .debounce(500, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeOnIoAndObserveOnMainThread({
                if (it.length > 3) {
                    registerViewModel.checkUsername(it.toString())
                } else {
                    binding.progressBarUsername.visibility = View.GONE
                    binding.ivUsername.visibility = View.GONE
                }
            }, {
                Timber.e(it)
            }).autoDispose()


        binding.etConfirmPassword.textChanges()
            .debounce(400, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeAndObserveOnMainThread {
                if(!binding.etConfirmPassword.text.isNullOrEmpty() && binding.etConfirmPassword.text.toString().equals(binding.etPassword.text.toString())) {
                    binding.samePwdAppCompatTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.validation_check, 0, 0, 0);
                } else {
                    binding.samePwdAppCompatTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.bg_validation_password, 0, 0, 0);
                }
            }.autoDispose()

        binding.etPassword.textChanges()
            .debounce(400, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeAndObserveOnMainThread {
                if(binding.etPassword.text.toString().length >= 8) {
                    binding.minCharsAppCompatTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.validation_check, 0, 0, 0);
                } else {
                    binding.minCharsAppCompatTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.bg_validation_password, 0, 0, 0);
                }

                if(binding.etPassword.text.toString().contains("[A-Z]".toRegex())) {
                    binding.upperCaseAppCompatTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.validation_check, 0, 0, 0);
                } else {
                    binding.upperCaseAppCompatTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.bg_validation_password, 0, 0, 0);
                }

                if(binding.etPassword.text.toString().contains("[0-9]".toRegex())) {
                    binding.numberAppCompatTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.validation_check, 0, 0, 0);
                } else {
                    binding.numberAppCompatTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.bg_validation_password, 0, 0, 0);
                }

                val special = Pattern.compile("[!@#$%&*()_+=|<>?{}\\[\\]~-]")

                if(special.matcher(binding.etPassword.text.toString()).find()) {
                    binding.specialCharCaseAppCompatTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.validation_check, 0, 0, 0);
                } else {
                    binding.specialCharCaseAppCompatTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.bg_validation_password, 0, 0, 0);
                }
            }.autoDispose()

        binding.btnSignUp.throttleClicks().subscribeAndObserveOnMainThread {
            if (isValidate()) {
                XXPermissions.with(this)
                    .permission(Permission.ACCESS_COARSE_LOCATION)
                    .permission(Permission.ACCESS_FINE_LOCATION)
                    .request(object : OnPermissionCallback {
                        @SuppressLint("MissingPermission")
                        override fun onGranted(permissions: List<String>, all: Boolean) {
                            var task = fusedLocationClient?.lastLocation
                            var latitude = 0.0
                            var longitude = 0.0

                            task?.addOnSuccessListener { location ->
                                latitude = location?.latitude ?: 0.0
                                longitude = location?.longitude ?: 0.0

                                requireActivity().hideKeyboard()
                                registerViewModel.register(
                                    RegisterRequest(
                                        name = binding.etName.text.toString(),
                                        username = binding.etUsername.text.toString(),
                                        email = binding.etEmailId.text.toString(),
                                        password = binding.etPassword.text.toString(),
                                        latitude = latitude,
                                        longitude = longitude
                                    )
                                )
                            }
                        }

                        override fun onDenied(permissions: List<String>, never: Boolean) {

                            requireActivity().hideKeyboard()
                            val name = binding.etName.text.toString()
                            val username = binding.etUsername.text.toString()
                            val emailId = binding.etEmailId.text.toString()
                            val password = binding.etPassword.text.toString()
                            registerViewModel.register(
                                RegisterRequest(
                                    name = name,
                                    username = username,
                                    email = emailId,
                                    password = password
                                )
                            )
                        }
                    })

            }
        }.autoDispose()
    }

    private fun listenToViewModel() {
        registerViewModel.registerState.subscribeAndObserveOnMainThread {
            when (it) {
                is RegisterViewModel.RegisterViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is RegisterViewModel.RegisterViewState.LoadingState -> {
                    buttonVisibility(it.isLoading)
                }
                is RegisterViewModel.RegisterViewState.SuccessMessage -> {
                    showLongToast(it.successMessage)
                }
                is RegisterViewModel.RegisterViewState.VerificationNavigation -> {
                    dismissBottomSheet()
                    loginClickSubscribe.onNext(binding.etEmailId.text.toString())
                }
                is RegisterViewModel.RegisterViewState.DeactivateAccount -> {
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setMessage(it.deactivateMessage)
                    builder.setPositiveButton(getString(R.string.label_contact_support)) { _, _ ->
                        startActivityWithDefaultAnimation(
                            ActivateAccountActivity.getIntent(
                                requireContext()
                            )
                        )
                    }
                    builder.setNeutralButton(getString(R.string.label_not_now)) { dialogInterface, _ ->
                        dialogInterface.dismiss()
                    }
                    val alertDialog: AlertDialog = builder.create()
                    alertDialog.setCancelable(false)
                    alertDialog.show()
                }
                is RegisterViewModel.RegisterViewState.CheckUsernameLoading -> {
                    if (it.isLoading) {
                        binding.progressBarUsername.visibility = View.VISIBLE
                    } else {
                        binding.progressBarUsername.visibility = View.GONE
                    }
                }
                is RegisterViewModel.RegisterViewState.CheckUsernameExist -> {
                    binding.ivUsername.visibility = View.VISIBLE
                    if (it.isUsernameExist == 1) {
                        isValidUsername = false
                        binding.ivUsername.setImageResource(R.drawable.ic_username_exist)
                    } else {
                        isValidUsername = true
                        binding.ivUsername.setImageResource(R.drawable.ic_username_not_exist)
                    }
                }
            }
        }.autoDispose()
    }

    private fun buttonVisibility(isLoading: Boolean) {
        if (isLoading) {
            binding.btnSignUp.visibility = View.INVISIBLE
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.btnSignUp.visibility = View.VISIBLE
            binding.progressBar.visibility = View.INVISIBLE
        }
    }

    private fun isValidate(): Boolean {
        var isValidate = true
        if (binding.etName.text.isNullOrEmpty()) {
            showToast(resources.getString(R.string.empty_name))
            isValidate = false
        } else if (binding.etUsername.text.isNullOrEmpty()) {
            showToast(resources.getString(R.string.empty_username))
            isValidate = false
        } else if (binding.etUsername.text.toString().length < 5) {
            showToast(resources.getString(R.string.msg_username_not_length))
            isValidate = false
        } else if (!isValidUsername) {
            showToast(resources.getString(R.string.msg_username_already_taken_please_for_try_another))
            isValidate = false
        } else if (binding.etEmailId.text.isNullOrEmpty()) {
            showToast(resources.getString(R.string.empty_email))
            isValidate = false
        } else if (!emailPattern.matcher(binding.etEmailId.text.toString()).matches()) {
            showToast(resources.getString(R.string.invalid_email))
            isValidate = false
        } else if (binding.etPassword.text.isNullOrEmpty()) {
            showToast(resources.getString(R.string.empty_password))
            isValidate = false
        } else if (binding.etPassword.text.toString().length < 8) {
            showToast(resources.getString(R.string.password_minimum_length))
            isValidate = false
        } else if (binding.etConfirmPassword.text.isNullOrEmpty()) {
            showToast(resources.getString(R.string.confirm_your_password))
            isValidate = false
        } else if (binding.etPassword.text.toString() != binding.etConfirmPassword.text.toString()) {
            showToast(resources.getString(R.string.msg_password_should_match))
            isValidate = false
        } else if (!binding.agreeTermsCheckBox.isChecked) {
            showToast(resources.getString(R.string.msg_agree_terms))
            isValidate = false
        }
        return isValidate
    }

    fun dismissBottomSheet() {
        dismiss()
    }
}