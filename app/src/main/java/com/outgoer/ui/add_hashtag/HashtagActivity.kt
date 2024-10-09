package com.outgoer.ui.add_hashtag

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.jakewharton.rxbinding3.widget.textChanges
import com.outgoer.api.hashtag.model.HashtagResponse
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showToast
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivityHashtagBinding
import com.outgoer.ui.add_hashtag.view.HashtagAdapter
import com.outgoer.ui.add_hashtag.view.RemoveHashtagAdapter
import com.outgoer.ui.add_hashtag.viewmodel.HashtagViewModel
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class HashtagActivity : BaseActivity() {

    private lateinit var binding: ActivityHashtagBinding
    private lateinit var hashtagAdapter: HashtagAdapter

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<HashtagViewModel>
    private lateinit var hashtagViewModel: HashtagViewModel

    private lateinit var removeHashtagAdapter: RemoveHashtagAdapter

    private var listOfHashtags: ArrayList<HashtagResponse> = arrayListOf()
    private var removeHashtags: ArrayList<String> = arrayListOf()

    private var stringInfo = ""

    companion object {
        val HASHTAG_LIST = "HASHTAG_LIST"
        val HASHTAG = "HASHTAG"
        fun getIntent(context: Context): Intent {
            return Intent(context, HashtagActivity::class.java)
        }

        fun getIntent(context: Context, existingHashtag: String): Intent {
            var intent = Intent(context, HashtagActivity::class.java)
            intent.putExtra(HASHTAG, existingHashtag)

            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)

        binding = ActivityHashtagBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hashtagViewModel = getViewModelFromFactory(viewModelFactory)
        initUI()

        listenToViewModel()
        listenToViewEvent()
    }

    private fun listenToViewModel() {
        hashtagViewModel.hashtagState.subscribeAndObserveOnMainThread {
            when (it) {
                is HashtagViewModel.HashtagInfoViewState.LoadingState -> {}
                is HashtagViewModel.HashtagInfoViewState.GetHashtagList -> {
                    listOfHashtags = it.listofHashtagInfo as ArrayList<HashtagResponse>
                    hashtagAdapter.listOfHashtagInfo = listOfHashtags
                }

                is HashtagViewModel.HashtagInfoViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
            }
        }.autoDispose()
    }

    private fun listenToViewEvent() {

        val flexboxLayoutManager = FlexboxLayoutManager(this)
        flexboxLayoutManager.apply {
            flexDirection = FlexDirection.ROW
            flexWrap = FlexWrap.WRAP
            justifyContent = JustifyContent.FLEX_START
        }

        removeHashtagAdapter = RemoveHashtagAdapter(this@HashtagActivity).apply {
            hashtagClick.subscribeAndObserveOnMainThread {
                removeHashtags.remove(it)

                removeHashtagAdapter.listOfHashtagInfo = removeHashtags
                buttonEnableDisable()
            }
        }

        binding.addhashtagListAppCompatEditText.apply {
            adapter = removeHashtagAdapter
            layoutManager = flexboxLayoutManager
        }

        removeHashtagAdapter.listOfHashtagInfo = removeHashtags

        binding.addHashtagMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            setResult(Activity.RESULT_OK, Intent().putExtra(HASHTAG_LIST, removeHashtags.joinToString(",")))
            finish()
        }

        hashtagViewModel.resetPagination()
    }

    private fun buttonEnableDisable() {

        if(removeHashtags.size <= 0) {
            binding.addHashtagMaterialButton.isEnabled = false
            binding.addHashtagMaterialButton.setBackgroundColor(Color.parseColor("#242426"))
            binding.addHashtagMaterialButton.setTextColor(Color.parseColor("#AEAEB2"))
        } else {
            binding.addHashtagMaterialButton.setBackgroundColor(Color.parseColor("#BF5AF2"))
            binding.addHashtagMaterialButton.setTextColor(Color.parseColor("#ffffff"))
            binding.addHashtagMaterialButton.isEnabled = true
        }
    }

    private fun initUI() {

        intent?.let {
            it.getStringExtra(HASHTAG)?.split(",")?.let { it1 ->
                removeHashtags.addAll(it1)
                buttonEnableDisable()
            }
        }

        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            finish()
        }

        binding.etHashtag.textChanges()
            .debounce(400, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeAndObserveOnMainThread {
                if (it.length > 3) {
                    hashtagViewModel.resetPagination(it.toString())
                } else {
                    hashtagViewModel.resetPagination()
                }
            }.autoDispose()

        binding.addMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            if(!binding.etHashtag.text.toString().isNullOrEmpty()) {
//                stringInfo = stringInfo.plus(if(stringInfo.isNullOrEmpty()) "" else " ").plus(if(binding.etHashtag.text.toString().get(0) == '#') binding.etHashtag.text.toString() else "#".plus(binding.etHashtag.text.toString()))
                if(binding.etHashtag.text.toString().get(0) == '#') {
                    removeHashtags.add(binding.etHashtag.text.toString())
                } else {
                    removeHashtags.add("#${binding.etHashtag.text.toString()}")
                }

                buttonEnableDisable()
                binding.etHashtag.text?.clear()
                removeHashtagAdapter.listOfHashtagInfo = removeHashtags
            }

//            binding.addhashtagListAppCompatEditText.text = stringInfo
        }


        hashtagAdapter = HashtagAdapter(this@HashtagActivity).apply {
            hashtagClick.subscribeAndObserveOnMainThread {
//                stringInfo = stringInfo.plus(if(stringInfo.isNullOrEmpty()) "" else " ").plus(it.title.toString())
//                binding.addhashtagListAppCompatEditText.text = stringInfo
                removeHashtags.add(it.title.toString())
                removeHashtagAdapter.listOfHashtagInfo = removeHashtags
                buttonEnableDisable()
            }.autoDispose()
        }

        binding.hashtagRecyclerView.apply {
            adapter = hashtagAdapter
            layoutManager =
                LinearLayoutManager(this@HashtagActivity, LinearLayoutManager.VERTICAL, false)

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, state: Int) {
                    super.onScrollStateChanged(recyclerView, state)
                    if (state == RecyclerView.SCROLL_STATE_IDLE) {
                        val layoutManager = recyclerView.layoutManager ?: return
                       hashtagViewModel.loadMore()
                    }
                }
            })
        }
    }
}