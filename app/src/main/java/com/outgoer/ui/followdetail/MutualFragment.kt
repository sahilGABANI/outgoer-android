package com.outgoer.ui.followdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.jakewharton.rxbinding3.widget.editorActions
import com.jakewharton.rxbinding3.widget.textChanges
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.follow.model.FollowActionState
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseFragment
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.FragmentMutualBinding
import com.outgoer.ui.followdetail.view.FollowersAdapter
import com.outgoer.ui.followdetail.viewmodel.MutualViewModel
import com.outgoer.ui.followdetail.viewmodel.MutualViewState
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import com.outgoer.utils.SnackBarUtils
import com.outgoer.utils.UiUtils
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.properties.Delegates

class MutualFragment(
    private val userId: Int
) : BaseFragment() {

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<MutualViewModel>
    private lateinit var mutualViewModel: MutualViewModel
    private var _binding: FragmentMutualBinding? = null
    private val binding get() = _binding!!
    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var loggedInUserId by Delegates.notNull<Int>()
    private lateinit var followersAdapter: FollowersAdapter
    private var listOfFollowers: List<FollowUser> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)
        mutualViewModel = getViewModelFromFactory(viewModelFactory)
        loggedInUserId = loggedInUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMutualBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listenToViewModel()
        listenToViewEvents()
    }

    fun listenToViewModel() {
        mutualViewModel.mutualViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is MutualViewState.LoadingState -> {

                }
                is MutualViewState.ErrorMessage -> {
                    Timber.tag("ErrorMessage").e("MutualViewState -> it.errorMessage: ${it.ErrorMessage}")
                    if (it.ErrorMessage.startsWith("Unable to resolve host")) {
                        SnackBarUtils.showTopSnackBar(requireView())
                    } else {
                        showLongToast(it.ErrorMessage)
                    }
                }
                is MutualViewState.FollowerList -> {
                    listOfFollowers = it.listOfMutual
                    followersAdapter.listOfDataItem = listOfFollowers
                    if (it.listOfMutual.isNotEmpty()) {
                        binding.llNoData.visibility = View.GONE
                    } else {
                        binding.llNoData.visibility = View.VISIBLE
                    }
                }
            }
        }.autoDispose()
    }

    fun listenToViewEvents() {
        followersAdapter = FollowersAdapter(requireContext())
        followersAdapter.apply {
            followActionState.subscribeAndObserveOnMainThread { state ->
                when (state) {
                    is FollowActionState.FollowClick -> {
                        val userPos = listOfFollowers.indexOfFirst { followUser ->
                            followUser.id == state.followUser.id
                        }
                        if (userPos != -1) {
                            listOfFollowers[userPos].followStatus = 1
                            listOfFollowers[userPos].totalFollowers =
                                state.followUser.totalFollowers?.let { it + 1 } ?: 0
                            followersAdapter.listOfDataItem = listOfFollowers
                        }
                        mutualViewModel.acceptRejectFollowRequest(state.followUser)
                    }
                    is FollowActionState.FollowingClick -> {
                        val userPos = listOfFollowers.indexOfFirst { followUser ->
                            followUser.id == state.followUser.id
                        }
                        if (userPos != -1) {
                            if (loggedInUserId == userId) {
                                val tempList = listOfFollowers.toMutableList()
                                tempList.removeAt(userPos)
                                listOfFollowers = tempList
                                followersAdapter.listOfDataItem = tempList
                            } else {
                                listOfFollowers[userPos].followStatus = 0
                                listOfFollowers[userPos].totalFollowers =
                                    state.followUser.totalFollowers?.let { it - 1 } ?: 0
                                followersAdapter.listOfDataItem = listOfFollowers
                            }
                            hideShowNoData(listOfFollowers)
                        }
                        mutualViewModel.acceptRejectFollowRequest(state.followUser)
                    }
                    is FollowActionState.UserProfileClick -> {
                        if (state.followUser.userType == MapVenueUserType.VENUE_OWNER.type) {
                            if (loggedInUserCache.getUserId() == state.followUser.id) {
                                RxBus.publish(RxEvent.OpenVenueUserProfile)
                            } else {
                                startActivityWithDefaultAnimation(
                                    NewVenueDetailActivity.getIntent(
                                        requireContext(), 0,
                                        state.followUser.id
                                    )
                                )
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

        binding.rvFollowersList.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = followersAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                mutualViewModel.loadMoreMutualList(userId)
                            }
                        }
                    }
                }
            })
        }

        binding.etSearch.editorActions()
            .filter { action -> action == EditorInfo.IME_ACTION_SEARCH }
            .subscribeAndObserveOnMainThread {
//                UiUtils.hideKeyboard(requireContext())
            }.autoDispose()

        binding.etSearch.textChanges()
            .skipInitialValue()
            .doOnNext {
                if (it.isNullOrEmpty()) {
                    binding.ivClear.visibility = View.INVISIBLE
                } else {
                    binding.ivClear.visibility = View.VISIBLE
                }
            }
            .debounce(300, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeOnIoAndObserveOnMainThread({
                if (it.length > 2) {
                    mutualViewModel.searchMutualList(userId, it.toString())
                } else if (it.isEmpty()) {
                    mutualViewModel.searchMutualList(userId, "")
                }
//                UiUtils.hideKeyboard(requireContext())
            }, {
                Timber.e(it)
            }).autoDispose()

        binding.ivClear.throttleClicks().subscribeAndObserveOnMainThread {
            UiUtils.hideKeyboard(requireContext())
            binding.etSearch.setText("")
        }.autoDispose()

        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            binding.swipeRefreshLayout.isRefreshing = false
            binding.etSearch.setText("")
        }.autoDispose()
    }

    private fun hideShowNoData(followingList: List<FollowUser>) {
        if (followingList.isNotEmpty()) {
            binding.llNoData.visibility = View.GONE
        } else {
            binding.llNoData.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        if (binding.etSearch.text.isNullOrEmpty()) {
            mutualViewModel.searchMutualList(userId, "")
        } else {
            binding.etSearch.setText("")
        }
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}