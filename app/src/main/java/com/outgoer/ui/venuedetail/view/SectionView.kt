package com.outgoer.ui.venuedetail.view

import android.content.Context
import android.view.View
import com.outgoer.R
import com.outgoer.api.venue.model.SectionViewInfo
import com.outgoer.api.venue.model.SectionViewSectionItem
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewVenueDetailSectionBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SectionView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val sectionViewClickSubject: PublishSubject<SectionViewSectionItem> = PublishSubject.create()
    val sectionViewClick: Observable<SectionViewSectionItem> = sectionViewClickSubject.hide()

    private lateinit var binding: ViewVenueDetailSectionBinding
    private lateinit var sectionViewSectionItem: SectionViewSectionItem

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_venue_detail_section, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewVenueDetailSectionBinding.bind(view)

        binding.tvSeeAll.throttleClicks().subscribeAndObserveOnMainThread {
            sectionViewClickSubject.onNext(sectionViewSectionItem)
        }.autoDispose()
    }

    fun bind(sectionViewSectionItem: SectionViewSectionItem) {
        this.sectionViewSectionItem = sectionViewSectionItem
        binding.let {
            val sectionViewInfo: SectionViewInfo = when (sectionViewSectionItem) {
                is SectionViewSectionItem.LatestEventSection -> {
                    sectionViewSectionItem.sectionViewInfo
                }
                is SectionViewSectionItem.OtherNearPlacesSection -> {
                    sectionViewSectionItem.sectionViewInfo
                }
            }
            it.tvSectionTitle.text = sectionViewInfo.sectionText
            if (sectionViewInfo.isSeeAllEnable) {
                it.tvSeeAll.visibility = View.VISIBLE
            } else {
                it.tvSeeAll.visibility = View.GONE
            }
        }
    }
}