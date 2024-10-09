package com.outgoer.ui.create_story

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.R
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseFragment
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showToast
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.databinding.FragmentOutgoerVenueBinding
import com.outgoer.databinding.FragmentStoryListBinding
import com.outgoer.ui.create_story.CreateStoryActivity.Companion.GOOGLE_MAP_INFO
import com.outgoer.ui.create_story.CreateStoryActivity.Companion.VIDEO_MAP_INFO
import com.outgoer.ui.createevent.view.NearVenueAdapter
import com.outgoer.ui.createevent.viewmodel.CreateEventsViewModel
import com.outgoer.ui.createevent.viewmodel.EventViewState
import com.outgoer.ui.vennue_list.VenueListActivity
import com.outgoer.utils.SnackBarUtils.showTopSnackBar
import timber.log.Timber
import javax.inject.Inject

class OutgoerVenueFragment : BaseFragment() {

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<CreateEventsViewModel>
    private lateinit var createEventsViewModel: CreateEventsViewModel

    private lateinit var nearVenueAdapter: NearVenueAdapter

    private var _binding: FragmentOutgoerVenueBinding? = null
    private val binding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)
        createEventsViewModel = getViewModelFromFactory(viewModelFactory)

    }


    override fun onResume() {
        super.onResume()
        RxBus.listen(RxEvent.SearchStoryLocation::class.java).subscribeAndObserveOnMainThread {

            arguments?.let { arg ->
                if(arg.getString(TYPE_INFO).equals("1")) {
                    if(it.searchString != null) {

                        it.searchString?.let { it1 -> createEventsViewModel.searchVenueList(it1) }
                    } else {
                        createEventsViewModel.searchVenueList("")
                    }
                } else {
                    createEventsViewModel.getNearGooglePlaces(it.searchString)
                }
            }


        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentOutgoerVenueBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        arguments?.let {
            if(it.getString(TYPE_INFO).equals("1")) {
                createEventsViewModel.resetPagination()
            } else {
                createEventsViewModel.getNearGooglePlaces()
            }
        }


        initAdapter()
        listenToViewModel()
    }

    private fun initAdapter() {
        nearVenueAdapter = NearVenueAdapter(requireContext()).apply {
            venueClick.subscribeAndObserveOnMainThread {
                requireActivity().setResult(Activity.RESULT_OK, Intent().putExtra(VIDEO_MAP_INFO, it))
                requireActivity().finish()
            }.autoDispose()

            googlePlaceClick.subscribeAndObserveOnMainThread {
                requireActivity().setResult(Activity.RESULT_OK, Intent().putExtra(GOOGLE_MAP_INFO, it))
                requireActivity().finish()
            }.autoDispose()
        }
        binding.rvNearVenueList.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        binding.rvNearVenueList.apply {
            adapter = nearVenueAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                createEventsViewModel.loadMore()
                            }
                        }
                    }
                }
            })
        }
    }

    private fun listenToViewModel() {
        createEventsViewModel.eventsViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is EventViewState.LoadingState -> {

                }
                is EventViewState.ErrorMessage -> {
                    Timber.tag("ErrorMessage").e("EventViewState -> it.errorMessage: ${it.errorMessage}")
                    if (it.errorMessage.startsWith("Unable to resolve host")) {
                        showTopSnackBar(requireView())
                    } else {
                        showToast(it.errorMessage)
                    }
                }
                is EventViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is EventViewState.EventDetails -> {

                }
                is EventViewState.ListOfGoogleMap -> {
                    nearVenueAdapter.listOfGooglePlaces = it.event

                }
                is EventViewState.VenueMapList -> {
                    arguments?.let {
                        if(it.getString(TYPE_INFO).equals("1")) {
                            nearVenueAdapter.listOfGooglePlaces = null
                        }
                    }
                    nearVenueAdapter.listOfDataItems = it.event
                }
                is EventViewState.VenueInfoList -> {
                    arguments?.let {
                        if(it.getString(TYPE_INFO).equals("1")) {
                            nearVenueAdapter.listOfGooglePlaces = null
                        }
                    }
                    nearVenueAdapter.listOfDataItems = it.listOfVenueInfo
                }
                else -> {

                }
            }
        }
    }

    companion object {

        private val TYPE_INFO =  "TYPE_INFO"

        @JvmStatic
        fun newInstance(type: String): OutgoerVenueFragment {
            var outgoerVenueFragment = OutgoerVenueFragment()

            var bundle = Bundle()
            bundle.putString(TYPE_INFO, type)

            outgoerVenueFragment.arguments = bundle
            return outgoerVenueFragment
        }
    }
}