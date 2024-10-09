package com.outgoer.ui.home.profile.newprofile.setting

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.outgoer.base.BaseActivity
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivityNewProfileSettingCategoryBinding

class NewProfileSettingCategoryActivity : BaseActivity() {

    companion object{
        fun getIntent(context: Context): Intent {
            return Intent(context, NewProfileSettingCategoryActivity::class.java)
        }
    }

    private lateinit var binding:ActivityNewProfileSettingCategoryBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNewProfileSettingCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        listenToViewEvent()
    }

    private fun listenToViewEvent() {
        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }
    }
}