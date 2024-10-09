package com.outgoer.ui.newnotification

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.outgoer.api.notification.model.NotificationActionState
import com.outgoer.api.notification.model.NotificationInfo
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showLongToast
import com.outgoer.base.extension.startActivityWithDefaultAnimation
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivityNewNotificationBinding
import com.outgoer.service.NotificationService
import com.outgoer.ui.home.newmap.venueevents.VenueEventDetailActivity
import com.outgoer.ui.newnotification.view.ActivityNotificationAdapter
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import com.outgoer.ui.notification.viewmodel.NotificationViewModel
import com.outgoer.ui.notification.viewmodel.NotificationViewState
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import com.outgoer.ui.postdetail.PostDetailActivity
import com.outgoer.ui.reelsdetail.ReelsDetailActivity
import com.outgoer.ui.sponty.SpontyDetailsActivity
import javax.inject.Inject

class NewNotificationActivity : BaseActivity() {

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<NotificationViewModel>
    private lateinit var notificationViewModel: NotificationViewModel

    private lateinit var activityNotificationAdapter: ActivityNotificationAdapter

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, NewNotificationActivity::class.java)
        }
    }

    private lateinit var binding:ActivityNewNotificationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)

        binding = ActivityNewNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)
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
                is NotificationViewState.LoadingState -> {
                    binding.progressbarInfo.visibility = if(it.isLoading) View.VISIBLE else View.GONE
                }
                is NotificationViewState.GetAllNotificationInfo -> {
                    activityNotificationAdapter.listOfDataItems = it.notificationInfoList
                    RxBus.publish(RxEvent.UpdateNotificationBadge(false))
                    hideShowNoData(it.notificationInfoList)
                }
            }
        }.autoDispose()
    }

    private fun listenToViewEvents() {
        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressedDispatcher.onBackPressed()
        }.autoDispose()

        activityNotificationAdapter = ActivityNotificationAdapter(this@NewNotificationActivity)
        activityNotificationAdapter.apply {
            notificationActionState.subscribeAndObserveOnMainThread { state ->
                when (state) {
                    is NotificationActionState.UpdateReadStatus -> {
                        notificationViewModel.updateNotificationReadStatus(state.notificationInfo.id)
                    }
                    is NotificationActionState.RowViewClick -> {
                        val notificationType = state.notificationInfo.notificationType
                        val postReelId = state.notificationInfo.postReelId
                        if (!notificationType.isNullOrEmpty()) {
                            val intent = when (notificationType) {
                                //EVENT
                                NotificationService.N_TYPE_EVENT_REQUEST, NotificationService.N_TYPE_EVENT_REQUEST_ACCEPTED, NotificationService.N_TYPE_EVENT_REQUEST_REJECTED -> {
                                    VenueEventDetailActivity.getIntentWithId(this@NewNotificationActivity, state.notificationInfo.id)
                                }
                                //Follow
                                NotificationService.N_TYPE_FOLLOW -> {
                                    if (state.notificationInfo.sender?.userType == MapVenueUserType.VENUE_OWNER.type) {
                                        NewVenueDetailActivity.getIntent(this@NewNotificationActivity, 0, state.notificationInfo.sender.id
                                            ?: 0)
                                    } else {
                                        NewOtherUserProfileActivity.getIntent(this@NewNotificationActivity, state.notificationInfo.sender?.id ?: 0)
                                    }
                                }
                                //Post
                                NotificationService.N_TYPE_POST_LIKED -> {
                                    PostDetailActivity.getIntent(this@NewNotificationActivity, postReelId ?: 0)
                                }
                                NotificationService.N_TYPE_POST_COMMENT, NotificationService.N_TYPE_POST_COMMENT_LIKED, NotificationService.N_TYPE_POST_COMMENT_REPLY -> {
                                    PostDetailActivity.getIntent(this@NewNotificationActivity, postReelId ?: 0, showComments = true)
                                }
                                NotificationService.N_TYPE_POST_TAG_POST -> {
                                    PostDetailActivity.getIntent(this@NewNotificationActivity, postReelId ?: 0, showTaggedPeople = true)
                                }
                                //Reel
                                NotificationService.N_TYPE_REEL_LIKED -> {
                                    ReelsDetailActivity.getIntent(this@NewNotificationActivity, postReelId ?: 0)
                                }
                                NotificationService.N_TYPE_REEL_COMMENT, NotificationService.N_TYPE_REEL_COMMENT_LIKED, NotificationService.N_TYPE_REEL_COMMENT_REPLY -> {
                                    ReelsDetailActivity.getIntent(this@NewNotificationActivity, postReelId ?: 0, showComments = true)
                                }
                                NotificationService.N_TYPE_REEL_TAG_REEL -> {
                                    ReelsDetailActivity.getIntent(this@NewNotificationActivity, postReelId ?: 0, showTaggedPeople = true)
                                }
                                NotificationService.N_TYPE_SPONTY_JOINED -> {
                                    SpontyDetailsActivity.getIntent(this@NewNotificationActivity, postReelId ?: 0)
                                }
                                NotificationService.N_TYPE_SPONTY_LIKED -> {
                                    SpontyDetailsActivity.getIntent(this@NewNotificationActivity, postReelId ?: 0)
                                }
                                NotificationService.N_TYPE_SPONTY_COMMENT -> {
                                    SpontyDetailsActivity.getIntent(this@NewNotificationActivity, postReelId ?: 0,true)
                                }
                                else -> {
                                    null
                                }
                            }

                            if (intent != null) {
                                startActivityWithDefaultAnimation(intent)
                            }
                        }
                    }
                    is NotificationActionState.UserProfileClick -> {
                        if(state.notificationInfo.sender?.userType == MapVenueUserType.VENUE_OWNER.type) {
                            startActivityWithDefaultAnimation(
                                NewVenueDetailActivity.getIntent(
                                    this@NewNotificationActivity,
                                    0,
                                    state.notificationInfo.sender.id ?: 0
                                )
                            )
                        } else {
                            startActivityWithDefaultAnimation(
                                NewOtherUserProfileActivity.getIntent(
                                    this@NewNotificationActivity,
                                    state.notificationInfo.sender?.id ?: 0
                                )
                            )
                        }
                    }
                }
            }.autoDispose()
        }

        binding.rvActivityNotification.apply {
            layoutManager = LinearLayoutManager(this@NewNotificationActivity, RecyclerView.VERTICAL, false)
            adapter = activityNotificationAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                notificationViewModel.loadMore()
                            }
                        }
                    }
                }
            })
        }

        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            binding.swipeRefreshLayout.isRefreshing = false
            notificationViewModel.pullToRefresh(false)
        }.autoDispose()

        notificationViewModel.pullToRefresh(true)
    }

    private fun hideShowNoData(notificationInfoList: List<NotificationInfo>) {
        if (notificationInfoList.isNotEmpty()) {
            binding.llNoData.visibility = View.GONE
        } else {
            binding.llNoData.visibility = View.VISIBLE
        }
    }
}