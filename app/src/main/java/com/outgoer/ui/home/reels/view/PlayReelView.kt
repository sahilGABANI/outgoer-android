package com.outgoer.ui.home.reels.view

import android.annotation.SuppressLint
import android.content.Context
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.method.ScrollingMovementMethod
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.reels.model.ReelInfo
import com.outgoer.api.reels.model.ReelsPageState
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.extension.prettyCount
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.base.view.DoubleTapLikeView
import com.outgoer.databinding.PlayReelViewBinding
import com.outgoer.videoplayer.VideoDoubleClick
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject
import kotlin.properties.Delegates

class PlayReelView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val playReelViewClicksSubject: PublishSubject<ReelsPageState> = PublishSubject.create()
    val playReelViewClicks: Observable<ReelsPageState> = playReelViewClicksSubject.hide()

    private var binding: PlayReelViewBinding? = null
    private lateinit var reelInfo: ReelInfo

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var loggedInUserId by Delegates.notNull<Int>()

    init {
        inflateUi()
    }

    private fun inflateUi() {
        OutgoerApplication.component.inject(this)
        loggedInUserId = loggedInUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0

        val view = View.inflate(context, R.layout.play_reel_view, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        binding = PlayReelViewBinding.bind(view)

        binding?.apply {
            outgoerVideoPlayer.setVideoDoubleClick(object : VideoDoubleClick {
                override fun onDoubleClick() {
                    if (!reelInfo.reelsLike) {
                        updateLikeStatusCount()
                    }
                    DoubleTapLikeView().animateIcon(ivDoubleTapToLike)
                }

                override fun onSingleClick() {
                }
            })

            ivMutePlayer.throttleClicks().subscribeAndObserveOnMainThread {
                val isMute = !reelInfo.isMute
                outgoerVideoPlayer.isVideMute = isMute
                if (isMute) {
                    outgoerVideoPlayer.mute()
                } else {
                    outgoerVideoPlayer.unMute()
                }
                playReelViewClicksSubject.onNext(ReelsPageState.MuteUnmuteClick(isMute))
            }

            ivUserProfile.throttleClicks().subscribeAndObserveOnMainThread {
                playReelViewClicksSubject.onNext(ReelsPageState.UserProfileClick(reelInfo))
            }.autoDispose()

            tvReelDescription.throttleClicks().subscribeAndObserveOnMainThread {
                val reelsTagsList = reelInfo.reelsTags
                if (!reelsTagsList.isNullOrEmpty()) {
                    playReelViewClicksSubject.onNext(ReelsPageState.TaggedPeopleClick(reelInfo))
                }
            }.autoDispose()

            ivLike.throttleClicks().subscribeAndObserveOnMainThread {
                updateLikeStatusCount()
            }.autoDispose()

            ivComment.throttleClicks().subscribeAndObserveOnMainThread {
                playReelViewClicksSubject.onNext(ReelsPageState.CommentClick(reelInfo))
            }.autoDispose()

            ivShare.throttleClicks().subscribeAndObserveOnMainThread {
                playReelViewClicksSubject.onNext(ReelsPageState.ShareClick(reelInfo))
            }.autoDispose()

            ivBookmark.throttleClicks().subscribeAndObserveOnMainThread {
                reelInfo.bookmarkStatus = !reelInfo.bookmarkStatus
                if (reelInfo.bookmarkStatus) {
                    updateReelBookmark()
                    playReelViewClicksSubject.onNext(ReelsPageState.AddBookmarkClick(reelInfo))
                } else {
                    updateReelBookmark()
                    playReelViewClicksSubject.onNext(ReelsPageState.RemoveBookmarkClick(reelInfo))
                }
            }.autoDispose()

            ivMore.throttleClicks().subscribeAndObserveOnMainThread {
                if (loggedInUserId != reelInfo.userId) {
                    playReelViewClicksSubject.onNext(ReelsPageState.MoreClick(reelInfo,true))
                } else {
                    playReelViewClicksSubject.onNext(ReelsPageState.MoreClick(reelInfo,false))
                }
            }.autoDispose()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun bind(reelInfo: ReelInfo) {
        this.reelInfo = reelInfo
        binding?.apply {
            val userName = reelInfo.user?.username ?: ""

            tvUsername.text = userName
            tvUserAbout.text = reelInfo.user?.about ?: ""

            Glide.with(context)
                .load(reelInfo.user?.avatar)
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .circleCrop()
                .into(ivUserProfile)

            tvReelDescription.setOnTouchListener { p0, _ ->
                p0?.parent?.requestDisallowInterceptTouchEvent(true)
                false
            }
            tvReelDescription.movementMethod = ScrollingMovementMethod()
            Glide.with(feedThumbnailView.context).load(reelInfo.thumbnailUrl).preload()
            Glide.with(feedThumbnailView.context).load(reelInfo.thumbnailUrl).diskCacheStrategy(DiskCacheStrategy.ALL).into(feedThumbnailView)
            val caption = reelInfo.caption ?: ""
            val captionWithTagText = if (caption.isNotEmpty()) {
                caption.plus(" ")
            } else {
                ""
            }

            val reelsTagsList = reelInfo.reelsTags
            if (!reelsTagsList.isNullOrEmpty()) {
                tvReelDescription.visibility = View.VISIBLE
                val postInfoSize = reelsTagsList.size
                when {
                    postInfoSize == 1 -> {
                        val userNameFirst = reelsTagsList[0].user?.username ?: ""
                        val userNameWithTagText = SpannableStringBuilder()
                            .append(captionWithTagText)
                            .append(" ")
                            .bold { append("@".plus(userNameFirst)) }
                        tvReelDescription.text = userNameWithTagText
                    }
                    postInfoSize == 2 -> {
                        val userNameFirst = reelsTagsList[0].user?.username ?: ""
                        val userNameSecond = reelsTagsList[1].user?.username ?: ""
                        val userNameWithTagText = SpannableStringBuilder()
                            .append(captionWithTagText)
                            .append(" ")
                            .bold { append("@".plus(userNameFirst)) }
                            .append(", ")
                            .bold { append("@".plus(userNameSecond)) }
                        tvReelDescription.text = userNameWithTagText
                    }
                    postInfoSize >= 3 -> {
                        val userNameFirst = reelsTagsList[0].user?.username ?: ""
                        val userNameSecond = reelsTagsList[1].user?.username ?: ""

                        val otherCountColor = ContextCompat.getColor(context, R.color.purple)
                        val otherWithCountText =
                            SpannableString((postInfoSize - 2).toString().plus(" ").plus(context.getString(R.string.label_other)))
                        otherWithCountText.setSpan(ForegroundColorSpan(otherCountColor), 0, otherWithCountText.length, 0)

                        val userNameWithTagText = SpannableStringBuilder()
                            .append(captionWithTagText)
                            .append(" ")
                            .bold { append("@".plus(userNameFirst)) }
                            .append(", ")
                            .bold { append("@".plus(userNameSecond)) }
                            .append(" ").append(context.getString(R.string.label_and)).append(" ")
                            .bold { append(otherWithCountText) }
                        tvReelDescription.text = userNameWithTagText
                    }
                    else -> {
                        if (caption.isNotEmpty()) {
                            tvReelDescription.visibility = View.VISIBLE
                            tvReelDescription.text = reelInfo.caption
                        } else {
                            tvReelDescription.visibility = View.GONE
                            tvReelDescription.text = ""
                        }
                    }
                }
            } else {
                if (caption.isNotEmpty()) {
                    tvReelDescription.visibility = View.VISIBLE
                    tvReelDescription.text = reelInfo.caption
                } else {
                    tvReelDescription.visibility = View.GONE
                    tvReelDescription.text = ""
                }
            }

            if (!reelInfo.reelLocation.isNullOrEmpty()) {
                tvReelLocation.visibility = View.VISIBLE
                tvReelLocation.text = reelInfo.reelLocation
            } else {
                tvReelLocation.visibility = View.GONE
                tvReelLocation.text = ""
            }

            updateReelLike()
            updateReelComment()
            updateReelBookmark()

            outgoerVideoPlayer.videoUrl = "${ reelInfo.videoUrl }?clientBandwidthHint=2.5"
            outgoerVideoPlayer.isVideMute = reelInfo.isMute

            if (reelInfo.isMute) {
                ivMutePlayer.setImageResource(R.drawable.ic_reel_mute)
            } else {
                ivMutePlayer.setImageResource(R.drawable.ic_reel_unmute)
            }

            Glide.with(context)
                .load(reelInfo.thumbnailUrl)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(outgoerVideoPlayer.posterImageView)
        }
    }

    private fun updateReelLike() {
        binding?.apply {
            if (reelInfo.reelsLike) {
                ivLike.setImageResource(R.drawable.ic_selected_like)
            } else {
                ivLike.setImageResource(R.drawable.ic_like)
            }

            val totalLikes = reelInfo.totalLikes
            if (totalLikes != null) {
                if (totalLikes != 0) {
                    tvLikeCount.text = totalLikes.prettyCount().toString()
                    tvLikeCount.visibility = View.VISIBLE
                } else {
                    tvLikeCount.text = ""
                    tvLikeCount.visibility = View.GONE
                }
            } else {
                tvLikeCount.text = ""
                tvLikeCount.visibility = View.GONE
            }
        }
    }

    private fun updateReelComment() {
        binding?.apply {
            val totalComments = reelInfo.totalComments
            if (totalComments != null) {
                if (totalComments != 0) {
                    tvCommentCount.text = totalComments.prettyCount().toString()
                    tvCommentCount.visibility = View.VISIBLE
                } else {
                    tvCommentCount.text = ""
                    tvCommentCount.visibility = View.GONE
                }
            } else {
                tvCommentCount.text = ""
                tvCommentCount.visibility = View.GONE
            }
        }
    }

    private fun updateReelBookmark() {
        binding?.apply {
            if (reelInfo.bookmarkStatus) {
                ivBookmark.setImageResource(R.drawable.ic_selected_bookmark)
            } else {
                ivBookmark.setImageResource(R.drawable.ic_bookmark)
            }
        }
    }

    private fun updateLikeStatusCount() {
        reelInfo.reelsLike = !reelInfo.reelsLike
        if (reelInfo.reelsLike) {
            reelInfo.totalLikes = reelInfo.totalLikes?.let { it + 1 } ?: 0
            updateReelLike()
            playReelViewClicksSubject.onNext(ReelsPageState.AddReelLikeClick(reelInfo))
        } else {
            reelInfo.totalLikes = reelInfo.totalLikes?.let { it - 1 } ?: 0
            updateReelLike()
            playReelViewClicksSubject.onNext(ReelsPageState.RemoveReelLikeClick(reelInfo))
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}