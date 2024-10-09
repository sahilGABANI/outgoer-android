package com.outgoer.ui.editprofile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.authentication.model.OutgoerUser
import com.outgoer.api.profile.model.UpdateProfileRequest
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityAddExternalLinkBinding
import com.outgoer.ui.editprofile.viewmodel.EditProfileViewModel
import java.net.URL
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.inject.Inject


class AddExternalLinkActivity : BaseActivity() {

    private lateinit var binding: ActivityAddExternalLinkBinding

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private lateinit var outgoerUser: OutgoerUser

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<EditProfileViewModel>
    private lateinit var editProfileViewModel: EditProfileViewModel

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, AddExternalLinkActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)

        binding = ActivityAddExternalLinkBinding.inflate(layoutInflater)
        setContentView(binding.root)

        editProfileViewModel = getViewModelFromFactory(viewModelFactory)
        outgoerUser = loggedInUserCache.getLoggedInUser()?.loggedInUser ?: return

        initUI()
        listenToViewModel()
    }

    private fun initUI() {

        outgoerUser?.let {
            binding.urlAppCompatEditText.setText(if(it.webLink.isNullOrEmpty()) "" else it.webLink)
            binding.titleAppCompatEditText.setText(if(it.webTitle.isNullOrEmpty()) "" else it.webTitle)
        }

        binding.backAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }

        binding.continueAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            if(binding.urlAppCompatEditText.text.toString().isNullOrEmpty()) {
                showToast(resources.getString(R.string.label_enter_url))
            } else if(binding.titleAppCompatEditText.text.toString().isNullOrEmpty()) {
                showToast(resources.getString(R.string.label_enter_title))
            } else if (!isValidUrl(binding.urlAppCompatEditText.text.toString())) {
                showToast(getString(R.string.label_enter_valid_url))
            }else {
                val updateProfileRequest = UpdateProfileRequest(
                    name = outgoerUser.name ?: "",
                    username = outgoerUser.username ?: "",
                    image = outgoerUser.avatar ?: "",
                    about = outgoerUser.about ?: "",
                    webLink = binding.urlAppCompatEditText.text.toString(),
                    webTitle = binding.titleAppCompatEditText.text.toString()
                )

                editProfileViewModel.uploadProfile(updateProfileRequest)
            }
        }
    }

    private fun listenToViewModel() {
        editProfileViewModel.editProfileViewStates.subscribeAndObserveOnMainThread {
            when (it) {
                is EditProfileViewModel.EditProfileViewState.LoadingState -> {
                    buttonVisibility(it.isLoading)
                }
                is EditProfileViewModel.EditProfileViewState.SuccessMessage -> {
                    showLongToast(it.successMessage)
                    onBackPressed()
                }
                is EditProfileViewModel.EditProfileViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }

                else -> {}
            }
        }
    }

    private fun buttonVisibility(isLoading: Boolean) {
        if (isLoading) {
            binding.continueAppCompatImageView.visibility = View.INVISIBLE
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.continueAppCompatImageView.visibility = View.VISIBLE
            binding.progressBar.visibility = View.INVISIBLE
        }
    }

    private fun isValidUrl(url: String): Boolean {
        val p: Pattern = Patterns.WEB_URL
        val m: Matcher = p.matcher(url.toLowerCase())
        return m.matches()
    }
}