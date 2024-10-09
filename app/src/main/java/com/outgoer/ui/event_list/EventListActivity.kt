package com.outgoer.ui.event_list

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.jakewharton.rxbinding3.widget.textChanges
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.authentication.model.OutgoerUser
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityEventListBinding
import com.outgoer.ui.createevent.CreateEventsActivity
import com.outgoer.ui.home.newmap.venueevents.VenueEventDetailActivity
import com.outgoer.ui.home.newmap.venueevents.view.EventVenueAdapter
import com.outgoer.ui.home.newmap.venueevents.view.UpcomingEventsAdapter
import com.outgoer.ui.home.newmap.venueevents.viewmodel.EventViewState
import com.outgoer.ui.home.newmap.venueevents.viewmodel.VenueEventViewModel
import com.outgoer.ui.home.newmap.venuemap.view.NewVenueCategoryAdapter
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import com.outgoer.utils.UiUtils
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class EventListActivity : BaseActivity() {

    private lateinit var binding: ActivityEventListBinding

    companion object {
        @JvmStatic
        fun newInstance(context: Context): Intent {
            return Intent(context, EventListActivity::class.java)
        }
    }

    private lateinit var upcomingEventsAdapter: UpcomingEventsAdapter
    private lateinit var venueEventExploreAdapter: UpcomingEventsAdapter
    private lateinit var eventVenueAdapter: EventVenueAdapter
    private lateinit var eventOVenueAdapter: EventVenueAdapter

    private lateinit var venueCategoryAdapter: NewVenueCategoryAdapter

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<VenueEventViewModel>
    private lateinit var venueEventViewModel: VenueEventViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)

        binding = ActivityEventListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        venueEventViewModel = getViewModelFromFactory(viewModelFactory)

        initUI()
        listenToViewModel()
        listenToViewEvents()
    }

    private fun listenToViewEvents() {
        venueEventViewModel.getEventsList()
        venueEventViewModel.getEventCategoryList()


        venueCategoryAdapter = NewVenueCategoryAdapter(this@EventListActivity)
        venueCategoryAdapter.apply {
            venueCategoryAllClick.subscribeAndObserveOnMainThread {
                venueCategoryAdapter.isAllSelected = true

                val list = venueCategoryAdapter.listOfDataItems
                list?.forEach { it.isSelected = false }
                venueCategoryAdapter.listOfDataItems = list

                venueEventViewModel.getEventsList(
                    if (binding.etSearch.text.toString()
                            .isNullOrEmpty()
                    ) null else binding.etSearch.text.toString(), 0
                )
            }.autoDispose()

            venueCategoryClick.subscribeAndObserveOnMainThread { venueCategory ->
                venueCategoryAdapter.isAllSelected = false


                val list = venueCategoryAdapter.listOfDataItems
                list?.forEach {
                    it.isSelected =
                        if (venueCategory.id == it.id) !venueCategory.isSelected else false
                }
                venueCategoryAdapter.listOfDataItems = list

                venueEventViewModel.getEventsList(
                    if (binding.etSearch.text.toString()
                            .isNullOrEmpty()
                    ) null else binding.etSearch.text.toString(), venueCategory.id
                )
            }.autoDispose()
        }

        binding.rvVenueEventsCategory.apply {
            adapter = venueCategoryAdapter
            layoutManager =
                LinearLayoutManager(this@EventListActivity, LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun listenToViewModel() {
        venueEventViewModel.eventsViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is EventViewState.AcceptEventDetails -> {}
                is EventViewState.LoadingState -> {}
                is EventViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is EventViewState.VenueCategoryList -> {
                    venueCategoryAdapter.listOfDataItems = it.venueCategoryList
                }
                is EventViewState.EventListDetails -> {

                    if (it.listofevent.upcomming.size > 0) {
                        binding.rvUpcomingEvents.visibility = View.VISIBLE
                        upcomingEventsAdapter.listOfDataItems = it.listofevent.upcomming
                    } else {
                        binding.rvUpcomingEvents.visibility = View.GONE
                    }

                    if (it.listofevent.ongoing.size > 0) {
                        binding.ongoingEventsRecyclerView.visibility = View.VISIBLE
                        venueEventExploreAdapter.listOfDataItems = it.listofevent.ongoing
                    } else {
                        binding.ongoingEventsRecyclerView.visibility = View.GONE
                    }

                    binding.llNoData.visibility =
                        if (it.listofevent.upcomming.size == 0 && it.listofevent.ongoing.size == 0) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    if (binding.etSearch.length() > 0) {

                        if (it.listofevent.upcomming.size > 0) {
                            binding.rvUpcomingEvents.visibility = View.GONE
                            binding.rvHUpcomingEvents.visibility =
                                if (it.listofevent.upcomming.size > 0) View.VISIBLE else View.GONE
                            eventVenueAdapter.listOfDataItems = it.listofevent.upcomming
                        } else {
                            binding.rvUpcomingEvents.visibility = View.GONE
                            binding.rvHUpcomingEvents.visibility = View.GONE
                        }

                        if (it.listofevent.ongoing.size > 0) {
                            binding.ongoingEventsRecyclerView.visibility = View.GONE
                            binding.rvHOnGoingEvents.visibility =
                                if (it.listofevent.ongoing.size > 0) View.VISIBLE else View.GONE
                            eventOVenueAdapter.listOfDataItems = it.listofevent.ongoing
                        } else {
                            binding.ongoingEventsRecyclerView.visibility = View.GONE
                            binding.rvHOnGoingEvents.visibility = View.GONE
                        }
                    } else {
                        binding.rvHUpcomingEvents.visibility = View.GONE
                        binding.rvHOnGoingEvents.visibility = View.GONE
                    }

                }
                is EventViewState.AddRemoveEventDetails -> {}
                is EventViewState.ListRequestDetails -> {}
                is EventViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                else -> {}
            }
        }
    }

    private fun initUI() {

        binding.addEventFloatingActionButton.throttleClicks().subscribeAndObserveOnMainThread {
            startActivity(CreateEventsActivity.getIntent(this@EventListActivity, null))
        }

        binding.etSearch.textChanges()
            .skipInitialValue()
            .debounce(500, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeOnIoAndObserveOnMainThread({
                UiUtils.hideKeyboard(this@EventListActivity)

                if (it.length > 2) {
                    venueEventViewModel.getEventsList(it.toString())
                } else {
                    venueEventViewModel.getEventsList()
                }
            }, {
                Timber.e(it)
            }).autoDispose()

        upcomingEventsAdapter = UpcomingEventsAdapter(this@EventListActivity).apply {
            upcomingEventsViewClick.subscribeAndObserveOnMainThread {
                startActivity(VenueEventDetailActivity.getIntent(this@EventListActivity, it))
            }
            profileViewClick.subscribeAndObserveOnMainThread {
                if (MapVenueUserType.VENUE_OWNER.type.equals(it.userType)) {
                    startActivity(
                        NewVenueDetailActivity.getIntent(
                            this@EventListActivity,
                            0,
                            it.id
                        )
                    )
                } else {
                    startActivity(
                        NewOtherUserProfileActivity.getIntent(
                            this@EventListActivity,
                            it.userId
                        )
                    )
                }
            }
        }
        venueEventExploreAdapter = UpcomingEventsAdapter(this@EventListActivity).apply {
            upcomingEventsViewClick.subscribeAndObserveOnMainThread {
                startActivity(VenueEventDetailActivity.getIntent(this@EventListActivity, it))
            }
            profileViewClick.subscribeAndObserveOnMainThread {
                if (MapVenueUserType.VENUE_OWNER.type.equals(it.userType)) {
                    startActivity(
                        NewVenueDetailActivity.getIntent(
                            this@EventListActivity,
                            0,
                            it.id
                        )
                    )
                } else {
                    startActivity(
                        NewOtherUserProfileActivity.getIntent(
                            this@EventListActivity,
                            it.userId
                        )
                    )
                }
            }
        }

        binding.rvUpcomingEvents.apply {
            adapter = upcomingEventsAdapter
        }

        binding.ongoingEventsRecyclerView.apply {
            adapter = venueEventExploreAdapter
        }


        eventVenueAdapter = EventVenueAdapter(this@EventListActivity).apply {
            venueClick.subscribeAndObserveOnMainThread {
                startActivity(VenueEventDetailActivity.getIntent(this@EventListActivity, it))
            }
        }

        binding.rvHUpcomingEvents.apply {
            adapter = eventVenueAdapter
        }

        eventOVenueAdapter = EventVenueAdapter(this@EventListActivity).apply {
            venueClick.subscribeAndObserveOnMainThread {
                startActivity(VenueEventDetailActivity.getIntent(this@EventListActivity, it))
            }
        }

        binding.rvHOnGoingEvents.apply {
            adapter = eventOVenueAdapter
        }
    }
}