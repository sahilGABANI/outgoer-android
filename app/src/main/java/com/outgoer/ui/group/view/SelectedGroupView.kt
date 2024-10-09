package com.outgoer.ui.group.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.SelectedItemsBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SelectedGroupView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val removeItemClickStateSubject: PublishSubject<FollowUser> = PublishSubject.create()
    val removeItemClick: Observable<FollowUser> = removeItemClickStateSubject.hide()

    private var binding: SelectedItemsBinding? = null
    private lateinit var userInfo: FollowUser

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.selected_items, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = SelectedItemsBinding.bind(view)

        binding?.apply {
            removeAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
                removeItemClickStateSubject.onNext(userInfo)
            }.autoDispose()
        }
    }

    fun bind(user: FollowUser) {
        this.userInfo = user
        binding?.let {
            Glide.with(context)
                .load(user.avatar)
                .centerCrop()
                .placeholder(resources.getDrawable(R.drawable.ic_chat_user_placeholder, null))
                .into(it.ivUserProfileImage)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}
