package com.outgoer.ui.home.search.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.follow.model.FollowActionState
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseFragment
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showLongToast
import com.outgoer.base.extension.startActivityWithDefaultAnimation
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.databinding.FragmentSearchAccountsBinding
import com.outgoer.ui.home.search.account.view.SearchAccountsAdapter
import com.outgoer.ui.home.search.account.viewmodel.SearchAccountsViewModel
import com.outgoer.ui.home.search.account.viewmodel.SearchAccountsViewState
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import com.outgoer.utils.SnackBarUtils
import timber.log.Timber
import javax.inject.Inject

class SearchAccountsFragment : BaseFragment() {

    companion object {
        fun getInstance(): Fragment {
            return SearchAccountsFragment()
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<SearchAccountsViewModel>
    private lateinit var searchAccountsViewModel: SearchAccountsViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var _binding: FragmentSearchAccountsBinding? = null
    private val binding get() = _binding!!

    private lateinit var searchAccountsAdapter: SearchAccountsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        OutgoerApplication.component.inject(this)
        searchAccountsViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchAccountsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listenToViewModel()
        listenToViewEvents()
        searchAccountsViewModel.resetSearchAccountPagination(true)
    }



    private fun listenToViewEvents() {
        searchAccountsAdapter = SearchAccountsAdapter(requireContext())
        searchAccountsAdapter.apply {
            searchAccountsState.subscribeAndObserveOnMainThread { state ->
                when (state) {
                    is FollowActionState.FollowClick -> {
                        searchAccountsViewModel.acceptRejectFollowRequest(state.followUser)
                    }
                    is FollowActionState.FollowingClick -> {
                        searchAccountsViewModel.acceptRejectFollowRequest(state.followUser)
                    }
                    is FollowActionState.UserProfileClick -> {
                        if(state.followUser.userType == MapVenueUserType.VENUE_OWNER.type) {
                            if(loggedInUserCache.getUserId() == state.followUser.id) {
                                RxBus.publish(RxEvent.OpenVenueUserProfile)
                            }else {
                                startActivityWithDefaultAnimation(NewVenueDetailActivity.getIntent(requireContext(),0,
                                    state.followUser.id
                                ))
                            }
                        } else {
                            startActivityWithDefaultAnimation(
                                NewOtherUserProfileActivity.getIntent(
                                    requireContext(),
                                    state.followUser.id
                                )
                            )
                        }
                    }
                }
            }.autoDispose()
        }

        binding.rvAccountsSearch.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = searchAccountsAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                searchAccountsViewModel.loadMoreSearchAccount()
                            }
                        }
                    }
                }
            })
        }

        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            binding.swipeRefreshLayout.isRefreshing = false
            searchAccountsViewModel.resetSearchAccountPagination(false)
        }.autoDispose()
    }

    private fun listenToViewModel() {
        searchAccountsViewModel.searchAccountsViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is SearchAccountsViewState.ErrorMessage -> {
                    Timber.tag("ErrorMessage").e("SearchAccountsViewState -> it.errorMessage: ${it.ErrorMessage}")
                    if (it.ErrorMessage.startsWith("Unable to resolve host")) {
                        SnackBarUtils.showTopSnackBar(requireView())
                    } else {
                        showLongToast(it.ErrorMessage)
                    }
                }
                is SearchAccountsViewState.SearchAccountList -> {
                    searchAccountsAdapter.listOfDataItem = it.listOfSearchAccountData
                    hideShowNoData(it.listOfSearchAccountData)
                }
                is SearchAccountsViewState.LoadingState -> {
                    if (it.isLoading) {
                        binding.progressbar.visibility = View.VISIBLE
                    } else {
                        binding.progressbar.visibility = View.GONE
                    }
                }
            }
        }.autoDispose()
    }

    private fun hideShowNoData(listOfSearchAccountData: List<FollowUser>) {
        if (listOfSearchAccountData.isNotEmpty()) {
            binding.llNoData.visibility = View.GONE
        } else {
            binding.llNoData.visibility = View.VISIBLE
        }
    }
}