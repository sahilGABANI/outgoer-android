package com.outgoer.ui.sponty
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.sponty.model.AllJoinSpontyRequest
import com.outgoer.api.sponty.model.SpontyActionRequest
import com.outgoer.api.sponty.model.SpontyResponse
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivitySpontyDetailsBinding
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import com.outgoer.ui.sponty.comment.SpontyReplyBottomSheet
import com.outgoer.ui.sponty.viewmodel.SpontyViewModel
import com.outgoer.utils.Utility
import javax.inject.Inject

class SpontyDetailsActivity : AppCompatActivity() {


    private lateinit var binding: ActivitySpontyDetailsBinding
    private var userId: Int = -1
    private var spontyResponse: SpontyResponse? = null

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<SpontyViewModel>
    private lateinit var spontyViewModel: SpontyViewModel
    private var showComments: Boolean = false


    companion object {
        const val SPONTY_RESPONSE = "SPONTY_RESPONSE"
        const val SPONTY_SHOW_COMMENTS = "SPONTY_SHOW_COMMENTS"
        fun getIntent(context: Context, spontyId: Int, isShowComment:Boolean ? = false): Intent {
            val intent = Intent(context, SpontyDetailsActivity::class.java)
            intent.putExtra(SPONTY_RESPONSE, spontyId)
            intent.putExtra(SPONTY_SHOW_COMMENTS,isShowComment)

            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)

        binding = ActivitySpontyDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        spontyViewModel = getViewModelFromFactory(viewModelFactory)

        listenToViewEvents()
        listenToViewModel()

    }

    private fun listenToViewEvents() {

        binding.backAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }
        userId = loggedInUserCache.getUserId() ?: -1

        intent?.let {
            val spontyId = it.getIntExtra(SPONTY_RESPONSE,0)
            this.showComments = it.getBooleanExtra(SPONTY_SHOW_COMMENTS, false)

            spontyViewModel.getSpecificSpontyInfo(spontyId)
        }


        binding.joinMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            spontyViewModel.addRemoveSponty(AllJoinSpontyRequest(spontyId = spontyResponse?.id ?: 0))
        }
        binding.checkJoinMaterialButton.throttleClicks().subscribeAndObserveOnMainThread { item ->
            spontyResponse?.apply {
                joinUsers?.let {
                    if (joinUsers.size > 0) {
                        var spontyUserBottomsheet = SpontyUserBottomsheet.newInstance(joinUsers)
                        spontyUserBottomsheet?.show(
                            supportFragmentManager,
                            SpontyUserBottomsheet.TAG
                        )
                    }
                }
            }
        }

        binding.aboutAppCompatTextView.setOnMentionClickListener { _, text ->
            val loggedInUserId = loggedInUserCache.getLoggedInUser()?.loggedInUser?.id
            val clickedText = text
            val tagsList = spontyResponse?.spontyTags
            if (!tagsList.isNullOrEmpty()) {
                val tag = tagsList.firstOrNull { cInfo ->
                    cInfo.user?.username == clickedText
                }
                if (tag != null) {
                    if (loggedInUserId != tag.userId) {
                        if(tag.user?.userType == MapVenueUserType.VENUE_OWNER.type) {
                            if(loggedInUserCache.getUserId() == tag.userId ) {
                                RxBus.publish(RxEvent.OpenVenueUserProfile)
                            }else {
                                startActivityWithDefaultAnimation(NewVenueDetailActivity.getIntent(this,0,tag.userId ?: 0))
                            }
                        } else {
                            startActivityWithDefaultAnimation(
                                NewOtherUserProfileActivity.getIntent(
                                    this,
                                    tag.userId
                                )
                            )
                        }
                    }
                }
            }
        }

        binding.commentLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
           showCommentBottomSheet()
        }

        binding.likeLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
            spontyResponse?.id?.let {
                spontyViewModel.addRemoveSpontyLike(SpontyActionRequest(it))
            }
        }
    }

    private fun showCommentBottomSheet() {
        var spontyId = spontyResponse?.id ?: 0
        var spontyReplyBottomSheet = SpontyReplyBottomSheet.newInstance(spontyId)
        spontyReplyBottomSheet.commentActionState.subscribeAndObserveOnMainThread { res ->
            binding.tvCommentCount.text = res.toString()

        }
        spontyReplyBottomSheet.show(supportFragmentManager, "SpontyReplyBottomSheet")
    }

    private fun listenToViewModel() {
        spontyViewModel.spontyDataState.subscribeAndObserveOnMainThread {
            when (it) {
                is SpontyViewModel.SpontyDataState.LoadingState -> {}
                is SpontyViewModel.SpontyDataState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is SpontyViewModel.SpontyDataState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is SpontyViewModel.SpontyDataState.SpontyInfo -> {

                }
                is SpontyViewModel.SpontyDataState.SpecificSpontyInfo -> {
                    spontyResponse = it.specificSponty
                    setDetails(it.specificSponty)
                    if(showComments){
                        showCommentBottomSheet()
                    }
                }
                is SpontyViewModel.SpontyDataState.SpontyJoinInfo -> {

                }
                is SpontyViewModel.SpontyDataState.AddSpontyJoin -> {
                    finish()
                }
                is SpontyViewModel.SpontyDataState.AddRemoveSpontyLike -> {
                }

                else -> {}
            }
        }
    }

    private fun setDetails(sponty: SpontyResponse) {

        binding?.apply {
            joinFrameLayout.visibility = if (userId != -1 && userId.equals(sponty.user?.id)) {
                unjoinMaterialButton.visibility = View.GONE
                joinMaterialButton.visibility = View.GONE
                checkJoinMaterialButton.visibility = View.VISIBLE
                View.VISIBLE
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


            cvVenueTaggedContainer.visibility = if(sponty.venueTags != null) View.VISIBLE else View.GONE

            Glide.with(this@SpontyDetailsActivity)
                .load(sponty.venueTags?.avatar)
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .centerCrop()
                .into(profileVenueAppCompatImageView)

            venueTaggedAppCompatTextView.text = sponty.venueTags?.name ?: sponty.venueTags?.username

            if(sponty.spontyLike) {
                likeAppCompatImageView.setImageDrawable(resources.getDrawable(R.drawable.ic_post_filled_like, null))
            } else {
                likeAppCompatImageView.setImageDrawable(resources.getDrawable(R.drawable.ic_post_like, null))
            }


            tvLikeCount.text = sponty.totalLikes.toString()
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

                        Glide.with(this@SpontyDetailsActivity)
                            .load(sponty.joinUsers?.get(0)?.avatar)
                            .placeholder(
                                resources.getDrawable(
                                    R.drawable.ic_chat_user_placeholder,
                                    null
                                )
                            )
                            .into(firstRoundedImageView)
                    }
                    2 -> {
                        checkJoinMaterialButton.visibility = View.VISIBLE
                        firstRoundedImageView.visibility = View.VISIBLE
                        secondRoundedImageView.visibility = View.VISIBLE
                        moreFrameLayout.visibility = View.GONE
                        Glide.with(this@SpontyDetailsActivity)
                            .load(sponty.joinUsers?.get(0)?.avatar)
                            .placeholder(
                                resources.getDrawable(
                                    R.drawable.ic_chat_user_placeholder,
                                    null
                                )
                            )
                            .into(firstRoundedImageView)
                        Glide.with(this@SpontyDetailsActivity)
                            .load(sponty.joinUsers?.get(1)?.avatar)
                            .placeholder(
                                resources.getDrawable(
                                    R.drawable.ic_chat_user_placeholder,
                                    null
                                )
                            )
                            .into(secondRoundedImageView)
                    }
                    3 -> {
                        checkJoinMaterialButton.visibility = View.VISIBLE
                        firstRoundedImageView.visibility = View.VISIBLE
                        secondRoundedImageView.visibility = View.VISIBLE
                        moreFrameLayout.visibility = View.VISIBLE
                        Glide.with(this@SpontyDetailsActivity)
                            .load(sponty.joinUsers?.get(0)?.avatar)
                            .placeholder(
                                resources.getDrawable(
                                    R.drawable.ic_chat_user_placeholder,
                                    null
                                )
                            )
                            .into(firstRoundedImageView)
                        Glide.with(this@SpontyDetailsActivity)
                            .load(sponty.joinUsers?.get(1)?.avatar)
                            .placeholder(
                                resources.getDrawable(
                                    R.drawable.ic_chat_user_placeholder,
                                    null
                                )
                            )
                            .into(secondRoundedImageView)
                        Glide.with(this@SpontyDetailsActivity)
                            .load(sponty.joinUsers?.get(2)?.avatar)
                            .placeholder(
                                resources.getDrawable(
                                    R.drawable.ic_chat_user_placeholder,
                                    null
                                )
                            )
                            .into(thirdRoundedImageView)

                    }
                    else -> {
                        checkJoinMaterialButton.visibility = View.VISIBLE
                        firstRoundedImageView.visibility = View.VISIBLE
                        secondRoundedImageView.visibility = View.VISIBLE
                        moreFrameLayout.visibility = View.VISIBLE
                        thirdRoundedImageView.visibility = View.VISIBLE
                        maxRoundedImageView.visibility = View.VISIBLE

                        Glide.with(this@SpontyDetailsActivity)
                            .load(sponty.joinUsers?.get(0)?.avatar)
                            .placeholder(
                                resources.getDrawable(
                                    R.drawable.ic_chat_user_placeholder,
                                    null
                                )
                            )
                            .into(firstRoundedImageView)
                        Glide.with(this@SpontyDetailsActivity)
                            .load(sponty.joinUsers?.get(1)?.avatar)
                            .placeholder(
                                resources.getDrawable(
                                    R.drawable.ic_chat_user_placeholder,
                                    null
                                )
                            )
                            .into(secondRoundedImageView)
                        Glide.with(this@SpontyDetailsActivity)
                            .load(sponty.joinUsers?.get(2)?.avatar)
                            .placeholder(
                                resources.getDrawable(
                                    R.drawable.ic_chat_user_placeholder,
                                    null
                                )
                            )
                            .into(thirdRoundedImageView)

                        maxRoundedImageView.text =
                            (sponty.joinUsers?.size ?: 0 - 3).toString().plus("+")


                    }
                }
            } else {
                checkJoinMaterialButton.visibility = View.GONE
            }

            sponty.user?.let { user ->
                Glide.with(this@SpontyDetailsActivity)
                    .load(user.avatar)
                    .centerCrop()
                    .placeholder(R.drawable.ic_chat_user_placeholder)
                    .into(ivProfile)

                usernameAppCompatTextView.text = if(MapVenueUserType.VENUE_OWNER.type.equals(user?.userType)) user.name ?: "" else user.username ?: ""
            }
            aboutAppCompatTextView.text = sponty.caption
            timeAppCompatTextView.text = sponty.humanReadableTime
            locationAppCompatTextView.text = sponty.location


            val venueCategory = sponty.venueTags
            val storyCount = (venueCategory?.storyCount ?: 0) > 0
            profileVenueAppCompatImageView.background = when {
                storyCount -> Utility.ringGradientColor
                else -> null
            }
/*            if(venueCategory?.isLive?: 0 > 0) {
                profileVenueAppCompatImageView.background = resources.getDrawable(R.drawable.ring_gredient_color, null)
                liveAppCompatTextView.visibility = View.VISIBLE

            } else {
                if(venueCategory?.reelCount ?: 0 ?: 0 > 0) {
                    profileVenueAppCompatImageView.background = resources.getDrawable(R.drawable.ring_gredient_color, null)
                    liveAppCompatTextView.visibility = View.GONE
                } else if(venueCategory?.postCount ?: 0 > 0) {
                    profileVenueAppCompatImageView.background = resources.getDrawable(R.drawable.ring_gredient_color, null)
                    liveAppCompatTextView.visibility = View.GONE
                } else if(venueCategory?.spontyCount ?: 0 > 0) {
                    profileVenueAppCompatImageView.background = resources.getDrawable(R.drawable.ring_blue_gredient_color, null)
                    liveAppCompatTextView.visibility = View.GONE
                } else {
                    profileVenueAppCompatImageView.background = null
                    liveAppCompatTextView.visibility = View.GONE
                }
            }*/

            val venueUser = sponty.user
            val venueStoryCount = (venueUser?.storyCount ?: 0) > 0
            ivProfile.background = when {
                venueStoryCount -> Utility.ringGradientColor
                else -> null
            }
/*            if(venueUser?.isLive?: 0 > 0) {
                ivProfile.background = resources.getDrawable(R.drawable.ring_gredient_color, null)
                liveProfileAppCompatTextView.visibility = View.VISIBLE

            } else {
                if(venueUser?.reelCount ?: 0 ?: 0 > 0) {
                    ivProfile.background = resources.getDrawable(R.drawable.ring_gredient_color, null)
                    liveProfileAppCompatTextView.visibility = View.GONE
                } else if(venueUser?.postCount ?: 0 > 0) {
                    ivProfile.background = resources.getDrawable(R.drawable.ring_gredient_color, null)
                    liveProfileAppCompatTextView.visibility = View.GONE
                } else if(venueUser?.spontyCount ?: 0 > 0) {
                    ivProfile.background = resources.getDrawable(R.drawable.ring_blue_gredient_color, null)
                    liveProfileAppCompatTextView.visibility = View.GONE
                } else {
                    ivProfile.background = null
                    liveProfileAppCompatTextView.visibility = View.GONE
                }
            }*/
        }
    }
}