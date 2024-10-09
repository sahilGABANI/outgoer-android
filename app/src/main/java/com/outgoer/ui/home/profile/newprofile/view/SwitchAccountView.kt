package com.outgoer.ui.home.profile.newprofile.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.post.model.PeopleForTag
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.TagPeopleViewBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SwitchAccountView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val tagPeopleClickSubject: PublishSubject<PeopleForTag> = PublishSubject.create()
    val tagPeopleClick: Observable<PeopleForTag> = tagPeopleClickSubject.hide()

    private lateinit var binding: TagPeopleViewBinding
    private lateinit var peopleForTag: PeopleForTag

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.tag_people_view, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = TagPeopleViewBinding.bind(view)

        binding.apply {
            rlMain.throttleClicks().subscribeAndObserveOnMainThread {
                tagPeopleClickSubject.onNext(peopleForTag)
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
                ivCheck.setImageResource(R.drawable.ic_switch_select)
            } else {
                ivCheck.setImageResource(R.drawable.ic_switch_deselect)
            }
        }
    }
}