package com.outgoer.ui.home.profile.newprofile.setting

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.ContextThemeWrapper
import com.google.firebase.messaging.FirebaseMessaging
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.authentication.model.OutgoerUser
import com.outgoer.api.profile.model.SetVisibilityRequest
import com.outgoer.ui.userverification.VerificationActivity
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityNewProfileSettingBinding
import com.outgoer.mediapicker.constants.BaseConstants.PRIVACY_POLICY
import com.outgoer.mediapicker.constants.BaseConstants.TERMS_N_CONDITIONS
import com.outgoer.mediapicker.constants.BaseConstants.USER_AGREEMENT
import com.outgoer.ui.block.BlockProfileActivity
import com.outgoer.ui.home.HomeActivity
import com.outgoer.ui.home.profile.viewmodel.ProfileViewModel
import com.outgoer.ui.onboarding.OnBoardingActivity
import com.outgoer.ui.save_post_reels.SavePostReelsActivity
import javax.inject.Inject
import kotlin.properties.Delegates

class NewProfileSettingActivity : BaseActivity() {

    companion object{
        fun getIntent(context: Context): Intent {
            return Intent(context, NewProfileSettingActivity::class.java)
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ProfileViewModel>
    private lateinit var profileViewModel: ProfileViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private lateinit var outgoerUser: OutgoerUser
    private var loggedInUserId by Delegates.notNull<Int>()

    private lateinit var binding:ActivityNewProfileSettingBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNewProfileSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        OutgoerApplication.component.inject(this)
        profileViewModel = getViewModelFromFactory(viewModelFactory)

        outgoerUser = loggedInUserCache.getLoggedInUser()?.loggedInUser ?: return
        loggedInUserId = loggedInUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0

        binding.displayMapSwitchCompat.isChecked = outgoerUser.isVisible == 1

        listenToViewEvent()
        listenToViewModel()
    }

    private fun listenToViewEvent() {

        binding.addAccountAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            startActivity(Intent(this@NewProfileSettingActivity, OnBoardingActivity::class.java))
        }

        binding.logoutAllAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            var deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            logoutAll(deviceId)
        }


        binding.rlBlocked.throttleClicks().subscribeAndObserveOnMainThread {
            startActivity(BlockProfileActivity.getIntent(this@NewProfileSettingActivity))
        }.autoDispose()

        binding.rlPrivacyContainer.throttleClicks().subscribeAndObserveOnMainThread {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY))
            startActivity(browserIntent)
        }.autoDispose()

        binding.rlTermsAndConditionContainer.throttleClicks().subscribeAndObserveOnMainThread {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(TERMS_N_CONDITIONS))
            startActivity(browserIntent)
        }.autoDispose()

        binding.userAgreementRelativeLayout.throttleClicks().subscribeAndObserveOnMainThread {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(USER_AGREEMENT))
            startActivity(browserIntent)
        }.autoDispose()


        binding.displayMapSwitchCompat.setOnCheckedChangeListener { compoundButton, b ->
            profileViewModel.setVisibility(SetVisibilityRequest(if(b) 1 else 0))
        }

        binding.rlAccountContainer.throttleClicks().subscribeAndObserveOnMainThread {
            startActivityWithDefaultAnimation(AccountStatusActivity.getIntent(this))
        }

        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }

        binding.logoutAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
//            profileViewModel.logout()
         logout()
        }

        binding.rlDeactivateAccount.throttleClicks().subscribeAndObserveOnMainThread {
            openDeactivateProfile()
        }

//        binding.rlNotificationContainer.throttleClicks().subscribeAndObserveOnMainThread {
//            startActivityWithDefaultAnimation(NewNotificationActivity.getIntent(this))
//        }

        binding.rlVerificationContainer.throttleClicks().subscribeAndObserveOnMainThread {
            startActivityWithDefaultAnimation(VerificationActivity.getIntent(this))
        }.autoDispose()

        binding.rlSaved.throttleClicks().subscribeAndObserveOnMainThread {
            startActivityWithDefaultAnimation(SavePostReelsActivity.getIntent(this))
        }.autoDispose()
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

    private fun logout() {
        val builder = AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialogCustom))
        builder.setTitle(getString(R.string.label_logout))
        builder.setMessage(getString(R.string.are_you_sure_you_want_to_logout))
        builder.setPositiveButton(getString(R.string.lable_logout)) { dialogInterface, which ->
            var androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            profileViewModel.logout(androidId)
            dialogInterface.dismiss()
        }
        builder.setNeutralButton(getString(R.string.label_cancel)) { dialogInterface, _ ->
            dialogInterface.dismiss()
        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }


    private fun logoutAll(deviceId: String) {
        val builder = AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialogCustom))
        builder.setTitle(getString(R.string.label_logout))
        builder.setMessage(getString(R.string.are_you_sure_you_want_to_logout_all))
        builder.setPositiveButton(getString(R.string.lable_logout)) { dialogInterface, which ->
            profileViewModel.logoutAll(deviceId)
            dialogInterface.dismiss()
        }
        builder.setNeutralButton(getString(R.string.label_cancel)) { dialogInterface, _ ->
            dialogInterface.dismiss()
        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
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
                is ProfileViewModel.ProfileViewState.ConversationSuccessMessage -> {
                    it.successMessage.message?.let { msg ->
                        showLongToast(msg)
                    }

                    for (topic in it.successMessage.conversationId ?: arrayListOf()) {
                        FirebaseMessaging.getInstance().unsubscribeFromTopic("conversation_group_${topic}")
                    }
                }
                is ProfileViewModel.ProfileViewState.SwitchProfileData -> {
                    println("Switch activity")
                    startActivity(HomeActivity.getIntent(this@NewProfileSettingActivity))
                }
                is ProfileViewModel.ProfileViewState.MyProfileData -> {
                    binding.displayMapSwitchCompat.isChecked = it.outgoerUser.isVisible == 1
                }
                is ProfileViewModel.ProfileViewState.LogoutSuccess -> {
                    finish()
                    startActivityWithDefaultAnimation(OnBoardingActivity.getIntent(this))
                }
                is ProfileViewModel.ProfileViewState.DeactivateProfile -> {
                    finish()
                    startActivityWithDefaultAnimation(OnBoardingActivity.getIntent(this))
                }
                else -> {}
            }
        }
    }
}