package com.outgoer.ui.login.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.follow.model.SuggestedUser
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewCommentTagPeopleBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SuggestedUserView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val suggestedActionStateSubject: PublishSubject<SuggestedUser> = PublishSubject.create()
    val suggestedActionState: Observable<SuggestedUser> = suggestedActionStateSubject.hide()

    private var binding: ViewCommentTagPeopleBinding? = null
    private lateinit var suggestedUser: SuggestedUser

    init {
        inflateUi()
    }

    private fun inflateUi() {

        val view = View.inflate(context, R.layout.view_comment_tag_people, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        binding = ViewCommentTagPeopleBinding.bind(view)
        binding?.apply {
            rlMain.throttleClicks().subscribeAndObserveOnMainThread {
                suggestedActionStateSubject.onNext(suggestedUser)
            }.autoDispose()
        }
    }

    fun bind(suggestUser: SuggestedUser) {
        this.suggestedUser = suggestUser
        binding?.apply {
            tvUsername.text = suggestUser.uName
            Glide.with(context)
                .load(suggestUser.avatar)
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .into(ivUserProfile)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}