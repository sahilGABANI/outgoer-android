package com.outgoer.ui.group.create

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.widget.editorActions
import com.jakewharton.rxbinding3.widget.textChanges
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.group.model.GroupUserInfo
import com.outgoer.api.group.model.ManageGroupRequest
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityCreateGroupBinding
import com.outgoer.ui.followdetail.viewmodel.FollowingViewModel
import com.outgoer.ui.followdetail.viewmodel.FollowingViewState
import com.outgoer.ui.group.view.GroupAdapter
import com.outgoer.ui.group.view.SelectedGroupAdapter
import com.outgoer.utils.SnackBarUtils
import com.outgoer.utils.UiUtils
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class CreateGroupActivity : BaseActivity() {

    private lateinit var binding: ActivityCreateGroupBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<FollowingViewModel>
    private lateinit var followingViewModel: FollowingViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    companion object {

        val ADD_NEW_MEMBER = "ADD_NEW_MEMBER"
        val CONVERSATION_ID = "CONVERSATION_ID"
        val EXISTING_USERS = "EXISTING_USERS"
        fun getIntent(context: Context): Intent {
            return Intent(context, CreateGroupActivity::class.java)
        }

        fun getIntentWithData(context: Context, isAddMembers: Boolean, conversationId: Int, groupUserInfo: ArrayList<GroupUserInfo>): Intent {
            val intent = Intent(context, CreateGroupActivity::class.java)
            intent.putExtra(ADD_NEW_MEMBER, isAddMembers)
            intent.putExtra(CONVERSATION_ID, conversationId)
            intent.putExtra(EXISTING_USERS, groupUserInfo)

            return intent
        }
    }

    private var isAddMembers: Boolean = false
    private var conversationId: Int = 0

    private lateinit var groupAdapter: GroupAdapter
    private lateinit var selectedGroupAdapter: SelectedGroupAdapter
    private var listofuser: ArrayList<FollowUser> = arrayListOf()
    private var listofgroupuser: ArrayList<GroupUserInfo> = arrayListOf()
    private var selectedlistofuser: ArrayList<FollowUser> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)

        binding = ActivityCreateGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        followingViewModel = getViewModelFromFactory(viewModelFactory)

        initUI()
        listenToViewModel()
    }

    private fun initUI() {

        intent?.let {
            isAddMembers = it.getBooleanExtra(ADD_NEW_MEMBER, false)
            conversationId = it.getIntExtra(CONVERSATION_ID, 0)
            if(it.hasExtra(EXISTING_USERS)) {
                listofgroupuser = it.getParcelableArrayListExtra<GroupUserInfo>(EXISTING_USERS) as ArrayList<GroupUserInfo>
            }
        }

        if(isAddMembers) {
            binding.titleAppCompatTextView.setText(resources.getString(R.string.label_add_new_participants))
        } else {
            binding.titleAppCompatTextView.setText(resources.getString(R.string.label_create_new_group))
        }


        binding.searchAppCompatEditText.editorActions()
            .filter { action -> action == EditorInfo.IME_ACTION_SEARCH }
            .subscribeAndObserveOnMainThread {
                UiUtils.hideKeyboard(this@CreateGroupActivity)
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
                if (it.length > 2) {
                    followingViewModel.searchFollowingList(loggedInUserCache.getUserId() ?: 0, it.toString())
                } else {
                    followingViewModel.resetPagination(loggedInUserCache.getUserId() ?: 0)
                }
               // UiUtils.hideKeyboard(this@CreateGroupActivity)
            }, {
                Timber.e(it)
            }).autoDispose()
        binding.ivClear.throttleClicks().subscribeAndObserveOnMainThread {
            UiUtils.hideKeyboard(this@CreateGroupActivity)
            binding.searchAppCompatEditText.setText("")
        }.autoDispose()
        binding.backAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }

        followingViewModel.resetPagination(loggedInUserCache.getUserId() ?: 0)
        binding.continueMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            if(isAddMembers) {
                var selectedUser: ArrayList<Int> = arrayListOf()
                selectedlistofuser.forEach {
                    selectedUser.add(it.id)
                }

                followingViewModel.addGroupUser(ManageGroupRequest(
                    conversationId = conversationId,
                    userIds = selectedUser.joinToString(",")
                ))
            } else {
                if(!selectedlistofuser.isNullOrEmpty()) {
                    finish()
                    startActivity(
                        AddGroupActivity.getIntentWithData(
                            this@CreateGroupActivity,
                            selectedlistofuser
                        )
                    )
                } else {
                    showToast(getString(R.string.label_select_participants))
                }
            }
        }

        binding.backAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }

        groupAdapter = GroupAdapter(this@CreateGroupActivity).apply {
            groupItemClick.subscribeAndObserveOnMainThread { user ->

                if (selectedlistofuser.firstOrNull { it.id == user.id } == null) {

                    selectedlistofuser.add(user)
                    selectedGroupAdapter.listOfUsers = selectedlistofuser

                    if (selectedlistofuser.size > 0) {
                        binding.selectedAppCompatTextView.visibility = View.VISIBLE
                        binding.selectedAppCompatTextView.text =
                            "${(listofuser.filter { it.isSelected == true }).size + 1} ${resources.getString(R.string.label_selected_items_new)}"
                    }
                }

                binding.selectedRecyclerView.visibility = if (listofuser.size > 0) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

                listofuser.find { it.id == user.id }?.apply {
                    isSelected = true
                }

                groupAdapter.listOfUsers = listofuser
            }
        }

        binding.listOfUsersRecyclerView.apply {
            adapter = groupAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                followingViewModel.loadMoreFollowingList(
                                    loggedInUserCache.getUserId() ?: 0
                                )
                            }
                        }
                    }
                }
            })
        }

        selectedGroupAdapter = SelectedGroupAdapter(this@CreateGroupActivity).apply {
            removeItemClick.subscribeAndObserveOnMainThread { user ->
                selectedlistofuser.remove(user)
                selectedGroupAdapter.listOfUsers = selectedlistofuser

                listofuser.find { it.id == user.id }?.apply {
                    isSelected = false
                }

                groupAdapter.listOfUsers = listofuser

                if (selectedlistofuser.size == 0) {
                    binding.selectedAppCompatTextView.visibility = View.GONE
                } else {
                    binding.selectedAppCompatTextView.visibility = View.VISIBLE
                    binding.selectedAppCompatTextView.text =
                        "${selectedlistofuser.size} ${resources.getString(R.string.label_selected_items_new)}"
                }
            }
        }

        binding.selectedRecyclerView.apply {
            adapter = selectedGroupAdapter
        }
    }


    fun listenToViewModel() {
        followingViewModel.followingViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is FollowingViewState.GroupLoadingState -> {
                    binding.progressBar.visibility = if(it.isLoading) View.VISIBLE else View.GONE
                    binding.continueMaterialButton.visibility = if(!it.isLoading) View.VISIBLE else View.GONE
                }
                is FollowingViewState.ErrorMessage -> {
                    Timber.tag("ErrorMessage").e("FollowingViewState -> it.errorMessage: ${it.ErrorMessage}")
                    if (it.ErrorMessage.startsWith("Unable to resolve host")) {
                        SnackBarUtils.showTopSnackBar(findViewById(android.R.id.content))
                    } else {
                        showLongToast(it.ErrorMessage)
                    }
                }
                is FollowingViewState.FollowingList -> {
                    listofuser = it.listOfFollowing as ArrayList<FollowUser>

                    listofgroupuser.forEach { user ->
                        listofuser.find { it.id == user.userId }?.apply {
                            isSelected = true
                        }
                    }

                    groupAdapter.listOfUsers = listofuser
                }
                is FollowingViewState.AddUserToSuccessMessage -> {
                    showToast(it.successMessage)
                    val intent = Intent().apply {
                        putExtra("selectedUser", selectedlistofuser)
                    }
                    setResult(Activity.RESULT_OK, intent)

                    finish()
                }

                else -> {}
            }
        }.autoDispose()
    }
}