package com.outgoer.ui.comment.view

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.github.marlonlom.utilities.timeago.TimeAgo
import com.github.marlonlom.utilities.timeago.TimeAgoMessages
import com.outgoer.R
import com.outgoer.api.post.model.CommentInfo
import com.outgoer.api.post.model.PostCommentActionState
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewCommentBinding
import com.outgoer.utils.formatTo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone


class PostCommentView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val postCommentActionStateSubject: PublishSubject<PostCommentActionState> = PublishSubject.create()
    val postCommentActionState: Observable<PostCommentActionState> = postCommentActionStateSubject.hide()

    private var binding: ViewCommentBinding? = null
    private lateinit var postCommentAdapter: PostCommentAdapter

    private lateinit var commentInfo: CommentInfo

    var localeByLanguageTag: Locale = Locale.forLanguageTag("en")
    var sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH) // 2021-03-24 13:12:18
    var messages: TimeAgoMessages = TimeAgoMessages.Builder().withLocale(localeByLanguageTag).build()
    var calendar: Calendar = Calendar.getInstance()

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_comment, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewCommentBinding.bind(view)

        postCommentAdapter = PostCommentAdapter(context).apply {
            postCommentActionState.subscribe { postCommentActionStateSubject.onNext(it) }
        }

        binding?.apply {
            replyRecyclerView.apply {
                adapter = postCommentAdapter
            }

            tvComment.setOnMentionClickListener { _, text ->
                postCommentActionStateSubject.onNext(PostCommentActionState.TaggedUser(text.toString(), commentInfo))
            }

            ivLike.throttleClicks().subscribeAndObserveOnMainThread {
                commentInfo.commentLike = !commentInfo.commentLike
                if (commentInfo.commentLike) {
                    commentInfo.totalLikes = commentInfo.totalLikes?.plus(1)
                    updateCommentLike()
                    postCommentActionStateSubject.onNext(PostCommentActionState.Like(commentInfo))
                } else {
                    commentInfo.totalLikes = commentInfo.totalLikes?.minus(1)
                    updateCommentLike()
                    postCommentActionStateSubject.onNext(PostCommentActionState.DisLike(commentInfo))
                }
            }.autoDispose()

            tvReply.throttleClicks().subscribeAndObserveOnMainThread {
                postCommentActionStateSubject.onNext(PostCommentActionState.ReplyComment(commentInfo))
            }.autoDispose()

            rlComment.throttleClicks().subscribeAndObserveOnMainThread {
                if (commentInfo.parentId != null){
                    postCommentActionStateSubject.onNext(PostCommentActionState.ClickComment(commentInfo,true))
                } else {
                    postCommentActionStateSubject.onNext(PostCommentActionState.ClickComment(commentInfo,false))
                }
            }.autoDispose()

            rivUserProfile.throttleClicks().subscribeAndObserveOnMainThread {
                postCommentActionStateSubject.onNext(PostCommentActionState.UserImageClick(commentInfo))
            }.autoDispose()
        }
    }

    fun bind(commentInfo: CommentInfo) {
        this.commentInfo = commentInfo
        binding?.apply {
            postCommentAdapter.listOfCommentInfo = commentInfo.replies ?: listOf()

            usernameAppCompatTextView.text = commentInfo.commentUserInfo?.username ?: ""
            val userName = commentInfo.commentUserInfo?.username ?: ""
            val spaceSize = (userName.length * 2.3).toInt()
            val space = " ".repeat(spaceSize)
            tvComment.text = "$space${commentInfo.comment}"


            Glide.with(context)
                .load(commentInfo.commentUserInfo?.avatar)
                .placeholder(R.drawable.venue_placeholder)
                .into(rivUserProfile)

            updateCommentLike()

            if (commentInfo.parentId != null) {
                tvReply.visibility = GONE
            } else {
                tvReply.visibility = VISIBLE
            }

            commentInfo.createdAt?.let { createdAt ->
                tvDateTime.text = createdAt
                try {
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                    val date = sdf.parse(createdAt)?.formatTo(TimeZone.getDefault(), sdf) ?: return
                    calendar.time = sdf.parse(date) ?: return
                    val timeMessage = TimeAgo.using(calendar.timeInMillis, messages)
                    tvDateTime.text = timeMessage
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
            ivVerified.isVisible = commentInfo.commentUserInfo?.profileVerified == 1
        }
    }

    private fun updateCommentLike() {
        binding?.apply {
            if (this@PostCommentView.commentInfo.commentLike) {
                ivLike.setImageResource(R.drawable.ic_post_filled_like)
                ivLike.setColorFilter(ContextCompat.getColor(getContext(), com.clk.progress.R.color.transparent))
            } else {
                ivLike.setImageResource(R.drawable.ic_post_like)
                ivLike.setColorFilter(ContextCompat.getColor(getContext(), R.color.grey_medium));
            }
            if (this@PostCommentView.commentInfo.totalLikes != 0) {
                tvLikeCount.visibility = View.VISIBLE
                tvLikeCount.text = context.getString(R.string.post_like, this@PostCommentView.commentInfo.totalLikes ?: 0)
            } else {
                tvLikeCount.visibility = View.GONE
            }
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}