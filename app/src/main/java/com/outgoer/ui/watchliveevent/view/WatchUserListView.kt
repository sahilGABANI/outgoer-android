package com.outgoer.ui.watchliveevent.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.live.model.LiveJoinResponse
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewLiveWatchingUserBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class WatchUserListView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val watchUsersViewClicksSubject: PublishSubject<LiveJoinResponse> = PublishSubject.create()
    val watchUsersViewClicks: Observable<LiveJoinResponse> = watchUsersViewClicksSubject.hide()

    private lateinit var binding: ViewLiveWatchingUserBinding
    private lateinit var liveJoinResponse: LiveJoinResponse

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_live_watching_user, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewLiveWatchingUserBinding.bind(view)

        binding.apply {
            ivUserProfile.throttleClicks().subscribeAndObserveOnMainThread {
                watchUsersViewClicksSubject.onNext(liveJoinResponse)
            }.autoDispose()
        }
    }

    fun bind(liveJoinResponse: LiveJoinResponse) {
        this.liveJoinResponse = liveJoinResponse
        binding.apply {

            tvUsername.text = liveJoinResponse.username ?: ""

            Glide.with(context)
                .load(liveJoinResponse.profileUrl ?: "")
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .into(ivUserProfile)

        }
    }
}