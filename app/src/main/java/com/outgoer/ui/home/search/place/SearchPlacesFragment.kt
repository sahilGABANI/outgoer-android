package com.outgoer.ui.home.search.place

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.outgoer.api.follow.model.FollowActionState
import com.outgoer.api.follow.model.PlaceFollowActionState
import com.outgoer.api.venue.model.VenueListInfo
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showLongToast
import com.outgoer.base.extension.startActivityWithDefaultAnimation
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.databinding.FragmentSearchPlacesBinding
import com.outgoer.ui.home.search.place.view.SearchPlacesAdapter
import com.outgoer.ui.home.search.place.viewmodel.SearchPlacesViewModel
import com.outgoer.ui.home.search.place.viewmodel.SearchPlacesViewState
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import com.outgoer.utils.SnackBarUtils
import timber.log.Timber
import javax.inject.Inject

class SearchPlacesFragment : BaseFragment() {

    companion object {
        fun getInstance(): Fragment {
            return SearchPlacesFragment()
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<SearchPlacesViewModel>
    private lateinit var searchPlacesViewModel: SearchPlacesViewModel

    private var _binding: FragmentSearchPlacesBinding? = null
    private val binding get() = _binding!!

    private lateinit var searchPlacesAdapter: SearchPlacesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        OutgoerApplication.component.inject(this)
        searchPlacesViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchPlacesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listenToViewModel()
        listenToViewEvents()
        searchPlacesViewModel.resetSearchPlacesPagination(true)
    }

    private fun listenToViewEvents() {
        searchPlacesAdapter = SearchPlacesAdapter(requireContext())
        searchPlacesAdapter.apply {
            searchPlaceClick.subscribeAndObserveOnMainThread {
                startActivityWithDefaultAnimation(NewVenueDetailActivity.getIntent(requireContext(), it.categoryId ?: 0, it.id))
            }.autoDispose()

            followActionState.subscribeAndObserveOnMainThread {
                when(it) {
                    is PlaceFollowActionState.FollowClick -> {
                        val mPos = listOfDataItem?.indexOfFirst { followUser ->
                            followUser.id == it.followUser.id
                        }
                        if (mPos != -1) {
                            listOfDataItem?.get(mPos ?: 0)?.followStatus = 1
                            searchPlacesAdapter.listOfDataItem = listOfDataItem
                        }
                        searchPlacesViewModel.acceptRejectFollowRequest(it.followUser.id)
                    }
                    is PlaceFollowActionState.FollowingClick -> {
                        val mPos = listOfDataItem?.indexOfFirst { followUser ->
                            followUser.id == it.followUser.id
                        }
                        if (mPos != -1) {
                            listOfDataItem?.get(mPos ?: 0)?.followStatus = 0
                            searchPlacesAdapter.listOfDataItem = listOfDataItem
                        }
                        searchPlacesViewModel.acceptRejectFollowRequest(it.followUser.id)

                    }
                    else -> {}
                }
            }
        }

        binding.rvPlaceSearch.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = searchPlacesAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                searchPlacesViewModel.loadMoreSearchPlaces()
                            }
                        }
                    }
                }
            })
        }

        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            binding.swipeRefreshLayout.isRefreshing = false
            searchPlacesViewModel.resetSearchPlacesPagination(false)
        }.autoDispose()
    }

    private fun listenToViewModel() {
        searchPlacesViewModel.searchPlacesViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is SearchPlacesViewState.ErrorMessage -> {
                    Timber.tag("ErrorMessage").e("SearchPlacesViewState -> it.errorMessage: ${it.ErrorMessage}")
                    if (it.ErrorMessage.startsWith("Unable to resolve host")) {
                        SnackBarUtils.showTopSnackBar(requireView())
                    } else {
                        showLongToast(it.ErrorMessage)
                    }
                }
                is SearchPlacesViewState.SearchPlacesList -> {
                    searchPlacesAdapter.listOfDataItem = it.listOfSearchPlacesData
                    hideShowNoData(it.listOfSearchPlacesData)
                }
                is SearchPlacesViewState.LoadingState -> {
                    if (it.isLoading) {
                        binding.progressbar.visibility = View.VISIBLE
                    } else {
                        binding.progressbar.visibility = View.GONE
                    }
                }
            }
        }.autoDispose()
    }

    private fun hideShowNoData(listOfSearchPlacesData: List<VenueListInfo>) {
        if (listOfSearchPlacesData.isNotEmpty()) {
            binding.llNoData.visibility = View.GONE
        } else {
            binding.llNoData.visibility = View.VISIBLE
        }
    }




}