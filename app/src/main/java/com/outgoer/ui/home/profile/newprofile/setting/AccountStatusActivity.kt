package com.outgoer.ui.home.profile.newprofile.setting

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ContextThemeWrapper
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.messaging.FirebaseMessaging
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.authentication.model.OutgoerUser
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showLongToast
import com.outgoer.base.extension.startActivityWithDefaultAnimation
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivityAccountStatusBinding
import com.outgoer.ui.home.profile.viewmodel.ProfileViewModel
import com.outgoer.ui.onboarding.OnBoardingActivity
import javax.inject.Inject

class AccountStatusActivity : BaseActivity() {

    private lateinit var binding: ActivityAccountStatusBinding

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ProfileViewModel>
    private lateinit var profileViewModel: ProfileViewModel

    private lateinit var outgoerUser: OutgoerUser

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, AccountStatusActivity::class.java)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)

        binding = ActivityAccountStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)
        profileViewModel = getViewModelFromFactory(viewModelFactory)

        initUI()
        listenToViewModel()
    }

    private fun listenToViewModel(){
        profileViewModel.profileViewStates.subscribeAndObserveOnMainThread {

            when (it) {
                is ProfileViewModel.ProfileViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is ProfileViewModel.ProfileViewState.SuccessMessage -> {
                    showLongToast(it.successMessage)
                }
                is ProfileViewModel.ProfileViewState.DeactivateProfile -> {
                    finish()
                    startActivityWithDefaultAnimation(OnBoardingActivity.getIntent(this))
                }
                else -> {}
            }
        }
    }

    private fun initUI() {

        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            finish()
        }

        outgoerUser = loggedInUserCache.getLoggedInUser()?.loggedInUser ?: return

        binding.tvName.text = outgoerUser.username
        binding.ivVerified.isVisible = outgoerUser.profileVerified == 1

        if (outgoerUser.userType == MapVenueUserType.VENUE_OWNER.type) {
            outgoerUser.gallery?.let {
                val galleryUrl = if((outgoerUser.gallery?.size ?: 0) > 0) it?.get(0)?.media else null
                Glide.with(this)
                    .load(galleryUrl)
                    .placeholder(R.drawable.ic_chat_user_placeholder)
                    .error(R.drawable.ic_chat_user_placeholder)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(binding.ivMyProfile)
            }
        }
        else {
            Glide.with(this)
                .load(outgoerUser.avatar)
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .error(R.drawable.ic_chat_user_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(binding.ivMyProfile)
        }

        binding.deactivateAccount.throttleClicks().subscribeAndObserveOnMainThread {
            openDeactivateProfile()
        }
    }

    private fun openDeactivateProfile() {
//        val builder = AlertDialog.Builder(this)
        val builder = AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialogCustom))
        builder.setTitle(getString(R.string.label_deactivate_profile))
        builder.setMessage(getString(R.string.msg_deactivate_profile))
        builder.setPositiveButton(getString(R.string.label_deactivate)) { dialogInterface, which ->
            profileViewModel.deactivate()
            dialogInterface.dismiss()
        }
        builder.setNeutralButton(getString(R.string.label_cancel)) { dialogInterface, _ ->
            dialogInterface.dismiss()
        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

}