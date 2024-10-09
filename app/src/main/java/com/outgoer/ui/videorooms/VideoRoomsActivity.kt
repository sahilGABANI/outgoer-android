package com.outgoer.ui.videorooms

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.authentication.model.OutgoerUser
import com.outgoer.api.live.model.LiveEventInfo
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityVideoRoomsBinding
import com.outgoer.ui.livestreamuser.LiveStreamUserActivity
import com.outgoer.ui.livestreamvenue.LiveStreamVenueActivity
import com.outgoer.ui.videorooms.view.LiveVenueRoomAdapter
import com.outgoer.ui.videorooms.view.VideoRoomUserAdapter
import com.outgoer.ui.videorooms.viewmodel.VideoRoomsViewModel
import com.outgoer.ui.videorooms.viewmodel.VideoRoomsViewState
import com.outgoer.ui.watchliveevent.WatchLiveEventActivity
import javax.inject.Inject

class VideoRoomsActivity : BaseActivity() {

    companion object {
        private const val LIVE_EVENT_INFO = "LIVE_EVENT_INFO"
        private const val LIVE_EVENT_ID = "LIVE_EVENT_ID"
        fun getIntent(context: Context, liveEventInfo: LiveEventInfo? = null): Intent {
            val intent = Intent(context, VideoRoomsActivity::class.java)
            intent.putExtra(LIVE_EVENT_INFO, liveEventInfo)
            return intent
        }

        fun getIntentLive(context: Context, liveId: Int? = null): Intent {
            val intent = Intent(context, VideoRoomsActivity::class.java)
            intent.putExtra(LIVE_EVENT_ID, liveId)
            return intent
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<VideoRoomsViewModel>
    private lateinit var videoRoomsViewModel: VideoRoomsViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var loggedInUser: OutgoerUser? = null

    private lateinit var binding: ActivityVideoRoomsBinding

    private lateinit var videoRoomUserAdapter: VideoRoomUserAdapter
    private lateinit var liveVenueRoomAdapter: LiveVenueRoomAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityVideoRoomsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        OutgoerApplication.component.inject(this)
        videoRoomsViewModel = getViewModelFromFactory(viewModelFactory)

        listenToViewModel()
        listenToViewEvents()

        loggedInUser = loggedInUserCache.getLoggedInUser()?.loggedInUser

        intent?.getParcelableExtra<LiveEventInfo>(LIVE_EVENT_INFO)?.let {
            openWatchLiveEventActivity(it)
        }
    }

    private fun listenToViewModel() {
        videoRoomsViewModel.videoRoomsState.subscribeAndObserveOnMainThread {
            when (it) {
                is VideoRoomsViewState.LoadingState -> {}
                is VideoRoomsViewState.ErrorMessage -> {}
                is VideoRoomsViewState.SuccessMessage -> {}
                is VideoRoomsViewState.LoadAllActiveEventList -> {
                    it.allActiveEventInfo.apply {
                        videoRoomUserAdapter.listOfDataItems = userEventList
                        liveVenueRoomAdapter.listOfDataItems = venueEventList

                        if (intent.hasExtra(LIVE_EVENT_ID)) {
                            venueEventList?.find { it.id == intent.getIntExtra(LIVE_EVENT_ID, -1) }
                                ?.apply {
                                    openWatchLiveEventActivity(this)
                                }
                        }

                        if (venueEventList.isNullOrEmpty()) {
                            binding.llNoData.visibility = View.VISIBLE
                        } else {
                            binding.llNoData.visibility = View.GONE
                        }
                        if (userEventList.isNullOrEmpty()) {
                            binding.llNoDataLinearLayout.visibility = View.VISIBLE
                        } else {
                            binding.llNoDataLinearLayout.visibility = View.GONE
                        }
                    }
                }
            }
        }.autoDispose()
    }

    private fun listenToViewEvents() {
        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            videoRoomsViewModel.getAllActiveLiveEvent()
            binding.swipeRefreshLayout.isRefreshing = false
        }.autoDispose()
        binding.refreshAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            videoRoomsViewModel.getAllActiveLiveEvent()
        }.autoDispose()

        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()

        videoRoomUserAdapter = VideoRoomUserAdapter(this)
        videoRoomUserAdapter.apply {
            videoRoomCreateNewClick.subscribeAndObserveOnMainThread {
                checkPermissions(callBack = {
                    loggedInUser?.let {
                        val intent = when (it.userType) {
                            MapVenueUserType.USER.type -> {
                                LiveStreamUserActivity.getIntent(this@VideoRoomsActivity)
                            }
                            MapVenueUserType.VENUE_OWNER.type -> {
                                LiveStreamVenueActivity.getIntent(this@VideoRoomsActivity)
                            }
                            else -> {
                                LiveStreamUserActivity.getIntent(this@VideoRoomsActivity)
                            }
                        }
                        startActivityWithDefaultAnimation(intent)
                    }
                })
            }.autoDispose()

            videoRoomUserClick.subscribeAndObserveOnMainThread {
                openWatchLiveEventActivity(it)
            }.autoDispose()
        }
        binding.rvVideoRoomList.apply {
            layoutManager =
                LinearLayoutManager(this@VideoRoomsActivity, RecyclerView.HORIZONTAL, false)
            adapter = videoRoomUserAdapter
        }
        videoRoomUserAdapter.listOfDataItems = listOf()

        liveVenueRoomAdapter = LiveVenueRoomAdapter(this)
        liveVenueRoomAdapter.apply {
            liveVenueRoomClick.subscribeAndObserveOnMainThread {
                openWatchLiveEventActivity(it)
            }.autoDispose()
        }
        binding.rvLiveVenueRoomList.apply {
            layoutManager =
                GridLayoutManager(this@VideoRoomsActivity, 2, RecyclerView.VERTICAL, false)
            adapter = liveVenueRoomAdapter
        }
    }

    private fun openWatchLiveEventActivity(liveEventInfo: LiveEventInfo) {
        checkPermissions(callBack = {
            if (liveEventInfo.isLock == 1) {
                openVerifyDialog(liveEventInfo)
            } else {
                startActivityWithDefaultAnimation(
                    WatchLiveEventActivity.getIntent(
                        this@VideoRoomsActivity,
                        liveEventInfo
                    )
                )
            }
        })
    }

    private fun checkPermissions(callBack: () -> Unit) {
        XXPermissions.with(this)
            .permission(
                listOf(
                    Permission.CAMERA, Permission.RECORD_AUDIO,
                    Permission.READ_MEDIA_VIDEO, Permission.READ_MEDIA_IMAGES
                )
            )
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: List<String>, all: Boolean) {
                    if (all) {
                        callBack.invoke()
                    } else {
                        showToast(getString(R.string.msg_some_permission_denied))
                    }
                }

                override fun onDenied(permissions: List<String>, never: Boolean) {
                    if (never) {
                        showToast(getString(R.string.msg_permission_permanently_denied))
                        XXPermissions.startPermissionActivity(this@VideoRoomsActivity, permissions)
                    } else {
                        showToast(getString(R.string.msg_permission_denied))
                    }
                }
            })
    }

    private fun openVerifyDialog(liveEventInfo: LiveEventInfo, byAdmin: Boolean = false) {
        val liveEventLockDialogFragment = LiveEventLockDialogFragment(liveEventInfo)
        liveEventLockDialogFragment.apply {
            verify.subscribeAndObserveOnMainThread {
                if (byAdmin) {
                } else {
                    startActivityWithDefaultAnimation(
                        WatchLiveEventActivity.getIntent(
                            this@VideoRoomsActivity,
                            liveEventInfo
                        )
                    )
                }
                liveEventLockDialogFragment.dismiss()
            }.autoDispose()
        }
        liveEventLockDialogFragment.show(
            supportFragmentManager,
            "LiveEventLockDialogFragment"
        )
    }

    override fun onResume() {
        super.onResume()
        videoRoomsViewModel.getAllActiveLiveEvent()
    }
}