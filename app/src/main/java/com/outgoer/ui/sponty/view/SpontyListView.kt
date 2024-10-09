package com.outgoer.ui.sponty.view

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.post.model.SpontyActionState
import com.outgoer.api.sponty.model.SpontyResponse
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.application.Outgoer
import com.outgoer.base.extension.prettyCount
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.SpontyListItemBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.text.SimpleDateFormat
import java.util.*


class SpontyListView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val spontyActionStateSubject: PublishSubject<SpontyActionState> = PublishSubject.create()
    val spontyActionState: Observable<SpontyActionState> = spontyActionStateSubject.hide()

    private var binding: SpontyListItemBinding? = null
    private lateinit var spontyResponse: SpontyResponse

    private var userId: Int = 0

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.sponty_list_item, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = SpontyListItemBinding.bind(view)

        binding?.apply {

            locationAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
                spontyActionStateSubject.onNext(SpontyActionState.LocationSpontyClick(spontyResponse))
            }

            placeVenueFrameLayout.throttleClicks().subscribeAndObserveOnMainThread {
                spontyActionStateSubject.onNext(SpontyActionState.VenueClick(spontyResponse))
            }

            checkJoinMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
                spontyActionStateSubject.onNext(SpontyActionState.CheckAction(spontyResponse))
            }

            joinMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
                spontyActionStateSubject.onNext(SpontyActionState.JoinUnJoinClick(spontyResponse))
                spontyResponse.spontyJoin = !spontyResponse.spontyJoin
                joinFrameLayout.visibility = if (userId != -1 && userId.equals(spontyResponse.user?.id)) {
                    unjoinMaterialButton.visibility = View.GONE
                    joinMaterialButton.visibility = View.INVISIBLE
                    checkJoinMaterialButton.visibility = View.VISIBLE
                    View.VISIBLE
                } else {
                    checkJoinMaterialButton.visibility = View.GONE
                    if (spontyResponse.spontyJoin) {
                        unjoinMaterialButton.visibility = View.VISIBLE
                        joinMaterialButton.visibility = View.GONE
                    } else {
                        unjoinMaterialButton.visibility = View.GONE
                        joinMaterialButton.visibility = View.VISIBLE
                    }
                    View.VISIBLE
                }
            }

            unjoinMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
                spontyActionStateSubject.onNext(SpontyActionState.JoinUnJoinClick(spontyResponse))

                spontyResponse.spontyJoin = !spontyResponse.spontyJoin
                joinFrameLayout.visibility = if (userId != -1 && userId.equals(spontyResponse.user?.id)) {
                    unjoinMaterialButton.visibility = View.GONE
                    joinMaterialButton.visibility = View.INVISIBLE
                    checkJoinMaterialButton.visibility = View.VISIBLE
                    View.VISIBLE
                } else {
                    checkJoinMaterialButton.visibility = View.GONE
                    if (spontyResponse.spontyJoin) {
                        unjoinMaterialButton.visibility = View.VISIBLE
                        joinMaterialButton.visibility = View.GONE
                    } else {
                        unjoinMaterialButton.visibility = View.GONE
                        joinMaterialButton.visibility = View.VISIBLE
                    }
                    View.VISIBLE
                }
            }

            aboutAppCompatTextView.setOnMentionClickListener { _, text ->
                spontyActionStateSubject.onNext(SpontyActionState.TaggedUser(text.toString(), spontyResponse))
            }
            aboutAppCompatTextView.setOnHashtagClickListener { _, text ->
                spontyActionStateSubject.onNext(SpontyActionState.TaggedUser(text.toString(), spontyResponse))
            }

            commentLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
                spontyActionStateSubject.onNext(SpontyActionState.CommentClick(spontyResponse))
            }

            spontyFirstImage.throttleClicks().subscribeAndObserveOnMainThread {
                spontyActionStateSubject.onNext(SpontyActionState.ImageClick (spontyResponse.images?.get(0)?.image ?: ""))
            }
            spontySecondImage.throttleClicks().subscribeAndObserveOnMainThread {
                spontyActionStateSubject.onNext(SpontyActionState.ImageClick (spontyResponse.images?.get(1)?.image ?: ""))
            }
            spontyThirdImage.throttleClicks().subscribeAndObserveOnMainThread {
                spontyActionStateSubject.onNext(SpontyActionState.ImageClick (spontyResponse.images?.get(2)?.image ?: ""))
            }

            ivProfile.throttleClicks().subscribeAndObserveOnMainThread {
                spontyActionStateSubject.onNext(SpontyActionState.UserImageClick(spontyResponse))
            }
            cvDelete.throttleClicks().subscribeAndObserveOnMainThread {
                spontyActionStateSubject.onNext(SpontyActionState.DeleteSponty(spontyResponse))
            }
            spontyFirstVideo.throttleClicks().subscribeAndObserveOnMainThread {
                spontyActionStateSubject.onNext(SpontyActionState.VideoViewClick(spontyResponse.video?.videoUrl ?: "", spontyResponse.video?.thumbnailUrl))
            }
            moreMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
                spontyActionStateSubject.onNext(SpontyActionState.ReportSponty(spontyResponse))
            }

            likeAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
                updateLikeStatusCount()
            }
        }
    }

    fun bind(sponty: SpontyResponse, userId: Int) {
        val circularProgressDrawable = CircularProgressDrawable(context)
        circularProgressDrawable.strokeWidth = 5f
        circularProgressDrawable.centerRadius = 30f
        circularProgressDrawable.start()

/*        val gradientDrawableBlue = GradientDrawable(
            GradientDrawable.Orientation.BL_TR, // 135 degrees (bottom-left to top-right)
            intArrayOf(
                resources.getColor(R.color.color_76c1ed),
                resources.getColor(R.color.color_4152c1),
                resources.getColor(R.color.color_4152c1)
            )
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

        this.spontyResponse = sponty
        this.userId = userId
//        val icLogoPlaceholder = ContextCompat.getDrawable(context, R.drawable.ic_chat_user_placeholder)
        binding?.apply {
            cvDelete.visibility = if (userId != -1 && userId == sponty.user?.id) View.VISIBLE else View.GONE
            moreMaterialCardView.visibility = if(!(userId != -1 && userId == sponty.user?.id)) View.VISIBLE else View.GONE
            joinFrameLayout.visibility = if (userId != -1 && userId.equals(sponty.user?.id)) {
                unjoinMaterialButton.visibility = View.GONE
                joinMaterialButton.visibility = View.INVISIBLE
                checkJoinMaterialButton.visibility = View.VISIBLE


                val params: ViewGroup.LayoutParams = joinFrameLayout.getLayoutParams()
                params.height = 0
                joinFrameLayout.setLayoutParams(params)
                View.INVISIBLE
            } else {
                checkJoinMaterialButton.visibility = View.GONE
                if (sponty.spontyJoin) {
                    unjoinMaterialButton.visibility = View.VISIBLE
                    joinMaterialButton.visibility = View.GONE
                } else {
                    unjoinMaterialButton.visibility = View.GONE
                    joinMaterialButton.visibility = View.VISIBLE
                }
                View.VISIBLE
            }


            cvVenueTaggedContainer.visibility = if (sponty.venueTags != null) View.VISIBLE else View.GONE

            Glide.with(context).load(sponty.venueTags?.avatar).placeholder(R.drawable.venue_placeholder).centerCrop()
                .into(profileVenueAppCompatImageView)

            venueTaggedAppCompatTextView.text = sponty.venueTags?.name ?: sponty.venueTags?.username

            updateSpontyLike()
            tvCommentCount.text = sponty.totalComments.toString()

            if (userId != -1 && userId.equals(sponty.user?.id)) {
                when (sponty.joinUsers?.size ?: 0) {
                    0 -> {
                        checkJoinMaterialButton.visibility = View.GONE
                    }
                    1 -> {
                        checkJoinMaterialButton.visibility = View.VISIBLE
                        firstRoundedImageView.visibility = View.VISIBLE
                        secondRoundedImageView.visibility = View.GONE
                        moreFrameLayout.visibility = View.GONE



                        Glide.with(context)
                            .load(sponty.joinUsers?.get(0)?.avatar)
                            .placeholder(circularProgressDrawable)
                            .into(firstRoundedImageView)
                    }
                    2 -> {
                        checkJoinMaterialButton.visibility = View.VISIBLE
                        firstRoundedImageView.visibility = View.VISIBLE
                        secondRoundedImageView.visibility = View.VISIBLE
                        moreFrameLayout.visibility = View.GONE

                        Glide.with(context).load(sponty.joinUsers?.get(0)?.avatar).placeholder(circularProgressDrawable).into(firstRoundedImageView)

                        Glide.with(context).load(sponty.joinUsers?.get(1)?.avatar).placeholder(circularProgressDrawable).into(secondRoundedImageView)
                    }
                    3 -> {
                        checkJoinMaterialButton.visibility = View.VISIBLE
                        firstRoundedImageView.visibility = View.VISIBLE
                        secondRoundedImageView.visibility = View.VISIBLE
                        moreFrameLayout.visibility = View.VISIBLE

                        Glide.with(context).load(sponty.joinUsers?.get(0)?.avatar).placeholder(circularProgressDrawable).into(firstRoundedImageView)

                        Glide.with(context).load(sponty.joinUsers?.get(1)?.avatar).placeholder(circularProgressDrawable).into(secondRoundedImageView)

                        Glide.with(context).load(sponty.joinUsers?.get(2)?.avatar).placeholder(circularProgressDrawable).into(thirdRoundedImageView)

                    }
                    else -> {
                        checkJoinMaterialButton.visibility = View.VISIBLE
                        firstRoundedImageView.visibility = View.VISIBLE
                        secondRoundedImageView.visibility = View.VISIBLE
                        moreFrameLayout.visibility = View.VISIBLE
                        thirdRoundedImageView.visibility = View.VISIBLE
                        maxRoundedImageView.visibility = View.VISIBLE

                        Glide.with(context)
                            .load(sponty.joinUsers?.get(0)?.avatar)
                            .placeholder(circularProgressDrawable)
                            .into(firstRoundedImageView)

                        Glide.with(context)
                            .load(sponty.joinUsers?.get(1)?.avatar)
                            .placeholder(circularProgressDrawable)
                            .into(secondRoundedImageView)

                        Glide.with(context)
                            .load(sponty.joinUsers?.get(2)?.avatar)
                            .placeholder(circularProgressDrawable)
                            .into(thirdRoundedImageView)

                        maxRoundedImageView.text =
                            ((sponty.joinUsers?.size ?: (0 - 3))).toString().plus("+")


                    }
                }
            } else {
                checkJoinMaterialButton.visibility = View.GONE
            }

            when (sponty.images?.size ?: 0) {
                0 -> {
                    llSpontyImage.visibility = View.GONE
                }
                1 -> {
                    llSpontyImage.visibility = View.VISIBLE
                    spontyFirstImage.visibility = View.VISIBLE
                    spontySecondImage.visibility = View.GONE
                    spontyThirdImage.visibility = View.GONE

                    Glide.with(context)
                        .load(sponty.images?.get(0)?.image)
                        .placeholder(circularProgressDrawable)
                        .into(spontyFirstImage)
                }
                2 -> {
                    llSpontyImage.visibility = View.VISIBLE
                    spontyFirstImage.visibility = View.VISIBLE
                    spontySecondImage.visibility = View.VISIBLE
                    spontyThirdImage.visibility = View.GONE

                    Glide.with(context).load(sponty.images?.get(0)?.image).placeholder(circularProgressDrawable).into(spontyFirstImage)

                    Glide.with(context).load(sponty.images?.get(1)?.image).placeholder(circularProgressDrawable).into(spontySecondImage)
                }
                3 -> {
                    llSpontyImage.visibility = View.VISIBLE
                    spontyFirstImage.visibility = View.VISIBLE
                    spontySecondImage.visibility = View.VISIBLE
                    spontyThirdImage.visibility = View.VISIBLE

                    Glide.with(context).load(sponty.images?.get(0)?.image).placeholder(circularProgressDrawable).into(spontyFirstImage)

                    Glide.with(context).load(sponty.images?.get(1)?.image).placeholder(circularProgressDrawable).into(spontySecondImage)

                    Glide.with(context).load(sponty.images?.get(2)?.image).placeholder(circularProgressDrawable).into(spontyThirdImage)
                }
                else -> {
                    llSpontyImage.visibility = View.GONE
                    spontyFirstImage.visibility = View.GONE
                    spontySecondImage.visibility = View.GONE
                    spontyThirdImage.visibility = View.GONE

                }
            }

            if (sponty.video != null) {
                llSpontyImage.visibility = View.VISIBLE
                videoFrameLayout.visibility = View.VISIBLE

                Outgoer.exoCacheManager.prepareCacheVideo(sponty.video.videoUrl.plus("?clientBandwidthHint=2.5"))

                if (sponty.images?.size ?: 0 == 0) {
                    spontyFirstImage.visibility = View.GONE
                    spontySecondImage.visibility = View.GONE
                    spontyThirdImage.visibility = View.GONE
                }

                Glide.with(context).load(sponty.video.thumbnailUrl).placeholder(circularProgressDrawable).into(spontyFirstVideo)
            } else {
                videoFrameLayout.visibility = View.GONE
            }

            sponty.user?.let { user ->
                Glide.with(context).load(user.avatar).centerCrop().placeholder(R.drawable.ic_chat_user_placeholder).into(ivProfile)
                usernameAppCompatTextView.text = if (MapVenueUserType.VENUE_OWNER.type.equals(user.userType)) user.name ?: "" else user.username ?: ""
            }

            if (!sponty.caption.isNullOrEmpty()) {
                aboutAppCompatTextView.text = sponty.caption.plus("\n")
            }

            sponty.spontyTags?.forEachIndexed { index, spontyJoinResponse ->
                aboutAppCompatTextView.text = aboutAppCompatTextView.text.toString().plus("@${spontyJoinResponse.user?.username ?: ""}")

                if (index < sponty.spontyTags.size - 1) {
                    aboutAppCompatTextView.text = aboutAppCompatTextView.text.toString().plus(", ")
                }
            }
            timeAppCompatTextView.text = sponty.humanReadableTime
            dateTime.text = sponty.dateTime?.let { convertDateFormat(it) }

            locationAppCompatTextView.isVisible = !sponty.location.isNullOrEmpty()
            locationAppCompatTextView.text = sponty.location

            val venueCategory = spontyResponse.venueTags
            val storyCount = (venueCategory?.storyCount ?: 0) > 0
            profileVenueAppCompatImageView.background = when {
                storyCount -> gradientDrawablePurple
                else -> null
            }
            /*if ((venueCategory?.isLive ?: 0) > 0) {
                profileVenueAppCompatImageView.background = gradientDrawablePurple
                liveAppCompatTextView.visibility = View.VISIBLE

            } else {
                if ((venueCategory?.reelCount ?: 0) > 0) {
                    profileVenueAppCompatImageView.background = gradientDrawablePurple
                    liveAppCompatTextView.visibility = View.GONE
                } else if ((venueCategory?.postCount ?: 0) > 0) {
                    profileVenueAppCompatImageView.background = gradientDrawablePurple
                    liveAppCompatTextView.visibility = View.GONE
                } else if ((venueCategory?.spontyCount ?: 0) > 0) {
                    profileVenueAppCompatImageView.background = gradientDrawableBlue
                    liveAppCompatTextView.visibility = View.GONE
                } else {
                    profileVenueAppCompatImageView.background = null
                    liveAppCompatTextView.visibility = View.GONE
                }
            }*/

            val venueUser = spontyResponse.user
            val userStoryCount = (venueUser?.storyCount ?: 0) > 0
            ivProfile.background = when {
                userStoryCount -> gradientDrawablePurple
                else -> null
            }
            /*if ((venueUser?.isLive ?: 0) > 0) {
                ivProfile.background = gradientDrawablePurple
                liveProfileAppCompatTextView.visibility = View.VISIBLE
            } else {
                if ((venueUser?.reelCount ?: 0) > 0) {
                    ivProfile.background = gradientDrawablePurple
                    liveProfileAppCompatTextView.visibility = View.GONE
                } else if ((venueUser?.postCount ?: 0) > 0) {
                    ivProfile.background = gradientDrawablePurple
                    liveProfileAppCompatTextView.visibility = View.GONE
                } else if ((venueUser?.spontyCount ?: 0) > 0) {
                    ivProfile.background = gradientDrawableBlue
                    liveProfileAppCompatTextView.visibility = View.GONE
                } else {
                    ivProfile.background = null
                    liveProfileAppCompatTextView.visibility = View.GONE
                }
            }*/

            ivVerified.isVisible = (spontyResponse.user?.profileVerified ?: 0) == 1
        }
    }

    private fun updateSpontyLike() {
        binding?.apply {
            if (spontyResponse.spontyLike) {
                likeAppCompatImageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_post_filled_like))
            } else {
                likeAppCompatImageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_post_like))
            }

            val totalLikes = spontyResponse.totalLikes
            if (totalLikes != null) {
                if (totalLikes != 0) {
                    tvLikeCount.text = totalLikes.prettyCount().toString()
                    tvLikeCount.visibility = View.VISIBLE
                } else {
                    tvLikeCount.text = "0"
                }
            } else {
                tvLikeCount.text = "0"
            }
        }
    }

    private fun updateLikeStatusCount() {
        spontyResponse.spontyLike = !spontyResponse.spontyLike

        if (spontyResponse.spontyLike) {
            spontyResponse.totalLikes = spontyResponse.totalLikes?.let { it + 1 } ?: 0
            updateSpontyLike()
            spontyActionStateSubject.onNext(SpontyActionState.LikeDisLike(spontyResponse))
        } else {
            spontyResponse.totalLikes = spontyResponse.totalLikes?.let { it - 1 } ?: 0
            updateSpontyLike()
            spontyActionStateSubject.onNext(SpontyActionState.LikeDisLike(spontyResponse))
        }
    }

    fun convertDateFormat(inputDateString: String): String {
        // Define input and output date formats
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val outputFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())

        // Parse input string to Date object
        val date = inputFormat.parse(inputDateString)

        // Format the Date object to desired output format
        return outputFormat.format(date)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}