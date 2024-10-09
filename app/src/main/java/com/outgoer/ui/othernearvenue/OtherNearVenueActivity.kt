package com.outgoer.ui.othernearvenue

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.outgoer.api.venue.model.OtherNearPlaceClickState
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityOtherNearVenueBinding
import com.outgoer.ui.othernearvenue.view.OtherNearVenueAdapter
import com.outgoer.ui.othernearvenue.viewmodel.OtherNearVenueViewModel
import com.outgoer.ui.othernearvenue.viewmodel.OtherNearVenueViewState
import com.outgoer.ui.venuedetail.VenueDetailActivity
import javax.inject.Inject

class OtherNearVenueActivity : BaseActivity() {

    companion object {
        private const val INTENT_EXTRA_CATEGORY_ID = "INTENT_EXTRA_CATEGORY_ID"
        private const val INTENT_EXTRA_VENUE_ID = "INTENT_EXTRA_VENUE_ID"
        fun getIntent(context: Context, categoryId: Int, venueId: Int): Intent {
            val intent = Intent(context, OtherNearVenueActivity::class.java)
            intent.putExtra(INTENT_EXTRA_CATEGORY_ID, categoryId)
            intent.putExtra(INTENT_EXTRA_VENUE_ID, venueId)
            return intent
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<OtherNearVenueViewModel>
    private lateinit var otherNearVenueViewModel: OtherNearVenueViewModel

    private lateinit var binding: ActivityOtherNearVenueBinding

    private lateinit var otherNearVenueAdapter: OtherNearVenueAdapter
    private var categoryId = -1
    private var venueId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityOtherNearVenueBinding.inflate(layoutInflater)
        setContentView(binding.root)

        OutgoerApplication.component.inject(this)
        otherNearVenueViewModel = getViewModelFromFactory(viewModelFactory)

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
        otherNearVenueViewModel.otherNearVenueListState.subscribeAndObserveOnMainThread {
            when (it) {
                is OtherNearVenueViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is OtherNearVenueViewState.OtherNearVenueInfoList -> {
                    otherNearVenueAdapter.listOfDataItems = it.listOfVenueMapInfo
                }

                else -> {}
            }
        }.autoDispose()
    }

    private fun listenToViewEvents() {
        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()

        otherNearVenueAdapter = OtherNearVenueAdapter(this)
        otherNearVenueAdapter.apply {
            otherNearPlaceClick.subscribeAndObserveOnMainThread { state ->
                when (state) {
                    is OtherNearPlaceClickState.OtherNearPlaceClick -> {
                        startActivityWithDefaultAnimation(VenueDetailActivity.getIntent(this@OtherNearVenueActivity, state.venueMapInfo.category?.firstOrNull()?.id ?: 0, state.venueMapInfo.id))
                    }
                    is OtherNearPlaceClickState.AddRemoveVenueFavClick -> {
                        otherNearVenueViewModel.addRemoveFavouriteVenue(state.venueMapInfo.id)
                    }
                    is OtherNearPlaceClickState.DirectionViewClick -> {
                        openGoogleMapWithProvidedLatLng(this@OtherNearVenueActivity, state.venueMapInfo.latitude, state.venueMapInfo.longitude)
                    }
                }
            }.autoDispose()
        }
        binding.rvOtherNearVenueList.apply {
            layoutManager = LinearLayoutManager(this@OtherNearVenueActivity, RecyclerView.VERTICAL, false)
            adapter = otherNearVenueAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                otherNearVenueViewModel.loadMoreOtherNearVenue(categoryId, venueId)
                            }
                        }
                    }
                }
            })
        }

        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            binding.swipeRefreshLayout.isRefreshing = false
            if (categoryId != -1 && venueId != -1) {
                otherNearVenueViewModel.pullToRefresh(categoryId, venueId)
            }
        }.autoDispose()
    }

    override fun onResume() {
        super.onResume()
        if (categoryId != -1 && venueId != -1) {
            otherNearVenueViewModel.pullToRefresh(categoryId, venueId)
        }
    }
}