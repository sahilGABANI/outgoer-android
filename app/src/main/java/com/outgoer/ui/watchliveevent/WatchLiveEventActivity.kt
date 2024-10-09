package com.outgoer.ui.watchliveevent

import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.jakewharton.rxbinding3.widget.editorActions
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.live.model.LiveEventInfo
import com.outgoer.api.live.model.LiveStreamNoOfCoHost
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityWatchLiveEventBinding
import com.outgoer.ui.livestreamuser.liveuserinfo.LiveUserInfoBottomSheet
import com.outgoer.ui.livestreamuser.view.LiveEventCommentAdapter
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import com.outgoer.ui.videorooms.UpdateInviteCoHostStatusDialog
import com.outgoer.ui.watchliveevent.viewmodel.WatchLiveVideoViewModel
import com.outgoer.utils.UiUtils
import com.petersamokhin.android.floatinghearts.HeartsView
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.models.ChannelMediaOptions
import io.agora.rtc.video.VideoCanvas
import io.agora.rtc.video.VideoEncoderConfiguration
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.abs

class WatchLiveEventActivity : BaseActivity() {

    companion object {
        private const val LIVE_EVENT_INFO = "LIVE_EVENT_INFO"
        fun getIntent(context: Context, liveEventInfo: LiveEventInfo): Intent {
            val intent = Intent(context, WatchLiveEventActivity::class.java)
            intent.putExtra(LIVE_EVENT_INFO, liveEventInfo)
            return intent
        }
    }

    private var engine: RtcEngine? = null
    private var myUid = 0
    private var joined = false
    private var handler: Handler? = null

    private lateinit var liveEventInfo: LiveEventInfo

    private var isCoHost: Boolean = false

    private var firstCoHost: Boolean = false
    private var secondCoHost: Boolean = false
    private var thirdCoHost: Boolean = false
    private var fourthCoHost: Boolean = false

    private var isKickUser: Int? = null

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<WatchLiveVideoViewModel>
    private lateinit var watchLiveVideoViewModel: WatchLiveVideoViewModel
    private lateinit var liveEventCommentAdapter: LiveEventCommentAdapter

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    var liveStreamNoOfCoHostHashMap = HashMap<Int, String>()

    private lateinit var binding: ActivityWatchLiveEventBinding

    private val getThumbModel: HeartsView.Model by lazy {
        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_heart_gred)
        val bitmap = (drawable as BitmapDrawable).bitmap
        HeartsView.Model(
            0,
            bitmap
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWatchLiveEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        OutgoerApplication.component.inject(this)
        watchLiveVideoViewModel = getViewModelFromFactory(viewModelFactory)

        handler = Handler(Looper.getMainLooper())

        liveEventInfo = intent.getParcelableExtra(LIVE_EVENT_INFO) ?: return
        watchLiveVideoViewModel.updateLiveEventInfo(liveEventInfo)

        updateUserInfo()
        listenToViewModel()
        listenToViewEvents()
    }

    private fun listenToViewModel() {
        watchLiveVideoViewModel.watchLiveVideoListStates.subscribeAndObserveOnMainThread {
            when (it) {
                is WatchLiveVideoViewModel.WatchLiveVideoListState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                    onBackPressed()
                }
                is WatchLiveVideoViewModel.WatchLiveVideoListState.SuccessMessage -> {

                }
                is WatchLiveVideoViewModel.WatchLiveVideoListState.UpdateComment -> {
                    liveEventCommentAdapter.listOfComments = it.listOfLiveEventSendOrReadComment
                    binding.commentRecyclerView.scrollToPosition(it.listOfLiveEventSendOrReadComment.size - 1)
                }
                is WatchLiveVideoViewModel.WatchLiveVideoListState.InviteCoHostNotification -> {
                    if (it.liveEventInfo.isPublisherRole()) {
                        if (it.liveEventInfo.userId != loggedInUserCache.getLoggedInUser()?.loggedInUser?.id) {
                            if (liveEventInfo.channelId == it.liveEventInfo.channelId) {
                                openUpdateCoHostStatusDialog(true)
                            }
                        }
                    }
                }
                is WatchLiveVideoViewModel.WatchLiveVideoListState.JoinEventTokenInfo -> {
                    this.isCoHost = it.isCoHost
                    if (it.isCoHost) {
                        binding.cameraFlipAppCompatImageView.visibility = View.VISIBLE
                    } else {
                        binding.cameraFlipAppCompatImageView.visibility = View.INVISIBLE
                    }
                    joinChannel(it.liveEventInfo.channelId, it.liveEventInfo.token, isCoHost)
                }
                is WatchLiveVideoViewModel.WatchLiveVideoListState.LiveWatchingCount -> {
                    binding.tagTextView.text = it.liveWatchingCount.prettyCount().toString().plus(" ").plus(getString(R.string.label_watching))
                }
                is WatchLiveVideoViewModel.WatchLiveVideoListState.LoadingState -> {

                }
                is WatchLiveVideoViewModel.WatchLiveVideoListState.LiveEventEnd -> {
                    onBackPressed()
                }
                is WatchLiveVideoViewModel.WatchLiveVideoListState.KickUserComment -> {
                    if (it.liveEventKickUser.userId == loggedInUserCache.getLoggedInUser()?.loggedInUser?.id) {
                        isKickUser = 1
                        showLongToast(getString(R.string.msg_kick_user))
                        onBackPressed()
                    }
                }
                is WatchLiveVideoViewModel.WatchLiveVideoListState.LiveHeart -> {
                    binding.likeImageView.isSelected = !binding.likeImageView.isSelected
                    binding.heartsView.emitHeart(getThumbModel)
                }
            }
        }.autoDispose()
    }

    private fun updateUserInfo() {
        Glide.with(this)
            .load(liveEventInfo.profileUrl ?: "")
            .placeholder(R.drawable.ic_chat_user_placeholder)
            .into(binding.userProfileImageView)

        binding.userNameTextView.text = liveEventInfo.userName ?: ""
        val totalFollowers = (liveEventInfo.followers ?: 0).toString()
        binding.userFollowersTextView.text = totalFollowers.plus(" ").plus(getString(R.string.label_followers))
        binding.ivVerified.isVisible = liveEventInfo?.profileVerified == 1
    }

    private fun listenToViewEvents() {
        val linearLayoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        linearLayoutManager.stackFromEnd = true
        liveEventCommentAdapter = LiveEventCommentAdapter(this).apply {
            liveStreamCommentViewClicks.subscribeAndObserveOnMainThread { commentInfo ->
                val userId = commentInfo.userId ?: -1
                if (userId != loggedInUserCache.getLoggedInUser()?.loggedInUser?.id) {
                    val bottomSheet = LiveUserInfoBottomSheet(false, userId, isCoHost)
                    bottomSheet.show(supportFragmentManager, LiveUserInfoBottomSheet::class.java.name)
                }
            }.autoDispose()
        }

        binding.commentRecyclerView.apply {
            adapter = liveEventCommentAdapter
            layoutManager = linearLayoutManager
        }

        binding.commentEditTextView.editorActions()
            .filter { action -> action == EditorInfo.IME_ACTION_SEND }
            .subscribeAndObserveOnMainThread {
                UiUtils.hideKeyboard(this)
                if (binding.commentEditTextView.text.toString().isNotEmpty()) {
                    watchLiveVideoViewModel.sendComment(binding.commentEditTextView.text.toString())
                }
                binding.commentEditTextView.setText("")
            }.autoDispose()

        binding.sendImageView.throttleClicks().subscribeAndObserveOnMainThread {
            UiUtils.hideKeyboard(this)
            if (binding.commentEditTextView.text.toString().isNotEmpty()) {
                watchLiveVideoViewModel.sendComment(binding.commentEditTextView.text.toString())
            }
            binding.commentEditTextView.setText("")
        }.autoDispose()

        binding.exitAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()

        binding.endLiveEventImageView.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()

        binding.likeImageView.throttleClicks().subscribeAndObserveOnMainThread {
            watchLiveVideoViewModel.sendHeart()
        }

        binding.cameraFlipAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            if (isCoHost) {
                engine?.switchCamera()
            }
        }.autoDispose()

        engine = RtcEngine.create(this, getString(R.string.agora_app_id), iRtcEngineEventHandler)
        if (liveEventInfo.isCoHost == 1) {
            when (liveEventInfo.hostStatus) {
                1 -> {
                    watchLiveVideoViewModel.joinLiveEvent(isCoHost = true, isFromNotification = false, "", liveEventInfo)
                }
                2 -> {
                    watchLiveVideoViewModel.joinLiveEvent(
                        isCoHost = false,
                        isFromNotification = false,
                        "",
                        liveEventInfo
                    )
                }
                else -> {
                    //Show popup and based on user action request for token and join
                    openUpdateCoHostStatusDialog(false)
                }
            }
        } else {
            watchLiveVideoViewModel.joinLiveEvent(isCoHost = false, isFromNotification = false, "", liveEventInfo)
        }
        binding.liveEventTitleTextView.text = liveEventInfo.eventName ?: ""

        binding.userNameTextView.throttleClicks().subscribeAndObserveOnMainThread {
            if (!isCoHost) {
                startActivityWithDefaultAnimation(NewOtherUserProfileActivity.getIntent(this@WatchLiveEventActivity, liveEventInfo.userId))
            }
        }.autoDispose()
    }

    private fun joinChannel(
        channelId: String,
        accessToken: String,
        isCoHost: Boolean
    ) {
        if (isCoHost) {
            // Create render view by RtcEngine
            val surfaceView = RtcEngine.CreateRendererView(this)
            if (binding.coHostSecondVideo.childCount > 0) {
                binding.coHostSecondVideo.removeAllViews()
            }
            // Add to the local container
            binding.coHostSecondVideo.addView(
                surfaceView,
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            )
            // Setup local video to render your local camera preview
            engine?.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
            binding.coHostSecondVideo.visibility = View.VISIBLE
            secondCoHost = true
        } else {
            secondCoHost = false
            binding.coHostSecondVideo.visibility = View.GONE
        }
        // Set audio route to microPhone
        engine?.setDefaultAudioRoutetoSpeakerphone(true)
        /** Sets the channel profile of the Agora RtcEngine.
         * CHANNEL_PROFILE_COMMUNICATION(0): (Default) The Communication profile.
         * Use this profile in one-on-one calls or group calls, where all users can talk freely.
         * CHANNEL_PROFILE_LIVE_BROADCASTING(1): The Live-Broadcast profile. Users in a live-broadcast
         * channel have a role as either broadcaster or audience. A broadcaster can both send and receive streams;
         * an audience can only receive streams. */
        engine?.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
        if (isCoHost) {
            engine?.setClientRole(IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_BROADCASTER)
            // Enable video module
            engine?.enableVideo()
            engine?.enableAudio()
            // Setup video encoding configs
            engine?.setVideoEncoderConfiguration(
                VideoEncoderConfiguration(
                    VideoEncoderConfiguration.VD_640x360,
                    VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                    VideoEncoderConfiguration.STANDARD_BITRATE,
                    VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
                )
            )
        } else {
            engine?.setClientRole(IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_AUDIENCE)
            // Enable video module
            engine?.enableVideo()
        }
        val option = ChannelMediaOptions()
        option.autoSubscribeAudio = true
        option.autoSubscribeVideo = true
        val res =
            engine?.joinChannel(
                accessToken,
                channelId,
                "Extra Optional Data",
                loggedInUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0,
                option
            ) ?: return
        if (res != 0) {
            // Usually happens with invalid parameters
            // Error code description can be found at:
            // en: https://docs.agora.io/en/Voice/API%20Reference/java/classio_1_1agora_1_1rtc_1_1_i_rtc_engine_event_handler_1_1_error_code.html
            showLongToast(RtcEngine.getErrorDescription(abs(res)))
            return
        }
    }

    private fun updateAsCoHost() {
        // Create render view by RtcEngine
        isCoHost = true
        binding.cameraFlipAppCompatImageView.visibility = View.VISIBLE
        val surfaceView = RtcEngine.CreateRendererView(this)
        when {
            !secondCoHost -> {
                if (binding.coHostSecondVideo.childCount > 0) {
                    binding.coHostSecondVideo.removeAllViews()
                }
                // Add to the local container
                binding.coHostSecondVideo.addView(
                    surfaceView,
                    FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                )
                binding.coHostSecondVideo.visibility = View.VISIBLE
                secondCoHost = true
            }
            !thirdCoHost -> {
                if (binding.coHostThirdVideo.childCount > 0) {
                    binding.coHostThirdVideo.removeAllViews()
                }
                // Add to the local container
                binding.coHostThirdVideo.addView(
                    surfaceView,
                    FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                )
                binding.coHostThirdVideo.visibility = View.VISIBLE
                thirdCoHost = true
            }
            !fourthCoHost -> {
                if (binding.coHostFourVideo.childCount > 0) {
                    binding.coHostFourVideo.removeAllViews()
                }
                // Add to the local container
                binding.coHostFourVideo.addView(
                    surfaceView,
                    FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                )
                binding.coHostFourVideo.visibility = View.VISIBLE
                fourthCoHost = true
            }
        }
        // Setup local video to render your local camera preview
        engine?.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))

        engine?.setClientRole(IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_BROADCASTER)
        // Enable video module
        engine?.enableVideo()
        engine?.enableAudio()
        // Setup video encoding configs
        engine?.setVideoEncoderConfiguration(
            VideoEncoderConfiguration(
                VideoEncoderConfiguration.VD_640x360,
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
            )
        )
    }

    /**
     * IRtcEngineEventHandler is an abstract class providing default implementation.
     * The SDK uses this class to report to the app on SDK runtime events.
     */
    private val iRtcEngineEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onWarning(warn: Int) {
        }

        override fun onError(err: Int) {
        }

        override fun onLeaveChannel(stats: RtcStats) {
            super.onLeaveChannel(stats)
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            myUid = uid
            joined = true
        }

        override fun onRemoteAudioStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
            super.onRemoteAudioStateChanged(uid, state, reason, elapsed)
            Timber.i("onRemoteAudioStateChanged->$uid, state->$state, reason->$reason")
        }

        override fun onRemoteVideoStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
            super.onRemoteVideoStateChanged(uid, state, reason, elapsed)
            Timber.i("onRemoteVideoStateChanged->$uid, state->$state, reason->$reason")
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            super.onUserJoined(uid, elapsed)
            Timber.i("onUserJoined->$uid")
            if (!firstCoHost) {
                liveStreamNoOfCoHostHashMap[uid] = LiveStreamNoOfCoHost.FirstCoHost.type
                firstCoHost = true
                handler?.post {
                    if (binding.coHostFirstVideo.childCount > 0) {
                        binding.coHostFirstVideo.removeAllViews()
                    }
                    // Create render view by RtcEngine
                    val surfaceView = RtcEngine.CreateRendererView(this@WatchLiveEventActivity)
                    surfaceView.setZOrderMediaOverlay(true)
                    // Add to the remote container
                    binding.coHostFirstVideo.addView(
                        surfaceView,
                        FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    )
                    // Setup remote video to render
                    engine?.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
                    binding.coHostFirstVideo.visibility = View.VISIBLE
                }
                return
            }
            if (!secondCoHost) {
                liveStreamNoOfCoHostHashMap[uid] = LiveStreamNoOfCoHost.SecondCoHost.type
                secondCoHost = true
                handler?.post {
                    if (binding.coHostSecondVideo.childCount > 0) {
                        binding.coHostSecondVideo.removeAllViews()
                    }
                    // Create render view by RtcEngine
                    val surfaceView = RtcEngine.CreateRendererView(this@WatchLiveEventActivity)
                    surfaceView.setZOrderMediaOverlay(true)
                    // Add to the remote container
                    binding.coHostSecondVideo.addView(
                        surfaceView,
                        FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    )
                    // Setup remote video to render
                    engine?.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
                    binding.coHostSecondVideo.visibility = View.VISIBLE
                }
                return
            }
            if (!thirdCoHost) {
                liveStreamNoOfCoHostHashMap[uid] = LiveStreamNoOfCoHost.ThirdCoHost.type
                thirdCoHost = true
                handler?.post {
                    if (binding.coHostThirdVideo.childCount > 0) {
                        binding.coHostThirdVideo.removeAllViews()
                    }
                    // Create render view by RtcEngine
                    val surfaceView = RtcEngine.CreateRendererView(this@WatchLiveEventActivity)
                    surfaceView.setZOrderMediaOverlay(true)
                    // Add to the remote container
                    binding.coHostThirdVideo.addView(
                        surfaceView,
                        FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    )
                    // Setup remote video to render
                    engine?.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
                    binding.coHostThirdVideo.visibility = View.VISIBLE
                }
                return
            }
            if (!fourthCoHost) {
                liveStreamNoOfCoHostHashMap[uid] = LiveStreamNoOfCoHost.FourthCoHost.type
                fourthCoHost = true
                handler?.post {
                    if (binding.coHostFourVideo.childCount > 0) {
                        binding.coHostFourVideo.removeAllViews()
                    }
                    // Create render view by RtcEngine
                    val surfaceView = RtcEngine.CreateRendererView(this@WatchLiveEventActivity)
                    surfaceView.setZOrderMediaOverlay(true)
                    // Add to the remote container
                    binding.coHostFourVideo.addView(
                        surfaceView,
                        FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    )
                    // Setup remote video to render
                    engine?.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
                    binding.coHostFourVideo.visibility = View.VISIBLE
                }
                return
            }
        }

        /**Occurs when a remote user (Communication)/host (Live Broadcast) leaves the channel.
         * @param uid ID of the user whose audio state changes.
         * @param reason Reason why the user goes offline:
         * USER_OFFLINE_QUIT(0): The user left the current channel.
         * USER_OFFLINE_DROPPED(1): The SDK timed out and the user dropped offline because no data
         * packet was received within a certain period of time. If a user quits the
         * call and the message is not passed to the SDK (due to an unreliable channel),
         * the SDK assumes the user dropped offline.
         * USER_OFFLINE_BECOME_AUDIENCE(2): (Live broadcast only.) The client role switched from
         * the host to the audience.
         */
        override fun onUserOffline(uid: Int, reason: Int) {
            handler?.post {
                /** Clear render view
                 * Note: The video will stay at its last frame, to completely remove it you will need to
                 * remove the SurfaceView from its parent */
                liveStreamNoOfCoHostHashMap.forEach { (key, value) ->
                    if (key == uid) {
                        if (value == LiveStreamNoOfCoHost.FirstCoHost.type) {
                            firstCoHost = false
                            handler?.post {
                                if (binding.coHostFirstVideo.childCount > 0) {
                                    binding.coHostFirstVideo.removeAllViews()
                                }
                                binding.coHostFirstVideo.visibility = View.GONE
                            }
                        }
                        if (value == LiveStreamNoOfCoHost.SecondCoHost.type) {
                            secondCoHost = false
                            handler?.post {
                                if (binding.coHostSecondVideo.childCount > 0) {
                                    binding.coHostSecondVideo.removeAllViews()
                                }
                                binding.coHostSecondVideo.visibility = View.GONE
                            }
                        }
                        if (value == LiveStreamNoOfCoHost.ThirdCoHost.type) {
                            thirdCoHost = false
                            handler?.post {
                                if (binding.coHostThirdVideo.childCount > 0) {
                                    binding.coHostThirdVideo.removeAllViews()
                                }
                                binding.coHostThirdVideo.visibility = View.GONE
                            }
                        }
                        if (value == LiveStreamNoOfCoHost.FourthCoHost.type) {
                            liveStreamNoOfCoHostHashMap[uid] = LiveStreamNoOfCoHost.FourthCoHost.type
                            fourthCoHost = false
                            handler?.post {
                                if (binding.coHostFourVideo.childCount > 0) {
                                    binding.coHostFourVideo.removeAllViews()
                                }
                                binding.coHostFourVideo.visibility = View.GONE
                            }
                        }
                    }
                }
                engine?.setupRemoteVideo(VideoCanvas(null, VideoCanvas.RENDER_MODE_HIDDEN, uid))
            }
        }

        /**
         * Occurs when the user role switches in a live streaming. For example, from a host to an audience or vice versa.
         *
         * The SDK triggers this callback when the local user switches the user role by calling the setClientRole method after joining the channel.
         * @param oldRole Role that the user switches from.
         * @param newRole Role that the user switches to.
         */
        override fun onClientRoleChanged(oldRole: Int, newRole: Int) {

        }
    }

    private fun openUpdateCoHostStatusDialog(isFromNotification: Boolean) {
        val dialog = UpdateInviteCoHostStatusDialog(liveEventInfo)
        dialog.apply {
            isCancelable = false
            inviteCoHostStatus.subscribeAndObserveOnMainThread {
                dismissDialog()
                if (isFromNotification) {
                    if (it) {
                        updateAsCoHost()
                        watchLiveVideoViewModel.joinLiveEvent(
                            isCoHost = true,
                            isFromNotification = true,
                            "",
                            liveEventInfo
                        )
                    } else {
                        watchLiveVideoViewModel.joinLiveEvent(
                            isCoHost = false,
                            isFromNotification = true,
                            "",
                            liveEventInfo
                        )
                    }
                } else {
                    if (it) {
                        updateAsCoHost()
                        watchLiveVideoViewModel.joinLiveEvent(
                            isCoHost = true,
                            isFromNotification = false,
                            "",
                            liveEventInfo
                        )
                    } else {
                        watchLiveVideoViewModel.joinLiveEvent(
                            isCoHost = false,
                            isFromNotification = false,
                            "",
                            liveEventInfo
                        )
                    }
                }
            }
        }
        dialog.show(supportFragmentManager, UpdateInviteCoHostStatusDialog::class.java.name)
    }

    override fun onDestroy() {
        super.onDestroy()
        watchLiveVideoViewModel.liveRoomDisconnect()
        engine?.leaveChannel()
        handler?.post { RtcEngine.destroy() }
        engine = null
    }
}