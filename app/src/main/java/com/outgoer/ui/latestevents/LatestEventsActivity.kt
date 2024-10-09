package com.outgoer.ui.latestevents

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.outgoer.api.venue.model.VenueEventInfo
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showLongToast
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivityLatestEventsBinding
import com.outgoer.ui.latestevents.view.LatestEventsAdapter
import com.outgoer.ui.latestevents.viewmodel.LatestEventsViewModel
import com.outgoer.ui.latestevents.viewmodel.LatestEventsViewState
import javax.inject.Inject

class LatestEventsActivity : BaseActivity() {

    companion object {
        private const val INTENT_EXTRA_CATEGORY_ID = "INTENT_EXTRA_CATEGORY_ID"
        private const val INTENT_EXTRA_VENUE_ID = "INTENT_EXTRA_VENUE_ID"
        fun getIntent(context: Context, categoryId: Int, venueId: Int): Intent {
            val intent = Intent(context, LatestEventsActivity::class.java)
            intent.putExtra(INTENT_EXTRA_CATEGORY_ID, categoryId)
            intent.putExtra(INTENT_EXTRA_VENUE_ID, venueId)
            return intent
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<LatestEventsViewModel>
    private lateinit var latestEventsViewModel: LatestEventsViewModel

    private lateinit var binding: ActivityLatestEventsBinding
    private lateinit var latestEventsAdapter: LatestEventsAdapter

    private var categoryId = -1
    private var venueId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        OutgoerApplication.component.inject(this)
        latestEventsViewModel = getViewModelFromFactory(viewModelFactory)

        binding = ActivityLatestEventsBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        latestEventsViewModel.latestEventsViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is LatestEventsViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is LatestEventsViewState.LatestEventsList -> {
                    latestEventsAdapter.listOfDataItems = it.venueEventInfoList
                }

                else -> {}
            }
        }.autoDispose()
    }

    private fun listenToViewEvents() {
        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()

        latestEventsAdapter = LatestEventsAdapter(this)
        latestEventsAdapter.apply {
            latestEventsViewClick.subscribeAndObserveOnMainThread {
                openLatestEventsBottomSheet(it)
            }
        }
        binding.rvLatestEventList.apply {
            adapter = latestEventsAdapter;
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as GridLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                latestEventsViewModel.loadMoreLatestEvents(venueId)
                            }
                        }
                    }
                }
            })
        }
        binding.rvLatestEventList.adapter = latestEventsAdapter;

        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            binding.swipeRefreshLayout.isRefreshing = false
            if (categoryId != -1 && venueId != -1) {
                latestEventsViewModel.pullToRefresh(venueId)
            }
        }.autoDispose()
    }

    private fun openLatestEventsBottomSheet(venueEventInfo: VenueEventInfo) {
        val bottomSheet = EventInfoBottomSheet(venueEventInfo)
        bottomSheet.refreshEvents.subscribeAndObserveOnMainThread {
            onResume()
        }.autoDispose()
        bottomSheet.show(supportFragmentManager, EventInfoBottomSheet::class.java.name)
    }

    override fun onResume() {
        super.onResume()
        latestEventsViewModel.pullToRefresh(venueId)
    }
}