package com.outgoer.ui.sponty.view

import android.content.Context
import android.text.SpannableStringBuilder
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.github.marlonlom.utilities.timeago.TimeAgoMessages
import com.outgoer.R
import com.outgoer.api.post.model.PostCommentActionState
import com.outgoer.api.post.model.SpontyActionState
import com.outgoer.api.post.model.SpontyCommentActionState
import com.outgoer.api.sponty.model.SpontyCommentResponse
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.SpontyReplyListItemBinding
import com.outgoer.ui.comment.view.PostCommentAdapter
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.text.SimpleDateFormat
import java.util.*


class SpontyReplyListView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val spontyCommentActionStateSubject: PublishSubject<SpontyCommentActionState> = PublishSubject.create()
    val spontyCommentActionState: Observable<SpontyCommentActionState> = spontyCommentActionStateSubject.hide()


    private var binding: SpontyReplyListItemBinding? = null
    private lateinit var spontyCommentResponse: SpontyCommentResponse
    private lateinit var spontyReplyListAdapter: SpontyReplyListAdapter
    var localeByLanguageTag: Locale = Locale.forLanguageTag("en")
    var sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH) // 2021-03-24 13:12:18
    var messages: TimeAgoMessages = TimeAgoMessages.Builder().withLocale(localeByLanguageTag).build()
    var calendar: Calendar = Calendar.getInstance()

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.sponty_reply_list_item, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = SpontyReplyListItemBinding.bind(view)
        spontyReplyListAdapter = SpontyReplyListAdapter(context).apply {
            spontyCommentActionState.subscribe { spontyCommentActionStateSubject.onNext(it) }
        }
        binding?.apply {
            replyRecyclerView.apply {
                adapter = spontyReplyListAdapter
            }
            removeCommentAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
//                spontyCommentActionStateSubject.onNext(SpontyCommentActionState.)
            }
            ivProfile.throttleClicks().subscribeAndObserveOnMainThread {
                spontyCommentActionStateSubject.onNext(SpontyCommentActionState.UserImageClick(spontyCommentResponse))
            }
            tvReply.throttleClicks().subscribeAndObserveOnMainThread {

                spontyCommentActionStateSubject.onNext(SpontyCommentActionState.ReplyComment(spontyCommentResponse))
            }
            rlComment.throttleClicks().subscribeAndObserveOnMainThread {
                if (spontyCommentResponse.parentId != null){
                    spontyCommentActionStateSubject.onNext(SpontyCommentActionState.ClickComment(spontyCommentResponse,true))
                } else {
                    spontyCommentActionStateSubject.onNext(SpontyCommentActionState.ClickComment(spontyCommentResponse,false))
                }
            }.autoDispose()
            tvComment.setOnMentionClickListener { _, text ->
                spontyCommentActionStateSubject.onNext(SpontyCommentActionState.TaggedUser(text.toString(), spontyCommentResponse))
            }
            ivLike.throttleClicks().subscribeAndObserveOnMainThread {
                spontyCommentResponse.commentLike = !spontyCommentResponse.commentLike
                if (spontyCommentResponse.commentLike) {
                    spontyCommentResponse.totalLikes = spontyCommentResponse.totalLikes?.plus(1)
                    updateCommentLike()
                    spontyCommentActionStateSubject.onNext(SpontyCommentActionState.Like(spontyCommentResponse))
                } else {
                    spontyCommentResponse.totalLikes = spontyCommentResponse.totalLikes?.minus(1)
                    updateCommentLike()
                    spontyCommentActionStateSubject.onNext(SpontyCommentActionState.DisLike(spontyCommentResponse))
                }
            }
        }
    }

    fun bind(sponty: SpontyCommentResponse, userId: Int) {
        spontyCommentResponse = sponty
        spontyReplyListAdapter.listOfSponty = sponty.replies ?: listOf()
        binding?.apply {
//            ivLike.visibility = View.GONE
//            tvReply.visibility = View.GONE

            if (spontyCommentResponse.parentId != 0) {
                tvReply.visibility = GONE
            } else {
                tvReply.visibility = VISIBLE
            }
            sponty.user?.let { user ->
                Glide.with(context)
                    .load(user.avatar)
                    .centerCrop()
                    .placeholder(R.drawable.ic_chat_user_placeholder)
                    .into(ivProfile)
                updateCommentLike()


                usernameAppCompatTextView.text = sponty.user?.username ?: ""
                val userName = sponty.user?.username ?: ""
                val spaceSize = (userName.length * 2.3).toInt()
                val space = " ".repeat(spaceSize)
                tvComment.text = "$space${sponty.comment}"

                tvDateTime.text = sponty.humanReadableTime
            }
            ivVerified.isVisible = sponty.user?.profileVerified == 1
        }

    }

    private fun updateCommentLike() {
        binding?.apply {
            if (this@SpontyReplyListView.spontyCommentResponse.commentLike) {
                ivLike.setImageResource(R.drawable.ic_post_filled_like)
                ivLike.setColorFilter(ContextCompat.getColor(getContext(), com.clk.progress.R.color.transparent))
            } else {
                ivLike.setImageResource(R.drawable.ic_post_like)
                ivLike.setColorFilter(ContextCompat.getColor(getContext(), R.color.grey_medium));
            }
            if (this@SpontyReplyListView.spontyCommentResponse.totalLikes != 0) {
                tvLikeCount.visibility = View.VISIBLE
                tvLikeCount.text = context.getString(R.string.post_like, this@SpontyReplyListView.spontyCommentResponse.totalLikes ?: 0)
            } else {
                tvLikeCount.visibility = View.GONE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}