package com.outgoer.ui.chat.reaction_bottom_sheet.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.chat.model.Reaction
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.RecyclerReactedUserViewBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ReactedUserView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private lateinit var binding: RecyclerReactedUserViewBinding
    private lateinit var reaction: Reaction

    private val reactionClickSubject: PublishSubject<Reaction> = PublishSubject.create()
    val reactionClick: Observable<Reaction> = reactionClickSubject.hide()

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.recycler_reacted_user_view, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = RecyclerReactedUserViewBinding.bind(view)
    }

    fun bind(reaction: Reaction, loggedInUserId: Int) {
        this.reaction = reaction
        binding.apply {
            reactedUsername.text = reaction.name

            if (loggedInUserId == reaction.userId) {
                tvDesc.text = context.resources.getString(R.string.tap_to_remove)
            } else {
                tvDesc.text = reaction.username
            }

            mainLayout.throttleClicks().subscribeAndObserveOnMainThread {
                if (loggedInUserId == reaction.userId) {
                    reactionClickSubject.onNext(reaction)
                }
            }

            Glide.with(context)
                .load(reaction.profileUrl)
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .centerCrop()
                .into(ivUserProfileImage)

            when (reaction.reactionType) {
                "like" -> ivEmoji.setImageResource(R.drawable.ic_thumbs_up)
                "love" -> ivEmoji.setImageResource(R.drawable.ic_like_heart)
                "laughing" -> ivEmoji.setImageResource(R.drawable.ic_laughing)
                "expression" -> ivEmoji.setImageResource(R.drawable.ic_shock_emoji)
                "sad" -> ivEmoji.setImageResource(R.drawable.ic_sad_emoji)
                "pray" -> ivEmoji.setImageResource(R.drawable.ic_hive_five)
            }
        }
    }
}