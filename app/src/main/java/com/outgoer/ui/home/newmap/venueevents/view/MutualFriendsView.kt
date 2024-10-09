package com.outgoer.ui.home.newmap.venueevents.view

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.event.model.MutualFriends
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.MutualfriendsBinding
import com.outgoer.databinding.ViewPostLikesBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class MutualFriendsView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val profileViewClickSubject: PublishSubject<MutualFriends> = PublishSubject.create()
    val profileViewClick: Observable<MutualFriends> = profileViewClickSubject.hide()

    private var binding: MutualfriendsBinding? = null
    private var mutualFriends: MutualFriends? = null

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout. mutualfriends, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = MutualfriendsBinding.bind(view)
        binding?.apply {
            profileLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
                mutualFriends?.let { it1 -> profileViewClickSubject.onNext(it1) }
            }
        }
    }

    fun bind(mutualFriend: MutualFriends) {
        this.mutualFriends = mutualFriend
        val icPlaceHolderProfile =
            ContextCompat.getDrawable(context, R.drawable.ic_chat_user_placeholder)
        binding?.let { binding ->
            binding.tvUsername.text = mutualFriend.name
            binding.tvDescription.text = mutualFriend.username
            binding.profileLinearLayout.background = null

            Glide.with(context)
                .load(mutualFriend.avatar)
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .into(binding.ivUserProfile)
        }
    }
}