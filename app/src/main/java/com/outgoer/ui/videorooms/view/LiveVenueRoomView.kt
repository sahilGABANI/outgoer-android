package com.outgoer.ui.videorooms.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.live.model.LiveEventInfo
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewLiveVenueRoomBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class LiveVenueRoomView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val liveVenueRoomClickSubject: PublishSubject<LiveEventInfo> = PublishSubject.create()
    val liveVenueRoomClick: Observable<LiveEventInfo> = liveVenueRoomClickSubject.hide()

    private lateinit var binding: ViewLiveVenueRoomBinding
    private lateinit var liveEventInfo: LiveEventInfo

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_live_venue_room, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewLiveVenueRoomBinding.bind(view)

        binding.apply {
            ivVenueImage.throttleClicks().subscribeAndObserveOnMainThread {
                liveVenueRoomClickSubject.onNext(liveEventInfo)
            }.autoDispose()

            cvVideo.throttleClicks().subscribeAndObserveOnMainThread {
                liveVenueRoomClickSubject.onNext(liveEventInfo)
            }.autoDispose()
        }
    }

    fun bind(liveEventInfo: LiveEventInfo) {
        this.liveEventInfo = liveEventInfo
        binding.apply {
            Glide.with(context)
                .load(liveEventInfo.venueThumbnailUrl ?: "")
                .placeholder(R.drawable.ic_place_holder_post)
                .centerCrop()
                .into(ivVenueImage)

            tvUsername.text = liveEventInfo.userName ?: ""
        }
    }
}