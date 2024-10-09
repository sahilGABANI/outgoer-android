package com.outgoer.ui.create_story

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import com.google.android.material.tabs.TabLayoutMediator
import com.jakewharton.rxbinding3.widget.editorActions
import com.jakewharton.rxbinding3.widget.textChanges
import com.outgoer.R
import com.outgoer.base.BaseActivity
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivityAddStoryLocationBinding
import com.outgoer.ui.create_story.view.LocationSelectionTabAdapter
import com.outgoer.ui.home.newReels.view.ReelsFragmentTabAdapter
import com.outgoer.utils.UiUtils
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit

class AddStoryLocationActivity : BaseActivity() {

    private lateinit var binding: ActivityAddStoryLocationBinding
    private lateinit var locationSelectionTabAdapter: LocationSelectionTabAdapter
    private var searchString: String? = null
    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, AddStoryLocationActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddStoryLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)


        initUI()
        searchListener()
    }

    private fun searchListener() {
        binding.searchAppCompatEditText.textChanges()
            .skipInitialValue()
            .doOnNext {
                if (it.isNullOrEmpty()) {
                    binding.clearAppCompatImageView.visibility = View.INVISIBLE
                } else {
                    binding.clearAppCompatImageView.visibility = View.VISIBLE
                }
            }
            .debounce(300, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeOnIoAndObserveOnMainThread({
                if (it.length > 2) {
                    searchString = it.toString()
                    RxBus.publish(RxEvent.SearchStoryLocation(searchString))
                } else {
                    searchString = null
                    RxBus.publish(RxEvent.SearchStoryLocation(searchString))
                }
            }, {
                Timber.e(it)
            }).autoDispose()

        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            finish()
        }

        binding.searchAppCompatEditText.editorActions()
            .filter { action -> action == EditorInfo.IME_ACTION_SEARCH }
            .subscribeAndObserveOnMainThread {
                UiUtils.hideKeyboard(this)
            }.autoDispose()

        binding.searchAppCompatEditText.textChanges()
            .doOnNext {
                if (it.isNullOrEmpty()) {
                    binding.clearAppCompatImageView.visibility = View.INVISIBLE
                } else {
                    binding.clearAppCompatImageView.visibility = View.VISIBLE
                }
            }
            .debounce(500, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeOnIoAndObserveOnMainThread({
                if (it.length > 2) {

                } else if (it.isEmpty()) {

                }
            }, {
                Timber.e(it)
            }).autoDispose()

        binding.clearAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            UiUtils.hideKeyboard(this)
            binding.searchAppCompatEditText.setText("")
        }.autoDispose()
    }

    private fun initUI() {

        locationSelectionTabAdapter = LocationSelectionTabAdapter(this@AddStoryLocationActivity)
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.offscreenPageLimit = 2
        binding.viewPager.adapter = locationSelectionTabAdapter
        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(1))
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = getString(R.string.outgoer_venue)
                }
                1 -> {
                    tab.text = getString(R.string.search)
                }
            }
        }.attach()
    }
}