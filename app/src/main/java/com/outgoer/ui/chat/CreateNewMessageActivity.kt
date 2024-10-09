package com.outgoer.ui.chat

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.jakewharton.rxbinding3.widget.editorActions
import com.jakewharton.rxbinding3.widget.textChanges
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.chat.model.ChatConversationInfo
import com.outgoer.api.profile.model.LocationUpdateRequest
import com.outgoer.api.profile.model.NearByUserResponse
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
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivityCreateNewMessageBinding
import com.outgoer.ui.chat.newview.FollowingListAdapter
import com.outgoer.ui.chat.newview.NewMessageFollowingAdapter
import com.outgoer.ui.chat.viewmodel.CreateMessageViewState
import com.outgoer.ui.chat.viewmodel.CreateNewMessageViewModel
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import com.outgoer.utils.SnackBarUtils
import com.outgoer.utils.UiUtils
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class CreateNewMessageActivity : BaseActivity() {

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, CreateNewMessageActivity::class.java)
        }
    }

    private lateinit var binding: ActivityCreateNewMessageBinding
    private lateinit var newMessageFollowingAdapter: NewMessageFollowingAdapter
    private lateinit var followingListAdapter: FollowingListAdapter
    private lateinit var searchListAdapter: FollowingListAdapter

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<CreateNewMessageViewModel>
    private lateinit var createNewMessageViewModel: CreateNewMessageViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var otherOutgoerUser: NearByUserResponse? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)
        binding = ActivityCreateNewMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createNewMessageViewModel = getViewModelFromFactory(viewModelFactory)
        initUI()
        listenToViewEvents()
        listenToViewModel()
    }

    private fun listenToViewEvents() {

        binding.searchAppCompatEditText.editorActions()
            .filter { action -> action == EditorInfo.IME_ACTION_SEARCH }
            .subscribeAndObserveOnMainThread {
//                UiUtils.hideKeyboard(this@CreateNewMessageActivity)
            }.autoDispose()

        binding.searchAppCompatEditText.textChanges()
            .skipInitialValue()
            .doOnNext {
                if (it.isNullOrEmpty()) {
                    binding.searchRecyclerView.visibility = View.GONE
                    binding.nearByRecyclerView.visibility = View.VISIBLE
                    binding.nearByAppCompatTextView.visibility = View.VISIBLE
                    binding.followingRecyclerView.visibility = View.VISIBLE
                    binding.followingTitleAppCompatTextView.visibility = View.VISIBLE
                } else {
                    binding.searchRecyclerView.visibility = View.VISIBLE
                    binding.nearByRecyclerView.visibility = View.GONE
                    binding.nearByAppCompatTextView.visibility = View.GONE
                    binding.followingRecyclerView.visibility = View.GONE
                    binding.followingTitleAppCompatTextView.visibility = View.GONE

                }
            }
            .debounce(300, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeOnIoAndObserveOnMainThread({
                if (it.length > 2) {
                    createNewMessageViewModel.searchText(it.toString())
                }
//                UiUtils.hideKeyboard(this@CreateNewMessageActivity)
            }, {
                Timber.e(it)
            }).autoDispose()


        binding.ivClear.throttleClicks().subscribeAndObserveOnMainThread {
            UiUtils.hideKeyboard(this)
            binding.searchAppCompatEditText.setText("")
        }.autoDispose()

        newMessageFollowingAdapter = NewMessageFollowingAdapter(this).apply {
            profileItemClickState.subscribeAndObserveOnMainThread {
                otherOutgoerUser = it
                if (it.userType == MapVenueUserType.VENUE_OWNER.type) {
                    if (loggedInUserCache.getUserId() == it.userId) {
                        RxBus.publish(RxEvent.OpenVenueUserProfile)
                    } else {
                        startActivityWithDefaultAnimation(
                            NewVenueDetailActivity.getIntent(
                                this@CreateNewMessageActivity,
                                0,
                                it.userId ?: 0
                            )
                        )
                    }
                } else {
                    //startActivityWithDefaultAnimation(NewOtherUserProfileActivity.getIntent(this@CreateNewMessageActivity, it.userId ?: 0))
                    createNewMessageViewModel.getConversation(it.userId ?: 0)
                }
            }
            followClickState.subscribeAndObserveOnMainThread {
                val listoffollowing = newMessageFollowingAdapter.chatUserList

                var index = listoffollowing?.indexOf(it) ?: 0
                listoffollowing?.get(index)?.followStatus = 1

                newMessageFollowingAdapter.chatUserList = listoffollowing
                createNewMessageViewModel.acceptRejectFollowRequest(it.userId)
            }
        }

        binding.nearByRecyclerView.apply {
            adapter = newMessageFollowingAdapter
        }

        followingListAdapter = FollowingListAdapter(this).apply {
            profileItemClickState.subscribeAndObserveOnMainThread {
                otherOutgoerUser = NearByUserResponse(
                    id = it.id,
                    userId = it.id,
                    username = it.username,
                    avatar = it.avatar,
                )
                if (it.userType == MapVenueUserType.VENUE_OWNER.type) {
                    if (loggedInUserCache.getUserId() == it.id) {
                        RxBus.publish(RxEvent.OpenVenueUserProfile)
                    } else {
                        startActivityWithDefaultAnimation(
                            NewVenueDetailActivity.getIntent(
                                this@CreateNewMessageActivity,
                                0,
                                it.id ?: 0
                            )
                        )
                    }
                } else {
                    createNewMessageViewModel.getConversation(it.id ?: 0)
                }
            }
        }

        searchListAdapter = FollowingListAdapter(this).apply {
            profileItemClickState.subscribeAndObserveOnMainThread {
                if (it.userType == MapVenueUserType.VENUE_OWNER.type) {
                    if (loggedInUserCache.getUserId() == it.id) {
                        RxBus.publish(RxEvent.OpenVenueUserProfile)
                    } else {
                        startActivityWithDefaultAnimation(
                            NewVenueDetailActivity.getIntent(
                                this@CreateNewMessageActivity,
                                0,
                                it.id ?: 0
                            )
                        )
                    }
                } else {
                    startActivityWithDefaultAnimation(
                        NewOtherUserProfileActivity.getIntent(
                            this@CreateNewMessageActivity,
                            it.id
                        )
                    )
                }
            }.autoDispose()
        }


        binding.searchRecyclerView.apply {
            adapter = searchListAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, state: Int) {
                    super.onScrollStateChanged(recyclerView, state)
                    if (state == RecyclerView.SCROLL_STATE_IDLE) {
                        val layoutManager = recyclerView.layoutManager ?: return
                        var lastVisibleItemPosition = 0
                        if (layoutManager is GridLayoutManager) {
                            lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                        }
                        val adjAdapterItemCount = layoutManager.itemCount
                        if (layoutManager.childCount > 0 && adjAdapterItemCount >= layoutManager.childCount) {
                            createNewMessageViewModel.loadMoreSearchAccount()
                        }
                    }
                }
            })
        }

        binding.followingRecyclerView.apply {
            adapter = followingListAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, state: Int) {
                    super.onScrollStateChanged(recyclerView, state)
                    if (state == RecyclerView.SCROLL_STATE_IDLE) {
                        val layoutManager = recyclerView.layoutManager ?: return
                        var lastVisibleItemPosition = 0
                        if (layoutManager is GridLayoutManager) {
                            lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                        }
                        val adjAdapterItemCount = layoutManager.itemCount
                        if (layoutManager.childCount > 0 && adjAdapterItemCount >= layoutManager.childCount) {
                            createNewMessageViewModel.loadMoreFollowingList(
                                loggedInUserCache.getUserId() ?: 0
                            )
                        }
                    }
                }
            })
        }
    }

    private fun initUI() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkLocationPermission()
        createNewMessageViewModel.searchFollowingList(loggedInUserCache.getUserId() ?: 0, "")
        binding.backAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }


    }

    private fun checkLocationPermission() {
        try {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        Timber.tag("<><>").e(
                            location.latitude.toString().plus(", ")
                                .plus(location.longitude.toString())
                        )
                        createNewMessageViewModel.loadNearByUser(
                            LocationUpdateRequest(
                                longitude = location.longitude.toString(),
                                latitude = location.latitude.toString()
                            )
                        )

                    }
                }.addOnFailureListener { exception ->
                    exception.localizedMessage?.let {
                        showLongToast(it)
                    }
                }
            }
        } catch (e: Exception) {
            e.localizedMessage?.let {
                showLongToast(it)
            }
        }
    }

    private fun listenToViewModel() {
        createNewMessageViewModel.createNewMessageState.subscribeAndObserveOnMainThread {
            when (it) {
                is CreateMessageViewState.ErrorMessage -> {
                    Timber.tag("ErrorMessage").e("CreateMessageViewState -> it.errorMessage: ${it.errorMessage}")
                    if (it.errorMessage.startsWith("Unable to resolve host")) {
                        SnackBarUtils.showTopSnackBar(findViewById(android.R.id.content))
                    } else {
                        showLongToast(it.errorMessage)
                    }
                }

                is CreateMessageViewState.SuccessMessage -> {
                    showLongToast(it.successMessage)
                }

                is CreateMessageViewState.LoadingState -> {

                }

                is CreateMessageViewState.FollowingList -> {
                    binding.followingTitleAppCompatTextView.visibility =
                        if (it.listOfFollowing.size == 0) View.GONE else View.VISIBLE

                    followingListAdapter.chatUserList = it.listOfFollowing
                }

                is CreateMessageViewState.LoadNearByUserList -> {
                    newMessageFollowingAdapter.chatUserList = it.listOfSuggestedUser
                }

                is CreateMessageViewState.SearchAccountList -> {
                    searchListAdapter.chatUserList = it.listOfSearchAccountData
                }

                is CreateMessageViewState.GetConversation -> {
                    startActivityWithDefaultAnimation(
                        NewChatActivity.getIntent(
                            this,
                            ChatConversationInfo(
                                conversationId = it.conversationId,
                                senderId = loggedInUserCache.getUserId() ?: 0,
                                receiverId = otherOutgoerUser?.id ?: 0,
                                name = otherOutgoerUser?.username,
                                email = "",
                                profileUrl = otherOutgoerUser?.avatar,
                                createdAt = "",
                                unreadCount = 0,
                                lastMessage = "",
                                fileType = null,
                                chatType = resources.getString(R.string.label_chat),
                            )
                        )
                    )
                }
            }
        }.autoDispose()
    }

}