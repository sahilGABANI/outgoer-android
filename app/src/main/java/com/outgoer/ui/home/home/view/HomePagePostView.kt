package com.outgoer.ui.home.home.view

import android.app.Activity
import android.content.Context
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
import com.outgoer.databinding.ViewHomePagePostBinding
import com.outgoer.ui.home.HomeActivity
import com.outgoer.ui.home.newReels.NewReelsFragment
import com.outgoer.ui.home.newReels.view.NewReelsHashtagAdapter
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject


class HomePagePostView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val homePagePostViewClickSubject: PublishSubject<HomePagePostInfoState> = PublishSubject.create()
    val homePagePostViewClick: Observable<HomePagePostInfoState> = homePagePostViewClickSubject.hide()

    private var binding: ViewHomePagePostBinding? = null
    private lateinit var postInfo: PostInfo
    private lateinit var hashtagAdapter: NewReelsHashtagAdapter
    private var lastCommentId: Int = 0
    private var lastDeleteCommentId: Int = 0

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
        val view = View.inflate(context, R.layout.view_home_page_post, this)
        OutgoerApplication.component.inject(this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewHomePagePostBinding.bind(view)
        binding?.apply {
            hashtagAdapter = NewReelsHashtagAdapter(context).apply {
                reelsHashtagItemClicks.subscribeAndObserveOnMainThread {
                    homePagePostViewClickSubject.onNext(HomePagePostInfoState.HashtagItemClicks(it))
                }
            }
            rvHashtag.adapter = hashtagAdapter
            ivUserProfile.throttleClicks().subscribeAndObserveOnMainThread {
                homePagePostViewClickSubject.onNext(HomePagePostInfoState.UserProfileClick(postInfo))
            }.autoDispose()

            llUsernameWithLocation.throttleClicks().subscribeAndObserveOnMainThread {
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
                postInfo.descriptionTags?.find { text.toString().equals(it.user?.username) }?.let {
                    it.user?.let { userInfo ->
                        homePagePostViewClickSubject.onNext(HomePagePostInfoState.OpenTaggedPeopleClick(userInfo))
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

//            postImageView.throttleClicks().subscribeAndObserveOnMainThread {
//                postInfo.images?.firstOrNull()?.image?.let { it1 ->
//                    homePagePostViewClickSubject.onNext(HomePagePostInfoState.PhotoViewClick(it1))
//                }
//            }.autoDispose()
        }
    }

    fun bind(postInfo: PostInfo) {
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
            val userName = postUser?.let {
                if (MapVenueUserType.VENUE_OWNER.type == it.userType) it.name ?: "" else it.username ?: ""
            } ?: ""
            tvUsername.text = userName

            if (!postInfo.postsHashTags.isNullOrEmpty()) {
                hashtagAdapter.listOfDataItems = postInfo.postsHashTags
            }
            venueTaggedAppCompatTextView.visibility = if (venueCategory == null) View.GONE else View.VISIBLE
            venueTaggedAppCompatTextView.text = venueCategory?.name ?: venueCategory?.username ?: ""

            if (postInfo.postLocation.isNullOrEmpty()) {
                tvLocationLinearlayout.visibility = View.GONE
                tvLocation.text = ""
            } else {
                tvLocationLinearlayout.visibility = View.VISIBLE
                tvLocation.text = postInfo.postLocation
            }

            Glide.with(context)
                .load(postUser?.avatar ?: "")
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .circleCrop()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(ivUserProfile)

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

                }.autoDispose()
            }

            homePagePostMediaAdapter.deviceHeight = height
            viewPager2.adapter = homePagePostMediaAdapter

            val heightINfo = if (postInfo.images != null && postInfo.images.isNotEmpty()) {
                (postInfo.images[0].height * 1.5).toInt()
            } else {
                0
            }


            val displayMetrics = DisplayMetrics()
            (context as Activity).windowManager.defaultDisplay.getMetrics(displayMetrics)
            val height = displayMetrics.heightPixels

            val subHeight = height - HomeActivity.binding.tabBar.height - NewReelsFragment.binding.headerTab.height - NewReelsFragment.binding.tabBar.height
            if ((postInfo.images?.size ?: 0) > 1) {
                viewPager2.layoutParams.height = subHeight - 200
            } else {
                viewPager2.layoutParams.height = if (heightINfo < subHeight) {
                    heightINfo
                } else {
                    subHeight - 200
                }
            }

            val postMediaType = postInfo.type ?: 1
            val images = postInfo.images
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
                    images.let {
                        val currentPos = (position + 1)
                        tvMediaCount.text = currentPos.toString().plus("/").plus(it?.size)
                    }
                }
            })

            cvVenueTaggedContainer.visibility = if (venueCategory != null) View.VISIBLE else View.GONE

            Glide.with(context)
                .load(venueCategory?.avatar)
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(profileVenueAppCompatImageView)

            val postTagsList = postInfo.postTags
            if (postTagsList.isNullOrEmpty()) {
                tvTagPeople.visibility = View.GONE
                ivTagPeople.visibility = View.GONE
            } else {
                val userNameList = postTagsList.take(2).map { it.user?.username ?: "" }
                val postInfoSize = postTagsList.size
                val userNameWithTagText = buildSpannedString {
                    userNameList.forEachIndexed { index, userName ->
                        inSpans(
                            ForegroundColorSpan(
                                ContextCompat.getColor(
                                    context,
                                    R.color.purple
                                )
                            )
                        ) {
                            bold { append("@$userName") }
                        }
                        if (index < userNameList.size - 1) {
                            append(", ")
                        }
                    }
                    if (postInfoSize > 2) {
                        val otherCountColor = ContextCompat.getColor(context, R.color.purple)
                        val otherWithCountText = SpannableString(
                            (postInfoSize - 2).toString().plus(" ")
                                .plus(context.getString(R.string.label_other))
                        )
                        otherWithCountText.setSpan(
                            ForegroundColorSpan(otherCountColor),
                            0,
                            otherWithCountText.length,
                            0
                        )
                        append(" ")
                        append(context.getString(R.string.label_and))
                        append(" ")
                        bold { append(otherWithCountText) }
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

/*            val countLive = venueCategory?.isLive ?: 0
            val countReel = venueCategory?.reelCount ?: 0
            val countPost = venueCategory?.postCount ?: 0
            val countSponty = venueCategory?.spontyCount ?: 0*/
            val storyCount = venueCategory?.storyCount ?: 0
            profileVenueAppCompatImageView.background =
                when {
                    storyCount > 0 -> gradientDrawablePurple
                    else -> null
                }
/*            liveAppCompatTextView.visibility =
                when {
                    countLive > 0 -> View.VISIBLE
                    countReel > 0 || countPost > 0 -> View.GONE
                    else -> View.GONE
                }*/

/*            val postCountLive = postUser?.isLive ?: 0
            val postCountReel = postUser?.reelCount ?: 0
            val postCountPost = postUser?.postCount ?: 0
            val postCountSponty = postUser?.spontyCount ?: 0*/
            val postStoryCount = postUser?.storyCount ?: 0

            ivUserProfile.background =
                when {
                    postStoryCount > 0 -> gradientDrawablePurple
                    else -> null
                }

/*            liveProfileAppCompatTextView.visibility =
                when {
                    postCountLive > 0 -> View.VISIBLE
                    postCountReel > 0 || postCountPost > 0 || postCountSponty > 0 -> View.GONE
                    else -> View.GONE
                }*/
            ivVerified.isVisible = (postUser?.profileVerified ?: 0) == 1

            tvShareCount.text = (postInfo?.shareCount ?: 0).toString()
            tvSaveCount.text = (postInfo?.saveCount ?: 0).toString()
        }
        updateReelComment()
        updatePostLike()
        updatePostBookmark()
    }

    private fun updatePostLike() {
        binding?.apply {
            ivLike.setImageResource(if (postInfo.postLike) R.drawable.ic_post_filled_like else R.drawable.ic_post_like)
            val totalLikes = postInfo.totalLikes ?: 0
            tvLikeCount.text = totalLikes.takeIf { it != 0 }?.prettyCount() ?: "0"
        }
    }

    private fun updateReelComment() {
        binding?.apply {
            val totalComments = postInfo.totalComments
            tvCommentsCount.text = (totalComments?.takeIf { it > 0 } ?: 0).prettyCount().toString()
        }
    }

    private fun updatePostBookmark() {
        binding?.apply {
            ivBookmark.setImageResource(if (postInfo.bookmarkStatus) R.drawable.ic_post_filled_save else R.drawable.ic_post_save)
            val totalSave = postInfo.saveCount ?: 0
            tvSaveCount.text = totalSave.prettyCount()
        }
    }

    private fun updateLikeStatusCount() {
        postInfo.postLike = !postInfo.postLike
        val likesDelta = if (postInfo.postLike) 1 else -1
        postInfo.totalLikes = (postInfo.totalLikes ?: 0) + likesDelta
        updatePostLike()
        val clickState = if (postInfo.postLike) {
            HomePagePostInfoState.AddPostLikeClick(postInfo)
        } else {
            HomePagePostInfoState.RemovePostLikeClick(postInfo)
        }
        homePagePostViewClickSubject.onNext(clickState)
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

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}