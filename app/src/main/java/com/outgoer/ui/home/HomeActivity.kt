package com.outgoer.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Point
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.PopupWindow
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager2.widget.ViewPager2
import cn.jzvd.Jzvd
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.authentication.model.OutgoerUser
import com.outgoer.api.authentication.model.UpdateNotificationTokenRequest
import com.outgoer.api.aws.CreateMediaType
import com.outgoer.api.chat.model.ChatConversationInfo
import com.outgoer.api.event.model.EventData
import com.outgoer.api.live.model.LiveEventInfo
import com.outgoer.api.sponty.model.NotificationSponty
import com.outgoer.api.sponty.model.SpontyResponse
import com.outgoer.api.venue.model.GeoFenceResponse
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.api.venue.model.VenueDetail
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityHomeBinding
import com.outgoer.databinding.CreateSelectionPopupBinding
import com.outgoer.service.UploadingPostReelsService
import com.outgoer.ui.chat.NewChatActivity
import com.outgoer.ui.createevent.CreateEventsActivity
import com.outgoer.ui.deepar.DeeparEffectsActivity
import com.outgoer.ui.home.FragmentHomeContainer.tagName
import com.outgoer.ui.home.create.CreateNewReelInfoActivity
import com.outgoer.ui.home.newReels.NewReelsFragment
import com.outgoer.ui.home.newReels.viewmodel.ReelsViewModel
import com.outgoer.ui.home.newReels.viewmodel.ReelsViewModel.Companion.reelsViewState
import com.outgoer.ui.home.newReels.viewmodel.ReelsViewState
import com.outgoer.ui.home.newmap.NewMapFragment
import com.outgoer.ui.home.newmap.venueevents.VenueEventDetailActivity
import com.outgoer.ui.home.profile.newprofile.SwitchAccountBottomSheet
import com.outgoer.ui.home.profile.venue_profile.VenueProfileFragment
import com.outgoer.ui.home.view.OutgoerTabBarView
import com.outgoer.ui.home.viewmodel.MainViewModel
import com.outgoer.ui.home.viewmodel.MainViewModel.Companion.mainPageState
import com.outgoer.ui.home.viewmodel.MainViewModel.Companion.mainPageStateSubjects
import com.outgoer.ui.livestreamuser.LiveStreamUserActivity
import com.outgoer.ui.livestreamvenue.LiveStreamVenueActivity
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import com.outgoer.ui.post.PostCameraActivity
import com.outgoer.ui.postdetail.PostDetailActivity
import com.outgoer.ui.reelsdetail.ReelsDetailActivity
import com.outgoer.ui.sponty.SpontyDetailsActivity
import com.outgoer.ui.videorooms.VideoRoomsActivity
import com.outgoer.utils.FileUtils
import com.outgoer.utils.Utility.TabItemClickListener
import com.outgoer.utils.Utility.isSpontyOpen
import com.outgoer.utils.Utility.prefetchedUrls
import com.outgoer.cache.VideoPrefetch
import timber.log.Timber
import javax.inject.Inject

class HomeActivity : BaseActivity() {

    companion object {

        lateinit var binding: ActivityHomeBinding


        private const val RC_GPS_SETTINGS = 100001
        private const val CHAT_CONVERSATION_INFO = "CHAT_CONVERSATION_INFO"

        private const val OTHER_USER_ID = "OTHER_USER_ID"
        private const val SPONTY_RESPONSE = "SPONTY_RESPONSE"
        private const val EVENT_RESPONSE = "EVENT_RESPONSE"

        private const val POST_ID = "POST_ID"
        private const val POST_SHOW_COMMENTS = "POST_SHOW_COMMENTS"
        private const val POST_SHOW_TAGGED_PEOPLE = "POST_SHOW_TAGGED_PEOPLE"

        private const val REEL_ID = "REEL_ID"
        private const val REEL_SHOW_COMMENTS = "REEL_SHOW_COMMENTS"
        private const val REEL_SHOW_TAGGED_PEOPLE = "REEL_SHOW_TAGGED_PEOPLE"

        private const val LIVE_EVENT_INFO = "LIVE_EVENT_INFO"
        private const val SPONTY_INFO = "SPONTY_INFO"
        private const val SPONTY_NAVIGATION = "SPONTY_NAVIGATION"
        private var count = 0
        const val INTENT_USER_TYPE = "INTENT_USER_TYPE"


        fun getIntent(context: Context): Intent {
            val intent = Intent(context, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            return intent
        }

        fun getIntentWithSponty(context: Context, isSponty: Boolean): Intent {
            val intent = Intent(context, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            isSpontyOpen = isSponty
            intent.putExtra(SPONTY_NAVIGATION, isSponty)
            return intent
        }

        fun launchFromSpontyNotification(context: Context, spontyResponse: SpontyResponse): Intent {
            val intent = Intent(context, HomeActivity::class.java)
            intent.putExtra(SPONTY_RESPONSE, spontyResponse)

            return intent
        }

        fun launchFromEventNotification(context: Context, eventData: EventData): Intent {
            val intent = Intent(context, HomeActivity::class.java)
            intent.putExtra(EVENT_RESPONSE, eventData)

            return intent
        }


        fun getIntentWithData(context: Context, venueOwner: String): Intent {
            val intent = Intent(context, HomeActivity::class.java)
            intent.putExtra(INTENT_USER_TYPE, venueOwner)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            return intent
        }

        fun launchFromChatNotification(
            context: Context, chatConversationInfo: ChatConversationInfo
        ): Intent {
            val intent = Intent(context, HomeActivity::class.java)
            intent.putExtra(CHAT_CONVERSATION_INFO, chatConversationInfo)
            return intent
        }

        fun launchFromSpontyNotification(
            context: Context, notificationSponty: NotificationSponty
        ): Intent {
            val intent = Intent(context, HomeActivity::class.java)
            intent.putExtra(SPONTY_INFO, notificationSponty)
            return intent
        }


        fun launchFromFollowNotification(context: Context, otherUserId: Int): Intent {
            val intent = Intent(context, HomeActivity::class.java)
            intent.putExtra(OTHER_USER_ID, otherUserId)
            return intent
        }

        fun launchFromPostNotification(
            context: Context,
            postId: Int,
            showComments: Boolean = false,
            showTaggedPeople: Boolean = false,
        ): Intent {
            val intent = Intent(context, HomeActivity::class.java)
            intent.putExtra(POST_ID, postId)
            intent.putExtra(POST_SHOW_COMMENTS, showComments)
            intent.putExtra(POST_SHOW_TAGGED_PEOPLE, showTaggedPeople)
            return intent
        }

        fun launchFromReelNotification(
            context: Context,
            reelId: Int,
            showComments: Boolean = false,
            showTaggedPeople: Boolean = false,
        ): Intent {
            val intent = Intent(context, HomeActivity::class.java)
            intent.putExtra(REEL_ID, reelId)
            intent.putExtra(REEL_SHOW_COMMENTS, showComments)
            intent.putExtra(REEL_SHOW_TAGGED_PEOPLE, showTaggedPeople)
            return intent
        }

        fun launchFromLiveNotification(context: Context, liveEventInfo: LiveEventInfo): Intent {
            val intent = Intent(context, HomeActivity::class.java)
            intent.putExtra(LIVE_EVENT_INFO, liveEventInfo)
            return intent
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<MainViewModel>
    private lateinit var mainViewModel: MainViewModel

    @Inject
    internal lateinit var viewModelFactoryReel: ViewModelFactory<ReelsViewModel>
    private lateinit var reelsViewModel: ReelsViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    lateinit var tabManager: HomeTabManager
    private lateinit var locationManager: LocationManager
    private var loggedInUser: OutgoerUser? = null

    private lateinit var popupWindow: PopupWindow
    private lateinit var popupBinding: CreateSelectionPopupBinding
    private var navigationBarHeight: Int = 0

    private var geoFenceRes: GeoFenceResponse? = null
    var isAddIconDisable = false
    private lateinit var videoPrefetch: VideoPrefetch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        OutgoerApplication.component.inject(this)
        mainViewModel = getViewModelFromFactory(viewModelFactory)
        reelsViewModel = getViewModelFromFactory(viewModelFactoryReel)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        videoPrefetch = VideoPrefetch(
            this,
            lifecycleScope
        )
        manageNotification()
        loggedInUser = loggedInUserCache.getLoggedInUser()?.loggedInUser

        val tabItemClickListener = object : TabItemClickListener {
            override fun onTabItemClicked(tabType: String) {
                Timber.tag("OnTabItemClicked").d("Tab item clicked HomeActivity: $tabType")
                when(tabType) {
                    "f0" -> {
                        Jzvd.goOnPlayOnPause()
                        val newReelsFragment = NewReelsFragment.newInstance(isSpontyOpen)
                        newReelsFragment.refreshPages()
                    }
                    "f1" -> {
                        Jzvd.goOnPlayOnPause()
                        val newMapFragment = NewMapFragment.newInstance()
                        newMapFragment.refreshPages()
                    }
                    "f3" -> {
                        Jzvd.goOnPlayOnPause()
                        RxBus.publish(RxEvent.DataReloadReel(selectedTab = "Reels"))
                    }
                    "f4" -> {
                        Jzvd.goOnPlayOnPause()
                        when (tagName) {
                            "VenueProfileFragmentTag" -> {
                                RxBus.publish(RxEvent.DataReload(selectedTab = tagName))
                            }
                            "NewMyProfileFragmentTag" -> {
                                RxBus.publish(RxEvent.DataReload(selectedTab = tagName))
                            }
                            else -> {
                                Timber.tag("OnTabItemClicked").d("tagName: $tagName")
                            }
                        }
                    }
                    else -> {
                        Jzvd.goOnPlayOnPause()
                        Timber.tag("OnTabItemClicked").d("tagName: $tagName & tabType: $tabType")
                    }
                }
            }
        }
        tabManager = HomeTabManager(this,
            loggedInUser, intent?.getBooleanExtra(SPONTY_NAVIGATION, false), tabItemClickListener)

        RxBus.listen(RxEvent.GeoFenceResEntry::class.java).subscribeAndObserveOnMainThread {
            Timber.tag("Geofence").d("GeoFenceResEntry: it.geoFenceResponse: ${it.geoFenceResponse}")
            geoFenceRes = it.geoFenceResponse
            mainPageStateSubjects.onNext(MainViewModel.MainPageViewState.GetLocationInfo(it.geoFenceResponse))
        }.autoDispose()

        RxBus.listen(RxEvent.GeoFenceResExit::class.java).subscribeAndObserveOnMainThread {
            Timber.tag("Geofence").d("GeoFenceResExit: it.geoFenceResponse: ${it.geoFenceResponse}")
            if (it.geoFenceResponse.venueStatus == 1) {
                openBottomSheet(it.geoFenceResponse, true)
            }
        }.autoDispose()


        XXPermissions.with(this)
            .permission(
                listOf(
                    Permission.POST_NOTIFICATIONS
                )
            )
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                    if (!all) {
                        showToast(getString(R.string.msg_some_permission_denied))
                    }
                }

                override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                    super.onDenied(permissions, never)
                    showToast(getString(R.string.msg_permission_denied))
                }
            })

        listenToViewEvents()
        listenToViewModel()

        count += 1
        LocalBroadcastManager.getInstance(this).registerReceiver(
            mMessageReceiver, IntentFilter("ShowReelsActivity")
        )
        LocalBroadcastManager.getInstance(this).registerReceiver(
            fMessageReceiver, IntentFilter("HideProcessingShowFinished")
        )
        LocalBroadcastManager.getInstance(this).registerReceiver(
            cMessageReceiver, IntentFilter("cancelUploading")
        )
        LocalBroadcastManager.getInstance(this).registerReceiver(
            sMessageReceiver, IntentFilter("IconDisable")
        )
        LocalBroadcastManager.getInstance(this).registerReceiver(
            storyShowFinishMessageReceiver, IntentFilter("showFinish")
        )

        intent?.getStringExtra(INTENT_USER_TYPE)?.let {
            if (it == MapVenueUserType.VENUE_OWNER.type) {
                binding.venueBottomSheet.visibility = View.GONE
                binding.viewPager.currentItem = 4
                binding.tabBar.activatedTab = OutgoerTabBarView.TAB_MY_PROFILE
            }
        }

    }

    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val postType = intent.getStringExtra(UploadingPostReelsService.INTENT_EXTRA_POST_TYPE)
            Timber.tag("HomeActivity").i("postType :$postType")
            if (postType == CreateMediaType.reels.name || postType == CreateMediaType.reels_video.name) {
                binding.venueBottomSheet.visibility = View.GONE
                binding.viewPager.currentItem = 3
                binding.tabBar.activatedTab = OutgoerTabBarView.TAB_MESSAGE
                isAddIconDisable = true
            } else {
                binding.venueBottomSheet.visibility = View.GONE
                binding.viewPager.currentItem = 0
                binding.tabBar.activatedTab = OutgoerTabBarView.TAB_HOME
                isAddIconDisable = true
            }
        }
    }

    private val fMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val postType = intent.getStringExtra(UploadingPostReelsService.INTENT_EXTRA_POST_TYPE)
            Timber.tag("DiscoverReelsFragment").i("postType :$postType")

            isAddIconDisable = false
        }
    }
    private val sMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val postType = intent.getStringExtra(UploadingPostReelsService.INTENT_EXTRA_POST_TYPE)
            Timber.tag("DiscoverReelsFragment").i("postType :$postType")
            isAddIconDisable = true
        }
    }
    private val cMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            isAddIconDisable = false
            showToast("Video compression failed. Please try again after some time")
        }
    }
    private val storyShowFinishMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            binding.viewPager.currentItem = 0
            binding.tabBar.activatedTab = OutgoerTabBarView.TAB_HOME
            isAddIconDisable = false
        }
    }

    private fun listenToViewEvents() {
        setPopUpWindow()
        reelsViewModel.pullToRefresh(1)
        navigationBarHeight = getNavigationBarHeightInfo()

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        loggedInUserCache.userAuthenticationFail.subscribeAndObserveOnMainThread {
            loggedInUserCache.clearLoggedInUserLocalPrefs()

            startActivityWithFadeInAnimation(getIntent(this))
        }.autoDispose()

        binding.venueBottomSheet.visibility = View.GONE
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (tabManager.activatedTab) {
                    OutgoerTabBarView.TAB_HOME -> {
                        binding.venueBottomSheet.visibility = View.GONE
                    }
                    OutgoerTabBarView.TAB_REELS -> {
                        binding.venueBottomSheet.visibility = View.GONE
                    }
                    OutgoerTabBarView.TAB_MAP -> {
                        checkLocationPermission()
                    }
                    OutgoerTabBarView.TAB_MESSAGE -> {
                        binding.venueBottomSheet.visibility = View.GONE
                    }
                    OutgoerTabBarView.TAB_MY_PROFILE -> {
                        binding.venueBottomSheet.visibility = View.GONE
                    }
                    else -> {}
                }
            }
        })

        loadDefaults()
        RxBus.listen(RxEvent.OpenVenueUserProfile::class.java).subscribeAndObserveOnMainThread {
            binding.venueBottomSheet.visibility = View.GONE
            binding.viewPager.currentItem = 4
            binding.tabBar.activatedTab = OutgoerTabBarView.TAB_MY_PROFILE
        }.autoDispose()

        println("Height home: " + binding.tabBar.height)

    }

    fun hideMapFragment() {
        binding.venueBottomSheet.visibility = View.GONE
    }

    private fun listenToViewModel() {
        reelsViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is ReelsViewState.GetAllReelsInfo -> {
                    val listOfReels = it.listOfReelsInfo.take(3).map { it.videoUrl.plus("?clientBandwidthHint=2.5") }
                    listOfReels.forEach { videoUrl ->
                        if (!videoUrl.isNullOrEmpty() && prefetchedUrls.add(videoUrl)) {
                            videoPrefetch.prefetchHlsVideo(Uri.parse(videoUrl))
                        }
                    }
                }
                else -> {}
            }
        }.autoDispose()

        mainPageState.subscribeAndObserveOnMainThread {
            when (it) {
                is MainViewModel.MainPageViewState.SuccessMessage -> {

                }
                is MainViewModel.MainPageViewState.ErrorMessage -> {

                }
                is MainViewModel.MainPageViewState.LoadingState -> {

                }
                is MainViewModel.MainPageViewState.NotificationAlertState -> {
                    RxBus.publish(RxEvent.UpdateNotificationBadge(it.notificationStatus))
                }
                is MainViewModel.MainPageViewState.GetLocationInfo -> {
                    if (it.geoFence != null) {
                        if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.userType != MapVenueUserType.VENUE_OWNER.type) {
                            openBottomSheet(it.geoFence, it.geoFence.venueStatus == 1)
                        }
                    }
                }
                else -> {}
            }
        }.autoDispose()
    }


    private fun manageNotification() {
        intent?.getParcelableExtra<EventData>(EVENT_RESPONSE)?.let {
            startActivityWithDefaultAnimation(VenueEventDetailActivity.getIntentWithId(this, it.id))
        }

        intent?.getParcelableExtra<SpontyResponse>(SPONTY_RESPONSE)?.let {
            startActivityWithDefaultAnimation(SpontyDetailsActivity.getIntent(this, it.id))
        }

        intent?.getParcelableExtra<NotificationSponty>(SPONTY_INFO)?.let {
            startActivityWithDefaultAnimation(SpontyDetailsActivity.getIntent(this, it.spontyId, it.objectType.equals("sponty_comment")))
        }

        intent?.getParcelableExtra<ChatConversationInfo>(CHAT_CONVERSATION_INFO)?.let {
            startActivityWithDefaultAnimation(NewChatActivity.getIntent(this, it))
        }

        intent?.getIntExtra(OTHER_USER_ID, -1)?.let { otherUserId ->
            if (otherUserId != -1) {
                startActivityWithDefaultAnimation(
                    NewOtherUserProfileActivity.getIntent(
                        this, otherUserId
                    )
                )
            }
        }

        intent?.getIntExtra(POST_ID, -1)?.let { postId ->
            if (postId != -1) {
                val showComments = intent?.getBooleanExtra(POST_SHOW_COMMENTS, false) ?: false
                val showTaggedPeople = intent?.getBooleanExtra(POST_SHOW_TAGGED_PEOPLE, false) ?: false
                startActivityWithDefaultAnimation(
                    PostDetailActivity.getIntent(
                        this, postId, showComments, showTaggedPeople
                    )
                )
            }
        }

        intent?.getIntExtra(REEL_ID, -1)?.let { reelId ->
            if (reelId != -1) {
                val showComments = intent?.getBooleanExtra(REEL_SHOW_COMMENTS, false) ?: false
                val showTaggedPeople = intent?.getBooleanExtra(REEL_SHOW_TAGGED_PEOPLE, false) ?: false
                startActivityWithDefaultAnimation(
                    ReelsDetailActivity.getIntent(
                        this, reelId, showComments, showTaggedPeople
                    )
                )
            }
        }

        intent?.getParcelableExtra<LiveEventInfo>(LIVE_EVENT_INFO)?.let {
            if (it.isLock == 1) {
                startActivityWithDefaultAnimation(VideoRoomsActivity.getIntent(this))
            } else {
                startActivityWithDefaultAnimation(VideoRoomsActivity.getIntent(this, it))
            }
        }

    }

    override fun onBackPressed() {
        if (!tabManager.onBackPressed()) {
            if (tabManager.activatedTab != OutgoerTabBarView.TAB_HOME) {
                tabManager.selectTab(OutgoerTabBarView.TAB_HOME, true)
                return
            } else {
                finish()
            }
        }
        popupWindow.dismiss()
    }

    @SuppressLint("HardwareIds")
    private fun loadDefaults() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@OnCompleteListener
            }
            val token = task.result
            var androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            if (androidId.isNullOrEmpty()) {
                androidId = token
            }
            val updateNotificationTokenRequest = UpdateNotificationTokenRequest(
                firebaseToken = token,
                platform = "android",
                deviceId = androidId,
            )
            mainViewModel.updateNotificationToken(updateNotificationTokenRequest)
        })
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            XXPermissions.with(this)
                .permission(Permission.ACCESS_COARSE_LOCATION)
                .permission(Permission.ACCESS_FINE_LOCATION)
                .request(object : OnPermissionCallback {

                    override fun onGranted(permissions: List<String>, all: Boolean) {
                        if (all) {
                            val isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                            if (isGPSEnabled) {
                                RxBus.publish(RxEvent.RefreshMapPage)
                            } else {
                                showGPSSettingsAlert()
                            }
                        } else {
                            showToast(getString(R.string.msg_location_permission_required_for_venue))
                        }
                    }

                    override fun onDenied(permissions: List<String>, never: Boolean) {
                        showToast(getString(R.string.msg_location_permission_required_for_venue))
                        val isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                        if (isGPSEnabled) {
                            RxBus.publish(RxEvent.RefreshMapPage)
                        } else {
                            showGPSSettingsAlert()
                        }
                    }
                })
        } else {
            val isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            if (!isGPSEnabled) {
                showGPSSettingsAlert()
            }
        }
    }

    private fun showGPSSettingsAlert() {
        val alertDialog = AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialogCustom))
        alertDialog.setTitle(getString(R.string.msg_gps_settings))
        alertDialog.setMessage(getString(R.string.msg_gps_settings_confirmation))
        alertDialog.setPositiveButton(getString(R.string.label_settings)) { _, _ ->
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivityForResult(intent, RC_GPS_SETTINGS)
            tabManager.selectTab(OutgoerTabBarView.TAB_HOME, true)
        }
        alertDialog.setNegativeButton(getString(R.string.label_cancel)) { dialog, _ ->
            dialog.cancel()
            tabManager.selectTab(OutgoerTabBarView.TAB_HOME, true)
        }
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == FileUtils.PICK_IMAGE_1) {
            VenueProfileFragment().onActivityResult(requestCode, resultCode, data)
        }

        if (requestCode == RC_GPS_SETTINGS) {
            val isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            if (isGPSEnabled) {
                RxBus.publish(RxEvent.RefreshMapPage)
            }
        }

        if (requestCode == PostCameraActivity.RC_CAPTURE_PICTURE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    val filePath = data.getStringExtra(PostCameraActivity.INTENT_EXTRA_FILE_PATH)
                    if (!filePath.isNullOrEmpty()) {
                        startActivityWithDefaultAnimation(
                            CreateNewReelInfoActivity.launchActivity(
                                this, filePath
                            )
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        firebaseDynamicLink()
    }


    private fun firebaseDynamicLink() {
        Firebase.dynamicLinks.getDynamicLink(intent).addOnSuccessListener(this) { linkData ->
            Timber.tag("FirebaseDynamicLinks").i("Firebase Link %s", linkData?.link)
            linkData?.link?.let {
                when {
                    !it.getQueryParameter("post_id").isNullOrEmpty() -> {
                        val postId: Int? = it.getQueryParameter("post_id")?.toIntOrNull()
                        if (postId != null) {
                            startActivityWithDefaultAnimation(
                                PostDetailActivity.getIntent(
                                    this, postId
                                )
                            )
                        }
                    }
                    !it.getQueryParameter("reels_id").isNullOrEmpty() -> {
                        val reelId: Int? = it.getQueryParameter("reels_id")?.toIntOrNull()
                        if (reelId != null) {
                            startActivityWithDefaultAnimation(
                                ReelsDetailActivity.getIntent(
                                    this, reelId
                                )
                            )
                        }
                    }
                }
            }
        }.addOnFailureListener {
            Timber.e(it)
        }
    }

    private fun checkPermissionsForCreateReels(isReel: Boolean) {
        XXPermissions.with(this).permission(
            listOf(
                Permission.CAMERA, Permission.RECORD_AUDIO
            )
        ).permission(Permission.READ_MEDIA_VIDEO).permission(Permission.READ_MEDIA_IMAGES).request(object : OnPermissionCallback {

            override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                if (all) {
                    if (isReel) {
                        startActivity(DeeparEffectsActivity.getIntent(this@HomeActivity))
                    } else {
                        startActivity(DeeparEffectsActivity.getIntent(this@HomeActivity))
                    }
                } else {
                    showToast(getString(R.string.msg_some_permission_denied))
                }
            }

            override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                super.onDenied(permissions, never)
                XXPermissions.startPermissionActivity(this@HomeActivity, permissions);
                showToast(getString(R.string.msg_permission_denied))
            }
        })
    }

    fun addPostViewClick() {

        XXPermissions.with(this).permission(
            listOf(
                Permission.CAMERA, Permission.RECORD_AUDIO
            )
        ).permission(Permission.READ_MEDIA_VIDEO).permission(Permission.READ_MEDIA_IMAGES).request(object : OnPermissionCallback {

            override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                if (all) {
                    if (isAddIconDisable) {
                        showToast("Uploading is in progress, after it is finished, you can upload others.")
                    } else {
                        startActivity(DeeparEffectsActivity.getIntent(this@HomeActivity))
                    }
                } else {
                    showToast(getString(R.string.msg_some_permission_denied))
                }
            }

            override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                super.onDenied(permissions, never)

                XXPermissions.startPermissionActivity(this@HomeActivity, permissions);
                showToast(getString(R.string.msg_permission_denied))
            }
        })


//        popupWindow.showAtLocation(
//            binding.viewPager,
//            Gravity.BOTTOM,
//            0,
//            binding.tabBar.height + navigationBarHeight
//        )
    }


    private fun getNavigationBarHeightInfo(): Int {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= 30) {
            windowManager.currentWindowMetrics.windowInsets.getInsets(WindowInsets.Type.navigationBars()).bottom

        } else {
            val currentDisplay = windowManager.defaultDisplay
            val appUsableSize = Point()
            val realScreenSize = Point()
            currentDisplay?.apply {
                getSize(appUsableSize)
                getRealSize(realScreenSize)
            }

            // navigation bar on the side
            if (appUsableSize.x < realScreenSize.x) {
                return realScreenSize.x - appUsableSize.x
            }

            // navigation bar at the bottom
            return if (appUsableSize.y < realScreenSize.y) {
                realScreenSize.y - appUsableSize.y - 200
            } else 0
        }
    }

    private fun setPopUpWindow() {
        val inflater = applicationContext.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        popupBinding = CreateSelectionPopupBinding.inflate(inflater)
        popupWindow = PopupWindow(
            popupBinding.root, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, true
        )
        popupBinding.postAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            popupWindow.dismiss()
            if (loggedInUser != null) {
                checkPermissions(false)
            }
        }.autoDispose()
        popupBinding.reelsAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            popupWindow.dismiss()
            if (loggedInUser != null) {
                checkPermissionsForCreateReels(true)
            }
        }.autoDispose()

        popupBinding.eventAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            popupWindow.dismiss()
            loggedInUser?.let {
                startActivityWithFadeInAnimation(
                    CreateEventsActivity.getIntent(
                        this@HomeActivity, VenueDetail(
                            it.id,
                            name = it.name,
                            venueAddress = it.venueAddress,
                            distance = 0.0,
                            reviewAvg = it.reviewAvg,
                            latitude = it.latitude,
                            longitude = it.longitude,
                            avatar = it.avatar
                        )
                    )
                )
            }
        }.autoDispose()

//show event bottom tab when venue user login
        popupBinding.eventAppCompatTextView.isVisible = loggedInUser?.userType != MapVenueUserType.USER.type
        popupBinding.liveAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            popupWindow.dismiss()

            loggedInUser?.let {
                val intent = when (it.userType) {
                    MapVenueUserType.USER.type -> {
                        LiveStreamUserActivity.getIntent(this@HomeActivity)
                    }
                    MapVenueUserType.VENUE_OWNER.type -> {
                        LiveStreamVenueActivity.getIntent(this@HomeActivity)
                    }
                    else -> {
                        LiveStreamUserActivity.getIntent(this@HomeActivity)
                    }
                }
                startActivityWithDefaultAnimation(intent)
            }
        }.autoDispose()

        popupBinding.spontyAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            popupWindow.dismiss()
            loggedInUser?.let {
//                startActivityWithFadeInAnimation(CreateSpontyActivity.getIntent(this@HomeActivity))
            }
        }.autoDispose()

    }

    private fun checkPermissions(isReel: Boolean) {
        XXPermissions.with(this).permission(
            listOf(
                Permission.CAMERA, Permission.RECORD_AUDIO
            )
        ).permission(Permission.READ_MEDIA_VIDEO).permission(Permission.READ_MEDIA_IMAGES).request(object : OnPermissionCallback {

            override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                if (all) {
                    if (isReel) {
                        startActivity(DeeparEffectsActivity.getIntent(this@HomeActivity))
                    } else {
                        startActivity(DeeparEffectsActivity.getIntent(this@HomeActivity))
                    }
                } else {
                    showToast(getString(R.string.msg_some_permission_denied))
                }
            }

            override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                super.onDenied(permissions, never)
                showToast(getString(R.string.msg_permission_denied))
                XXPermissions.startPermissionActivity(this@HomeActivity, permissions);

            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (binding.viewPager.currentItem == 0) {
            RxBus.publish(RxEvent.CheckHomeFragmentIsVisible(true))
        }
    }

    fun openSwitchAccountDialog() {
        val switchAccountBottomSheet: SwitchAccountBottomSheet = SwitchAccountBottomSheet.newInstance()
        switchAccountBottomSheet.switchAccount.subscribeAndObserveOnMainThread {
            startActivityWithFadeInAnimation(HomeActivity.getIntent(this@HomeActivity))
            finish()
        }
        switchAccountBottomSheet.show(supportFragmentManager, SwitchAccountBottomSheet.Companion::class.java.name)
    }
}