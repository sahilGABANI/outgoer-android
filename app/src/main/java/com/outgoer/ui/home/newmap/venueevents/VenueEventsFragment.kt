package com.outgoer.ui.home.newmap.venueevents

//import com.outgoer.api.event.EventViewModel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jakewharton.rxbinding3.widget.textChanges
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseFragment
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showToast
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.FragmentVenueEventsBinding
import com.outgoer.ui.createevent.CreateEventsActivity
import com.outgoer.ui.home.HomeActivity
import com.outgoer.ui.home.newmap.venueevents.view.EventVenueAdapter
import com.outgoer.ui.home.newmap.venueevents.view.UpcomingEventsAdapter
import com.outgoer.ui.home.newmap.venueevents.viewmodel.EventViewState
import com.outgoer.ui.home.newmap.venueevents.viewmodel.VenueEventViewModel
import com.outgoer.ui.home.newmap.venuemap.view.NewVenueCategoryAdapter
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import com.outgoer.utils.SnackBarUtils
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class VenueEventsFragment : BaseFragment() {

    companion object {
        @JvmStatic
        fun newInstance() = VenueEventsFragment()
    }

    private var _binding: FragmentVenueEventsBinding? = null
    private val binding get() = _binding!!

    private lateinit var upcomingEventsAdapter: UpcomingEventsAdapter
    private lateinit var venueEventExploreAdapter: UpcomingEventsAdapter
    private lateinit var eventVenueAdapter: EventVenueAdapter
    private lateinit var eventOVenueAdapter: EventVenueAdapter

    private lateinit var venueCategoryAdapter: NewVenueCategoryAdapter

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<VenueEventViewModel>
    private lateinit var venueEventViewModel: VenueEventViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
//    @Inject
//    internal lateinit var viewModelFactory1: ViewModelFactory<EventViewModel>
//    private lateinit var eventViewModel: EventViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)

        venueEventViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVenueEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()
        listenToViewModel()
        listenToViewEvents()
        RxBus.listen(RxEvent.DataReload::class.java).subscribeOnIoAndObserveOnMainThread({
            if (it.selectedTab == "Events") {
                lifecycleScope.launch {
                    delay(1000)
                    venueEventViewModel.getEventsList()
                    venueEventViewModel.getEventCategoryList()
                }
                (binding.rvHOnGoingEvents.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(0, 0)
                (binding.rvVenueEventsCategory.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(0, 0)
                (binding.rvUpcomingEvents.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(0, 0)
            }
        }, {
            Timber.e(it)
        }).autoDispose()
    }


    private fun listenToViewEvents() {
        venueCategoryAdapter = NewVenueCategoryAdapter(requireContext())
        venueCategoryAdapter.apply {
            venueCategoryAllClick.subscribeAndObserveOnMainThread {
                venueCategoryAdapter.isAllSelected = true

                val list = venueCategoryAdapter.listOfDataItems
                list?.forEach { it.isSelected = false }
                venueCategoryAdapter.listOfDataItems = list

                venueEventViewModel.getEventsList(if(binding.etSearch.text.toString().isNullOrEmpty()) null else binding.etSearch.text.toString(), 0)
            }.autoDispose()

            venueCategoryClick.subscribeAndObserveOnMainThread { venueCategory ->
                venueCategoryAdapter.isAllSelected = false


                val list = venueCategoryAdapter.listOfDataItems
                list?.forEach {
                    it.isSelected = if(venueCategory.id == it.id) !venueCategory.isSelected else false
                }
                venueCategoryAdapter.listOfDataItems = list

                venueEventViewModel.getEventsList(if(binding.etSearch.text.toString().isNullOrEmpty()) null else binding.etSearch.text.toString(), venueCategory.id)
            }.autoDispose()
        }

        binding.rvVenueEventsCategory.apply {
            adapter = venueCategoryAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun listenToViewModel() {
        venueEventViewModel.eventsViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is EventViewState.AcceptEventDetails -> {}
                is EventViewState.LoadingState -> {}
                is EventViewState.ErrorMessage -> {
                    if (isResumed) {
                        Timber.tag("ErrorMessage").e("EventViewState -> it.errorMessage: ${it.errorMessage}")
                        if (it.errorMessage.startsWith("Unable to resolve host")) {
                            SnackBarUtils.showTopSnackBar(requireView())
                        } else {
                            showToast(it.errorMessage)
                        }
                    }
                }
                is EventViewState.VenueCategoryList -> {
                    venueCategoryAdapter.listOfDataItems = it.venueCategoryList
                }
                is EventViewState.EventListDetails -> {

                    if(it.listofevent.upcomming.size > 0) {
                        binding.llNoDataUpcoming.visibility = View.GONE
                        binding.rvUpcomingEvents.visibility = View.VISIBLE
                        upcomingEventsAdapter.listOfDataItems = it.listofevent.upcomming
                    } else {
                        binding.llNoDataUpcoming.visibility = View.VISIBLE
                        binding.rvUpcomingEvents.visibility = View.GONE
                    }

                    if(it.listofevent.ongoing.size > 0) {
                        binding.llNoData.visibility = View.GONE
                        binding.ongoingEventsRecyclerView.visibility = View.VISIBLE
                        venueEventExploreAdapter.listOfDataItems = it.listofevent.ongoing
                    } else {
                        binding.llNoData.visibility = View.VISIBLE
                        binding.ongoingEventsRecyclerView.visibility = View.GONE
                    }
/*
                    binding.llNoData.visibility = if(it.listofevent.upcomming.size == 0 && it.listofevent.ongoing.size == 0) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }*/

                    if (binding.etSearch.length() > 0) {

                        if(it.listofevent.upcomming.size > 0) {
                            binding.rvUpcomingEvents.visibility = View.GONE
                            binding.rvHUpcomingEvents.visibility = if (it.listofevent.upcomming.size > 0) View.VISIBLE else View.GONE
                            eventVenueAdapter.listOfDataItems = it.listofevent.upcomming
                        } else {
                            binding.rvUpcomingEvents.visibility = View.GONE
                            binding.rvHUpcomingEvents.visibility = View.GONE
                        }

                        if ( it.listofevent.ongoing.size > 0) {
                            binding.ongoingEventsRecyclerView.visibility = View.GONE
                            binding.rvHOnGoingEvents.visibility = if (it.listofevent.ongoing.size > 0) View.VISIBLE else View.GONE
                            eventOVenueAdapter.listOfDataItems = it.listofevent.ongoing
                        } else {
                            binding.ongoingEventsRecyclerView.visibility = View.GONE
                            binding.rvHOnGoingEvents.visibility = View.GONE
                        }

                    } else {
                        binding.rvHUpcomingEvents.visibility = View.GONE
                        binding.rvHOnGoingEvents.visibility = View.GONE
                    }
                    binding.addEventFloatingActionButton.visibility =
                        if (MapVenueUserType.VENUE_OWNER.type.equals(loggedInUserCache.getLoggedInUser()?.loggedInUser?.userType)) View.VISIBLE else View.GONE
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
            startActivity(CreateEventsActivity.getIntent(requireContext(), venueDetail = null))
        }

        binding.etSearch.textChanges()
            .skipInitialValue()
            .debounce(500, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeOnIoAndObserveOnMainThread({
//                UiUtils.hideKeyboard(requireContext())

                if (it.length > 2) {
                    venueEventViewModel.getEventsList(it.toString())
                    binding.rvVenueEventsCategory.visibility = View.GONE
                } else {
                    if(isResumed) {
                        venueEventViewModel.getEventsList()
                        binding.rvVenueEventsCategory.visibility = View.VISIBLE
                    }
                }
            }, {
                Timber.e(it)
            }).autoDispose()

        upcomingEventsAdapter = UpcomingEventsAdapter(requireContext()).apply {
            upcomingEventsViewClick.subscribeAndObserveOnMainThread {
                startActivity(VenueEventDetailActivity.getIntentWithId(requireContext(), it.id))
            }
            profileViewClick.subscribeAndObserveOnMainThread {
                if(MapVenueUserType.VENUE_OWNER.type.equals(it.userType)) {
                    startActivity(NewVenueDetailActivity.getIntent(requireContext(), 0, it.id))
                } else {
                    startActivity(NewOtherUserProfileActivity.getIntent(requireContext(), it.userId))
                }
            }
            profileListViewClick.subscribeAndObserveOnMainThread {
                val mutualFriendsBottomSheet = MutualFriendsBottomSheet(it)
                mutualFriendsBottomSheet.show(childFragmentManager, "MutualFriendsBottomSheet")
            }
        }
        venueEventExploreAdapter = UpcomingEventsAdapter(requireContext()).apply {
            profileListViewClick.subscribeAndObserveOnMainThread {
                val mutualFriendsBottomSheet = MutualFriendsBottomSheet(it)
                mutualFriendsBottomSheet.show(childFragmentManager, "MutualFriendsBottomSheet")
            }
            upcomingEventsViewClick.subscribeAndObserveOnMainThread {
                startActivity(VenueEventDetailActivity.getIntent(requireContext(), it))
            }
            profileViewClick.subscribeAndObserveOnMainThread {
                if(MapVenueUserType.VENUE_OWNER.type.equals(it.userType)) {
                    startActivity(NewVenueDetailActivity.getIntent(requireContext(), 0, it.id))
                } else {
                    startActivity(NewOtherUserProfileActivity.getIntent(requireContext(), it.userId))
                }
            }
        }

        binding.rvUpcomingEvents.apply {
            adapter = upcomingEventsAdapter
        }

        binding.ongoingEventsRecyclerView.apply {
            adapter = venueEventExploreAdapter
        }


        eventVenueAdapter = EventVenueAdapter(requireContext()).apply {
            venueClick.subscribeAndObserveOnMainThread {
                startActivity(VenueEventDetailActivity.getIntent(requireContext(), it))
            }
        }

        binding.rvHUpcomingEvents.apply {
            adapter = eventVenueAdapter
        }

        eventOVenueAdapter = EventVenueAdapter(requireContext()).apply {
            venueClick.subscribeAndObserveOnMainThread {
                startActivity(VenueEventDetailActivity.getIntent(requireContext(), it))
            }
        }

        binding.rvHOnGoingEvents.apply {
            adapter = eventOVenueAdapter
        }
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as HomeActivity).hideMapFragment()

        if(isResumed) {
            venueEventViewModel.getEventsList()
            venueEventViewModel.getEventCategoryList()
        }
    }
}