package com.outgoer.ui.newvenuedetail.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.github.marlonlom.utilities.timeago.TimeAgo
import com.github.marlonlom.utilities.timeago.TimeAgoMessages
import com.outgoer.R
import com.outgoer.api.venue.model.VenueReviewModel
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewVenueDetailReviewBinding
import com.outgoer.utils.formatTo
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


class VenueDetailReviewView(context: Context) : ConstraintLayoutWithLifecycle(context)   {

    private var binding: ViewVenueDetailReviewBinding? = null
    private lateinit var venueListInfo: VenueReviewModel

    var localeByLanguageTag: Locale = Locale.forLanguageTag("en")
    var sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH) // 2021-03-24 13:12:18
    var messages: TimeAgoMessages = TimeAgoMessages.Builder().withLocale(localeByLanguageTag).build()
    var calendar: Calendar = Calendar.getInstance()
    private lateinit var reviewImageAdapter: ReviewImageAdapter

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_venue_detail_review, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewVenueDetailReviewBinding.bind(view)

        binding?.apply {
            reviewImageAdapter = ReviewImageAdapter(context)
            rvPhotos.apply {
                adapter = reviewImageAdapter
            }
        }
    }

    fun bind(venueReviewModel: VenueReviewModel) {
        this.venueListInfo = venueReviewModel

        binding?.apply {
            Glide.with(context)
                .load(venueReviewModel?.user?.avatar)
                .circleCrop()
                .error(R.drawable.ic_chat_user_placeholder)
                .into(profileRoundedImageView)

            usernameAppCompatTextView.text = venueReviewModel?.user?.username
            ratingAppCompatTextView.text = venueReviewModel?.rating?.toString()
            reviewtextAppCompatTextView.text = venueReviewModel?.reviewText

            reviewImageAdapter.listOfMedia = venueReviewModel?.images

            try {
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = sdf.parse(venueReviewModel?.updatedAt)?.formatTo(TimeZone.getDefault(), sdf) ?: return
                calendar.time = sdf.parse(date) ?: return
                val timeMessage = TimeAgo.using(calendar.timeInMillis, messages)
                timeAppCompatTextView.text = timeMessage
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }
}