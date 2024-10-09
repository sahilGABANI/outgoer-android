package com.outgoer.ui.chat.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.group.model.GroupUserInfo
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewCommentTagPeopleBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class GroupUserTagView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val groupUserClickSubject: PublishSubject<GroupUserInfo> = PublishSubject.create()
    val groupUserClick: Observable<GroupUserInfo> = groupUserClickSubject.hide()

    private lateinit var binding: ViewCommentTagPeopleBinding
    private lateinit var groupUserInfo: GroupUserInfo

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_comment_tag_people, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewCommentTagPeopleBinding.bind(view)

        binding.apply {
            rlMain.throttleClicks().subscribeAndObserveOnMainThread {
                groupUserClickSubject.onNext(groupUserInfo)
            }.autoDispose()
        }
    }

    fun bind(groupUser: GroupUserInfo) {
        this.groupUserInfo = groupUser
        binding.apply {
            tvUsername.text = groupUser.username ?: ""

            Glide.with(context)
                .load(groupUser.profileUrl)
                .centerCrop()
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .into(ivUserProfile)
        }
    }
}