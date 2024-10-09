package com.outgoer.ui.home.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import com.google.android.material.tabs.TabLayoutMediator
import com.outgoer.R
import com.outgoer.api.search.SearchRepository
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivitySearchBinding
import com.outgoer.ui.home.search.view.SearchViewPagerAdapter
import com.outgoer.utils.UiUtils.hideKeyboard
import javax.inject.Inject

class SearchActivity : BaseActivity() {

    private lateinit var binding: ActivitySearchBinding

    @Inject
    lateinit var searchRepository: SearchRepository

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, SearchActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listenToViewEvents()
    }

    private fun listenToViewEvents() {

        binding.backAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressedDispatcher.onBackPressed()
        }

        val searchViewPagerAdapter = SearchViewPagerAdapter(this)
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.offscreenPageLimit = 1
        binding.viewPager.adapter = searchViewPagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = getString(R.string.top)
                }
                1 -> {
                    tab.text = getString(R.string.people)
                }
                2 -> {
                    tab.text = getString(R.string.places)
                }
            }
        }.attach()
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                // This method is called after the text has been changed.
                if (s.isNullOrEmpty()) {
                    binding.ivClear.visibility = View.INVISIBLE
                    val newText = s.toString()
                    if (newText.isEmpty()) {
                        searchRepository.searchString("")
                    }
                } else {
                    binding.ivClear.visibility = View.VISIBLE
                    val newText = s.toString()
                    if (newText.length > 2) {
                        searchRepository.searchString(newText)
                    }
                }
            }
        })

        binding.ivClear.throttleClicks().subscribeAndObserveOnMainThread {
            hideKeyboard(this@SearchActivity)
            binding.etSearch.setText("")
        }.autoDispose()
    }
}