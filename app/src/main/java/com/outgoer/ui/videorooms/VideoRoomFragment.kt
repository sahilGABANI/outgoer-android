package com.outgoer.ui.videorooms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
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
import com.outgoer.base.BaseFragment
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showToast
import com.outgoer.base.extension.startActivityWithDefaultAnimation
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import com.outgoer.databinding.FragmentVideoRoomBinding
import com.outgoer.ui.livestreamuser.LiveStreamUserActivity
import com.outgoer.ui.livestreamvenue.LiveStreamVenueActivity
import com.outgoer.ui.videorooms.view.LiveVenueRoomAdapter
import com.outgoer.ui.videorooms.view.VideoRoomUserAdapter
import com.outgoer.ui.videorooms.viewmodel.VideoRoomsViewModel
import com.outgoer.ui.videorooms.viewmodel.VideoRoomsViewState
import com.outgoer.ui.watchliveevent.WatchLiveEventActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class VideoRoomFragment : BaseFragment() {

    private var _binding: FragmentVideoRoomBinding? = null
    private val binding get() = _binding!!

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<VideoRoomsViewModel>
    private lateinit var videoRoomsViewModel: VideoRoomsViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var loggedInUser: OutgoerUser? = null

    private lateinit var videoRoomUserAdapter: VideoRoomUserAdapter
    private lateinit var liveVenueRoomAdapter: LiveVenueRoomAdapter

    companion object {
        @JvmStatic
        fun newInstance() = VideoRoomFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoRoomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        OutgoerApplication.component.inject(this)
        videoRoomsViewModel = getViewModelFromFactory(viewModelFactory)

        listenToViewModel()
        listenToViewEvents()

        loggedInUser = loggedInUserCache.getLoggedInUser()?.loggedInUser

        RxBus.listen(RxEvent.DataReload::class.java).subscribeOnIoAndObserveOnMainThread({
            if (it.selectedTab == "Live") {
                lifecycleScope.launch {
                    delay(1000)
                    videoRoomsViewModel.getAllActiveLiveEvent()
                }
                (binding.rvVideoRoomList.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(0, 0)
                binding.rvLiveVenueRoomList.scrollToPosition(0)
            }
        }, {
            Timber.e(it)
        }).autoDispose()
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


        videoRoomUserAdapter = VideoRoomUserAdapter(requireContext())
        videoRoomUserAdapter.apply {
            videoRoomCreateNewClick.subscribeAndObserveOnMainThread {
                checkPermissions(callBack = {
                    loggedInUser?.let {
                        val intent = when (it.userType) {
                            MapVenueUserType.USER.type -> {
                                LiveStreamUserActivity.getIntent(requireContext())
                            }
                            MapVenueUserType.VENUE_OWNER.type -> {
                                LiveStreamVenueActivity.getIntent(requireContext())
                            }
                            else -> {
                                LiveStreamUserActivity.getIntent(requireContext())
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
                LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
            adapter = videoRoomUserAdapter
        }
        videoRoomUserAdapter.listOfDataItems = listOf()

        liveVenueRoomAdapter = LiveVenueRoomAdapter(requireContext())
        liveVenueRoomAdapter.apply {
            liveVenueRoomClick.subscribeAndObserveOnMainThread {
                openWatchLiveEventActivity(it)
            }.autoDispose()
        }
        binding.rvLiveVenueRoomList.apply {
            layoutManager =
                GridLayoutManager(requireContext(), 2, RecyclerView.VERTICAL, false)
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
                        requireContext(),
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
                    Permission.READ_MEDIA_IMAGES, Permission.READ_MEDIA_VIDEO
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
                        XXPermissions.startPermissionActivity(requireContext(), permissions)
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
                if (!byAdmin) {
                    startActivityWithDefaultAnimation(
                        WatchLiveEventActivity.getIntent(
                            requireContext(),
                            liveEventInfo
                        )
                    )
                }
                liveEventLockDialogFragment.dismiss()
            }.autoDispose()
        }
        liveEventLockDialogFragment.show(
            parentFragmentManager,
            "LiveEventLockDialogFragment"
        )
    }

    override fun onResume() {
        super.onResume()
        videoRoomsViewModel.getAllActiveLiveEvent()
    }
}