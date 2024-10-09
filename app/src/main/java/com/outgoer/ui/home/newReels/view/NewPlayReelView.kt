package com.outgoer.ui.home.newReels.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import androidx.core.view.isVisible
import cn.jzvd.JZDataSource
import cn.jzvd.Jzvd
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.clk.progress.ProgressDialog.textView
import com.devs.readmoreoption.ReadMoreOption
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.post.model.HomePagePostInfoState
import com.outgoer.api.reels.model.ReelInfo
import com.outgoer.api.reels.model.ReelsHashTagsItem
import com.outgoer.api.reels.model.ReelsPageState
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.extension.prettyCount
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.base.view.DoubleTapLikeView
import com.outgoer.databinding.NewPlayReelViewBinding
import com.outgoer.videoplayer.JZMediaExoKotlin
import com.outgoer.videoplayer.JzvdStd
import com.outgoer.videoplayer.VideoDoubleClick
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates


class NewPlayReelView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val playReelViewClicksSubject: PublishSubject<ReelsPageState> = PublishSubject.create()
    val playReelViewClicks: Observable<ReelsPageState> = playReelViewClicksSubject.hide()

    private val reelsHashtagItemClicksSubject: PublishSubject<ReelsHashTagsItem> = PublishSubject.create()
    val reelsHashtagItemClicks: Observable<ReelsHashTagsItem> = reelsHashtagItemClicksSubject.hide()

    private var binding: NewPlayReelViewBinding? = null
    private lateinit var reelInfo: ReelInfo

    private lateinit var hashtagAdapter: NewReelsHashtagAdapter

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var loggedInUserId by Delegates.notNull<Int>()
    var isTextViewClicked = false

    init {
        inflateUi()
    }

    private fun inflateUi() {
        OutgoerApplication.component.inject(this)
        loggedInUserId = loggedInUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0

        val view = View.inflate(context, R.layout.new_play_reel_view, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        binding = NewPlayReelViewBinding.bind(view)
        binding?.apply {
            outgoerVideoPlayer.setVideoDoubleClick(object : VideoDoubleClick {
                override fun onDoubleClick() {
                    if (!reelInfo.reelsLike && loggedInUserId > 0) {
                        updateLikeStatusCount()
                    }
                    DoubleTapLikeView().animateIcon(ivDoubleTapToLike)
                }

                override fun onSingleClick() {
                    if (outgoerVideoPlayer.state == JzvdStd.STATE_PLAYING) {
                        outgoerVideoPlayer.onPauseVideo()
                    } else if (outgoerVideoPlayer.state == JzvdStd.STATE_PAUSE) {
                        outgoerVideoPlayer.onStartVideo()
                    }
                }
            })

            ivMutePlayer.throttleClicks().subscribeAndObserveOnMainThread {
                val isMute = !reelInfo.isMute
                outgoerVideoPlayer.isVideMute = isMute

                if (isMute) {
                    outgoerVideoPlayer.mute()
                    ivMutePlayer.setImageResource(R.drawable.ic_reel_mute)
                } else {
                    outgoerVideoPlayer.unMute()
                    ivMutePlayer.setImageResource(R.drawable.ic_reel_unmute)
                }
                playReelViewClicksSubject.onNext(ReelsPageState.MuteUnmuteClick(isMute))
            }.autoDispose()

            ivUserProfile.throttleClicks().subscribeAndObserveOnMainThread {
                playReelViewClicksSubject.onNext(ReelsPageState.UserProfileClick(reelInfo))
            }.autoDispose()

            profileNameLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
                playReelViewClicksSubject.onNext(ReelsPageState.UserProfileClick(reelInfo))
            }.autoDispose()

            tvReelDescription.throttleClicks().subscribeAndObserveOnMainThread {
                isTextViewClicked = !isTextViewClicked
//                updateCaptionView()
                val reelsTagsList = reelInfo.reelsTags
                if (!reelsTagsList.isNullOrEmpty()) {
                    Timber.tag("TaggedPeopleClick tvReelDescription").i("$reelInfo")
                    playReelViewClicksSubject.onNext(ReelsPageState.TaggedPeopleClick(reelInfo))
                }
            }.autoDispose()

            cvVenueTaggedContainer.throttleClicks().subscribeAndObserveOnMainThread {
                Timber.tag("TaggedVenueClick cvVenueTaggedContainer").i("$reelInfo")
                playReelViewClicksSubject.onNext(ReelsPageState.VenueTaggedProfileClick(reelInfo))
            }.autoDispose()

            ivLike.setOnClickListener {
                if(loggedInUserId > 0)
                    updateLikeStatusCount()
            }

            commentLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
                playReelViewClicksSubject.onNext(ReelsPageState.CommentClick(reelInfo))
            }.autoDispose()

            ivShare.throttleClicks().subscribeAndObserveOnMainThread {
                playReelViewClicksSubject.onNext(ReelsPageState.ShareClick(reelInfo))
            }.autoDispose()

            ivBookmark.throttleClicks().subscribeAndObserveOnMainThread {
                if(loggedInUserId > 0) {
                    updateBookmarkCount()
                }
            }.autoDispose()

            ivMore.throttleClicks().subscribeAndObserveOnMainThread {
                if (loggedInUserId != reelInfo.userId) {
                    playReelViewClicksSubject.onNext(ReelsPageState.MoreClick(reelInfo,true))
                } else {
                    playReelViewClicksSubject.onNext(ReelsPageState.MoreClick(reelInfo,false))
                }
            }.autoDispose()

            @SuppressLint("CheckResult")
            hashtagAdapter = NewReelsHashtagAdapter(context).apply {
                reelsHashtagItemClicks.subscribeAndObserveOnMainThread {
                    reelsHashtagItemClicks.subscribe { reelsHashtagItemClicksSubject.onNext(it) }.autoDispose()
                }.autoDispose()
            }
            rvHashtag.adapter = hashtagAdapter
        }
    }

    private fun updateCaptionView(){
//        binding?.tvReelDescription?.maxLines = 4
        if(isTextViewClicked){
            binding?.tvReelDescription?.maxLines = Integer.MAX_VALUE
            binding?.tvReelDescription?.text?.replace(Regex("Read more") ,"Read less")
        } else {
            binding?.tvReelDescription?.maxLines = 4
            binding?.tvReelDescription?.text?.replace(Regex("Read less") ,"Read more")
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun bind(reelInfo: ReelInfo) {
        this.reelInfo = reelInfo
        Timber.tag("reelInfo").i("$reelInfo")

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
            Glide.with(feedThumbnailView.context).load(reelInfo.thumbnailUrl).preload()
            Glide.with(feedThumbnailView.context).load(reelInfo.thumbnailUrl).diskCacheStrategy(DiskCacheStrategy.ALL).into(feedThumbnailView)
            tvMusicName.isSelected = true
            shareCount.text = (reelInfo.shareCount ?: 0).toString()

            if(reelInfo.width != null && reelInfo.height != null) {
                if((reelInfo.width ?: 0 > reelInfo.height ?: 0 + 100) || (reelInfo.width ?: 0 > reelInfo.height ?: 0) || (reelInfo.width ?: 0 == reelInfo.height ?: 0 + 100)) {
                    Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE_ADAPTER)
                    outgoerVideoPlayer.posterImageView.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    outgoerVideoPlayer.posterImageView.scaleType = ImageView.ScaleType.FIT_CENTER

                } else {
                    Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE_ADAPTER)
                    outgoerVideoPlayer.posterImageView.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                    outgoerVideoPlayer.posterImageView.scaleType = ImageView.ScaleType.FIT_XY
                }
            } else {
                Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE_ADAPTER)
                outgoerVideoPlayer.posterImageView.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                outgoerVideoPlayer.posterImageView.scaleType = ImageView.ScaleType.FIT_CENTER
            }
            Glide.with(context).load(reelInfo.thumbnailUrl).diskCacheStrategy(DiskCacheStrategy.ALL).into(outgoerVideoPlayer.posterImageView)

            val userName = if(MapVenueUserType.VENUE_OWNER.type.equals(reelInfo.user?.userType)) reelInfo.user?.name ?: "" else reelInfo.user?.username ?: ""

            ivDoubleTapToLike.visibility = View.GONE
            venueTaggedAppCompatTextView.visibility = if(reelInfo.venueTags == null) View.GONE else View.VISIBLE
            venueTaggedAppCompatTextView.text = reelInfo.venueTags?.name ?: reelInfo.venueTags?.username

            tvUsername.text = userName
            tvUserAbout.text = reelInfo.user?.about ?: ""

            Glide.with(context).load(reelInfo.user?.avatar).placeholder(R.drawable.ic_chat_user_placeholder).centerCrop().into(ivUserProfile)
            cvVenueTaggedContainer.visibility = if(reelInfo.venueTags != null) View.VISIBLE else View.GONE
            if (reelInfo.music != null) {
                musicLinearLayout.isVisible = true
                tvMusicName.text = reelInfo.music.songTitle.plus(" • ").plus(reelInfo.music.artists)
            } else {
                musicLinearLayout.isVisible = true
                tvMusicName.text = "${userName} • Original Audio"
                tvMusicName.isSelected = true
            }
            Glide.with(context)
                .load(reelInfo.venueTags?.avatar)
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .centerCrop()
                .into(profileVenueAppCompatImageView)

//            tvReelDescription.setOnTouchListener { p0, _ ->
//                p0?.parent?.requestDisallowInterceptTouchEvent(true)
//                false
//            }
//            tvReelDescription.movementMethod = ScrollingMovementMethod()

            val caption = reelInfo.caption ?: ""
            val regex = Regex("@[\\w]+") // Regular expression to match @ mentions

            val matches = regex.findAll(caption)
            val temp = StringBuilder()

            for (match in matches) {
                temp.append(match.value).append(" ")
            }
            val extractedText = temp.toString().trim()

            val startIndex = caption.indexOf(extractedText)
            val endIndex = startIndex + extractedText.length

            var firstCaption = ""
            var lastCaption = ""
            val extractedIndex = caption.indexOf(extractedText)
            if (extractedIndex >= 0) {
                firstCaption = caption.substring(0, extractedIndex)
                Timber.tag("caption firstCaption").i(firstCaption)
                lastCaption = caption.substring(extractedIndex + extractedText.length)
                Timber.tag("caption lastCaption").i(lastCaption)
            } else {
                // Handle the case when no extracted text is found
                firstCaption = caption.plus(" ")
                lastCaption = ""
            }

            val reelsTagsList = reelInfo.reelsTags
            if (!reelsTagsList.isNullOrEmpty()) {
                tvReelDescription.visibility = View.VISIBLE
                val postInfoSize = reelsTagsList.size
                when {
                    postInfoSize == 1 -> {
                        val userNameFirst = reelsTagsList[0].user?.username ?: ""
                        val userNameWithTagText = buildSpannedString {
                            append(firstCaption.ifEmpty { "" })
                            inSpans(ForegroundColorSpan(ContextCompat.getColor(context, R.color.purple))) {
                                bold { append (
                                    extractedText.ifEmpty { "" }
                                )
                                }
                            }
                            append(lastCaption.ifEmpty { "" })
                            append(" ")
                            inSpans(ForegroundColorSpan(ContextCompat.getColor(context, R.color.purple))) {
                                bold { append("@".plus(userNameFirst)) }
                            }
                        }
                        tvReelDescription.text = userNameWithTagText
                        Timber.tag("caption postInfoSize 1").i("${tvReelDescription.text}")
                    }
                    postInfoSize == 2 -> {
                        val userNameFirst = reelsTagsList[0].user?.username ?: ""
                        val userNameSecond = reelsTagsList[1].user?.username ?: ""
                        val userNameWithTagText = buildSpannedString {
                            append(firstCaption.ifEmpty { "" })
                            inSpans(ForegroundColorSpan(ContextCompat.getColor(context, R.color.purple))) {
                                bold { append (
                                    extractedText.ifEmpty { "" }
                                )
                                }
                            }
                            append(lastCaption.ifEmpty { "" })
                            append(" ")
                            inSpans(ForegroundColorSpan(ContextCompat.getColor(context, R.color.purple))) {
                                bold { append("@".plus(userNameFirst)) }
                            }
                            append(", ")
                            inSpans(ForegroundColorSpan(ContextCompat.getColor(context, R.color.purple))) {
                                bold { append("@".plus(userNameSecond)) }
                            }
                        }
                        tvReelDescription.text = userNameWithTagText
                        Timber.tag("caption postInfoSize 2").i("${tvReelDescription.text}")
                    }
                    postInfoSize >= 3 -> {
                        val userNameFirst = reelsTagsList[0].user?.username ?: ""
                        val userNameSecond = reelsTagsList[1].user?.username ?: ""

                        val otherCountColor = ContextCompat.getColor(context, R.color.purple)
                        val otherWithCountText = SpannableString((postInfoSize - 2).toString().plus(" ").plus(context.getString(R.string.label_other)))
                        otherWithCountText.setSpan(ForegroundColorSpan(otherCountColor), 0, otherWithCountText.length, 0)

                        val userNameWithTagText = buildSpannedString {
                            append(firstCaption.ifEmpty { "" })
                            inSpans(ForegroundColorSpan(ContextCompat.getColor(context, R.color.purple))) {
                                bold { append (
                                    extractedText.ifEmpty { "" }
                                )
                                }
                            }
                            append(lastCaption.ifEmpty { "" })
                            append(" ")
                            append(" ")
                            inSpans(ForegroundColorSpan(ContextCompat.getColor(context, R.color.purple))) {
                                bold { append("@".plus(userNameFirst)) }
                            }
                            append(", ")
                            inSpans(ForegroundColorSpan(ContextCompat.getColor(context, R.color.purple))) {
                                bold { append("@".plus(userNameSecond)) }
                            }
                            append(" ")
                            append(context.getString(R.string.label_and))
                            append(" ")
                            bold { append(otherWithCountText) }
                        }
                        tvReelDescription.text = userNameWithTagText
                        Timber.tag("caption postInfoSize 3").i("${tvReelDescription.text}")
                    }
                    else -> {
                        if (caption.isNotEmpty()) {
                            tvReelDescription.visibility = View.VISIBLE
                            val userNameWithTagText = buildSpannedString {
                                append(firstCaption.ifEmpty { "" })
                                inSpans(ForegroundColorSpan(ContextCompat.getColor(context, R.color.purple))) {
                                    bold { append (
                                        extractedText.ifEmpty { "" }
                                    )
                                    }
                                }
                                append(lastCaption.ifEmpty { "" })
                                append(" ")
                            }
                            tvReelDescription.text = userNameWithTagText
                            Timber.tag("caption postInfoSize else 1").i("${tvReelDescription.text}")
                        } else {
                            tvReelDescription.visibility = View.GONE
                            tvReelDescription.text = ""
                        }
                    }
                }
            } else {
                if (caption.isNotEmpty()) {
                    tvReelDescription.visibility = View.VISIBLE
                    val userNameWithTagText = buildSpannedString {
                        append(firstCaption.ifEmpty { "" })
                        inSpans(ForegroundColorSpan(ContextCompat.getColor(context, R.color.purple))) {
                            bold { append (
                                extractedText.ifEmpty { "" }
                            )
                            }
                        }
                        append(lastCaption.ifEmpty { "" })
                        append(" ")
                    }
                    tvReelDescription.text = userNameWithTagText
                    Timber.tag("caption postInfoSize else 2").i("${tvReelDescription.text}")
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

            outgoerVideoPlayer.apply {
                videoUrl = reelInfo.videoUrl?.plus("?clientBandwidthHint=2.5")
                val jzDataSource = JZDataSource(this.videoUrl)
                jzDataSource.looping = true
                this.setUp(
                    jzDataSource,
                    Jzvd.SCREEN_NORMAL,
                    JZMediaExoKotlin::class.java
                )
                Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE_FILL_SCROP)
            }
            outgoerVideoPlayer.isVideMute = reelInfo.isMute

            if (reelInfo.isMute) {
                ivMutePlayer.setImageResource(R.drawable.ic_reel_mute)
            } else {
                ivMutePlayer.setImageResource(R.drawable.ic_reel_unmute)
            }

            println("Reel hashtags: " + reelInfo.reelHashTags)
            println("Reel hashtags: " + reelInfo.reelHashTags?.size)
            hashtagAdapter.listOfDataItems = reelInfo.reelHashTags

            val venueCategory = reelInfo.venueTags
            val storyCount = (venueCategory?.storyCount ?: 0) > 0
            profileVenueAppCompatImageView.background = when {
                storyCount -> gradientDrawablePurple
                else -> null
            }

            val venueUser = reelInfo.user
            val userStoryCount = (venueUser?.storyCount ?: 0) > 0
            ivUserProfile.background = when {
                userStoryCount -> gradientDrawablePurple
                else -> null
            }
/*            val venueCategory = reelInfo.venueTags
            if((venueCategory?.isLive ?: 0) > 0) {
                profileVenueAppCompatImageView.background = gradientDrawablePurple
                liveAppCompatTextView.visibility = View.VISIBLE

            } else {
                if((venueCategory?.reelCount ?: 0) > 0) {
                    profileVenueAppCompatImageView.background = gradientDrawablePurple
                    liveAppCompatTextView.visibility = View.GONE
                } else if((venueCategory?.postCount ?: 0) > 0) {
                    profileVenueAppCompatImageView.background = gradientDrawablePurple
                    liveAppCompatTextView.visibility = View.GONE
                } else if((venueCategory?.spontyCount ?: 0) > 0) {
                    profileVenueAppCompatImageView.background = gradientDrawableBlue
                    liveAppCompatTextView.visibility = View.GONE
                } else {
                    profileVenueAppCompatImageView.background = null
                    liveAppCompatTextView.visibility = View.GONE
                }
            }

            val venueUser = reelInfo.user

            if((venueUser?.isLive ?: 0) > 0) {
                ivUserProfile.background = gradientDrawablePurple
                liveProfileAppCompatTextView.visibility = View.VISIBLE

            } else {
                if((venueUser?.reelCount ?: 0) > 0) {
                    ivUserProfile.background = gradientDrawablePurple
                    liveProfileAppCompatTextView.visibility = View.GONE
                } else if((venueUser?.postCount ?: 0) > 0) {
                    ivUserProfile.background = gradientDrawablePurple
                    liveProfileAppCompatTextView.visibility = View.GONE
                } else if((venueUser?.spontyCount ?: 0) > 0) {
                    ivUserProfile.background = gradientDrawableBlue
                    liveProfileAppCompatTextView.visibility = View.GONE
                } else {
                    ivUserProfile.background = null
                    liveProfileAppCompatTextView.visibility = View.GONE
                }
            }*/

            if(tvReelDescription.text.isEmpty()){
                tvReelDescription.visibility = View.GONE
            } else {
                tvReelDescription.visibility = View.VISIBLE
            }

            println("Lines in description: " + tvReelDescription.lineCount)


            tvReelDescription.getViewTreeObserver().addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    tvReelDescription.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    println("Line count: " + tvReelDescription.getLineCount())

                    if(tvReelDescription.getLineCount() >= 4) {

                        val readMoreOption = ReadMoreOption.Builder(context)
                            .textLength(4, ReadMoreOption.TYPE_LINE) // OR
                            .moreLabel("See more")
                            .lessLabel("See less")
                            .moreLabelColor(Color.WHITE)
                            .lessLabelColor(Color.WHITE)
                            .labelUnderLine(false)
                            .expandAnimation(true)
                            .build()

                        readMoreOption.addReadMoreTo(tvReelDescription, tvReelDescription.text)

                    }
                }

            })

            ivVerified.isVisible = (reelInfo.user?.profileVerified ?: 0) == 1

            RxBus.listen(RxEvent.CurrentPostionReels::class.java).subscribeAndObserveOnMainThread {

            }.autoDispose()
        }
    }

    private fun updateReelLike() {
        binding?.apply {
            if (reelInfo.reelsLike) {
                ivLike.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_selected_like))
            } else {
                ivLike.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_like))
            }

            val totalLikes = reelInfo.totalLikes
            if (totalLikes != null) {
                if (totalLikes != 0) {
                    tvLikeCount.text = totalLikes.prettyCount().toString()
                    tvLikeCount.visibility = View.VISIBLE
                } else {
                    tvLikeCount.text = ""
                    tvLikeCount.visibility = View.INVISIBLE
                }
            } else {
                tvLikeCount.text = ""
                tvLikeCount.visibility = View.INVISIBLE
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
                    tvCommentCount.visibility = View.INVISIBLE
                }
            } else {
                tvCommentCount.text = ""
                tvCommentCount.visibility = View.INVISIBLE
            }
        }
    }

    private fun updateReelBookmark() {
        binding?.apply {
            if (reelInfo.bookmarkStatus) {
                ivBookmark.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_selected_bookmark))
            } else {
                ivBookmark.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_bookmark))
            }
            val totalSave = reelInfo.saveCount ?: 0
            bookmarkCount.text = totalSave.prettyCount()
        }
    }

    private fun updateBookmarkCount() {
        reelInfo.apply {
            bookmarkStatus = !bookmarkStatus
            saveCount =
                saveCount?.let { if (bookmarkStatus) it + 1 else it - 1 } ?: if (bookmarkStatus) 1 else 0
        }
        updateReelBookmark()
        playReelViewClicksSubject.onNext(
            if (reelInfo.bookmarkStatus) ReelsPageState.AddBookmarkClick(reelInfo)
            else ReelsPageState.RemoveBookmarkClick(reelInfo)
        )
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
        binding?.ivDoubleTapToLike?.visibility = View.GONE
//        binding = null
        super.onDestroy()
    }
}