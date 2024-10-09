package com.outgoer.ui.login.bottomsheet

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.jakewharton.rxbinding3.widget.textChanges
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.authentication.model.LoginRequest
import com.outgoer.api.follow.model.SuggestedUser
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.SignInBottomsheetBinding
import com.outgoer.ui.activateaccount.ActivateAccountActivity
import com.outgoer.ui.home.HomeActivity
import com.outgoer.ui.login.view.SuggestedUserAdapter
import com.outgoer.ui.login.viewmodel.LoginViewModel
import com.outgoer.ui.savecredentials.SaveInfoActivity
import com.outgoer.ui.savecredentials.utils.SecurePreferences
import com.outgoer.ui.suggested.SuggestedUsersActivity
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class SignInBottomSheet : BaseBottomSheetDialogFragment() {

    private var _binding: SignInBottomsheetBinding? = null
    private val binding get() = _binding!!

    companion object {
        val TAG: String = "SignInBottomSheet"

        @JvmStatic
        fun newInstance(): SignInBottomSheet {
            return SignInBottomSheet()
        }
    }


    private val emailPattern = Patterns.EMAIL_ADDRESS

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<LoginViewModel>
    private lateinit var loginViewModel: LoginViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var signupClickSubscribe: PublishSubject<String> = PublishSubject.create()
    val signupClicks: Observable<String> = signupClickSubscribe.hide()

    private var isInsert: Boolean = true
    private var isInsertLength: Int = 0

    private var listOfUsers: ArrayList<SuggestedUser> = arrayListOf()
    private lateinit var suggestedUserAdapter: SuggestedUserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)
        loginViewModel = getViewModelFromFactory(viewModelFactory)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SignInBottomsheetBinding.inflate(inflater, container, false)
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
        var securePreferences = SecurePreferences(requireContext())
        listOfUsers = securePreferences.getCredentials()

        println("listOfUsers: " + listOfUsers)
        binding.tvForgotPassword.throttleClicks().subscribeAndObserveOnMainThread {
            signupClickSubscribe.onNext(resources.getString(R.string.label_forgot_password))
        }.autoDispose()

        binding.etEmailId.textChanges()
            .debounce(400, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeAndObserveOnMainThread { charIn ->
                println("Filter list: " + listOfUsers.filter { (it.uId?: "").contains(charIn.toString()) })
                if(charIn.length > 0) {
                    var listofsuggesteduser = listOfUsers.filter { (it.uId?: "").contains(charIn.toString()) || (it.uName?: "").contains(charIn.toString()) }
                    suggestedUserAdapter.listOfSuggestedUser = listofsuggesteduser
                    binding.suggestedUserRecyclerView.isVisible = (listofsuggesteduser.size > 0 && isInsertLength != charIn.length)
                } else {
                    binding.suggestedUserRecyclerView.isVisible = false
                }
            }.autoDispose()

        binding.btnLogin.throttleClicks().subscribeAndObserveOnMainThread {
            loginWithEmailIdPassword()
        }.autoDispose()

        suggestedUserAdapter = SuggestedUserAdapter(requireContext()).apply {
            suggestedActionState.subscribeAndObserveOnMainThread {

                println("uId: " + it.uId)
                println("uName: " + it.uName)
                println("uPass: " + it.uPass)
                println("avatar: " + it.avatar)
                binding.etPassword.setText(it.uPass)
                binding.etEmailId.text = Editable.Factory.getInstance().newEditable(it.uId)
                isInsert = false
                isInsertLength = binding.etEmailId.text.toString().length
                binding.suggestedUserRecyclerView.isVisible = false
            }.autoDispose()
        }

        binding.suggestedUserRecyclerView.apply {
            adapter = suggestedUserAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        }
    }

    private fun loginWithEmailIdPassword() {
        if (isValidate()) {
            val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            imm!!.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
            requireActivity().hideKeyboard()
            loginViewModel.login(
                LoginRequest(
                    email = binding.etEmailId.text.toString(),
                    password = binding.etPassword.text.toString()
                )
            )
        }
    }

    private fun isValidate(): Boolean {
        var isValidate = true
        if (binding.etEmailId.text.toString().isEmpty()) {
            showToast(resources.getString(R.string.empty_email))
            isValidate = false
        } else if (!emailPattern.matcher(binding.etEmailId.text.toString()).matches()) {
            showToast(resources.getString(R.string.invalid_email))
            isValidate = false
        } else if (binding.etPassword.text.toString().isEmpty()) {
            showToast(resources.getString(R.string.empty_password))
            isValidate = false
        }
        return isValidate
    }


    private fun listenToViewModel() {
        loginViewModel.loginState.subscribeAndObserveOnMainThread {
            when (it) {
                is LoginViewModel.LoginViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is LoginViewModel.LoginViewState.LoadingState -> {
                    buttonVisibility(it.isLoading)
                }
                is LoginViewModel.LoginViewState.SuccessMessage -> {
//                    showLongToast(it.successMessage)
                }
                is LoginViewModel.LoginViewState.SubscribeTopicAfterLogin -> {
                    it.topicIds?.forEach {
                        Firebase.messaging.subscribeToTopic("conversation_group_${it}")
                            .addOnCompleteListener { task ->
                                var msg = "Subscribed"
                                if (!task.isSuccessful) {
                                    msg = "Subscribe failed"
                                }
                                Log.d(TAG, msg)
                            }
                    }
                }
                is LoginViewModel.LoginViewState.HomePageNavigation -> {
                    println("Info share: " + binding.etEmailId.text.toString())
                    println("Info share: " + binding.etPassword.text.toString())

                    var item = listOfUsers.find { it.uId.equals(binding.etEmailId.text.toString()) }

                    if(item == null) {
                        startActivity(SaveInfoActivity.getIntent(context = requireContext(), binding.etEmailId.text.toString(), binding.etPassword.text.toString(), it.outgoerUser.username ?: "", it.outgoerUser.avatar ?: ""))
                    } else {
                        startActivityWithDefaultAnimation(HomeActivity.getIntent(requireContext()))
                    }

//                    startActivityWithDefaultAnimation(HomeActivity.getIntent(requireContext()))
                    dismissBottomSheet()
                }
                is LoginViewModel.LoginViewState.VerificationNavigation -> {
                    openVerifyOTPBottomSheet(binding.etEmailId.text.toString())
                }
                is LoginViewModel.LoginViewState.DeactivateAccount -> {
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

                else -> {}
            }
        }.autoDispose()
    }

    private fun openVerifyOTPBottomSheet(it: String) {
        val resetOtpVerificationBottomSheet = OtpVerificationBottomSheet.newInstance(it).apply {
            otpVerificationSuccessClick.subscribeAndObserveOnMainThread {
                dismissBottomSheet()
                startActivityWithFadeInAnimation(
                    SuggestedUsersActivity.getIntent(
                        requireContext()
                    )
                )
            }.autoDispose()
        }
//        resetOtpVerificationBottomSheet..subscribeAndObserveOnMainThread {
//            val resetPasswordBottomSheet = ResetPasswordBottomSheet.newInstance(it)
//            resetPasswordBottomSheet.show(childFragmentManager, ResetPasswordBottomSheet.TAG)
//
//        }
        resetOtpVerificationBottomSheet.show(
            childFragmentManager,
            ResetOtpVerificationBottomSheet.TAG
        )
    }

    private fun buttonVisibility(isLoading: Boolean) {
        if (isLoading) {
            binding.btnLogin.visibility = View.INVISIBLE
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.btnLogin.visibility = View.VISIBLE
            binding.progressBar.visibility = View.INVISIBLE
        }
    }

    fun dismissBottomSheet() {
        dismiss()
    }
}