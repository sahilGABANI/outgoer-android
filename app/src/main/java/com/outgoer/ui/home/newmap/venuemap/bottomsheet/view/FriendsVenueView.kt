package com.outgoer.ui.home.newmap.venuemap.bottomsheet.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.friend_venue.model.UserVenueResponse
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.FriendVenueViewBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class FriendsVenueView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val profileClickSubject: PublishSubject<UserVenueResponse> = PublishSubject.create()
    val profileClick: Observable<UserVenueResponse> = profileClickSubject.hide()

    private val messageClickSubject: PublishSubject<UserVenueResponse> = PublishSubject.create()
    val messageClick: Observable<UserVenueResponse> = messageClickSubject.hide()

    private var binding: FriendVenueViewBinding? = null
    private lateinit var userVenue: UserVenueResponse
    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.friend_venue_view, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = FriendVenueViewBinding.bind(view)

        binding?.apply {
            ivUserProfileImage.throttleClicks().subscribeAndObserveOnMainThread {
                profileClickSubject.onNext(userVenue)
            }.autoDispose()

            messageAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
                messageClickSubject.onNext(userVenue)
            }.autoDispose()
        }
    }

    fun bind(userVenueResponse: UserVenueResponse) {
        userVenue = userVenueResponse
        binding?.apply {

            Glide.with(context)
                .load(userVenueResponse.avatar)
                .placeholder(R.drawable.venue_placeholder)
                .into(ivUserProfileImage)

            tvUserName.text = userVenueResponse.name
        }
    }
}