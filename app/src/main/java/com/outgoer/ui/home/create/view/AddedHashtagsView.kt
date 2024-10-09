package com.outgoer.ui.home.create.view

import android.content.Context
import android.view.View
import com.outgoer.R
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.NewReelsHashtagViewBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class AddedHashtagsView(context: Context) : ConstraintLayoutWithLifecycle(context)  {

    private val removeItemClickStateSubject: PublishSubject<String> = PublishSubject.create()
    val removeItemClick: Observable<String> = removeItemClickStateSubject.hide()

    private lateinit var binding: NewReelsHashtagViewBinding
    private lateinit var hashtagsItem :String

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.new_reels_hashtag_view, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = NewReelsHashtagViewBinding.bind(view)

        binding.apply {
            removeAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
                removeItemClickStateSubject.onNext(hashtagsItem)
            }
        }
    }

    fun bind(hashtagsItem: String, isHashtagRemove: Boolean) {
        this.hashtagsItem = hashtagsItem

        binding.removeHashtagConstraintLayout.visibility = View.VISIBLE
        binding.tvHashtag.visibility = View.GONE

        binding.hashTagAppCompatTextView.text = hashtagsItem

        binding.removeAppCompatImageView.visibility = if(isHashtagRemove) View.VISIBLE else View.GONE

    }
}