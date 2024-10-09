package com.outgoer.ui.newvenuedetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.outgoer.api.event.model.EventData
import com.outgoer.api.venue.model.VenueDetail
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showToast
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.databinding.FragmentVenueDetailsEventBinding
import com.outgoer.ui.newvenuedetail.viewmodel.VenueListViewModel
import com.outgoer.ui.newvenuedetail.viewmodel.VenueViewState
import com.outgoer.ui.home.newmap.venueevents.VenueEventDetailActivity
import com.outgoer.ui.home.newmap.venueevents.view.EventVenueAdapter
import javax.inject.Inject


class VenueDetailsEventFragment : BaseFragment() {

    private var _binding: FragmentVenueDetailsEventBinding? = null
    private val binding get() = _binding!!

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<VenueListViewModel>
    private lateinit var venueListViewModel: VenueListViewModel

    private lateinit var ongoingEventVenueAdapter: EventVenueAdapter
    private lateinit var upcomingEventVenueAdapter: EventVenueAdapter

    private var venueDetail: VenueDetail? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)
        venueListViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVenueDetailsEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()
        listeToViewModel()
    }

    private fun initUI() {

        ongoingEventVenueAdapter = EventVenueAdapter(requireContext()).apply {
            venueClick.subscribeAndObserveOnMainThread {
                startActivity(VenueEventDetailActivity.getIntent(requireContext(), it))
            }
        }

        binding.ongoingRecyclerView.apply {
            adapter = ongoingEventVenueAdapter
        }


        upcomingEventVenueAdapter = EventVenueAdapter(requireContext()).apply {
            venueClick.subscribeAndObserveOnMainThread {
                startActivity(VenueEventDetailActivity.getIntent(requireContext(), it))
            }
        }

        binding.upcomingRecyclerView.apply {
            adapter = upcomingEventVenueAdapter
        }
    }

    private fun listeToViewModel() {
       venueListViewModel.venueListState.subscribeAndObserveOnMainThread {
           when(it) {
               is VenueViewState.LoadingState -> {}
               is VenueViewState.ErrorMessage -> {
                   showToast(it.errorMessage)
               }
               is VenueViewState.SuccessMessage -> {
                   showToast(it.successMessage)
               }
               is VenueViewState.EventDetailsInfo -> {

                   upcomingEventVenueAdapter.listOfDataItems = it.listOfVenueInfo.upcomming
                   ongoingEventVenueAdapter.listOfDataItems = it.listOfVenueInfo.ongoing

                   hideShowNoData(it.listOfVenueInfo.upcomming)
                   hideShowNoData(it.listOfVenueInfo.ongoing)
               }
           }
       }
    }

    private fun hideShowNoData(postInfoList: ArrayList<EventData>) {
        if (postInfoList.isNotEmpty()) {
            binding.llNoData.visibility = View.GONE
        } else {
            binding.llNoData.visibility = View.VISIBLE
        }
    }
    override fun onResume() {
        super.onResume()

        arguments?.let {
            venueDetail = it.getParcelable(VENUE_DETAILS)
            venueListViewModel.getEventVenueData(venueDetail?.id ?: 0)

        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = VenueDetailsEventFragment()

        private val VENUE_DETAILS = "venueDetail"

        @JvmStatic
        fun newInstanceWithData(venueDetail: VenueDetail): VenueDetailsEventFragment {
            var venueDetailsEventFragment = VenueDetailsEventFragment()

            var bundle = Bundle()
            bundle.putParcelable(VENUE_DETAILS, venueDetail)

            venueDetailsEventFragment.arguments = bundle

            return venueDetailsEventFragment
        }
    }
}