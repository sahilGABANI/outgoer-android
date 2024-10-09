package com.outgoer.ui.newnotification

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.outgoer.R
import com.outgoer.api.notification.model.NotificationActionState
import com.outgoer.api.notification.model.NotificationInfo
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseFragment
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showLongToast
import com.outgoer.base.extension.startActivityWithDefaultAnimation
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.databinding.FragmentActivityNotificationBinding
import com.outgoer.databinding.FragmentGeneralNotificationBinding
import com.outgoer.service.NotificationService
import com.outgoer.ui.newnotification.view.ActivityNotificationAdapter
import com.outgoer.ui.newnotification.view.GeneralNotificationAdapter
import com.outgoer.ui.notification.viewmodel.NotificationViewModel
import com.outgoer.ui.notification.viewmodel.NotificationViewState
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import com.outgoer.ui.postdetail.PostDetailActivity
import com.outgoer.ui.reelsdetail.ReelsDetailActivity
import com.outgoer.ui.sponty.SpontyDetailsActivity
import javax.inject.Inject

class GeneralNotificationFragment : BaseFragment() {

    private var _binding: FragmentGeneralNotificationBinding? = null
    private val binding get() = _binding!!


    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<NotificationViewModel>
    private lateinit var notificationViewModel: NotificationViewModel

    private lateinit var generalNotificationAdapter: GeneralNotificationAdapter
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?, ): View? {
        _binding = FragmentGeneralNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        OutgoerApplication.component.inject(this)
        notificationViewModel = getViewModelFromFactory(viewModelFactory)

        listenToViewModel()
        listenToViewEvents()
    }

    private fun listenToViewModel() {
        notificationViewModel.notificationState.subscribeAndObserveOnMainThread {
            when (it) {
                is NotificationViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is NotificationViewState.SuccessMessage -> {
                    showLongToast(it.successMessage)
                }
                is NotificationViewState.LoadingState -> {}
                is NotificationViewState.GetAllNotificationInfo -> {
                    generalNotificationAdapter.listOfDataItems = it.notificationInfoList
                    RxBus.publish(RxEvent.UpdateNotificationBadge(false))
                    hideShowNoData(it.notificationInfoList)
                }
            }
        }.autoDispose()
    }

    private fun listenToViewEvents() {
//        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
//            onBackPressed()
//        }.autoDispose()

        generalNotificationAdapter = GeneralNotificationAdapter(requireContext())
        generalNotificationAdapter.apply {
//            notificationActionState.subscribeAndObserveOnMainThread { state ->
//                when (state) {
//                    is NotificationActionState.UpdateReadStatus -> {
//                        notificationViewModel.updateNotificationReadStatus(state.notificationInfo.id)
//                    }
//                    is NotificationActionState.RowViewClick -> {
//                        val notificationType = state.notificationInfo.notificationType
//                        val postReelId = state.notificationInfo.postReelId
//                        if (!notificationType.isNullOrEmpty()) {
//                            val intent = when (notificationType) {
//                                //Follow
//                                NotificationService.N_TYPE_FOLLOW -> {
//                                    NewOtherUserProfileActivity.getIntent(requireContext(), state.notificationInfo.senderId ?: 0)
//                                }
//                                //Post
//                                NotificationService.N_TYPE_POST_LIKED -> {
//                                    PostDetailActivity.getIntent(requireContext(), postReelId ?: 0)
//                                }
//                                NotificationService.N_TYPE_POST_COMMENT, NotificationService.N_TYPE_POST_COMMENT_LIKED, NotificationService.N_TYPE_POST_COMMENT_REPLY -> {
//                                    PostDetailActivity.getIntent(requireContext(), postReelId ?: 0, showComments = true)
//                                }
//                                NotificationService.N_TYPE_POST_TAG_POST -> {
//                                    PostDetailActivity.getIntent(requireContext(), postReelId ?: 0, showTaggedPeople = true)
//                                }
//                                //Reel
//                                NotificationService.N_TYPE_REEL_LIKED -> {
//                                    ReelsDetailActivity.getIntent(requireContext(), postReelId ?: 0)
//                                }
//                                NotificationService.N_TYPE_REEL_COMMENT, NotificationService.N_TYPE_REEL_COMMENT_LIKED, NotificationService.N_TYPE_REEL_COMMENT_REPLY -> {
//                                    ReelsDetailActivity.getIntent(requireContext(), postReelId ?: 0, showComments = true)
//                                }
//                                NotificationService.N_TYPE_REEL_TAG_REEL -> {
//                                    ReelsDetailActivity.getIntent(requireContext(), postReelId ?: 0, showTaggedPeople = true)
//                                }
//                                NotificationService.N_TYPE_SPONTY_JOINED -> {
//                                    SpontyDetailsActivity.getIntent(requireContext(), postReelId ?: 0)
//                                }
//                                NotificationService.N_TYPE_SPONTY_LIKED -> {
//                                    SpontyDetailsActivity.getIntent(requireContext(), postReelId ?: 0)
//                                }
//                                NotificationService.N_TYPE_SPONTY_COMMENT -> {
//                                    SpontyDetailsActivity.getIntent(requireContext(), postReelId ?: 0,true)
//                                }
//                                else -> {
//                                    null
//                                }
//                            }
//
//                            if (intent != null) {
//                                startActivityWithDefaultAnimation(intent)
//                            }
//                        }
//                    }
//                    is NotificationActionState.UserProfileClick -> {
//                        startActivityWithDefaultAnimation(
//                            NewOtherUserProfileActivity.getIntent(
//                                requireContext(),
//                                state.notificationInfo.sender?.id ?: 0
//                            )
//                        )
//                    }
//                }
//            }.autoDispose()
        }

        binding.rvActivityNotification.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = generalNotificationAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                notificationViewModel.loadMoreN()
                            }
                        }
                    }
                }
            })
        }

        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            binding.swipeRefreshLayout.isRefreshing = false
            notificationViewModel.pullToRefreshN()
        }.autoDispose()

        notificationViewModel.pullToRefreshN()
    }

    private fun hideShowNoData(notificationInfoList: List<NotificationInfo>) {
        if (notificationInfoList.isNotEmpty()) {
            binding.llNoData.visibility = View.GONE
        } else {
            binding.llNoData.visibility = View.VISIBLE
        }
    }
}