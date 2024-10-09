package com.outgoer.ui.block

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.post.model.BlockAccountPageState
import com.outgoer.api.profile.model.BlockUserRequest
import com.outgoer.api.profile.model.BlockUserResponse
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showToast
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivityBlockProfileBinding
import com.outgoer.ui.block.view.BlockAccountAdapter
import com.outgoer.ui.home.profile.viewmodel.ProfileViewModel
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import javax.inject.Inject

class BlockProfileActivity : BaseActivity() {

    private lateinit var binding: ActivityBlockProfileBinding

    private lateinit var blockAccountAdapter: BlockAccountAdapter

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ProfileViewModel>
    private lateinit var profileViewModel: ProfileViewModel

    private var blockUserList: ArrayList<BlockUserResponse> = arrayListOf()
    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, BlockProfileActivity::class.java)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)

        binding = ActivityBlockProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        profileViewModel = getViewModelFromFactory(viewModelFactory)
        initUI()
        listenToViewModel()
    }

    private fun listenToViewModel() {
        profileViewModel.profileViewStates.subscribeAndObserveOnMainThread {
            when(it) {
                is ProfileViewModel.ProfileViewState.LoadingState -> {
                    binding.progressBlockAccount.isVisible = it.isLoading
                }
                is ProfileViewModel.ProfileViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is ProfileViewModel.ProfileViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is ProfileViewModel.ProfileViewState.GetListOfBlockedUsers -> {
                    blockUserList = it.listOfBlockedUsers as ArrayList<BlockUserResponse>
                    blockAccountAdapter.listOfBlockAccount = blockUserList
                    if(blockUserList.isEmpty()){
                        binding.tvNoBlockUser.visibility = View.VISIBLE
                    } else {
                        binding.tvNoBlockUser.visibility = View.GONE
                    }
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun initUI() {
        profileViewModel.pullToRefreshBlockAccount()
        blockAccountAdapter = BlockAccountAdapter(this@BlockProfileActivity).apply {
            blockAccountViewClick.subscribeAndObserveOnMainThread {
                when(it) {
                    is BlockAccountPageState.UnblockAccountClick -> {
                        profileViewModel.blockUserProfile(BlockUserRequest(it.blockUserResponse.blockFor))

                        listOfBlockAccount?.remove(it.blockUserResponse)
                        blockAccountAdapter.listOfBlockAccount = listOfBlockAccount
                        if(listOfBlockAccount?.isEmpty() == true){
                            binding.tvNoBlockUser.visibility = View.VISIBLE
                        } else {
                            binding.tvNoBlockUser.visibility = View.GONE
                        }
                    }
                    is BlockAccountPageState.UserProfileClick -> {
                        startActivity(NewOtherUserProfileActivity.getIntent(this@BlockProfileActivity, it.blockUserResponse.blockFor))
                    }
                }
            }.autoDispose()
        }

        binding.blockedAccountRecyclerView.apply {
            adapter = blockAccountAdapter
            layoutManager = LinearLayoutManager(this@BlockProfileActivity, LinearLayoutManager.VERTICAL, false)

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, state: Int) {
                    super.onScrollStateChanged(recyclerView, state)
                    if (state == RecyclerView.SCROLL_STATE_IDLE) {
                        val layoutManager = recyclerView.layoutManager ?: return
                        val adjAdapterItemCount = layoutManager.itemCount
                        if (layoutManager.childCount > 0 && adjAdapterItemCount >= layoutManager.childCount) {
                            profileViewModel.loadMoreBlockAccount()
                        }
                    }
                }
            })
        }


        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            finish()
        }.autoDispose()
    }
}