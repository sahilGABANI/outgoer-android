package com.outgoer.ui.savecredentials

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.extension.startActivityWithFadeInAnimation
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivitySaveInfoBinding
import com.outgoer.ui.home.HomeActivity
import com.outgoer.ui.savecredentials.utils.SecurePreferences
import javax.inject.Inject

class SaveInfoActivity : BaseActivity() {

    private lateinit var binding: ActivitySaveInfoBinding

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private lateinit var securePreferences: SecurePreferences
    private var uName: String? = null
    private var passName: String? = null
    private var username: String? = null
    private var avatar: String? = null

    companion object {
        private const val EMAIL_INFO = "EMAIL_INFO"
        private const val PASSWORD_INFO = "PASSWORD_INFO"
        private const val USERNAME_INFO = "USERNAME_INFO"
        private const val AVATAR_INFO = "AVATAR_INFO"

        fun getIntent(
            context: Context,
            email: String,
            password: String,
            userName: String,
            avatar: String
        ): Intent {
            val intent = Intent(context, SaveInfoActivity::class.java)
            intent.putExtra(EMAIL_INFO, email)
            intent.putExtra(PASSWORD_INFO, password)
            intent.putExtra(USERNAME_INFO, userName)
            intent.putExtra(AVATAR_INFO, avatar)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)

        binding = ActivitySaveInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initUI()
    }

    private fun initUI() {
        securePreferences = SecurePreferences(this@SaveInfoActivity)

        intent?.let {
            uName = it.getStringExtra(EMAIL_INFO)
            passName = it.getStringExtra(PASSWORD_INFO)
            username = it.getStringExtra(USERNAME_INFO)
            avatar = it.getStringExtra(AVATAR_INFO)
        }

        binding.saveMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            securePreferences.saveCredentials(
                uName ?: "",
                passName ?: "",
                username ?: "",
                avatar ?: ""
            )
            startActivityWithFadeInAnimation(HomeActivity.getIntent(this@SaveInfoActivity))
        }.autoDispose()

        binding.notNowMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            startActivityWithFadeInAnimation(HomeActivity.getIntent(this@SaveInfoActivity))
        }.autoDispose()

        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            startActivityWithFadeInAnimation(HomeActivity.getIntent(this@SaveInfoActivity))
        }.autoDispose()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        startActivityWithFadeInAnimation(HomeActivity.getIntent(this@SaveInfoActivity))
    }
}