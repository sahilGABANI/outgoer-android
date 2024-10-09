package com.outgoer.ui.newvenuedetail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.outgoer.R
import com.outgoer.base.BaseActivity
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivityFullScreenBinding

class FullScreenActivity : BaseActivity() {
    companion object {
        private const val PHOTO_URL = "PHOTO_URL"
        fun getIntent(context: Context, url: String?): Intent {
            val intent = Intent(context, FullScreenActivity::class.java)
            intent.putExtra(PHOTO_URL,url)
            return intent
        }
    }
    private lateinit var binding : ActivityFullScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val url=intent.extras!!.getString(PHOTO_URL,"")
        Glide.with(this)
            .load(url)
            .placeholder(R.drawable.venue_placeholder)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.photoView)

        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()

    }
}