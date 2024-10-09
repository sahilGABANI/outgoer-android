package com.outgoer.ui.home.newmap.venueevents.view

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.event.model.EventData
import com.outgoer.api.event.model.MutualFriends
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewUpcomingEventsBinding
import com.outgoer.mediapicker.utils.DateUtils.getVideoDurationInHourMinSecFormat
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class UpcomingEventsView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val upcomingEventsViewClickSubject: PublishSubject<EventData> = PublishSubject.create()
    val upcomingEventsViewClick: Observable<EventData> = upcomingEventsViewClickSubject.hide()

    private val profileViewClickSubject: PublishSubject<MutualFriends> = PublishSubject.create()
    val profileViewClick: Observable<MutualFriends> = profileViewClickSubject.hide()

    private val profileListViewClickSubject: PublishSubject<ArrayList<MutualFriends>> = PublishSubject.create()
    val profileListViewClick: Observable<ArrayList<MutualFriends>> = profileListViewClickSubject.hide()

    private var binding: ViewUpcomingEventsBinding? = null
    private var eventData: EventData? = null

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_upcoming_events, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = ViewUpcomingEventsBinding.bind(view)
        binding?.apply {
            eventRelativeLayout.throttleClicks().subscribeAndObserveOnMainThread {
                eventData?.let {
                    upcomingEventsViewClickSubject.onNext(it)
                }
            }
            checkJoinMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
                eventData?.mutal?.let {
                    profileListViewClickSubject.onNext(it)
                }
            }
//            firstRoundedImageView.throttleClicks().subscribeAndObserveOnMainThread {
//                eventData?.mutal?.let {
//                    profileViewClickSubject.onNext(it[0])
//                }
//            }
//            secondRoundedImageView.throttleClicks().subscribeAndObserveOnMainThread {
//                eventData?.mutal?.let {
//                    profileViewClickSubject.onNext(it[1])
//                }
//            }
//            thirdRoundedImageView.throttleClicks().subscribeAndObserveOnMainThread {
//                eventData?.mutal?.let {
//                    profileViewClickSubject.onNext(it[2])
//                }
//            }
        }
    }

    fun bind(eventData: EventData) {
        this.eventData = eventData
        val icPlaceHolderProfile = ContextCompat.getDrawable(context, R.drawable.ic_chat_user_placeholder)
        binding?.let { binding ->

            binding.publicAppCompatImageView.isVisible = (eventData.isPrivate == 0)

            eventData.let {
                Glide.with(context)
                    .load(eventData.firstMedia?.image)
                    .into(binding.eventRoundedImageView)

                binding.hostNameAppCompatTextView.text = eventData.user?.name

                binding.eventNameAppCompatTextView.text = eventData.name?:""
                binding.eventDateAppCompatTextView.text = getVideoDurationInHourMinSecFormat(eventData.dateTime.toString())
                binding.eventEndDateAppCompatTextView.text = getVideoDurationInHourMinSecFormat(eventData.endDateTime.toString())

                binding.approvedAppCompatImageView.visibility = if (it.joinRequestStatus && it.eventRequest?.status == 1) View.VISIBLE else View.GONE

                when (eventData.mutal?.size ?: 0) {
                    0 -> {
                        binding.checkJoinMaterialButton.visibility = View.GONE
                    }
                    1 -> {
                        binding.checkJoinMaterialButton.visibility = View.VISIBLE
                        binding.firstRoundedImageView.visibility = View.VISIBLE
                        binding.secondRoundedImageView.visibility = View.GONE
                        binding.thirdRoundedImageView.visibility = View.GONE

                        binding.moreFrameLayout.visibility = View.GONE

                        Glide.with(context)
                            .load(eventData.mutal?.get(0)?.avatar)
                            .placeholder(icPlaceHolderProfile)
                            .centerCrop()
                            .into(binding.firstRoundedImageView)
                    }
                    2 -> {
                        binding.checkJoinMaterialButton.visibility = View.VISIBLE
                        binding.firstRoundedImageView.visibility = View.VISIBLE
                        binding.secondRoundedImageView.visibility = View.VISIBLE
                        binding.thirdRoundedImageView.visibility = View.GONE

                        binding.moreFrameLayout.visibility = View.GONE
                        Glide.with(context)
                            .load(eventData.mutal?.get(0)?.avatar)
                            .placeholder(icPlaceHolderProfile)
                            .centerCrop()
                            .into(binding.firstRoundedImageView)

                        Glide.with(context)
                            .load(eventData.mutal?.get(1)?.avatar)
                            .placeholder(icPlaceHolderProfile)
                            .centerCrop()
                            .into(binding.secondRoundedImageView)
                    }
                    3 -> {
                        binding.checkJoinMaterialButton.visibility = View.VISIBLE
                        binding.firstRoundedImageView.visibility = View.VISIBLE
                        binding.secondRoundedImageView.visibility = View.VISIBLE
                        binding.thirdRoundedImageView.visibility = View.VISIBLE
                        binding.moreFrameLayout.visibility = View.GONE

                        Glide.with(context)
                            .load(eventData.mutal?.get(0)?.avatar)
                            .placeholder(icPlaceHolderProfile)
                            .centerCrop()
                            .into(binding.firstRoundedImageView)

                        Glide.with(context)
                            .load(eventData.mutal?.get(1)?.avatar)
                            .placeholder(icPlaceHolderProfile)
                            .centerCrop()
                            .into(binding.secondRoundedImageView)

                        Glide.with(context)
                            .load(eventData.mutal?.get(2)?.avatar)
                            .placeholder(icPlaceHolderProfile)
                            .centerCrop()
                            .into(binding.thirdRoundedImageView)
                    }
                    else -> {
                        binding.checkJoinMaterialButton.visibility = View.VISIBLE
                        binding.firstRoundedImageView.visibility = View.VISIBLE
                        binding.secondRoundedImageView.visibility = View.VISIBLE
                        binding.moreFrameLayout.visibility = View.VISIBLE
                        binding.thirdRoundedImageView.visibility = View.VISIBLE
                        binding.maxRoundedImageView.visibility = View.VISIBLE
                        binding.maxRoundedImageView.text = eventData.otherMutualFriend.toString().plus("+")

                        Glide.with(context)
                            .load(eventData.mutal?.get(0)?.avatar)
                            .placeholder(icPlaceHolderProfile)
                            .centerCrop()
                            .into(binding.firstRoundedImageView)

                        Glide.with(context)
                            .load(eventData.mutal?.get(1)?.avatar)
                            .placeholder(icPlaceHolderProfile)
                            .centerCrop()
                            .into(binding.secondRoundedImageView)

                        Glide.with(context)
                            .load(eventData.mutal?.get(2)?.avatar)
                            .placeholder(icPlaceHolderProfile)
                            .centerCrop()
                            .into(binding.thirdRoundedImageView)

                        binding.maxRoundedImageView.text = eventData.otherMutualFriend.toString().plus("+")
                    }
                }
            }
        }
    }
}