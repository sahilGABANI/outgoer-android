package com.outgoer.ui.livestreamvenue

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jakewharton.rxbinding3.widget.editorActions
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.authentication.model.OutgoerUser
import com.outgoer.api.live.model.LiveStreamNoOfCoHost
import com.outgoer.api.live.model.Time
import com.outgoer.api.live.model.secondToTime
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityLiveStreamVenueBinding
import com.outgoer.ui.livestreamuser.liveuserinfo.LiveUserInfoBottomSheet
import com.outgoer.ui.livestreamuser.setting.LiveStreamCreateEventSettingBottomSheet
import com.outgoer.ui.livestreamuser.view.LiveEventCommentAdapter
import com.outgoer.ui.livestreamvenue.viewmodel.LiveStreamVenueViewModel
import com.outgoer.ui.livestreamvenue.viewmodel.LiveStreamVenueViewState
import com.outgoer.ui.watchliveevent.LiveWatchBottomSheetFragment
import com.outgoer.utils.UiUtils
import com.petersamokhin.android.floatinghearts.HeartsView
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.models.ChannelMediaOptions
import io.agora.rtc.video.VideoCanvas
import io.agora.rtc.video.VideoEncoderConfiguration
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.abs
import kotlin.properties.Delegates

class LiveStreamVenueActivity : BaseActivity() {

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, LiveStreamVenueActivity::class.java)
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<LiveStreamVenueViewModel>
    private lateinit var liveStreamVenueViewModel: LiveStreamVenueViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var loggedInUser: OutgoerUser? = null
    private var loggedInUserId by Delegates.notNull<Int>()

    private var countTimerDisposable: Disposable? = null

    private lateinit var binding: ActivityLiveStreamVenueBinding
    private lateinit var liveEventCommentAdapter: LiveEventCommentAdapter

    private var engine: RtcEngine? = null
    private var myUid = 0
    private var joined = false
    private var handler: Handler? = null

    private var firstCoHost: Boolean = false
    private var secondCoHost: Boolean = false
    private var thirdCoHost: Boolean = false
    private var fourthCoHost: Boolean = false

    private var firstCoHostUid = 0 // It is always main host UID
    private var secondCoHostUid = 0
    private var thirdCoHostUid = 0
    private var fourthCoHostUid = 0

    private var isEndingStream = false
    private var channelId: String? = null
    private var liveId = -1

    private var liveStreamNoOfCoHostHashMap = HashMap<Int, String>()

    private val getThumbModel: HeartsView.Model by lazy {
        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_red_like)
        val bitmap = (drawable as BitmapDrawable).bitmap
        HeartsView.Model(
            0,
            bitmap
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        OutgoerApplication.component.inject(this)
        liveStreamVenueViewModel = getViewModelFromFactory(viewModelFactory)

        binding = ActivityLiveStreamVenueBinding.inflate(layoutInflater)
        setContentView(binding.root)

        handler = Handler(Looper.getMainLooper())

        loggedInUser = loggedInUserCache.getLoggedInUser()?.loggedInUser
        loggedInUserId = loggedInUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0

        updateUserInfo()

        listenToViewModel()
        listenToViewEvent()
    }

    private fun updateUserInfo() {
        Glide.with(this)
            .load(loggedInUser?.avatar ?: "")
            .placeholder(R.drawable.ic_chat_user_placeholder)
            .into(binding.userProfileImageView)

        binding.userNameTextView.text = loggedInUser?.username ?: ""
        val totalFollowers = (loggedInUser?.totalFollowers ?: 0).toString()
        binding.userFollowersTextView.text = totalFollowers.plus(" ").plus(getString(R.string.label_followers))
    }

    private fun listenToViewModel() {
        liveStreamVenueViewModel.liveStreamVenueState.subscribeAndObserveOnMainThread { state ->
            when (state) {
                is LiveStreamVenueViewState.ErrorMessage -> {
                    showToast(state.errorMessage)
                }
                is LiveStreamVenueViewState.LoadingState -> {

                }
                is LiveStreamVenueViewState.SuccessMessage -> {
                    showToast(state.successMessage)
                }
                is LiveStreamVenueViewState.LeaveLiveRoom -> {
                    isEndingStream = true
                    onBackPressed()
                }
                is LiveStreamVenueViewState.LiveWatchingCount -> {
                    binding.tagTextView.text = state.liveWatchingCount.prettyCount().toString().plus(" ").plus(getString(R.string.label_watching))
                }
                is LiveStreamVenueViewState.UpdateComment -> {
                    liveEventCommentAdapter.listOfComments = state.listOfLiveEventSendOrReadComment
                    binding.commentRecyclerView.scrollToPosition(state.listOfLiveEventSendOrReadComment.size - 1)
                }
                is LiveStreamVenueViewState.LiveHeart -> {
                    binding.likeImageView.isSelected = !binding.likeImageView.isSelected
                    binding.heartsView.emitHeart(getThumbModel)
                }
            }
        }.autoDispose()
    }

    private fun listenToViewEvent() {
        updateTimerText(0.secondToTime())

        binding.tagTextView.throttleClicks().subscribeAndObserveOnMainThread {
            val bottomSheetFragment = LiveWatchBottomSheetFragment(liveId)
            bottomSheetFragment.show(supportFragmentManager, LiveWatchBottomSheetFragment::class.java.name)
        }

        liveEventCommentAdapter = LiveEventCommentAdapter(this).apply {
            liveStreamCommentViewClicks.subscribeAndObserveOnMainThread { commentInfo ->
                val userId = commentInfo.userId ?: -1
                if (userId != loggedInUserCache.getLoggedInUser()?.loggedInUser?.id) {
                    val bottomSheet = LiveUserInfoBottomSheet(false, userId)
                    bottomSheet.show(supportFragmentManager, LiveUserInfoBottomSheet::class.java.name)
                }
            }.autoDispose()
        }

        binding.commentRecyclerView.apply {
            val linearLayoutManager = LinearLayoutManager(this@LiveStreamVenueActivity, RecyclerView.VERTICAL, false)
            linearLayoutManager.stackFromEnd = true
            layoutManager = linearLayoutManager
            adapter = liveEventCommentAdapter
        }

        binding.cameraFlipAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            engine?.switchCamera()
        }.autoDispose()

        binding.commentEditTextView.editorActions()
            .filter { action -> action == EditorInfo.IME_ACTION_SEND }
            .subscribeAndObserveOnMainThread {
                UiUtils.hideKeyboard(this)
                if (binding.commentEditTextView.text.toString().isNotEmpty()) {
                    liveStreamVenueViewModel.sendComment(binding.commentEditTextView.text.toString())
                }
                binding.commentEditTextView.setText("")
            }.autoDispose()

        binding.sendImageView.throttleClicks().subscribeAndObserveOnMainThread {
            UiUtils.hideKeyboard(this)
            if (binding.commentEditTextView.text.toString().isNotEmpty()) {
                liveStreamVenueViewModel.sendComment(binding.commentEditTextView.text.toString())
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
            liveStreamVenueViewModel.sendHeart()
        }

        engine = RtcEngine.create(this, getString(R.string.agora_app_id), iRtcEngineEventHandler)
        openLiveStreamCreateEventSettingBottomSheet()
    }

    private fun joinChannel(channelId: String, accessToken: String) {
        // Create render view by RtcEngine
        val surfaceView = RtcEngine.CreateRendererView(this)
        if (binding.coHostFirstVideo.childCount > 0) {
            binding.coHostFirstVideo.removeAllViews()
        }
        // Add to the local container
        binding.coHostFirstVideo.addView(
            surfaceView,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        firstCoHost = true
        // Setup local video to render your local camera preview
        engine?.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
        // Set audio route to microPhone
        engine?.setDefaultAudioRoutetoSpeakerphone(true)
        engine?.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
        /**In the demo, the default is to enter as the anchor. */
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
        val option = ChannelMediaOptions()
        option.autoSubscribeAudio = true
        option.autoSubscribeVideo = true
        val res = engine?.joinChannel(
            accessToken,
            channelId,
            "Extra Optional Data",
            loggedInUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0, option
        ) ?: return
        if (res != 0) {
            // Usually happens with invalid parameters
            // Error code description can be found at:
            // en: https://docs.agora.io/en/Voice/API%20Reference/java/classio_1_1agora_1_1rtc_1_1_i_rtc_engine_event_handler_1_1_error_code.html
            showLongToast(RtcEngine.getErrorDescription(abs(res)))
            return
        }
        startTimer()
    }

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
            firstCoHostUid = uid
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
                    val surfaceView = RtcEngine.CreateRendererView(this@LiveStreamVenueActivity)
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
                secondCoHostUid = uid
                handler?.post {
                    if (binding.coHostSecondVideo.childCount > 0) {
                        binding.coHostSecondVideo.removeAllViews()
                    }
                    // Create render view by RtcEngine
                    val surfaceView = RtcEngine.CreateRendererView(this@LiveStreamVenueActivity)
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
                thirdCoHostUid = uid
                handler?.post {
                    if (binding.coHostThirdVideo.childCount > 0) {
                        binding.coHostThirdVideo.removeAllViews()
                    }
                    // Create render view by RtcEngine
                    val surfaceView = RtcEngine.CreateRendererView(this@LiveStreamVenueActivity)
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
                fourthCoHostUid = uid
                handler?.post {
                    if (binding.coHostFourVideo.childCount > 0) {
                        binding.coHostFourVideo.removeAllViews()
                    }
                    // Create render view by RtcEngine
                    val surfaceView = RtcEngine.CreateRendererView(this@LiveStreamVenueActivity)
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

        override fun onUserOffline(uid: Int, reason: Int) {
            handler?.post {
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

        override fun onClientRoleChanged(oldRole: Int, newRole: Int) {

        }
    }

    private fun openLiveStreamCreateEventSettingBottomSheet() {
        val bottomSheet = LiveStreamCreateEventSettingBottomSheet().apply {
            liveNowSuccess.subscribeAndObserveOnMainThread {
                channelId = it.channelId
                liveId = it.id
                binding.liveEventTitleTextView.text = it.eventName ?: ""
                liveStreamVenueViewModel.updateChannelId(channelId, liveId)
                joinChannel(it.channelId, it.token)

                dismissBottomSheet()
            }
            closeIconClick.subscribeAndObserveOnMainThread {
                onBackPressed()
            }
        }
        bottomSheet.show(supportFragmentManager, LiveStreamCreateEventSettingBottomSheet::class.java.name)
    }

    private fun startTimer() {
        var countTime = 0
        countTimerDisposable?.dispose()
        countTimerDisposable =
            Observable.interval(1, TimeUnit.SECONDS).subscribeAndObserveOnMainThread {
                countTime++
                updateTimerText(countTime.secondToTime())
            }
    }

    override fun onBackPressed() {
        if (isEndingStream) {
            super.onBackPressed()
        } else {
            streamEndingConfirmation()
        }
    }

    private fun updateTimerText(secondToTime: Time) {
        val minString = secondToTime.min.toString().padStart(2, '0')
        val secondString = secondToTime.second.toString().padStart(2, '0')
        binding.timerTextView.text = minString.plus(":").plus(secondString)
    }

    private fun streamEndingConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.label_live_stream))
            .setMessage(getString(R.string.msg_are_you_sure_do_want_end_this_event))
            .setNegativeButton(getString(R.string.label_no)) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(getString(R.string.label_yes)) { dialog, _ ->
                dialog.dismiss()
                liveStreamVenueViewModel.endLiveEvent()
            }.show()
    }

    //leaveChannel and Destroy the RtcEngine instance
    override fun onDestroy() {
        engine?.leaveChannel()
        handler?.post { RtcEngine.destroy() }
        countTimerDisposable?.dispose()
        engine = null
        super.onDestroy()
    }
}