package com.outgoer.ui.add_hashtag.view

import android.content.Context
import android.view.View
import com.outgoer.R
import com.outgoer.api.hashtag.model.HashtagResponse
import com.outgoer.api.music.model.MusicResponse
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.HashtagItemViewBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class HashtagView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val hashtagClickSubject: PublishSubject<HashtagResponse> = PublishSubject.create()
    val hashtagClick: Observable<HashtagResponse> = hashtagClickSubject.hide()

    private lateinit var binding: HashtagItemViewBinding
    private lateinit var hashtagResponse: HashtagResponse

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.hashtag_item_view, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = HashtagItemViewBinding.bind(view)

        binding.apply {
            rlMain.throttleClicks().subscribeAndObserveOnMainThread {
                hashtagClickSubject.onNext(hashtagResponse)
            }.autoDispose()
        }
    }

    fun bind(hashtagRes: HashtagResponse) {
        this.hashtagResponse = hashtagRes
        binding.apply {
            musicTitleAppCompatTextView.text = hashtagResponse.title
            singerNameAppCompatTextView.text = "0 public post"
        }
    }
}