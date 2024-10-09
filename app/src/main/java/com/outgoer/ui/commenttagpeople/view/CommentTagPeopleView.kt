package com.outgoer.ui.commenttagpeople.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewCommentTagPeopleBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class CommentTagPeopleView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val commentTagPeopleClickSubject: PublishSubject<FollowUser> = PublishSubject.create()
    val commentTagPeopleClick: Observable<FollowUser> = commentTagPeopleClickSubject.hide()

    private lateinit var binding: ViewCommentTagPeopleBinding
    private lateinit var followUser: FollowUser

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_comment_tag_people, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewCommentTagPeopleBinding.bind(view)

        binding.apply {
            rlMain.throttleClicks().subscribeAndObserveOnMainThread {
                commentTagPeopleClickSubject.onNext(followUser)
            }.autoDispose()
        }
    }

    fun bind(followUser: FollowUser) {
        this.followUser = followUser
        binding.apply {
            tvUsername.text = followUser.username ?: ""

            Glide.with(context)
                .load(followUser.avatar)
                .centerCrop()
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .into(ivUserProfile)
        }
    }
}