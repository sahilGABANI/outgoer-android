package com.outgoer.ui.home.newmap.venuemap.view

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.venue.model.VenueCategory
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewNewVenueCategoryBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class NewVenueCategoryView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val venueCategoryClickSubject: PublishSubject<VenueCategory> = PublishSubject.create()
    val venueCategoryClick: Observable<VenueCategory> = venueCategoryClickSubject.hide()

    private lateinit var binding: ViewNewVenueCategoryBinding
    private lateinit var venueCategory: VenueCategory

    private val leftTopRightMargin = context.resources.getDimension(com.intuit.sdp.R.dimen._6sdp).toInt()
    private val bottomMargin = context.resources.getDimension(com.intuit.sdp.R.dimen._8sdp).toInt()

    private val colorDefault = ContextCompat.getColor(context, R.color.md_white)
    private val colorSelected = ContextCompat.getColor(context, R.color.color4AC7F1)

    private val backgroundSelected = ContextCompat.getDrawable(context, R.drawable.new_map_category_selected_background)
    private val backgroundDefault = ContextCompat.getDrawable(context, R.drawable.new_map_category_unselected_background)

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_new_venue_category, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        (layoutParams as LayoutParams).setMargins(leftTopRightMargin, leftTopRightMargin, leftTopRightMargin, bottomMargin)
        binding = ViewNewVenueCategoryBinding.bind(view)

        binding.apply {
            rlVenueCategory.throttleClicks().subscribeAndObserveOnMainThread {
                venueCategoryClickSubject.onNext(venueCategory)
            }.autoDispose()
        }
    }

    fun bind(venueCategory: VenueCategory) {
        this.venueCategory = venueCategory
        binding.apply {

            Glide.with(context)
                .load(venueCategory.thumbnailImage)
                .placeholder(R.drawable.venue_placeholder)
                .into(ivVenueCategoryImage)

            tvVenueCategoryName.text = venueCategory.categoryName

            if (venueCategory.isSelected) {
                //tvVenueCategoryName.setTextColor(colorSelected)
                //ivViewSelected.visibility = View.VISIBLE
                binding.rlVenueCategory.background = backgroundSelected
            } else {
                //tvVenueCategoryName.setTextColor(colorDefault)
                //ivViewSelected.visibility = View.INVISIBLE
                binding.rlVenueCategory.background = backgroundDefault
            }
        }
    }
}