package com.outgoer.ui.home.chat

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.jakewharton.rxbinding3.widget.editorActions
import com.jakewharton.rxbinding3.widget.textChanges
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.chat.model.ChatConversationActionState
import com.outgoer.api.chat.model.ChatConversationInfo
import com.outgoer.api.profile.model.LocationUpdateRequest
import com.outgoer.api.profile.model.NearByUserResponse
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityNewChatConversationBinding
import com.outgoer.ui.chat.CreateNewMessageActivity
import com.outgoer.ui.chat.NewChatActivity
import com.outgoer.ui.group.create.CreateGroupActivity
import com.outgoer.ui.home.chat.view.NearbyPeopleAdapter
import com.outgoer.ui.home.chat.view.NewChatUserListAdapter
import com.outgoer.ui.home.chat.view.NewFindFriendAdapter
import com.outgoer.ui.home.chat.viewmodel.ChatMessageViewState
import com.outgoer.ui.home.chat.viewmodel.ConversationViewModel
import com.outgoer.ui.home.newmap.venuemap.bottomsheet.CastMessagingBottomSheet
import com.outgoer.ui.home.newmap.venuemap.bottomsheet.CastMessagingDisplayBottomSheet
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import com.outgoer.utils.SnackBarUtils
import com.outgoer.utils.UiUtils
import com.outgoer.utils.Utility.storyListUtil
import com.outgoer.utils.Utility.toggleSelectedStory
import com.tutorialsbuzz.halfswipe.SwipeHelper
import com.tutorialsbuzz.halfswipe.SwipeHelper.UnderlayButtonClickListener
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class NewChatConversationActivity : BaseActivity() {
    private lateinit var binding: ActivityNewChatConversationBinding

    private lateinit var newChatUserListAdapter: NewChatUserListAdapter
    private lateinit var nearbyPeopleAdapter: NearbyPeopleAdapter
    private lateinit var newFindFriendAdapter: NewFindFriendAdapter

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ConversationViewModel>
    private lateinit var conversationViewModel: ConversationViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var conversationList: MutableList<ChatConversationInfo> = mutableListOf()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    val nearBroadCastList: ArrayList<NearByUserResponse> = arrayListOf()
    private var search = false

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, NewChatConversationActivity::class.java)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewChatConversationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        OutgoerApplication.component.inject(this)
        conversationViewModel = getViewModelFromFactory(viewModelFactory)
        search = false
        listenToViewModel()
        listenToViewEvents()
        initUI()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this@NewChatConversationActivity)
        checkLocationPermission()
        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            search = false
            binding.searchAppCompatEditText.setText("")
            binding.swipeRefreshLayout.isRefreshing = false
        }.autoDispose()
        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
           finish()
        }.autoDispose()
    }

    private fun initUI() {

        binding.loadMoreConversationList.throttleClicks().subscribeAndObserveOnMainThread {
            conversationViewModel.loadMoreConversationList(binding.searchAppCompatEditText.text.toString())
        }

        binding.loadMorefriendsList.throttleClicks().subscribeAndObserveOnMainThread {
            conversationViewModel.loadMore()
        }

        nearbyPeopleAdapter = NearbyPeopleAdapter(this@NewChatConversationActivity).apply {
            nearbyPeopleItemClickState.subscribeAndObserveOnMainThread {data ->
                if (data.storyCount == 1) {
                    chatUserList?.find { it.userId == data.userId }?.storyCount = 0
                    nearbyPeopleAdapter.chatUserList = chatUserList
                    toggleSelectedStory(
                        this@NewChatConversationActivity,
                        storyListUtil,
                        data.userId
                    )
                } else if(data.userType == MapVenueUserType.VENUE_OWNER.type) {
                    if(loggedInUserCache.getUserId() == data.userId) {
                        RxBus.publish(RxEvent.OpenVenueUserProfile)
                    }else {
                        startActivityWithDefaultAnimation(NewVenueDetailActivity.getIntent(this@NewChatConversationActivity,0,data.userId ?: 0))
                    }
                } else {
                    if(loggedInUserCache.getUserId() != data.userId) {
                        startActivityWithDefaultAnimation(
                            NewOtherUserProfileActivity.getIntent(
                                this@NewChatConversationActivity,
                                data.userId
                            )
                        )
                    }
                }
            }

            castMessageClickState.subscribeAndObserveOnMainThread {
                if (it.userId == loggedInUserCache.getUserId()) {
                    val castMessagingBottomSheet =
                        CastMessagingBottomSheet.newInstance(it.broadcastMessage)
                    castMessagingBottomSheet.apply {
                        dismissClick.subscribeAndObserveOnMainThread { str ->
                            val items = nearbyPeopleAdapter.chatUserList ?: arrayListOf()
                            val index = items.indexOf(it)
                            if (index != -1) {
                                items[index].broadcastMessage = str
                                nearbyPeopleAdapter.chatUserList = items
                            } else {
                                items[0].broadcastMessage = str
                                nearbyPeopleAdapter.chatUserList = items
                            }
                        }
                    }

                    castMessagingBottomSheet.show(
                        supportFragmentManager, CastMessagingBottomSheet.Companion::class.java.name
                    )
                } else if (!it.broadcastMessage.isNullOrEmpty()) {
                    val castMessagingBottomSheet = CastMessagingDisplayBottomSheet.newInstance(it)
                    castMessagingBottomSheet.apply {
                        dismissClick.subscribeAndObserveOnMainThread { str ->
                            val items = nearbyPeopleAdapter.chatUserList ?: arrayListOf()
                            val index = items.indexOf(it)
                            items.get(index).broadcastMessage = str
                            nearbyPeopleAdapter.chatUserList = items
                        }
                    }

                    castMessagingBottomSheet.show(
                        supportFragmentManager, CastMessagingBottomSheet.Companion::class.java.name
                    )
                }
            }
        }

        newFindFriendAdapter = NewFindFriendAdapter(this@NewChatConversationActivity).apply {
            followClickState.subscribeAndObserveOnMainThread {
                val listofsuggested = newFindFriendAdapter.chatUserList
                val mPos = listofsuggested?.indexOfFirst { followUser ->
                    followUser.id == it.id
                }

                if (mPos != -1) {
                    listofsuggested?.get(mPos?:0)?.followStatus = 1
                    listofsuggested?.get(mPos?:0)?.totalFollowers = it.totalFollowers?.let { it + 1 } ?: 0
                    newFindFriendAdapter.chatUserList = listofsuggested
                }

                conversationViewModel.followUnfollowUser(it)
            }

            followingClickState.subscribeAndObserveOnMainThread {
                val listofsuggested = newFindFriendAdapter.chatUserList
                val mPos = listofsuggested?.indexOfFirst { followUser ->
                    followUser.id == it.id
                }

                if (mPos != -1) {
                    listofsuggested?.get(mPos?:0)?.followStatus = 0
                    listofsuggested?.get(mPos?:0)?.totalFollowers = it.totalFollowers?.let { it - 1 } ?: 0
                    newFindFriendAdapter.chatUserList = listofsuggested
                }

                conversationViewModel.followUnfollowUser(it)
            }

            findFriendItemClickState.subscribeAndObserveOnMainThread {
                if(it.userType.equals(MapVenueUserType.VENUE_OWNER.type)) {
                    if(loggedInUserCache.getUserId() == it.id ) {
                        RxBus.publish(RxEvent.OpenVenueUserProfile)
                    }else {
                        startActivityWithDefaultAnimation(NewVenueDetailActivity.getIntent(this@NewChatConversationActivity,0,it.id ?: 0))
                    }

                } else {
                    startActivityWithDefaultAnimation(NewOtherUserProfileActivity.getIntent(this@NewChatConversationActivity, it.id))
                }
            }
        }
        binding.rvNearbyPeopleList.apply {
            adapter = nearbyPeopleAdapter
        }

        binding.rvFindFriend.apply {
            adapter = newFindFriendAdapter
        }
    }

    private fun listenToViewModel() {
        conversationViewModel.messageViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is ChatMessageViewState.ErrorMessage -> {
                    Timber.tag("ErrorMessage").e("ChatMessageViewState -> it.errorMessage: ${it.errorMessage}")
                    if (it.errorMessage.startsWith("Unable to resolve host")) {
                        SnackBarUtils.showTopSnackBar(findViewById(android.R.id.content))
                    } else {
                        showLongToast(it.errorMessage)
                    }
                }
                is ChatMessageViewState.DoneLoading -> {
                    binding.loadMorefriendsList.visibility = View.GONE
                }
                is ChatMessageViewState.LoadConversationDetail -> {
                    conversationList = it.conversationList.toMutableList()
                    newChatUserListAdapter.chatUserList = conversationList as ArrayList<ChatConversationInfo>
                    hideShowNoData()
                }
                is ChatMessageViewState.LoadGroupConversationDetail -> {


                   /* binding.loadMoreConversationList.visibility = if(it.conversationList.size == 10) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }*/
                    conversationList = it.conversationList.toMutableList()
                    newChatUserListAdapter.chatUserList = (if(conversationList.size > 1) conversationList else conversationList) as ArrayList<ChatConversationInfo>
                    hideShowNoData()
                }
                is ChatMessageViewState.SuccessMessage -> {
                    showLongToast(it.successMessage)
                }
                is ChatMessageViewState.LoadMessageDetail -> {

                }
                is ChatMessageViewState.LoadingState -> {

                }
                is ChatMessageViewState.OtherNewMessages -> {
                    val mPos = conversationList.indexOfFirst { mInfo ->
                        mInfo.conversationId == it.chatConversationInfo.conversationId
                    }
                    if (mPos != -1) {
                        conversationList[mPos].createdAt = it.chatConversationInfo.createdAt
                        conversationList[mPos].conversationUpdatedAt = it.chatConversationInfo.conversationUpdatedAt
                        conversationList[mPos].unreadCount = it.chatConversationInfo.unreadCount
                        conversationList[mPos].lastMessage = it.chatConversationInfo.lastMessage
                        conversationList[mPos].fileType = it.chatConversationInfo.fileType

                        var chatInfo = conversationList[mPos]
                        conversationList.removeAt(mPos)
                        conversationList.add(0, chatInfo)
//                        conversationList.sortByDescending { chatConInfo ->
//                            chatConInfo.createdAt
//                        }

                        newChatUserListAdapter.chatUserList = conversationList as ArrayList<ChatConversationInfo>
                    }
                }
                is ChatMessageViewState.LoadSuggestedUserList -> {
                    newFindFriendAdapter.chatUserList = if(conversationList.size > 2) it.listOfSuggestedUser.distinct() else it.listOfSuggestedUser


//                    binding.loadMorefriendsList.visibility = View.VISIBLE

                }
                is ChatMessageViewState.LoadNearByUserList -> {
                    if(it.listOfSuggestedUser.isNotEmpty()) {
                        binding.nearbyPeopleLinearLayout.visibility = View.VISIBLE
                        createStaticData()?.let { data -> nearBroadCastList.add(data) }
                        nearBroadCastList.addAll(it.listOfSuggestedUser)
                        nearbyPeopleAdapter.chatUserList = nearBroadCastList
                    } else {
                        binding.nearbyPeopleLinearLayout.visibility = View.GONE
                    }

                    /*binding.loadMorefriendsList.visibility = if(it.listOfSuggestedUser.size == 10) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }*/
                }
                is ChatMessageViewState.ClearDetailsNeedLogin -> {

                }
                is ChatMessageViewState.DeleteConversationInfo -> {
                    val info = newChatUserListAdapter.chatUserList
                    info?.remove(it.chatUserInfo)
                    newChatUserListAdapter.chatUserList = info
                }
            }
        }.autoDispose()
    }

    private fun createStaticData(): NearByUserResponse? {
        val loggedInUser = loggedInUserCache.getLoggedInUser()?.loggedInUser ?: return null

        return NearByUserResponse(
            loggedInUser.id,
            loggedInUser.id,
            loggedInUser.name.toString(),
            loggedInUser.username.toString(),
            loggedInUser.userType,
            loggedInUser.broadcastMessage,
            loggedInUser.about,
            loggedInUser.profileVerified,
            loggedInUser.avatar,
            loggedInUser.latitude,
            loggedInUser.longitude,
            null,
            loggedInUser.followStatus,
            loggedInUser.followingStatus,
            loggedInUser.postCount,
            loggedInUser.reelCount,
            loggedInUser.spontyCount,
            loggedInUser.isLive,
            loggedInUser.liveId
        )
    }

    private fun listenToViewEvents() {

        binding.findFriendAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
//            startActivity(SuggestedUsersActivity.getIntent(this@NewChatConversationActivity))
        }

        newChatUserListAdapter = NewChatUserListAdapter(this@NewChatConversationActivity)

        val swipeHelper = object : SwipeHelper(this@NewChatConversationActivity, binding.rvChatUserList, false) {

            override fun instantiateUnderlayButton(
                viewHolder: RecyclerView.ViewHolder?,
                underlayButtons: MutableList<UnderlayButton>?
            ) {

                underlayButtons?.add(
                    SwipeHelper.UnderlayButton(
                    resources.getString(R.string.delete),
                    AppCompatResources.getDrawable(
                        this@NewChatConversationActivity,
                        R.drawable.delete
                    ),
                    Color.parseColor("#FF0000"), Color.parseColor("#ffffff"),
                    UnderlayButtonClickListener { pos: Int ->
                        newChatUserListAdapter.chatUserList?.get(pos)?.let {
                            if(it.chatType.equals(resources.getString(R.string.label_group))) {
                                val builder = AlertDialog.Builder(ContextThemeWrapper(this@NewChatConversationActivity, R.style.AlertDialogCustom))
                                builder.setTitle(getString(R.string.label_delete_))
                                builder.setMessage(getString(R.string.label_delete_are_you_sure_you_want_to_delete_groupchat))
                                builder.setPositiveButton(getString(R.string.delete)) { dialogInterface, which ->
                                    conversationViewModel.deleteConversation(it)
                                    dialogInterface.dismiss()

                                }
                                builder.setNeutralButton(getString(R.string.label_cancel)) { dialogInterface, _ ->
                                    dialogInterface.dismiss()
                                }
                                val alertDialog: AlertDialog = builder.create()
                                alertDialog.setCancelable(false)
                                alertDialog.show()
                            } else {
                                val builder = AlertDialog.Builder(this@NewChatConversationActivity)
                                builder.setTitle(getString(R.string.label_delete_))
                                builder.setMessage(getString(R.string.label_delete_are_you_sure_you_want_to_delete_chat))
                                builder.setPositiveButton(getString(R.string.delete)) { dialogInterface, which ->
                                    conversationViewModel.deleteConversation(it)
                                    dialogInterface.dismiss()
                                }
                                builder.setNeutralButton(getString(R.string.label_cancel)) { dialogInterface, _ ->
                                    dialogInterface.dismiss()
                                }
                                val alertDialog: AlertDialog = builder.create()
                                alertDialog.setCancelable(false)
                                alertDialog.show()
                            }
                        }
                    }
                ))
            }
        }

        binding.rvChatUserList.apply {
            adapter = newChatUserListAdapter.apply {
                chatConversationActionState.subscribeAndObserveOnMainThread {
                    when (it) {
                        is ChatConversationActionState.ConversationClick -> {
                            startActivityWithDefaultAnimation(
                                NewChatActivity.getIntent(
                                    this@NewChatConversationActivity,
                                    it.chatConversationInfo
                                )
                            )
                        }

                        else -> {}
                    }
                }.autoDispose()
            }
            val scrollListener = object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (!recyclerView.canScrollVertically(1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                        conversationViewModel.loadMoreConversationList(binding.searchAppCompatEditText.text.toString())
                    }
                }
            }
            addOnScrollListener(scrollListener)
        }

        binding.ivCreateMessage.setOnClickListener {
            startActivityWithDefaultAnimation(CreateNewMessageActivity.getIntent(this@NewChatConversationActivity))
        }

        binding.btnCreateNew.setOnClickListener {
            startActivityWithDefaultAnimation(CreateNewMessageActivity.getIntent(this@NewChatConversationActivity))
        }

        binding.createGroupAppCompatTextView.setOnClickListener {
            startActivityWithDefaultAnimation(CreateGroupActivity.getIntent(this@NewChatConversationActivity))
        }

        binding.searchAppCompatEditText.editorActions()
            .filter { action -> action == EditorInfo.IME_ACTION_SEARCH }
            .subscribeAndObserveOnMainThread {
                UiUtils.hideKeyboard(this@NewChatConversationActivity)
            }.autoDispose()

        binding.searchAppCompatEditText.textChanges()
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

                if (it.length >= 2) {
                    conversationViewModel.resetPagination(it.toString())
//                    conversationViewModel.loadSuggestedUser(it.toString())
                } else if (it.isEmpty()) {
                    conversationViewModel.resetPagination("")
//                    conversationViewModel.loadSuggestedUser("")
                }
                //UiUtils.hideKeyboard(this@NewChatConversationActivity)
            }, {
                Timber.e(it)
            }).autoDispose()

        binding.ivClear.throttleClicks().subscribeAndObserveOnMainThread {
            UiUtils.hideKeyboard(this@NewChatConversationActivity)
            binding.searchAppCompatEditText.setText("")
        }.autoDispose()
    }

    private fun hideShowNoData() {
        if (conversationList.isNotEmpty()) {
            binding.llNoData.visibility = View.GONE
        } else {
            binding.llNoData.visibility = View.VISIBLE
        }
        if(!search) {
            binding.llSearch.isVisible = !binding.llNoData.isVisible
        }
        search = true
    }

    override fun onResume() {
        super.onResume()
        search = false
        conversationViewModel.resetPagination("")
//        conversationViewModel.loadSuggestedUser("")
    }

    private fun checkLocationPermission() {
        try {
            XXPermissions.with(this)
                .permission(Permission.ACCESS_COARSE_LOCATION)
                .request(object : OnPermissionCallback {

                    @SuppressLint("MissingPermission")
                    override fun onGranted(permissions: List<String>, all: Boolean) {
                        if (all) {
                            val task = fusedLocationClient.lastLocation
                            task.addOnSuccessListener { location ->
                                if (location != null) {
                                    Timber.tag("<><>").e(
                                        location.latitude.toString().plus(", ")
                                            .plus(location.longitude.toString())
                                    )
                                    conversationViewModel.loadNearByUser(
                                        LocationUpdateRequest(
                                            longitude = location.longitude.toString(),
                                            latitude = location.latitude.toString()
                                        )
                                    )

                                }
                            }
                        } else {
                            showToast(getString(R.string.msg_location_permission_required_for_venue))
                        }
                    }

                    override fun onDenied(permissions: List<String>, never: Boolean) {
                        showToast(getString(R.string.msg_location_permission_required_for_venue))
                    }
                })
        } catch (e: Exception) {
            e.localizedMessage?.let {
                showLongToast(it)
                Timber.e("checkLocationPermission -> e.localizedMessage: ${e.localizedMessage}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}