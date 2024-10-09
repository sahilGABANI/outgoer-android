package com.outgoer.ui.venuedetail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.appbar.AppBarLayout
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.venue.model.OtherNearPlaceClickState
import com.outgoer.api.venue.model.SectionViewSectionItem
import com.outgoer.api.venue.model.VenueDetail
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityVenueDetailBinding
import com.outgoer.ui.latestevents.LatestEventsActivity
import com.outgoer.ui.latestevents.EventInfoBottomSheet
import com.outgoer.ui.othernearvenue.OtherNearVenueActivity
import com.outgoer.ui.venuedetail.view.VenueDetailAdapter
import com.outgoer.ui.venuedetail.view.VenueDetailGalleryAdapter
import com.outgoer.ui.venuedetail.viewmodel.VenueDetailViewModel
import com.outgoer.ui.venuedetail.viewmodel.VenueDetailViewState
import com.outgoer.ui.venuegallerypreview.VenueGalleryPreviewActivity
import javax.inject.Inject

class VenueDetailActivity : BaseActivity() {

    companion object {
        private const val INTENT_EXTRA_CATEGORY_ID = "INTENT_EXTRA_CATEGORY_ID"
        private const val INTENT_EXTRA_VENUE_ID = "INTENT_EXTRA_VENUE_ID"
        fun getIntent(context: Context, categoryId: Int, venueId: Int): Intent {
            val intent = Intent(context, VenueDetailActivity::class.java)
            intent.putExtra(INTENT_EXTRA_CATEGORY_ID, categoryId)
            intent.putExtra(INTENT_EXTRA_VENUE_ID, venueId)
            return intent
        }
    }
    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<VenueDetailViewModel>
    private lateinit var venueDetailViewModel: VenueDetailViewModel

    private lateinit var binding: ActivityVenueDetailBinding

    private lateinit var venueDetailGalleryAdapter: VenueDetailGalleryAdapter
    private lateinit var venueDetailAdapter: VenueDetailAdapter

    private var categoryId = -1
    private var venueId = -1
    private var venueDetail: VenueDetail? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityVenueDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        OutgoerApplication.component.inject(this)
        venueDetailViewModel = getViewModelFromFactory(viewModelFactory)

        loadDataFromIntent()
    }

    private fun loadDataFromIntent() {
        intent?.let {
            categoryId = it.getIntExtra(INTENT_EXTRA_CATEGORY_ID, -1)
            venueId = it.getIntExtra(INTENT_EXTRA_VENUE_ID, -1)

            if (categoryId != -1 && venueId != -1) {
                listenToViewModel()
                listenToViewEvents()
            } else {
                onBackPressed()
            }
        } ?: onBackPressed()
    }

    private fun listenToViewModel() {
        venueDetailViewModel.venueDetailState.subscribeAndObserveOnMainThread {
            when (it) {
                is VenueDetailViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is VenueDetailViewState.LoadVenueDetail -> {
                    venueDetail = it.venueDetail
                    loadVenueDetail(this, it.venueDetail)
                }
                is VenueDetailViewState.OtherNearVenueInfoList -> {
                    venueDetailAdapter.listOfNearPlaces = it.listOfVenueMapInfo
                }

                else -> {}
            }
        }.autoDispose()
    }

    private fun listenToViewEvents() {
        binding.toolbarView.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.ivDirection.throttleClicks().subscribeAndObserveOnMainThread {
            venueDetail?.apply {
                openGoogleMapWithProvidedLatLng(this@VenueDetailActivity, latitude, longitude)
            }
        }.autoDispose()

        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            binding.swipeRefreshLayout.isRefreshing = false
        }.autoDispose()

        binding.appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            binding.swipeRefreshLayout.isEnabled = verticalOffset == 0
        })

        venueDetailGalleryAdapter = VenueDetailGalleryAdapter(this).apply {
            venueDetailGalleryViewClick.subscribeAndObserveOnMainThread {
                startActivityWithFadeInAnimation(VenueGalleryPreviewActivity.getIntent(this@VenueDetailActivity, it))
            }.autoDispose()

            venueDetailGalleryCountViewClick.subscribeAndObserveOnMainThread {

            }.autoDispose()
        }
        binding.rvVenueGallery.apply {
            layoutManager = LinearLayoutManager(this@VenueDetailActivity, RecyclerView.HORIZONTAL, false)
            adapter = venueDetailGalleryAdapter
        }

        venueDetailAdapter = VenueDetailAdapter(this).apply {
            sectionViewClick.subscribeAndObserveOnMainThread {
                when (it) {
                    is SectionViewSectionItem.LatestEventSection -> {
                        startActivityWithDefaultAnimation(LatestEventsActivity.getIntent(this@VenueDetailActivity, categoryId, venueId))
                    }
                    is SectionViewSectionItem.OtherNearPlacesSection -> {
                        startActivityWithDefaultAnimation(OtherNearVenueActivity.getIntent(this@VenueDetailActivity, categoryId, venueId))
                    }
                }
            }.autoDispose()

            latestEventsClick.subscribeAndObserveOnMainThread {
                val bottomSheet = EventInfoBottomSheet(it)
                bottomSheet.refreshEvents.subscribeAndObserveOnMainThread {
                    onResume()
                }.autoDispose()
                bottomSheet.show(supportFragmentManager, EventInfoBottomSheet::class.java.name)
            }.autoDispose()

            otherNearPlaceClick.subscribeAndObserveOnMainThread { state ->
                when (state) {
                    is OtherNearPlaceClickState.OtherNearPlaceClick -> {
                        startActivityWithDefaultAnimation(getIntent(this@VenueDetailActivity, state.venueMapInfo.category?.firstOrNull()?.id ?: 0, state.venueMapInfo.id))
                    }
                    is OtherNearPlaceClickState.AddRemoveVenueFavClick -> {
                        venueDetailViewModel.addRemoveFavouriteVenue(state.venueMapInfo.id)
                    }
                    is OtherNearPlaceClickState.DirectionViewClick -> {
                        openGoogleMapWithProvidedLatLng(this@VenueDetailActivity, state.venueMapInfo.latitude, state.venueMapInfo.longitude)
                    }
                }
            }.autoDispose()
        }
        binding.rvVenueDetailList.apply {
            layoutManager = LinearLayoutManager(this@VenueDetailActivity, RecyclerView.VERTICAL, false)
            adapter = venueDetailAdapter
        }

        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            binding.swipeRefreshLayout.isRefreshing = false
            if (categoryId != -1 && venueId != -1) {
                venueDetailViewModel.getVenueDetail(venueId)
                venueDetailViewModel.getOtherNearVenue(categoryId, venueId)
            }
        }.autoDispose()
    }

    private fun loadVenueDetail(context: Context, venueDetail: VenueDetail) {
        binding.apply {

            Glide.with(context)
                .load(venueDetail.avatar)
                .placeholder(R.drawable.venue_placeholder)
                .error(R.drawable.venue_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(binding.ivVenueImage)

            venueDetailGalleryAdapter.galleryCount = venueDetail.galleryCount
            venueDetailGalleryAdapter.listOfDataItems = venueDetail.gallery

            Glide.with(context)
                .load(venueDetail.category?.firstOrNull()?.thumbnailImage ?: "")
                .placeholder(R.drawable.venue_placeholder)
                .error(R.drawable.venue_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(binding.ivVenueCategoryImage)

            binding.tvAddress.text = venueDetail.venueAddress
            if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.isMiles == 1) {
                tvDistance.text = if (venueDetail.distance != null) {
                    venueDetail.distance.roundDoubleVal().plus(" ").plus(context.resources.getString(R.string.label_miles))
                } else {
                    "0 ".plus(context.resources.getString(R.string.label_miles))
                }
            } else {
                tvDistance.text = if (venueDetail.distance != null) {
                    venueDetail.distance.roundDoubleVal().plus(" ").plus(context.resources.getString(R.string.label_kms))
                } else {
                    "0 ".plus(context.resources.getString(R.string.label_kms))
                }
            }
            venueDetailAdapter.eventsCount = venueDetail.eventsCount ?: 0
            venueDetailAdapter.listOfLatestEvent = venueDetail.events
        }
    }

    override fun onResume() {
        super.onResume()
        if (categoryId != -1 && venueId != -1) {
            venueDetailViewModel.getVenueDetail(venueId)
            venueDetailViewModel.getOtherNearVenue(categoryId, venueId)
        }
    }
}