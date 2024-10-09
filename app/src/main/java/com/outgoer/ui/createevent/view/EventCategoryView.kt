package com.outgoer.ui.createevent.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.venue.model.VenueCategory
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.EventCategoryListItemBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class EventCategoryView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val eventCategoryActionStateSubject: PublishSubject<VenueCategory> = PublishSubject.create()
    val eventCategoryActionState: Observable<VenueCategory> = eventCategoryActionStateSubject.hide()

    private var binding: EventCategoryListItemBinding? = null

    private lateinit var categoryData: VenueCategory

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.event_category_list_item, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = EventCategoryListItemBinding.bind(view)

        binding?.apply {
            cardContainer.throttleClicks().subscribeAndObserveOnMainThread {
                eventCategoryActionStateSubject.onNext(categoryData)
            }.autoDispose()
        }
    }

    fun bind(category: VenueCategory) {
        this.categoryData = category
        binding?.apply {

            cardContainer.background = if (category.isSelected) {
                resources.getDrawable(R.drawable.purple_border_rounded, null)
            } else {
                resources.getDrawable(R.drawable.new_login_edittext_background, null)
            }

            Glide.with(context)
                .load(category.thumbnailImage)
                .placeholder(R.drawable.venue_placeholder)
                .error(R.drawable.venue_placeholder)
                .into(categoryRoundedImageView)

            categoryNameAppCompatTextView.text = category.categoryName
        }
    }


    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}