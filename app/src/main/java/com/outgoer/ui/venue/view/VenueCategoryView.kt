package com.outgoer.ui.venue.view

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.venue.model.VenueCategory
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.VenueCategoryViewBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class VenueCategoryView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val venueCategoryClickSubject: PublishSubject<VenueCategory> = PublishSubject.create()
    val venueCategoryClick: Observable<VenueCategory> = venueCategoryClickSubject.hide()

    private lateinit var binding: VenueCategoryViewBinding

    private val backgroundSelected = ContextCompat.getDrawable(context, R.drawable.venue_category_selected_background)
    private val backgroundDefault = ContextCompat.getDrawable(context, R.drawable.venue_category_unselected_background)

    private lateinit var venueCategory: VenueCategory
    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.venue_category_view, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = VenueCategoryViewBinding.bind(view)

        binding.apply {
            rlVenueCategory.throttleClicks().subscribeAndObserveOnMainThread {
                venueCategoryClickSubject.onNext(venueCategory)
            }.autoDispose()
        }
    }

    fun bind(venue: VenueCategory) {
        venueCategory = venue
        binding.apply {

            Glide.with(context)
                .load(venueCategory.registerThumbnail)
                .placeholder(R.drawable.venue_placeholder)
                .into(venueCategoryAppCompatImageView)

            venueCategoryAppCompatTextView.text = venueCategory.categoryName

            binding.rlVenueCategory.background = if (venueCategory.isSelected) backgroundSelected else backgroundDefault
        }
    }
}