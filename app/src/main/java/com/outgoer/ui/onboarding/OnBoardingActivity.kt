package com.outgoer.ui.onboarding

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer.OnPreparedListener
import android.net.Uri
import android.os.Bundle
import android.text.util.Linkify
import androidx.core.view.isVisible
import com.outgoer.BuildConfig
import com.outgoer.R
import com.outgoer.base.BaseActivity
import com.outgoer.base.extension.BASE_DEV_URL
import com.outgoer.base.extension.BASE_PROD_URL
import com.outgoer.base.extension.BASE_URL
import com.outgoer.base.extension.startActivityWithFadeInAnimation
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivityOnBoardingBinding
import com.outgoer.mediapicker.constants.BaseConstants.PRIVACY_POLICY
import com.outgoer.mediapicker.constants.BaseConstants.TERMS_N_CONDITIONS
import com.outgoer.mediapicker.constants.BaseConstants.USER_AGREEMENT
import com.outgoer.ui.login.LoginBottomSheet
import com.outgoer.ui.login.bottomsheet.*
import com.outgoer.ui.register.RegisterBottomSheet
import com.outgoer.ui.suggested.SuggestedUsersActivity
import java.util.regex.Pattern


class OnBoardingActivity : BaseActivity() {

    private lateinit var binding: ActivityOnBoardingBinding

    companion object {
        fun getIntent(context: Context): Intent {
            val intent = Intent(context, OnBoardingActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnBoardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        listenToViewEvent()
    }

    private fun listenToViewEvent() {

        binding.versionName.text = if(BASE_URL == BASE_DEV_URL) {
            "Dev - ${BuildConfig.VERSION_CODE} (${BuildConfig.VERSION_NAME})"
        } else if(BASE_URL == BASE_PROD_URL) {
            "Prod Server - ${BuildConfig.VERSION_CODE} (${BuildConfig.VERSION_NAME})"
        } else {
            "QA Server - ${BuildConfig.VERSION_CODE} (${BuildConfig.VERSION_NAME})"
        }

        val termsConditionsText = getString(R.string.text_terms_conditions)
        val privacyPolicyText = getString(R.string.text_privacy_policy)
        val userAgreementText = getString(R.string.text_user_agreement)
        val legalText = getString(R.string.account_create_info, userAgreementText, privacyPolicyText, termsConditionsText)
        binding.tvPrivacyPolicy.setText(legalText)

        Linkify.addLinks(
            binding.tvPrivacyPolicy,
            Pattern.compile(userAgreementText),
            null,
            null,
            { match, url -> USER_AGREEMENT }
        )

        Linkify.addLinks(
            binding.tvPrivacyPolicy,
            Pattern.compile(termsConditionsText),
            null,
            null,
            { match, url -> TERMS_N_CONDITIONS }
        )
        Linkify.addLinks(
            binding.tvPrivacyPolicy,
            Pattern.compile(privacyPolicyText),
            null,
            null,
            { match, url -> PRIVACY_POLICY }
        )

        binding.mbtnCreateAccount.throttleClicks().subscribeAndObserveOnMainThread {
            openSignUpBottomSheet()
            binding.mbtnCreateAccount.isVisible = false
            binding.tvSignIn.isVisible = false
            binding.tvPrivacyPolicy.isVisible = false
        }.autoDispose()

        binding.tvSignIn.throttleClicks().subscribeAndObserveOnMainThread {
            openSignInBottomSheet()
            binding.mbtnCreateAccount.isVisible = false
            binding.tvSignIn.isVisible = false
            binding.tvPrivacyPolicy.isVisible = false
        }.autoDispose()

        val uri: Uri = Uri.parse("android.resource://" + packageName + "/" + R.raw.outgoer_intro)
        binding.videoView.setOnPreparedListener(OnPreparedListener { mp ->
            mp.isLooping = true
            mp.setVolume(0f, 0f);
        })
        binding.videoView.requestFocus();
        binding.videoView.setVideoURI(uri);
        binding.videoView.start();

    }

    private fun openSignUpBottomSheet() {
        val singupSheetFragment = RegisterBottomSheet.newInstance()
        singupSheetFragment.loginClicks.subscribeAndObserveOnMainThread {
            if (it == resources.getString(R.string.label_signin_sheet)) {
                val signupsheetfragment = SignupBottomSheet.newInstance()
                signupsheetfragment.loginClicks.subscribeAndObserveOnMainThread {
                    openVerificationBottomSheet(it)
                }
                signupsheetfragment.show(supportFragmentManager, SignupBottomSheet.TAG)
            } else if (it.equals(resources.getString(R.string.label_back))) {
                binding.mbtnCreateAccount.isVisible = true
                binding.tvSignIn.isVisible = true
                binding.tvPrivacyPolicy.isVisible = true

            }
        }
        singupSheetFragment.show(supportFragmentManager, RegisterBottomSheet.TAG)
    }


    private fun openSignInBottomSheet() {
        val loginBottomSheet = LoginBottomSheet.newInstance()

        loginBottomSheet.signupClicks.subscribeAndObserveOnMainThread {
            if (it == resources.getString(R.string.label_signin)) {
                val signInBottomSheet = SignInBottomSheet.newInstance()
                signInBottomSheet.signupClicks.subscribeAndObserveOnMainThread {
                    if (it == resources.getString(R.string.label_forgot_password)) {
                        openForgotPasswordBottomSheet()
                    }
                }
                signInBottomSheet.show(supportFragmentManager, RegisterBottomSheet.TAG)
            } else if (it == resources.getString(R.string.label_back)) {
                binding.mbtnCreateAccount.isVisible = true
                binding.tvSignIn.isVisible = true
                binding.tvPrivacyPolicy.isVisible = true

            }
        }
        loginBottomSheet.show(supportFragmentManager, RegisterBottomSheet.TAG)
    }

    private fun openForgotPasswordBottomSheet() {
        val forgotPasswordBottomSheet = ForgotPasswordBottomSheet.newInstance()
        forgotPasswordBottomSheet.forgotPasswordClick.subscribeAndObserveOnMainThread {
            openVerifyOTPBottomSheet(it)
        }
        forgotPasswordBottomSheet.show(supportFragmentManager, RegisterBottomSheet.TAG)
    }

    private fun openVerifyOTPBottomSheet(it: String) {
        val resetOtpVerificationBottomSheet = ResetOtpVerificationBottomSheet.newInstance(it)
        resetOtpVerificationBottomSheet.otpVerifyClick.subscribeAndObserveOnMainThread {
            val resetPasswordBottomSheet = ResetPasswordBottomSheet.newInstance(it)
            resetPasswordBottomSheet.show(supportFragmentManager, ResetPasswordBottomSheet.TAG)

        }
        resetOtpVerificationBottomSheet.show(
            supportFragmentManager,
            ResetOtpVerificationBottomSheet.TAG
        )
    }

    private fun openVerificationBottomSheet(email: String) {
        val otpVerificationBottomSheet = OtpVerificationBottomSheet.newInstance(email).apply {
            otpVerificationSuccessClick.subscribeAndObserveOnMainThread {
                dismissBottomSheet()
                startActivityWithFadeInAnimation(
                    SuggestedUsersActivity.getIntent(
                        requireContext()
                    )
                )
            }.autoDispose()
        }
        otpVerificationBottomSheet.show(supportFragmentManager, SignupBottomSheet.TAG)
    }

    override fun onResume() {
        super.onResume()
        binding.videoView.start()
    }

}