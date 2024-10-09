package com.outgoer.ui.story.view

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.story.model.MentionUser
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.SpontyUserViewBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class StoryUserView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val storyUserActionStateSubject: PublishSubject<MentionUser> = PublishSubject.create()
    val storyUserActionState: Observable<MentionUser> = storyUserActionStateSubject.hide()

    private var binding: SpontyUserViewBinding? = null
    private lateinit var mentionUser: MentionUser

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.sponty_user_view, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = SpontyUserViewBinding.bind(view)

        binding?.apply {
            rlMain.throttleClicks().subscribeAndObserveOnMainThread {
                storyUserActionStateSubject.onNext(mentionUser)
            }
        }
    }

    fun bind(userViewedStory: MentionUser) {
        this.mentionUser = userViewedStory
        binding?.apply {
            if (userViewedStory.userType == MapVenueUserType.VENUE_OWNER.type) {
                usernameAppCompatTextView.text = userViewedStory.name ?: ""
            } else {
                usernameAppCompatTextView.text = userViewedStory.username ?: ""
            }
            ivVerified.isVisible = userViewedStory.profileVerified ?: 0 == 1

            Glide.with(context)
                .load(userViewedStory.avatar)
                .centerCrop()
                .placeholder(
                    resources.getDrawable(
                        R.drawable.ic_chat_user_placeholder,
                        null
                    )
                )
                .into(ivProfile)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}