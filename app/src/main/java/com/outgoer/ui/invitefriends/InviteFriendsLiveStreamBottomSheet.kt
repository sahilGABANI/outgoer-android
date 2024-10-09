package com.outgoer.ui.invitefriends

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.EditorInfo
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.jakewharton.rxbinding3.widget.editorActions
import com.jakewharton.rxbinding3.widget.textChanges
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseDialogFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.BottomSheetInviteFriendsLiveStreamBinding
import com.outgoer.ui.invitefriends.view.InviteFriendsLiveStreamAdapter
import com.outgoer.ui.invitefriends.viewmodel.InviteFriendsLiveStreamViewModel
import com.outgoer.ui.invitefriends.viewmodel.InviteFriendsLiveStreamViewState
import com.outgoer.utils.UiUtils
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.properties.Delegates

class InviteFriendsLiveStreamBottomSheet(
    private val selectedFollowList: ArrayList<FollowUser>
) : BaseDialogFragment() {

    private val inviteUpdatedSubject: PublishSubject<Map<Int, FollowUser>> = PublishSubject.create()
    val inviteUpdated: Observable<Map<Int, FollowUser>> = inviteUpdatedSubject.hide()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<InviteFriendsLiveStreamViewModel>
    private lateinit var inviteFriendsLiveStreamViewModel: InviteFriendsLiveStreamViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var loggedInUserId by Delegates.notNull<Int>()

    private var _binding: BottomSheetInviteFriendsLiveStreamBinding? = null
    private val binding get() = _binding!!

    private lateinit var inviteFriendsLiveStreamAdapter: InviteFriendsLiveStreamAdapter

    private var listOfFollowResponseWithIdMain: MutableMap<Int, FollowUser> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        OutgoerApplication.component.inject(this)
        inviteFriendsLiveStreamViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetInviteFriendsLiveStreamBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewEvent()
        listenToViewModel()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    private fun listenToViewModel() {
        inviteFriendsLiveStreamViewModel.inviteFriendsLiveStreamState.subscribeAndObserveOnMainThread { state ->
            when (state) {
                is InviteFriendsLiveStreamViewState.ErrorMessage -> {
                    showToast(state.errorMessage)
                }
                is InviteFriendsLiveStreamViewState.LoadingState -> {

                }
                is InviteFriendsLiveStreamViewState.SuccessMessage -> {
                    showToast(state.successMessage)
                }
                is InviteFriendsLiveStreamViewState.FollowerList -> {
                    updateAdapter(state.listOfFollowers)
                }
                is InviteFriendsLiveStreamViewState.UpdateFollowerList -> {
                    updateAdapterForInvite(state.selectedFollowList)
                }
            }
        }.autoDispose()
    }

    private fun listenToViewEvent() {
        loggedInUserId = loggedInUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0

        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            dismissBottomSheet()
        }.autoDispose()

        binding.ivDone.throttleClicks().subscribeAndObserveOnMainThread {
            inviteUpdatedSubject.onNext(listOfFollowResponseWithIdMain.toMap())
        }.autoDispose()

        inviteFriendsLiveStreamAdapter = InviteFriendsLiveStreamAdapter(requireContext())
        inviteFriendsLiveStreamAdapter.apply {
            inviteUpdated.subscribeAndObserveOnMainThread { map ->
                listOfFollowResponseWithIdMain.putAll(map)
            }.autoDispose()
        }

        binding.rvInviteFriends.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = inviteFriendsLiveStreamAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                inviteFriendsLiveStreamViewModel.loadMoreFollowersList(loggedInUserId)
                            }
                        }
                    }
                }
            })
        }

        binding.etSearch.editorActions()
            .filter { action -> action == EditorInfo.IME_ACTION_SEARCH }
            .subscribeAndObserveOnMainThread {
                UiUtils.hideKeyboard(requireContext())
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
                    inviteFriendsLiveStreamViewModel.searchFollowersList(loggedInUserId, it.toString())
                } else if (it.isEmpty()) {
                    inviteFriendsLiveStreamViewModel.searchFollowersList(loggedInUserId, "")
                }
                UiUtils.hideKeyboard(requireContext())
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

        if (binding.etSearch.text.isNullOrEmpty()) {
            inviteFriendsLiveStreamViewModel.searchFollowersList(loggedInUserId, "")
        } else {
            binding.etSearch.setText("")
        }
    }

    private fun updateAdapter(listOfFollowResponse: List<FollowUser>) {
        listOfFollowResponse.forEach { follower ->
            val findFollower = selectedFollowList.firstOrNull { it.id == follower.id }
            if (findFollower != null) {
                follower.isAlreadyInvited = true
            }
        }
        val listOfFollowResponseWithId = listOfFollowResponse.map { it.id to it }.toMap().toMutableMap()
        inviteFriendsLiveStreamAdapter.listOfFollowResponseWithId = listOfFollowResponseWithId

        if (listOfFollowResponseWithId.isNotEmpty()) {
            binding.llNoData.visibility = View.GONE
        } else {
            binding.llNoData.visibility = View.VISIBLE
        }
    }

    private fun updateAdapterForInvite(selectedFollowList: List<FollowUser>) {
        val listOfFollowResponse = inviteFriendsLiveStreamAdapter.listOfFollowResponseWithId?.values ?: return
        listOfFollowResponse.forEach { follower ->
            val findFollower = selectedFollowList.firstOrNull { it.id == follower.id }
            if (findFollower != null) {
                follower.isInvited = findFollower.isInvited
            }
        }
        val listOfFollowResponseWithId = listOfFollowResponse.map { it.id to it }.toMap().toMutableMap()
        inviteFriendsLiveStreamAdapter.listOfFollowResponseWithId = listOfFollowResponseWithId
    }

    fun dismissBottomSheet() {
        dismiss()
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}