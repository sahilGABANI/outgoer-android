package com.outgoer.ui.home.home.view

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.DisplayMetrics
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2

import cn.jzvd.Jzvd
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.outgoer.R
import com.outgoer.api.post.PostRepository
import com.outgoer.api.post.model.HomePagePostInfoState
import com.outgoer.api.post.model.PostInfo
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.application.OutgoerApplication

import com.outgoer.base.extension.prettyCount
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.base.view.DoubleTapLikeView
import com.outgoer.databinding.ViewHomePageVideoPostBinding
import com.outgoer.ui.home.HomeActivity
import com.outgoer.ui.home.newReels.NewReelsFragment
import com.outgoer.ui.home.newReels.view.NewReelsHashtagAdapter

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class HomePageVideoPostView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val homePagePostViewClickSubject: PublishSubject<HomePagePostInfoState> = PublishSubject.create()
    val homePagePostViewClick: Observable<HomePagePostInfoState> = homePagePostViewClickSubject.hide()


    private var binding: ViewHomePageVideoPostBinding? = null

    private lateinit var postInfo: PostInfo
    private var lastCurrentPosition: Int = 0
    private var lastCommentId: Int = 0
    private var lastDeleteCommentId: Int = 0
    private lateinit var hashtagAdapter: NewReelsHashtagAdapter
    private val colorDrawable = ColorDrawable(Color.parseColor("#616161"))

    @Inject
    lateinit var postRepository: PostRepository


    init {
        inflateUi()
        postRepository.commentCountState.subscribeAndObserveOnMainThread {
            if (postInfo.id == it.postId) {
                if (lastCommentId != it.id) {
                    lastCommentId = it.id
                    postInfo.totalComments = postInfo.totalComments?.plus(1)
                    updateReelComment()
                } else {
                    println("Last Comment is Same")
                }
            } else {
                println("Post is diff")
            }
        }.autoDispose()

        postRepository.deleteCommentState.subscribeAndObserveOnMainThread {
            if (postInfo.id == it.postId) {
                if (lastDeleteCommentId != it.id) {
                    lastDeleteCommentId = it.id
                    postInfo.totalComments = postInfo.totalComments?.minus(1)
                    updateReelComment()
                } else {
                    println("Last Comment is Same")
                }
            } else {
                println("Post is diff")
            }
        }.autoDispose()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_home_page_video_post, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        OutgoerApplication.component.inject(this)
        binding = ViewHomePageVideoPostBinding.bind(view)
        binding?.apply {
            hashtagAdapter = NewReelsHashtagAdapter(context).apply {
                reelsHashtagItemClicks.subscribeAndObserveOnMainThread {
                    homePagePostViewClickSubject.onNext(HomePagePostInfoState.HashtagItemClicks(it))
                }
            }
            rvHashtag.adapter = hashtagAdapter
            hashtagAdapter.isReels = true
//            mutePlayerImageView.throttleClicks().subscribeAndObserveOnMainThread {
//                mutePlayerImageView.isSelected = !mutePlayerImageView.isSelected
//                if (mutePlayerImageView.isSelected) {
//                    postVideoPlayer.isVideMute = true
//                    postVideoPlayer.mute()
//                    mutePlayerImageView.setImageResource(R.drawable.ic_post_mute)
//                } else {
//                    postVideoPlayer.isVideMute = false
//                    postVideoPlayer.unMute()
//                    mutePlayerImageView.setImageResource(R.drawable.ic_post_unmute)
//                }
//            }.autoDispose()

//            postVideoPlayer.setVideoDoubleClick(object : VideoDoubleClick {
//                override fun onDoubleClick() {
//                    updateLikeStatusCount()
//                    DoubleTapLikeView().animateIcon(ivDoubleTapToLike)
//                }
//            })
            ivUserProfile.throttleClicks().subscribeAndObserveOnMainThread {
                homePagePostViewClickSubject.onNext(HomePagePostInfoState.UserProfileClick(postInfo))
            }.autoDispose()

            tvUsername.throttleClicks().subscribeAndObserveOnMainThread {
                homePagePostViewClickSubject.onNext(HomePagePostInfoState.UserProfileClick(postInfo))
            }.autoDispose()


            ivMore.throttleClicks().subscribeAndObserveOnMainThread {
                homePagePostViewClickSubject.onNext(HomePagePostInfoState.MoreClick(postInfo))
            }.autoDispose()

            tvTagPeople.throttleClicks().subscribeAndObserveOnMainThread {
                val postTagsList = postInfo.postTags
                if (!postTagsList.isNullOrEmpty()) {
                    homePagePostViewClickSubject.onNext(HomePagePostInfoState.TaggedPeopleClick(postInfo))
                }
            }.autoDispose()

            tvPostDescription.setOnMentionClickListener { _, text ->
                postInfo.descriptionTags?.find { (it.user?.username?: "").equals(text.toString()) }?.apply {
                    user?.let {
                        homePagePostViewClickSubject.onNext(HomePagePostInfoState.OpenTaggedPeopleClick(user))
                    }
                }
            }

            ivTagPeople.throttleClicks().subscribeAndObserveOnMainThread {
                val postTagsList = postInfo.postTags
                if (!postTagsList.isNullOrEmpty()) {
                    homePagePostViewClickSubject.onNext(HomePagePostInfoState.TaggedPeopleClick(postInfo))
                }
            }.autoDispose()

            cvVenueTaggedContainer.throttleClicks().subscribeAndObserveOnMainThread {
                homePagePostViewClickSubject.onNext(HomePagePostInfoState.VenueTaggedProfileClick(postInfo))
            }.autoDispose()

            ivLike.throttleClicks().subscribeAndObserveOnMainThread {
                updateLikeStatusCount()
            }.autoDispose()

            tvLikeCount.throttleClicks().subscribeAndObserveOnMainThread {
                homePagePostViewClickSubject.onNext(HomePagePostInfoState.PostLikeCountClick(postInfo))
            }.autoDispose()

            commentLinearLayoutCompat.throttleClicks().subscribeAndObserveOnMainThread {
                homePagePostViewClickSubject.onNext(HomePagePostInfoState.CommentClick(postInfo))
            }.autoDispose()

            ivShare.throttleClicks().subscribeAndObserveOnMainThread {
                homePagePostViewClickSubject.onNext(HomePagePostInfoState.ShareClick(postInfo))
            }.autoDispose()

            ivBookmark.throttleClicks().subscribeAndObserveOnMainThread {
                updateBookmarkCount()
            }.autoDispose()

//            v.posterImageView.throttleClicks().subscribeAndObserveOnMainThread {
////                Timber.tag("inflateUi postVideoPlayer").e(postInfo.images?.firstOrNull()?.videoUrl)
//                postInfo.images?.firstOrNull()?.videoUrl?.let { it1 -> homePagePostViewClickSubject.onNext(HomePagePostInfoState.VideoViewClick(it1)) }
//            }.autoDispose()
        }
    }

    fun bind(postInfo: PostInfo, currentPosition: Int) {
        this.postInfo = postInfo

/*        val startColor = resources.getColor(R.color.color_76c1ed)
        val centerColor = resources.getColor(R.color.color_4152c1)
        val endColor = resources.getColor(R.color.color_4152c1)

        val gradientDrawableBlue = GradientDrawable(
            GradientDrawable.Orientation.BL_TR, // 135 degrees (bottom-left to top-right)
            intArrayOf(startColor, centerColor, endColor)
        ).apply {
            cornerRadius = resources.getDimension(com.intuit.sdp.R.dimen._200sdp)
        }*/

        val gradientDrawablePurple = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT, // Left to Right gradient
            intArrayOf(
                resources.getColor(R.color.color_FD8AFF), // Start color
                resources.getColor(R.color.color_B421FF)  // End color
            )
        ).apply {
            cornerRadius = resources.getDimension(com.intuit.sdp.R.dimen._200sdp)
        }

        binding?.apply {
            val postUser = postInfo.user
            val venueCategory = postInfo.venueTags

            tvUsername.text = postUser?.let {
                if (MapVenueUserType.VENUE_OWNER.type == it.userType) it.name ?: "" else it.username ?: ""
            } ?: ""

            if (!postInfo.postsHashTags.isNullOrEmpty()) {
                hashtagAdapter.listOfDataItems = postInfo.postsHashTags
            }

            venueTaggedAppCompatTextView.visibility = if (venueCategory == null) View.GONE else View.VISIBLE
            venueTaggedAppCompatTextView.text = venueCategory?.name ?: venueCategory?.username ?: ""

            tvLocationLinearlayout.visibility = if (!postInfo.postLocation.isNullOrEmpty()) View.VISIBLE else View.GONE
            tvLocation.text = postInfo.postLocation

            Glide.with(context).load(postUser?.avatar ?: "").placeholder(R.drawable.ic_chat_user_placeholder).circleCrop()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(ivUserProfile)

            cvVenueTaggedContainer.visibility = if (venueCategory != null) View.VISIBLE else View.GONE

            Glide.with(context).load(venueCategory?.avatar).placeholder(R.drawable.ic_chat_user_placeholder).centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(profileVenueAppCompatImageView)

            mutePlayerImageView.setImageResource(if (postInfo.images?.firstOrNull()?.isMute == true) R.drawable.ic_post_mute else R.drawable.ic_post_unmute)

            ivDoubleTapToLike.visibility = View.GONE

            val homePagePostMediaAdapter = HomePagePostMediaAdapter(context)
            homePagePostMediaAdapter.apply {
                doubleTap.subscribeAndObserveOnMainThread {
                    if (!postInfo.postLike) {
                        updateLikeStatusCount()
                    }
                    DoubleTapLikeView().animateIcon(ivDoubleTapToLike)
                }.autoDispose()
                mediaPhotoViewClick.subscribeAndObserveOnMainThread {
                    homePagePostViewClickSubject.onNext(HomePagePostInfoState.PhotoViewClick(it))
                }.autoDispose()
                mediaVideoViewClick.subscribeAndObserveOnMainThread {
                    homePagePostViewClickSubject.onNext(it)
                }.autoDispose()
            }

            viewPager2.adapter = homePagePostMediaAdapter
            val heightINfo = postInfo.images?.get(0)?.height ?: 0
            val postMediaType = postInfo.type ?: 2
            val images = postInfo.images

            val displayMetrics = DisplayMetrics()
            (context as Activity).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            val height = displayMetrics.heightPixels

            val subHeight = height - HomeActivity.binding.tabBar.height - NewReelsFragment.binding.headerTab.height - NewReelsFragment.binding.tabBar.height
            if((postInfo.images?.size ?: 0) > 1) {
                viewPager2.layoutParams.height = subHeight - 200
            } else {
                viewPager2.layoutParams.height = if(heightINfo < subHeight) {
                    heightINfo
                } else {
                    subHeight - 200
                }
            }
            homePagePostMediaAdapter.postMediaType = postMediaType
            homePagePostMediaAdapter.listOfDataItems = images
            dotsIndicator.setViewPager2(viewPager2)
            if (!images.isNullOrEmpty()) {
                if (images.size > 1) {
                    cvMediaCount.visibility = View.VISIBLE
                    dotsIndicator.visibility = View.VISIBLE
                } else {
                    cvMediaCount.visibility = View.INVISIBLE
                    dotsIndicator.visibility = View.GONE
                }
            } else {
                cvMediaCount.visibility = View.INVISIBLE
                dotsIndicator.visibility = View.GONE
            }
            viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    images?.let {
                        if (position < it.size) {
                            if (lastCurrentPosition != position) {
                                homePagePostViewClickSubject.onNext(HomePagePostInfoState.ChangesVideoPosition(position))
                                lastCurrentPosition = position
                            }
                            if (it.get(position)?.music != null) {
                                llMusic.isVisible = true
                                Glide.with(context).asGif().load(R.raw.music_reels).into(ivMusicLyricsWav)
                                tvMusicName.text = it[position].music?.songTitle.plus(", ").plus(it[position].music?.artists)
                            } else {
                                llMusic.isVisible = false
                            }
                            val currentPos = (position + 1)
                            tvMediaCount.text = currentPos.toString().plus("/").plus(it?.size)
                        }
                    }
                }

                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                    println("Change Position :$position,current Position :$currentPosition")
                }
            })

            val videoUrl = postInfo.images?.firstOrNull()?.videoUrl.plus("?clientBandwidthHint=2.5")

//            postVideoPlayer.videoUrl = videoUrl
//
//            Glide.with(context)
//                .load(postInfo.images?.firstOrNull()?.thumbnailUrl)
//                .centerCrop()
//                .placeholder(colorDrawable)
//                .into(postVideoPlayer.posterImageView)

//            if (postInfo.images?.firstOrNull()?.music != null) {
//                llMusic.isVisible = true
//                Glide.with(context)
//                    .asGif()
//                    .load(R.raw.music_reels)
//                    .into(ivMusicLyricsWav)
//                tvMusicName.text = postInfo.images.firstOrNull()?.music?.songTitle.plus(", ").plus(postInfo.images.firstOrNull()?.music?.artists)
//            } else {
//                llMusic.isVisible = false
//            }
            val postTagsList = postInfo.postTags

            if (postTagsList.isNullOrEmpty()) {
                tvTagPeople.visibility = View.GONE
                ivTagPeople.visibility = View.GONE
            } else {
                val postInfoSize = postTagsList.size
                val userNameWithTagText = buildSpannedString {
                    if (postInfoSize > 0) {
                        val otherCountColor = ContextCompat.getColor(context, R.color.purple)
                        val otherWithCountText = SpannableString(
                            (postInfoSize - 1).toString().plus(" ").plus(context.getString(R.string.label_other))
                        )
                        otherWithCountText.setSpan(
                            ForegroundColorSpan(otherCountColor), 0, otherWithCountText.length, 0
                        )
                        postTagsList.forEachIndexed { index, tag ->
                            inSpans(
                                ForegroundColorSpan(
                                    ContextCompat.getColor(
                                        context, R.color.purple
                                    )
                                )
                            ) {
                                bold { append("@${tag.user?.username ?: ""}") }
                            }
                            if (index < postInfoSize - 2) {
                                append(", ")
                            } else if (index == postInfoSize - 2) {
                                append(" ")
                                append(context.getString(R.string.label_and))
                                append(" ")
                            }
                        }
                        if (postInfoSize > 1) {
                            bold { append(otherWithCountText) }
                        }
                    }
                }
                tvTagPeople.text = userNameWithTagText
                tvTagPeople.visibility = View.VISIBLE
                ivTagPeople.visibility = View.VISIBLE
            }

            if (!postInfo.caption.isNullOrEmpty()) {
                tvPostDescription.visibility = View.VISIBLE
                tvPostDescription.text = postInfo.caption
            } else {
                tvPostDescription.visibility = View.GONE
                tvPostDescription.text = ""
            }

            if (!postInfo.humanReadableTime.isNullOrEmpty()) {
                tvPostDateTime.text = postInfo.humanReadableTime
            } else {
                tvPostDateTime.text = ""
            }

/*            val isLive = (venueCategory?.isLive ?: 0) > 0
            val isReelCount = (venueCategory?.reelCount ?: 0) > 0
            val isPostCount = (venueCategory?.postCount ?: 0) > 0
            val isSpontyCount = (venueCategory?.spontyCount ?: 0) > 0*/
            val storyCount = (venueCategory?.storyCount ?: 0) > 0

            profileVenueAppCompatImageView.background = when {
                storyCount -> gradientDrawablePurple
                else -> null
            }

/*            liveAppCompatTextView.visibility = when {
                isLive -> View.VISIBLE
                else -> View.GONE
            }*/

/*            val isUserLive = (postUser?.isLive ?: 0) > 0
            val isUserReelCount = (postUser?.reelCount ?: 0) > 0
            val isUserPostCount = (postUser?.postCount ?: 0) > 0
            val isUserSpontyCount = (postUser?.spontyCount ?: 0) > 0*/
            val postStoryCount = (postUser?.storyCount ?: 0) > 0
            ivUserProfile.background = when {
                postStoryCount -> gradientDrawablePurple
                else -> null
            }

/*            liveProfileAppCompatTextView.visibility = when {
                isUserLive -> View.VISIBLE
                else -> View.GONE
            }*/
            ivVerified.isVisible = (postUser?.profileVerified ?: 0) == 1
            tvShareCount.text = (postInfo?.shareCount ?: 0).toString()
        }
        updateReelComment()
        updatePostLike()
        updatePostBookmark()
    }

    private fun updatePostLike() {
        binding?.apply {
            ivLike.setImageResource(if (postInfo.postLike) R.drawable.ic_post_filled_like else R.drawable.ic_post_like)
            val totalLikes = postInfo.totalLikes ?: 0
            tvLikeCount.text = totalLikes.prettyCount()
        }
    }

    private fun updateReelComment() {
        binding?.apply {
            val totalComments = postInfo.totalComments
            tvCommentsCount.text = if (totalComments != null && totalComments > 0) totalComments.prettyCount() else "0"
        }
    }

    private fun updatePostBookmark() {
        binding?.apply {
            ivBookmark.setImageResource(if (postInfo.bookmarkStatus) R.drawable.ic_post_filled_save else R.drawable.ic_post_save)
            val totalSave = postInfo.saveCount ?: 0
            tvSaveCount.text = totalSave.prettyCount()
        }
    }

    private fun updateBookmarkCount() {
        postInfo.apply {
            bookmarkStatus = !bookmarkStatus
            saveCount =
                saveCount?.let { if (bookmarkStatus) it + 1 else it - 1 } ?: if (bookmarkStatus) 1 else 0
        }
        updatePostBookmark()
        homePagePostViewClickSubject.onNext(
            if (postInfo.bookmarkStatus) HomePagePostInfoState.AddBookmarkClick(postInfo)
            else HomePagePostInfoState.RemoveBookmarkClick(postInfo)
        )
    }

    private fun updateLikeStatusCount() {
        postInfo.apply {
            postLike = !postLike
            totalLikes = totalLikes?.let { if (postLike) it + 1 else it - 1 } ?: if (postLike) 1 else 0
        }
        updatePostLike()
        homePagePostViewClickSubject.onNext(
            if (postInfo.postLike) HomePagePostInfoState.AddPostLikeClick(postInfo)
            else HomePagePostInfoState.RemovePostLikeClick(postInfo)
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
        Jzvd.releaseAllVideos()
    }

}