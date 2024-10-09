package com.outgoer.ui.userverification

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.ui.userverification.viewmodel.UserVerificationViewModel
import com.outgoer.ui.userverification.viewmodel.VerificationViewState
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showLongToast
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivityVerificationBinding
import javax.inject.Inject

class VerificationActivity : BaseActivity() {

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, VerificationActivity::class.java)
        }
    }

    lateinit var binding: ActivityVerificationBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<UserVerificationViewModel>
    private lateinit var verificationViewModel: UserVerificationViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        OutgoerApplication.component.inject(this)
        verificationViewModel = getViewModelFromFactory(viewModelFactory)

        listenToViewModel()
        listenToViewEvent()
        callProfileAPI()

    }

    private fun listenToViewEvent() {
        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()

        binding.buttonSendRequest.throttleClicks().subscribeAndObserveOnMainThread {
            verificationViewModel.sendVerificationRequest(loggedInUserCache.getUserId() ?: 0)
        }.autoDispose()
    }

    private fun listenToViewModel() {
        verificationViewModel.verificationViewStates.subscribeAndObserveOnMainThread {
            when (it) {
                is VerificationViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is VerificationViewState.LoadingState -> {
                    binding.flButton.isVisible = !it.isLoading
                    binding.progressBar.isVisible = it.isLoading
                }
                is VerificationViewState.SuccessMessage -> {
                    binding.buttonSendRequest.isVisible = false
                    showLongToast(it.successMessage)
                    callProfileAPI()
                }

                is VerificationViewState.MyProfileData -> {
                    manageStatusButton(it.outgoerUser.badgeRequest, it.outgoerUser.profileVerified)
                }

                is VerificationViewState.LoadVenueDetail -> {
                    manageStatusButton(it.venueDetail.badgeRequest, it.venueDetail.profileVerified)
                }
            }
        }.autoDispose()
    }

    private fun callProfileAPI() {
        if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.userType == "venue_owner") verificationViewModel.getVenueDetail(
            loggedInUserCache.getUserId() ?: 0
        )
        else verificationViewModel.myProfile()
    }

    private fun manageStatusButton(badgeRequest: Int?, profileVerified: Int?) {
        if (badgeRequest == 1 && profileVerified == 1) {
            binding.buttonVerified.isVisible = true
            binding.tvDescription.isVisible = false
            binding.tvVerified.isVisible = true
        } else if (badgeRequest == 1 || badgeRequest == 2) {
            binding.buttonSendRequest.isVisible = true
        } else if (badgeRequest == 0) {
            binding.buttonPendingRequest.isVisible = true
        }
    }

}