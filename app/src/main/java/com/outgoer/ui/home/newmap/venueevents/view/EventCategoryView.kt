package com.outgoer.ui.home.newmap.venueevents.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.event.model.EventData
import com.outgoer.api.venue.model.VenueCategory
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.NewCategoryViewBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.math.RoundingMode

class EventCategoryView(context: Context) : ConstraintLayoutWithLifecycle(context) {


    private var binding: NewCategoryViewBinding? = null
    private lateinit var venueMapInfo: VenueCategory

    private val venueClickSubject: PublishSubject<VenueCategory> = PublishSubject.create()
    val venueClick: Observable<VenueCategory> = venueClickSubject.hide()

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.new_category_view, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = NewCategoryViewBinding.bind(view)

        binding?.apply {
            cvHashtagContainer.throttleClicks().subscribeAndObserveOnMainThread {
                venueClickSubject.onNext(venueMapInfo)
            }.autoDispose()
        }
    }


    fun bind(venueMapInfo: VenueCategory) {
        this.venueMapInfo = venueMapInfo
        binding?.apply {
            tvHashtag.text = venueMapInfo.categoryName

            Glide.with(context)
                .load(venueMapInfo.thumbnailImage)
                .error(R.drawable.ic_chat_user_placeholder)
                .into(logoAppCompatImageView)
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}