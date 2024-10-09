package com.outgoer.ui.otherprofile

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import cn.jzvd.Jzvd
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.engineer.linktextview.Linker
import com.engineer.linktextview.internal.OnLinkClickListener
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.authentication.model.OutgoerUser
import com.outgoer.api.chat.model.ChatConversationInfo
import com.outgoer.api.post.model.PostMoreOption
import com.outgoer.api.profile.model.BlockUserRequest
import com.outgoer.api.profile.model.ReportUserRequest
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityNewOtherUserProfileBinding
import com.outgoer.ui.followdetail.FollowDetailActivity
import com.outgoer.ui.chat.NewChatActivity
import com.outgoer.ui.home.home.PostMoreOptionBottomSheet
import com.outgoer.ui.newvenuedetail.BlockUserVenueBottomSheet
import com.outgoer.ui.otherprofile.view.NewOtherUserProfileFragmentTabAdapter
import com.outgoer.ui.otherprofile.view.OtherUserReelsAdapter
import com.outgoer.ui.otherprofile.viewmodel.OtherUserProfileViewModel
import com.outgoer.ui.reelsdetail.ReelsDetailActivity
import com.outgoer.ui.report.ReportBottomSheet
import timber.log.Timber
import javax.inject.Inject

class NewOtherUserProfileActivity : BaseActivity() {

    companion object {
        private const val INTENT_EXTRA_USER_ID = "INTENT_EXTRA_USER_ID"
        fun getIntent(context: Context, userId: Int): Intent {
            val intent = Intent(context, NewOtherUserProfileActivity::class.java)
            intent.putExtra(INTENT_EXTRA_USER_ID, userId)
            return intent
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<OtherUserProfileViewModel>
    private lateinit var otherUserProfileViewModel: OtherUserProfileViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private lateinit var binding: ActivityNewOtherUserProfileBinding

    private lateinit var otherUserReelsAdapter: OtherUserReelsAdapter
    private lateinit var otherUserProfileFragmentTabAdapter: NewOtherUserProfileFragmentTabAdapter

    private var userId = -1
    private var otherOutgoerUser: OutgoerUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        OutgoerApplication.component.inject(this)
        otherUserProfileViewModel = getViewModelFromFactory(viewModelFactory)

        binding = ActivityNewOtherUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Jzvd.goOnPlayOnPause()
        Jzvd.releaseAllVideos()

        loadDataFromIntent()
    }

    private fun loadDataFromIntent() {
        intent?.let {
            if (it.hasExtra(INTENT_EXTRA_USER_ID)) {
                val userId = it.getIntExtra(INTENT_EXTRA_USER_ID, -1)
                if (userId != -1) {
                    this.userId = userId

                    binding.otherActionLinearLayout.visibility = if(userId.equals(loggedInUserCache.getUserId())) View.GONE else View.VISIBLE

                    listenToViewEvents()
                    listenToViewModel()
                } else {
                    onBackPressed()
                }
            } else {
                onBackPressed()
            }
        } ?: onBackPressed()
    }

    private fun listenToViewEvents() {
        binding.moreAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            var postMoreOptionBottomSheet: PostMoreOptionBottomSheet = PostMoreOptionBottomSheet.newInstanceWithData(true)
            postMoreOptionBottomSheet.postMoreOptionClick.subscribeAndObserveOnMainThread {
                when(it) {
                    is PostMoreOption.BlockClick -> {
                        var blockUserVenueBottomSheet = BlockUserVenueBottomSheet.newInstanceWithData(otherOutgoerUser?.avatar ?: "", otherOutgoerUser?.username ?: "")
                        blockUserVenueBottomSheet.blockOptionClick.subscribeAndObserveOnMainThread {
                            otherUserProfileViewModel.blockUserProfile(BlockUserRequest(userId))
                            blockUserVenueBottomSheet.dismissBottomSheet()
                        }
                        blockUserVenueBottomSheet.show(supportFragmentManager, BlockUserVenueBottomSheet.javaClass.name)
                        postMoreOptionBottomSheet.dismissBottomSheet()
                    }
                    is PostMoreOption.ReportClick -> {
//                        otherUserProfileViewModel.reportUserVenue(ReportUserRequest(userId))
                        val reportOptionBottomSheet = ReportBottomSheet()
                        reportOptionBottomSheet.reasonClick.subscribeAndObserveOnMainThread { reportId ->
                            otherUserProfileViewModel.reportUserVenue(ReportUserRequest(userId, reportId))
                            reportOptionBottomSheet.dismiss()

                            postMoreOptionBottomSheet.dismissBottomSheet()
                        }.autoDispose()
                        reportOptionBottomSheet.show(
                            supportFragmentManager, ReportBottomSheet::class.java.name
                        )

                    }
                    is PostMoreOption.DismissClick -> {
                        postMoreOptionBottomSheet.dismissBottomSheet()
                    }
                    is PostMoreOption.DeleteClick -> {}
                }
            }

            postMoreOptionBottomSheet.show(supportFragmentManager, PostMoreOptionBottomSheet.javaClass.name)
        }.autoDispose()

        otherUserProfileFragmentTabAdapter = NewOtherUserProfileFragmentTabAdapter(this, userId)
        binding.viewpager.isUserInputEnabled = false
        binding.viewpager.offscreenPageLimit = 2
        binding.viewpager.adapter = otherUserProfileFragmentTabAdapter
        binding.viewpager.hackMatchParentCheckInViewPager()

        TabLayoutMediator(binding.tabLayout, binding.viewpager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = getString(R.string.label_post)
                }
                1 -> {
                    tab.text = getString(R.string.label_reels)
                }
            }
        }.attach()


        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })

        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()

        otherUserReelsAdapter = OtherUserReelsAdapter(this).apply {
            reelsViewClick.subscribeAndObserveOnMainThread {
                startActivityWithDefaultAnimation(
                    ReelsDetailActivity.getIntent(
                        this@NewOtherUserProfileActivity,
                        it.id
                    )
                )
            }.autoDispose()
        }

        binding.llFollowersCount.setOnClickListener {
            startActivityWithDefaultAnimation(
                FollowDetailActivity.getIntent(
                    this,
                    userId,
                    isFollower = true,
                    isFollowing = false,
                    isMutual = false
                )
            )
        }
        binding.llFollowingCount.setOnClickListener {
            startActivityWithDefaultAnimation(
                FollowDetailActivity.getIntent(
                    this, userId,
                    isFollower = false,
                    isFollowing = true,
                    isMutual = false
                )
            )
        }

        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            binding.swipeRefreshLayout.isRefreshing = false
            RxBus.publish(RxEvent.RefreshOtherUserProfile)
        }.autoDispose()

        binding.appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            binding.swipeRefreshLayout.isEnabled = verticalOffset == 0
        })

        binding.btnChat.throttleClicks().subscribeAndObserveOnMainThread {
            otherUserProfileViewModel.getConversation(userId)
        }

        binding.btnFollow.throttleClicks().subscribeAndObserveOnMainThread {
            binding.btnFollow.visibility = View.GONE
            binding.btnFollowing.visibility = View.VISIBLE

            val totalFollowers =
                binding.tvFollowersCount.text.toString().toInt()?.let { it + 1 } ?: 0
            binding.tvFollowersCount.text = "${totalFollowers.prettyCount() ?: 0}"

            otherUserProfileViewModel.followUnfollow(userId)
        }

        binding.btnFollowing.throttleClicks().subscribeAndObserveOnMainThread {
            binding.btnFollow.visibility = View.VISIBLE
            binding.btnFollowing.visibility = View.GONE

            val totalFollowers =
                binding.tvFollowersCount.text.toString().toInt()?.let { it - 1 } ?: 0
            binding.tvFollowersCount.text = "${totalFollowers.prettyCount() ?: 0}"

            otherUserProfileViewModel.followUnfollow(userId)
        }

        RxBus.listen(RxEvent.RefreshOtherUserProfile::class.java)
            .subscribeOnIoAndObserveOnMainThread({
                if (userId != -1) {
                    otherUserProfileViewModel.getUserProfile(userId)
                    otherUserProfileViewModel.pullToRefresh(userId)
                }
            }, {
                Timber.e(it)
            }).autoDispose()
    }

    private fun listenToViewModel() {
        otherUserProfileViewModel.profileViewStates.subscribeAndObserveOnMainThread {
            when (it) {
                is OtherUserProfileViewModel.OtherUserProfileViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is OtherUserProfileViewModel.OtherUserProfileViewState.SuccessMessage -> {
                    showLongToast(it.successMessage)
                }
                is OtherUserProfileViewModel.OtherUserProfileViewState.LoadingState -> {

                }
                is OtherUserProfileViewModel.OtherUserProfileViewState.OtherUserProfileData -> {
                    binding.profileBioAppCompatTextView.setOnMentionClickListener { _, text ->
                        if (!it.outgoerUser.mentions.isNullOrEmpty()) {
                            val tag = it.outgoerUser.mentions?.firstOrNull { cInfo ->
                                cInfo?.username == text.toString()
                            }

                            if (tag != null) {
                                if (loggedInUserCache.getUserId() != tag.mentionId) {
                                    startActivityWithDefaultAnimation(
                                        NewOtherUserProfileActivity.getIntent(
                                            this@NewOtherUserProfileActivity,
                                            tag.mentionId ?: 0
                                        )
                                    )
                                }
                            }
                        }
                    }

                    loadProfileData(it.outgoerUser)
                }
                is OtherUserProfileViewModel.OtherUserProfileViewState.GetConversation -> {
                    startActivityWithDefaultAnimation(
                        NewChatActivity.getIntent(
                            this,
                            ChatConversationInfo(
                                conversationId = it.conversationId,
                                senderId = loggedInUserCache.getUserId() ?: 0,
                                receiverId = otherOutgoerUser?.id ?: 0,
                                chatType = resources.getString(R.string.label_chat),
                                name = otherOutgoerUser?.username,
                                email = otherOutgoerUser?.email,
                                profileUrl = otherOutgoerUser?.avatar,
                                createdAt = "",
                                unreadCount = 0,
                                lastMessage = "",
                                fileType = null,
                            )
                        )
                    )
                }
                is OtherUserProfileViewModel.OtherUserProfileViewState.GetUserReelInfo -> {
                    otherUserReelsAdapter.listOfDataItems = it.listOfReelsInfo
                    if (it.listOfReelsInfo.isEmpty()) {
                        //binding.tvNoData.visibility = View.VISIBLE
                    } else {
                        //binding.tvNoData.visibility = View.GONE
                    }
                }
                else -> {}
            }
        }.autoDispose()
    }

    @SuppressLint("SuspiciousIndentation")
    private fun loadProfileData(outgoerUser: OutgoerUser) {
        otherOutgoerUser = outgoerUser

        binding.profileLinkAppCompatTextView.setOnClickListener {
            val uri = Uri.parse(outgoerUser.webLink) // missing 'http://' will cause crashed
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }

        if (outgoerUser.userType == MapVenueUserType.USER.type) {
            binding.btnChat.visibility = View.VISIBLE
            if (outgoerUser.followStatus == null) {
                binding.btnFollow.visibility = View.VISIBLE
                binding.btnFollowing.visibility = View.GONE
            } else {
                if (outgoerUser.followStatus == 1) {
                    binding.btnFollow.visibility = View.GONE
                    binding.btnFollowing.visibility = View.VISIBLE
                } else {
                    binding.btnFollow.visibility = View.VISIBLE
                    binding.btnFollowing.visibility = View.GONE
                }
            }
        } else if (outgoerUser.userType == MapVenueUserType.VENUE_OWNER.type) {
            binding.btnChat.visibility = View.GONE

            if (outgoerUser.followStatus == null) {
                binding.btnFollow.visibility = View.VISIBLE
                binding.btnFollowing.visibility = View.GONE
            } else {
                if (outgoerUser.followStatus == 1) {
                    binding.btnFollow.visibility = View.GONE
                    binding.btnFollowing.visibility = View.VISIBLE
                } else {
                    binding.btnFollow.visibility = View.VISIBLE
                    binding.btnFollowing.visibility = View.GONE
                }
            }
        }

        binding.tvName.text = outgoerUser.username ?: ""
        binding.fullNameAppCompatTextView.text = outgoerUser.name ?: ""
        binding.profileBioAppCompatTextView.text = outgoerUser.about ?: ""

        binding.profileBioAppCompatTextView.visibility = if(outgoerUser.about.isNullOrEmpty()) {
            View.GONE
        } else {
            View.VISIBLE
        }

        if (outgoerUser.webTitle.isNullOrEmpty()) {
            if(outgoerUser.webLink.isNullOrEmpty()) {
                binding.profileLinkAppCompatTextView.visibility = View.GONE
            } else
                binding.profileLinkAppCompatTextView.text = outgoerUser.webLink
        } else {
            if (outgoerUser.webTitle.isNullOrEmpty() && outgoerUser.webLink.isNullOrEmpty()) {
                binding.profileLinkAppCompatTextView.visibility = View.GONE
            } else {
                binding.profileLinkAppCompatTextView.visibility = View.VISIBLE
                binding.profileLinkAppCompatTextView.text = outgoerUser.webTitle
            }
        }

        Glide.with(this)
            .load(outgoerUser.avatar)
            .placeholder(R.drawable.ic_chat_user_placeholder)
            .error(R.drawable.ic_chat_user_placeholder)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .into(binding.ivMyProfile)

        binding.tvPostCount.text = "${outgoerUser.totalReels?.prettyCount() ?: 0}"
        binding.tvFollowersCount.text = "${outgoerUser.totalFollowers?.prettyCount() ?: 0}"
        binding.tvFollowingCount.text = "${outgoerUser.totalFollowing?.prettyCount() ?: 0}"
        if (!outgoerUser.broadcastMessage.isNullOrEmpty()) {
            binding.marqueeText.isVisible = true
            binding.marqueeText.text = outgoerUser.broadcastMessage
            binding.marqueeText.isSelected = true
        } else {
            binding.marqueeText.isVisible = false
        }

        var otherName = StringBuilder()

        if (outgoerUser.otherMutualFriend ?: 0 >= 1  && outgoerUser.mutualFriend?.size ?: 0 > 0) {
            binding.tvOtherMName.visibility = View.VISIBLE
            val builder = StringBuilder()
            otherName = builder.append("and ")
                .append("${outgoerUser.otherMutualFriend?.prettyCount() ?: 0}").append(" ")
                .append("others")
        }



        outgoerUser.mutualFriend?.forEach {
            val builder = StringBuilder()
            if (outgoerUser.mutualFriend?.size ?: 0 > 2) {

                var textInfo = builder.append("Followed by ")
                    .append("${outgoerUser.mutualFriend!!.get(0).name}")
                    .append(", ")
                    .append("${outgoerUser.mutualFriend!!.get(1).name}")
                    .append(" ${otherName}")


                val android = textInfo

                val androidRules = arrayOf(outgoerUser.mutualFriend?.get(0)?.name ?: "", outgoerUser.mutualFriend?.get(1)?.name ?: "", otherName.toString() ?: "")

                Linker.Builder()
                    .content(android.toString())
                    .textView(binding.tvMName)
                    .links(androidRules)
                    .linkColor(ContextCompat.getColor(this,R.color.white))
                    .addOnLinkClickListener(onLinkClickListener)
                    .apply()

                if (outgoerUser.mutualFriend!!.size < 2) {

                    Glide.with(applicationContext)
                        .load(outgoerUser.mutualFriend!!.get(0).avatar)
                        .placeholder(R.drawable.ic_chat_user_placeholder)
                        .error(R.drawable.ic_chat_user_placeholder)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .circleCrop()
                        .into(binding.ivThumbnail)


                } else {

                    Glide.with(applicationContext)
                        .load(outgoerUser.mutualFriend!!.get(0).avatar)
                        .placeholder(R.drawable.ic_chat_user_placeholder)
                        .error(R.drawable.ic_chat_user_placeholder)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .circleCrop()
                        .into(binding.ivThumbnail)


                    Glide.with(applicationContext)
                        .load(outgoerUser.mutualFriend!!.get(1).avatar)
                        .placeholder(R.drawable.ic_chat_user_placeholder)
                        .error(R.drawable.ic_chat_user_placeholder)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .circleCrop()
                        .into(binding.ivThumbnailSec)

                }


            } else if (outgoerUser.mutualFriend?.isNotEmpty() == true && outgoerUser.mutualFriend?.size ?: 0 <= 2) {
                var textNames = builder.append("Followed by ")
                    .append("${outgoerUser.mutualFriend!!.get(0).name} ")

                var androidRules = arrayOf<String>()
                if (outgoerUser.mutualFriend?.size ?: 0 < 2) {
                    androidRules = arrayOf(outgoerUser.mutualFriend?.get(0)?.name ?: "", otherName.toString() ?: "")
                    Glide.with(applicationContext)
                        .load(outgoerUser.mutualFriend!!.get(0).avatar)
                        .placeholder(R.drawable.ic_chat_user_placeholder)
                        .error(R.drawable.ic_chat_user_placeholder)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .circleCrop()
                        .into(binding.ivThumbnail)

                } else {
                    textNames.append(", ")
                        .append("${outgoerUser.mutualFriend!!.get(1).name}")

                    androidRules = arrayOf(outgoerUser.mutualFriend?.get(0)?.name ?: "", outgoerUser.mutualFriend?.get(1)?.name ?: "", otherName.toString() ?: "")

                    Glide.with(applicationContext)
                        .load(outgoerUser.mutualFriend!!.get(0).avatar)
                        .placeholder(R.drawable.ic_chat_user_placeholder)
                        .error(R.drawable.ic_chat_user_placeholder)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .circleCrop()
                        .into(binding.ivThumbnail)


                    Glide.with(applicationContext)
                        .load(outgoerUser.mutualFriend!!.get(1).avatar)
                        .placeholder(R.drawable.ic_chat_user_placeholder)
                        .error(R.drawable.ic_chat_user_placeholder)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .circleCrop()
                        .into(binding.ivThumbnailSec)

                }

                val android = textNames.append(" ${otherName}")

//                val androidRules = arrayOf(outgoerUser.mutualFriend?.get(0)?.name ?: "", otherName.toString() ?: "")

                Linker.Builder()
                    .content(android.toString())
                    .textView(binding.tvMName)
                    .links(androidRules)
                    .linkColor(ContextCompat.getColor(this,R.color.white))
                    .addOnLinkClickListener(onLinkClickListener)
                    .apply()

//                binding.tvMName.text = textNames.append(" ${otherName}")
            }

            binding.ivVerified.isVisible = outgoerUser.profileVerified == 1

        }

        binding.mutualLinearLayout.isVisible = outgoerUser.mutualFriend?.size ?: 0 > 0
    }

    private val onLinkClickListener = object: OnLinkClickListener {
        override fun onClick(view: View, content: String) {

            if(content.endsWith("others", true)) {
                startActivity(
                    FollowDetailActivity.getIntent(
                        this@NewOtherUserProfileActivity,
                        userId,
                        isFollower = false,
                        isFollowing = false,
                        isMutual = true
                    )
                )
            } else{
                otherOutgoerUser?.mutualFriend?.find { it.name == content }?.let {
                    startActivity(NewOtherUserProfileActivity.getIntent(this@NewOtherUserProfileActivity, it.id ?: 0))
                }
            }

//            Toast.makeText(this@NewOtherUserProfileActivity, "clicked link is : $content", Toast.LENGTH_SHORT).show()
        }
    }

    private fun changeSelectedTabIconColor() {
        when (binding.tabLayout.selectedTabPosition) {
            0 -> {
                binding.tabLayout.getTabAt(0)?.setIcon(R.drawable.ic_my_profile_post_active)
                binding.tabLayout.getTabAt(1)?.setIcon(R.drawable.ic_my_profile_tagged_inactive)
                binding.tabLayout.getTabAt(2)?.setIcon(R.drawable.ic_my_profile_bookmark_inactive)
            }
            1 -> {
                binding.tabLayout.getTabAt(0)?.setIcon(R.drawable.ic_my_profile_post_inactive)
                binding.tabLayout.getTabAt(1)?.setIcon(R.drawable.ic_my_profile_tagged_active)
                binding.tabLayout.getTabAt(2)?.setIcon(R.drawable.ic_my_profile_bookmark_inactive)
            }
            2 -> {
                binding.tabLayout.getTabAt(0)?.setIcon(R.drawable.ic_my_profile_post_inactive)
                binding.tabLayout.getTabAt(1)?.setIcon(R.drawable.ic_my_profile_tagged_inactive)
                binding.tabLayout.getTabAt(2)?.setIcon(R.drawable.ic_my_profile_bookmark_active)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (userId != -1) {
            otherUserProfileViewModel.getUserProfile(userId)
            otherUserProfileViewModel.getUserReel(userId)
        }
    }


    private fun ViewPager2.hackMatchParentCheckInViewPager() {
        (getChildAt(0) as RecyclerView).clearOnChildAttachStateChangeListeners()
    }
}

