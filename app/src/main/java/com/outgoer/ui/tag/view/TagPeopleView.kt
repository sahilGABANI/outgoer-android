package com.outgoer.ui.tag.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.post.model.PeopleForTag
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.TagPeopleViewBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class TagPeopleView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val tagPeopleClickSubject: PublishSubject<PeopleForTag> = PublishSubject.create()
    val tagPeopleClick: Observable<PeopleForTag> = tagPeopleClickSubject.hide()

    private val tagFollowClickSubject: PublishSubject<FollowUser> = PublishSubject.create()
    val tagFollowClick: Observable<FollowUser> = tagFollowClickSubject.hide()

    private lateinit var binding: TagPeopleViewBinding
    private lateinit var peopleForTag: PeopleForTag
    private lateinit var followUser: FollowUser

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.tag_people_view, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = TagPeopleViewBinding.bind(view)

        binding.apply {
            rlMain.throttleClicks().subscribeAndObserveOnMainThread {
                if(::peopleForTag.isInitialized)
                    tagPeopleClickSubject.onNext(peopleForTag)
                else if(::followUser.isInitialized)
                    tagFollowClickSubject.onNext(followUser)
            }.autoDispose()

            ivCheck.throttleClicks().subscribeAndObserveOnMainThread {
                if(::peopleForTag.isInitialized)
                    tagPeopleClickSubject.onNext(peopleForTag)
                else if(::followUser.isInitialized)
                    tagFollowClickSubject.onNext(followUser)
            }.autoDispose()
        }
    }

    fun bind(peopleForTag: PeopleForTag) {
        this.peopleForTag = peopleForTag
        binding.apply {
            userNameAppCompatTextView.text = peopleForTag.username
            fullNameAppCompatTextView.text = peopleForTag.name
            Glide.with(context)
                .load(peopleForTag.avatar)
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .centerCrop()
                .into(ivProfile)

            if (peopleForTag.isSelected) {
                ivCheck.setImageResource(R.drawable.ic_selected_radio)
            } else {
                ivCheck.setImageResource(R.drawable.ic_not_selected_radio)
            }
        }
    }

    fun bindFollow(followUser: FollowUser) {
        this.followUser = followUser
        binding.apply {
            userNameAppCompatTextView.text = followUser.username
            fullNameAppCompatTextView.text = followUser.name
            Glide.with(context)
                .load(followUser.avatar)
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .centerCrop()
                .into(ivProfile)

            if (followUser.isSelected) {
                ivCheck.setImageResource(R.drawable.ic_selected_radio)
            } else {
                ivCheck.setImageResource(R.drawable.ic_not_selected_radio)
            }
        }
    }
}