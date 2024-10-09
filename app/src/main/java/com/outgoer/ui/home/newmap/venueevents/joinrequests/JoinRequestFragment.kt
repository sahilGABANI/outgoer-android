package com.outgoer.ui.home.newmap.venueevents.joinrequests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.outgoer.R
import com.outgoer.api.event.model.EventData
import com.outgoer.api.event.model.RequestList
import com.outgoer.api.event.model.RequestResponseList
import com.outgoer.api.event.model.RequestResult
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showToast
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.FragmentJoinRequestBinding
import com.outgoer.ui.home.newmap.venueevents.joinrequests.view.JoinRequestAdapter
import com.outgoer.ui.home.newmap.venueevents.viewmodel.EventViewState
import com.outgoer.ui.home.newmap.venueevents.viewmodel.VenueEventViewModel
import javax.inject.Inject

class JoinRequestFragment : BaseFragment() {

    private var _binding: FragmentJoinRequestBinding? = null
    private val binding get() = _binding!!

    private lateinit var joinRequestAdapter: JoinRequestAdapter

        private var selectedType: String = ""

    private var eventData: EventData? = null

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<VenueEventViewModel>
    private lateinit var venueEventViewModel: VenueEventViewModel

    private lateinit var requestResponseList: RequestResponseList

    companion object {
        @JvmStatic
        fun newInstance() = JoinRequestFragment()

        private val EVENT_INFO = "EVENT_INFO"

        @JvmStatic
        fun newInstanceWithData(eventData: EventData): JoinRequestFragment {
            var joinRequestFragment = JoinRequestFragment()

            val args = Bundle()
            args.putParcelable(EVENT_INFO, eventData)

            joinRequestFragment.arguments = args

            return joinRequestFragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)
        venueEventViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentJoinRequestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()
        listenToViewModel()
    }

    private fun setSelectionStatus(type: String) {
        selectedType = type
        if (type.equals(resources.getString(R.string.label_new))) {
            binding.approveAppCompatTextView.background =
                resources.getDrawable(R.drawable.new_map_category_unselected_background, null)
            binding.rejectAppCompatTextView.background =
                resources.getDrawable(R.drawable.new_map_category_unselected_background, null)
            binding.newAppCompatTextView.background =
                resources.getDrawable(R.drawable.new_map_category_selected_background, null)
        } else if (type.equals(resources.getString(R.string.label_approve))) {
            binding.newAppCompatTextView.background =
                resources.getDrawable(R.drawable.new_map_category_unselected_background, null)
            binding.approveAppCompatTextView.background =
                resources.getDrawable(R.drawable.new_map_category_selected_background, null)
            binding.rejectAppCompatTextView.background =
                resources.getDrawable(R.drawable.new_map_category_unselected_background, null)
        } else if (type.equals(resources.getString(R.string.label_rejected))) {
            binding.newAppCompatTextView.background =
                resources.getDrawable(R.drawable.new_map_category_unselected_background, null)
            binding.approveAppCompatTextView.background =
                resources.getDrawable(R.drawable.new_map_category_unselected_background, null)
            binding.rejectAppCompatTextView.background =
                resources.getDrawable(R.drawable.new_map_category_selected_background, null)
        }
    }

    override fun onResume() {
        super.onResume()

        arguments?.let {
            eventData = it.getParcelable<EventData>(EVENT_INFO)

            eventData?.let {
                venueEventViewModel.joinRequestList(RequestList(eventId = it.id, 0))
            }
        }

//        binding.root.requestLayout()
    }

    private fun initUI() {

        arguments?.let {
            eventData = it.getParcelable<EventData>(EVENT_INFO)

            eventData?.let {
                venueEventViewModel.joinRequestList(RequestList(eventId = it.id, 0))
            }
        }

        binding.newFrameLayout.throttleClicks().subscribeAndObserveOnMainThread {
            setSelectionStatus(resources.getString(R.string.label_new))
            venueEventViewModel.joinRequestList(RequestList(eventId = eventData?.id ?: 0, 0))
        }

        binding.approveFrameLayout.throttleClicks().subscribeAndObserveOnMainThread {
            setSelectionStatus(resources.getString(R.string.label_approve))
            venueEventViewModel.joinRequestList(RequestList(eventId = eventData?.id ?: 0, 1))
        }

        binding.rejectFrameLayout.throttleClicks().subscribeAndObserveOnMainThread {
            setSelectionStatus(resources.getString(R.string.label_rejected))
            venueEventViewModel.joinRequestList(RequestList(eventId = eventData?.id ?: 0, 2))
        }


        joinRequestAdapter = JoinRequestAdapter(requireContext()).apply {
            approveActionState.subscribeAndObserveOnMainThread {
                requestResponseList = it
                venueEventViewModel.acceptRejectRequest(
                    it.id,
                    RequestResult(it.eventId, it.userId, 1)
                )
            }

            rejectActionState.subscribeAndObserveOnMainThread {
                requestResponseList = it
                venueEventViewModel.acceptRejectRequest(
                    it.id,
                    RequestResult(it.eventId, it.userId, 0)
                )
            }
        }

        binding.itemRecyclerView.apply {
            adapter = joinRequestAdapter
        }
    }

    private fun listenToViewModel() {
        venueEventViewModel.eventsViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is EventViewState.AcceptEventDetails -> {
                    var listJoin = joinRequestAdapter.listOfJoinRequest

                    listJoin?.find { requestResponseList.id == it.id }?.apply {
                        status = if (requestResponseList.status == 1) 0 else 1
                    }

                    joinRequestAdapter.listOfJoinRequest = listJoin
                }
                is EventViewState.LoadingState -> {}
                is EventViewState.ErrorMessage -> {
                    if (isResumed)
                        showToast(it.errorMessage)
                }
                is EventViewState.AddRemoveEventDetails -> {}
                is EventViewState.ListRequestDetails -> {
                    joinRequestAdapter.listOfJoinRequest = it.listofrequest
                }
                is EventViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                }

                else -> {}
            }
        }
    }
}