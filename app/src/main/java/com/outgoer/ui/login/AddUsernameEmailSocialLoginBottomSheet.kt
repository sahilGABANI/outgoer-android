package com.outgoer.ui.login

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding3.widget.textChanges
import com.outgoer.R
import com.outgoer.api.authentication.model.AddUsernameEmail
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.BottomSheetAddUsernameEmailSocialLoginBinding
import com.outgoer.ui.login.viewmodel.LoginViewModel
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AddUsernameEmailSocialLoginBottomSheet(
    private val username: String?,
    private val emailId: String?
) : BaseBottomSheetDialogFragment() {

    private val addUsernameEmailSubject: PublishSubject<AddUsernameEmail> = PublishSubject.create()
    val addUsernameEmail: Observable<AddUsernameEmail> = addUsernameEmailSubject.hide()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<LoginViewModel>
    private lateinit var loginViewModel: LoginViewModel

    private var _binding: BottomSheetAddUsernameEmailSocialLoginBinding? = null
    private val binding get() = _binding!!

    private var isValidUsername = true
    private val emailPattern = Patterns.EMAIL_ADDRESS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)

        OutgoerApplication.component.inject(this)
        loginViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetAddUsernameEmailSocialLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        listenToViewEvents()
        listenToViewModel()
    }

    private fun listenToViewEvents() {
//        binding.etUsername.setText(username ?: "")
        binding.etEmailId.setText(emailId ?: "")

//        binding.etUsername.textChanges()
//            .skipInitialValue()
//            .debounce(500, TimeUnit.MILLISECONDS, Schedulers.io())
//            .subscribeOnIoAndObserveOnMainThread({
//                binding.btnContinue.isEnabled = false
//                if (it.length > 3) {
//                    loginViewModel.checkUsername(it.toString())
//                } else {
//                    binding.progressBarUsername.visibility = View.GONE
//                    binding.ivUsername.visibility = View.GONE
//                }
//            }, {
//                Timber.e(it)
//            }).autoDispose()

        binding.btnContinue.throttleClicks().subscribeAndObserveOnMainThread {
            if (isValidate()) {
                addUsernameEmailSubject.onNext(
                    AddUsernameEmail(
                        "",
                        binding.etEmailId.text.toString()
                    )
                )
            }
        }.autoDispose()
    }

    private fun listenToViewModel() {
        loginViewModel.loginState.subscribeAndObserveOnMainThread {
            when (it) {
                is LoginViewModel.LoginViewState.CheckUsernameLoading -> {
                    if (it.isLoading) {
                        binding.progressBarUsername.visibility = View.VISIBLE
                    } else {
                        binding.progressBarUsername.visibility = View.GONE
                    }
                }
                is LoginViewModel.LoginViewState.CheckUsernameExist -> {
                    binding.ivUsername.visibility = View.VISIBLE
                    if (it.isUsernameExist == 1) {
                        isValidUsername = false
                        binding.ivUsername.setImageResource(R.drawable.ic_username_exist)
                    } else {
                        isValidUsername = true
                        binding.ivUsername.setImageResource(R.drawable.ic_username_not_exist)
                    }
                    binding.btnContinue.isEnabled = isValidUsername
                }

                else -> {}
            }
        }.autoDispose()
    }

    private fun isValidate(): Boolean {
        var isValidate = true
//        if (binding.etUsername.text.isNullOrEmpty()) {
//            showToast(getString(R.string.empty_username))
//            isValidate = false
//        } else if (binding.etUsername.text.toString().length < 5) {
//            showToast(getString(R.string.msg_username_not_length))
//            isValidate = false
//        } else if (!isValidUsername) {
//            showToast(getString(R.string.msg_username_already_taken_please_for_try_another))
//            isValidate = false
//        } else

            if (binding.etEmailId.text.toString().isEmpty()) {
            showToast(getString(R.string.empty_email))
            isValidate = false
        } else if (!emailPattern.matcher(binding.etEmailId.text.toString()).matches()) {
            showToast(getString(R.string.invalid_email))
            isValidate = false
        }
        return isValidate
    }

    fun dismissBottomSheet() {
        dismiss()
    }
}