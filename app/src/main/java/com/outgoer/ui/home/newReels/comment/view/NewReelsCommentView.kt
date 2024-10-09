package com.outgoer.ui.home.newReels.comment.view

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.github.marlonlom.utilities.timeago.TimeAgo
import com.github.marlonlom.utilities.timeago.TimeAgoMessages
import com.outgoer.R
import com.outgoer.api.reels.model.ReelCommentInfo
import com.outgoer.api.reels.model.ReelsCommentPageState
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewNewReelsCommentBinding
import com.outgoer.utils.formatTo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class NewReelsCommentView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val reelsCommentPageStateSubject: PublishSubject<ReelsCommentPageState> = PublishSubject.create()
    val reelsCommentPageState: Observable<ReelsCommentPageState> = reelsCommentPageStateSubject.hide()

    private var binding: ViewNewReelsCommentBinding? = null
    private lateinit var newReelsCommentAdapter: NewReelsCommentAdapter

    private lateinit var reelCommentInfo: ReelCommentInfo

    var localeByLanguageTag: Locale = Locale.forLanguageTag("en")
    var sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH) // 2021-03-24 13:12:18
    var messages: TimeAgoMessages = TimeAgoMessages.Builder().withLocale(localeByLanguageTag).build()
    var calendar: Calendar = Calendar.getInstance()

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_new_reels_comment, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewNewReelsCommentBinding.bind(view)

        newReelsCommentAdapter = NewReelsCommentAdapter(context).apply {
            reelsCommentPageState.subscribe { reelsCommentPageStateSubject.onNext(it) }
        }

        binding?.apply {
            replyRecyclerView.apply {
                adapter = newReelsCommentAdapter
            }

            tvComment.setOnMentionClickListener { _, text ->
                reelsCommentPageStateSubject.onNext(ReelsCommentPageState.TaggedUser(text.toString(), reelCommentInfo))
            }

            ivLike.throttleClicks().subscribeAndObserveOnMainThread {
                reelCommentInfo.reelsCommentLike = !reelCommentInfo.reelsCommentLike
                if (reelCommentInfo.reelsCommentLike) {
                    reelCommentInfo.totalLikes = reelCommentInfo.totalLikes?.plus(1)
                    updateCommentLike()
                    reelsCommentPageStateSubject.onNext(ReelsCommentPageState.Like(reelCommentInfo))
                } else {
                    reelCommentInfo.totalLikes = reelCommentInfo.totalLikes?.minus(1)
                    updateCommentLike()
                    reelsCommentPageStateSubject.onNext(ReelsCommentPageState.DisLike(reelCommentInfo))
                }
            }.autoDispose()

            tvReply.throttleClicks().subscribeAndObserveOnMainThread {
                reelsCommentPageStateSubject.onNext(ReelsCommentPageState.ReplyComment(reelCommentInfo))
            }.autoDispose()

            rlComment.throttleClicks().subscribeAndObserveOnMainThread {
                reelsCommentPageStateSubject.onNext(ReelsCommentPageState.ClickComment(reelCommentInfo))
            }.autoDispose()

            rivUserProfile.throttleClicks().subscribeAndObserveOnMainThread {
                reelsCommentPageStateSubject.onNext(ReelsCommentPageState.UserImageClick(reelCommentInfo))
            }.autoDispose()
        }
    }

    fun bind(reelCommentInfo: ReelCommentInfo) {
        this.reelCommentInfo = reelCommentInfo

        binding?.apply {
            newReelsCommentAdapter.listOfReelsComment = reelCommentInfo.replies ?: listOf()

            usernameAppCompatTextView.text = reelCommentInfo.commentUserInfo?.username ?: ""
            val userName = reelCommentInfo.commentUserInfo?.username ?: ""
            val spaceSize = (userName.length * 2.3).toInt()
            val space = " ".repeat(spaceSize)
            tvComment.text = "$space${reelCommentInfo.comment}"

            Glide.with(context)
                .load(reelCommentInfo.commentUserInfo?.avatar)
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .into(rivUserProfile)

            updateCommentLike()

            if (reelCommentInfo.parentId != null) {
                tvReply.visibility = GONE
            } else {
                tvReply.visibility = VISIBLE
            }

            reelCommentInfo.createdAt?.let { createdAt ->
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

            ivVerified.isVisible = reelCommentInfo.commentUserInfo?.profileVerified == 1

        }
    }

    private fun updateCommentLike() {
        binding?.apply {
            if (this@NewReelsCommentView.reelCommentInfo.reelsCommentLike) {
                ivLike.setImageResource(R.drawable.ic_post_filled_like)
                ivLike.setColorFilter(ContextCompat.getColor(getContext(), com.clk.progress.R.color.transparent))
            } else {
                ivLike.setImageResource(R.drawable.ic_post_like)
                ivLike.setColorFilter(ContextCompat.getColor(getContext(), R.color.grey_medium))
            }
            if (this@NewReelsCommentView.reelCommentInfo.totalLikes != 0) {
                tvLikeCount.visibility = View.VISIBLE
                tvLikeCount.text = context.getString(R.string.post_like, this@NewReelsCommentView.reelCommentInfo.totalLikes ?: 0)
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