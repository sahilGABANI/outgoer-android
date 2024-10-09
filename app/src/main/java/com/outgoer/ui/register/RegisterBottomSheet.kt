package com.outgoer.ui.register

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.facebook.CallbackManager
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.outgoer.R
import com.outgoer.api.authentication.model.CheckSocialIdExistRequest
import com.outgoer.api.authentication.model.SocialMediaLoginRequest
import com.outgoer.api.venue.model.RegisterVenueRequest
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.BottomSheetRegisterBinding
import com.outgoer.mediapicker.constants.BaseConstants
import com.outgoer.ui.activateaccount.ActivateAccountActivity
import com.outgoer.ui.home.HomeActivity
import com.outgoer.ui.login.AddUsernameEmailSocialLoginBottomSheet
import com.outgoer.ui.login.viewmodel.LoginViewModel
import com.outgoer.ui.venue.RegisterVenueActivity
import com.outgoer.ui.venue.VenueInfoActivity
import com.outgoer.utils.SnackBarUtils.showTopSnackBar
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.regex.Pattern
import javax.inject.Inject

class RegisterBottomSheet: BaseBottomSheetDialogFragment() {

    private var _binding: BottomSheetRegisterBinding? = null
    private val binding get() = _binding!!

    companion object {
        val TAG: String = "RegisterBottomSheet"
        private const val RC_SIGN_IN = 1001
        private const val SOCIAL_PLATFORM_TYPE_GOOGLE = "google"
        private const val SOCIAL_PLATFORM_TYPE_FACEBOOK = "facebook"

        @JvmStatic
        fun newInstance(): RegisterBottomSheet {
            return RegisterBottomSheet()
        }
    }

    private var loginClickSubscribe: PublishSubject<String> = PublishSubject.create()
    val loginClicks: Observable<String> = loginClickSubscribe.hide()


    private var socialId: String? = null
    private var socialPlatform: String? = null
    private var name: String? = null
    private var username: String? = null
    private var emailId: String? = null

    private lateinit var callbackManager: CallbackManager
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var loginManager: LoginManager

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<LoginViewModel>
    private lateinit var loginViewModel: LoginViewModel
    private var fusedLocationClient: FusedLocationProviderClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)
        loginViewModel = getViewModelFromFactory(viewModelFactory)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetRegisterBinding.inflate(inflater, container, false)
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
        val legalText = getString(R.string.desc_account_create, termsConditionsText, privacyPolicyText)
        binding.tvPrivacyPolicy.setText(legalText)

        Linkify.addLinks(
            binding.tvPrivacyPolicy,
            Pattern.compile(termsConditionsText),
            null,
            null,
            { match, url -> BaseConstants.TERMS_N_CONDITIONS }
        )
        Linkify.addLinks(
            binding.tvPrivacyPolicy,
            Pattern.compile(privacyPolicyText),
            null,
            null,
            { match, url -> BaseConstants.PRIVACY_POLICY }
        )


        initGoogleLogin()
        listenToViewEvents()
        listenToViewModel()
    }

    private val mBottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss()
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
    }

    private fun listenToViewEvents() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            dismissBottomSheet()
        }.autoDispose()

        binding.continueAsVenueLogin.throttleClicks().subscribeAndObserveOnMainThread {
            dismissBottomSheet()
            startActivity(RegisterVenueActivity.getIntent(requireContext()))
//            startActivity(VenueInfoActivity.getIntent(requireContext(), RegisterVenueRequest()))
        }

        binding.buttonContWithEmail.throttleClicks().subscribeAndObserveOnMainThread {
            dismissBottomSheet()
            loginClickSubscribe.onNext(resources.getString(R.string.label_signin_sheet))
        }.autoDispose()

        binding.buttonContWithGoogle.throttleClicks().subscribeAndObserveOnMainThread {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

        binding.tvBack.throttleClicks().subscribeAndObserveOnMainThread {
            dismissBottomSheet()
        }.autoDispose()
    }

    private fun initGoogleLogin() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
        googleSignInClient.signOut()

        loginManager = LoginManager.getInstance()
        callbackManager = CallbackManager.Factory.create()

        loginManager.logOut()
    }


    private fun listenToViewModel() {
        loginViewModel.loginState.subscribeAndObserveOnMainThread {
            when (it) {
                is LoginViewModel.LoginViewState.ErrorMessage -> {
                    Timber.tag("ErrorMessage").e("LoginViewState -> it.errorMessage: ${it.errorMessage}")
                    if (it.errorMessage.startsWith("Unable to resolve host")) {
                        showTopSnackBar(requireView())
                    } else {
                        showLongToast(it.errorMessage)
                    }
                }
                is LoginViewModel.LoginViewState.SuccessMessage -> {
                    showLongToast(it.successMessage)
                }
                is LoginViewModel.LoginViewState.AddUsernameEmailDialog -> {
                    openAddUsernameEmailBottomSheet(username, it.emailId)
//                    dismissBottomSheet()
                }
                is LoginViewModel.LoginViewState.ContinueSocialLogin -> {
                    loginWithSocialMedia(username ?: it.username, it.emailId)
                }
                is LoginViewModel.LoginViewState.HomePageNavigation -> {
                    startActivityWithDefaultAnimation(HomeActivity.getIntent(requireContext()))
                    dismissBottomSheet()
                }
                is LoginViewModel.LoginViewState.DeactivateAccount -> {
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setMessage(it.deactivateMessage)
                    builder.setPositiveButton(getString(R.string.label_contact_support)) { _, _ ->
                        startActivityWithDefaultAnimation(ActivateAccountActivity.getIntent(requireContext()))
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

    private fun openAddUsernameEmailBottomSheet(username: String?, emailId: String?) {
        val bottomSheetFragment = AddUsernameEmailSocialLoginBottomSheet(username, emailId)
        bottomSheetFragment.addUsernameEmail.subscribeAndObserveOnMainThread {
            loginWithSocialMedia(it.username, it.email)
            bottomSheetFragment.dismissBottomSheet()
        }.autoDispose()
        bottomSheetFragment.show(childFragmentManager, AddUsernameEmailSocialLoginBottomSheet::class.java.name)
    }

    private fun loginWithSocialMedia(username: String, emailId: String) {


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

                        loginViewModel.socialLogin(
                            SocialMediaLoginRequest(
                                socialId = socialId,
                                socialPlatform = socialPlatform,
                                name = name,
                                username = username,
                                email = emailId,
                                latitude = latitude,
                                longitude = longitude
                            )
                        )
                    }
                }

                override fun onDenied(permissions: List<String>, never: Boolean) {
                    loginViewModel.socialLogin(
                        SocialMediaLoginRequest(
                            socialId = socialId,
                            socialPlatform = socialPlatform,
                            name = name,
                            username = username,
                            email = emailId
                        )
                    )
                }
            })

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    Timber.tag("<><> G Login Response").e(account.id)
                    socialId = account.id
                    socialPlatform = SOCIAL_PLATFORM_TYPE_GOOGLE
                    name = account.displayName
                    username = account.account?.name
                    emailId = account.email
                    loginViewModel.checkSocialId(CheckSocialIdExistRequest(socialId = socialId))
                } else {
                    showLongToast("Error in social login")
                }
            } catch (e: ApiException) {
                e.printStackTrace()
                // Google Sign In failed, update UI appropriately
                Timber.tag("<><> Google sign in failed").e(e)
            }
        } else {
            callbackManager.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun dismissBottomSheet() {
        loginClickSubscribe.onNext(resources.getString(R.string.label_back))
        dismiss()
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissBottomSheet()
    }
}