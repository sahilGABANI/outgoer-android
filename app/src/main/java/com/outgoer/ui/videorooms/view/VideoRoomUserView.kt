package com.outgoer.ui.videorooms.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.live.model.LiveEventInfo
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewVideoRoomUserBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class VideoRoomUserView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val videoRoomUserClickSubject: PublishSubject<LiveEventInfo> = PublishSubject.create()
    val videoRoomUserClick: Observable<LiveEventInfo> = videoRoomUserClickSubject.hide()

    private lateinit var binding: ViewVideoRoomUserBinding
    private lateinit var liveEventInfo: LiveEventInfo

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_video_room_user, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = ViewVideoRoomUserBinding.bind(view)

        binding.apply {
            ivUserProfile.throttleClicks().subscribeAndObserveOnMainThread {
                videoRoomUserClickSubject.onNext(liveEventInfo)
            }.autoDispose()

            cvVideo.throttleClicks().subscribeAndObserveOnMainThread {
                videoRoomUserClickSubject.onNext(liveEventInfo)
            }.autoDispose()
        }
    }

    fun bind(liveEventInfo: LiveEventInfo) {
        this.liveEventInfo = liveEventInfo
        binding.apply {
            Glide.with(context)
                .load(liveEventInfo.profileUrl ?: "")
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .centerCrop()
                .into(ivUserProfile)

            tvUsername.text = liveEventInfo.userName ?: ""
        }
    }
}