package com.outgoer.ui.group.details

import android.R.attr.button
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.authentication.model.OutgoerUser
import com.outgoer.api.chat.model.ChatConversationInfo
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.group.model.CreateGroupRequest
import com.outgoer.api.group.model.GroupInfoResponse
import com.outgoer.api.group.model.GroupUserInfo
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showLongToast
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivityGroupDetailsBinding
import com.outgoer.ui.group.audio.AudioRecordBottomSheet
import com.outgoer.ui.group.create.CreateGroupActivity
import com.outgoer.ui.group.edit_profile.EditGroupActivity
import com.outgoer.ui.group.editgroup.EditAdminGroupBottomSheet
import com.outgoer.ui.group.editgroup.EditGroupBottomSheet
import com.outgoer.ui.group.view.GroupAdapter
import com.outgoer.ui.group.viewmodel.GroupViewModel
import javax.inject.Inject


class GroupDetailsActivity : BaseActivity() {

    private lateinit var binding: ActivityGroupDetailsBinding
    private lateinit var groupAdapter: GroupAdapter
    private var listofuser: ArrayList<FollowUser> = arrayListOf()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<GroupViewModel>
    private lateinit var groupViewModel: GroupViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var chatConversationInfo: ChatConversationInfo? = null
    companion object {
        val  REQUEST_CODE = 1230
        val  GROUP_ID = "GROUP_ID"
        fun getIntent(context: Context): Intent {
            return Intent(context, GroupDetailsActivity::class.java)
        }

        fun getIntentWithData(context: Context, chatConversationInfo: ChatConversationInfo): Intent {
            val intent = Intent(context, GroupDetailsActivity::class.java)
            intent.putExtra(GROUP_ID, chatConversationInfo)

            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)

        binding = ActivityGroupDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        groupViewModel = getViewModelFromFactory(viewModelFactory)

        initUI()
        listenToViewModel()
    }

    private fun initUI() {
        intent?.let {
            chatConversationInfo = it.getParcelableExtra(GROUP_ID)

            binding.progressBar.visibility = View.VISIBLE
            groupViewModel.getGroupInfo(chatConversationInfo?.conversationId ?: 0)
        }

        chatConversationInfo?.users?.find { it.userId.equals(loggedInUserCache.getLoggedInUser()?.loggedInUser?.id) }?.apply {
            binding.moreAppCompatImageView.visibility = if("1".equals(role)) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        binding.addParticipantsAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            startActivityForResult(CreateGroupActivity.getIntentWithData(this@GroupDetailsActivity, true, if(chatConversationInfo?.conversationId != 0) chatConversationInfo?.conversationId ?: 0 else chatConversationInfo?.id ?: 0, chatConversationInfo?.users?: arrayListOf()), REQUEST_CODE)
        }

        groupAdapter = GroupAdapter(this@GroupDetailsActivity).apply {
            groupItemClick.throttleClicks().subscribeAndObserveOnMainThread {
                var audioRecordBottomSheet = AudioRecordBottomSheet.newInstance()
                audioRecordBottomSheet.show(supportFragmentManager, AudioRecordBottomSheet.TAG)
            }
            groupItemClicked.throttleClicks().subscribeAndObserveOnMainThread {
                chatConversationInfo?.let { info ->

                    var users = info.users?.find { it1 -> (it1.userId == loggedInUserCache.getLoggedInUser()?.loggedInUser?.id && it1.role == "1") }

                    if((!it.userId.equals(chatConversationInfo?.senderId)) && it.role == "1" && (users != null)) {
                        var editGroupBottomSheet = EditAdminGroupBottomSheet.newInstanceWithData(info, it.userId)
                        editGroupBottomSheet.removeClicked.subscribeAndObserveOnMainThread { text ->

                            if(text.equals(resources.getString(R.string.edit))) {
                                var list = groupAdapter.listOfGroupUsers
                                list?.find { it1 -> it1 == it }?.apply {
                                    role = "0"
                                }
                                groupAdapter.listOfGroupUsers = list
                            } else if(text.equals(resources.getString(R.string.label_remove))) {
                                var list = groupAdapter.listOfGroupUsers
                                list?.remove(it)
                                groupAdapter.listOfGroupUsers = list
                            }

                            binding.participantsCountAppCompatTextView.text = (groupAdapter.listOfGroupUsers?.size ?: 0).toString().plus(" ").plus(resources.getString(R.string.label_participants_postfix))
                        }

                        editGroupBottomSheet.show(supportFragmentManager, EditGroupBottomSheet.TAG)

                    } else {
                        chatConversationInfo?.users?.filter { it.role == "1" }?.apply {
                            forEach { new ->
                                if(new.userId.equals(loggedInUserCache.getLoggedInUser()?.loggedInUser?.id) && new.role == "1" && it.role == "0") {
                                    var editGroupBottomSheet = EditGroupBottomSheet.newInstanceWithData(info, it.userId)

                                    editGroupBottomSheet.removeClicked.subscribeAndObserveOnMainThread { text ->
                                        if(text.equals(resources.getString(R.string.edit))) {

                                            var list = groupAdapter.listOfGroupUsers
                                            list?.find { it1 -> it1 == it }?.apply {
                                                role = "1"
                                            }

                                            groupAdapter.listOfGroupUsers = list
                                        } else if(text.equals(resources.getString(R.string.label_remove))) {
                                            var list = groupAdapter.listOfGroupUsers
                                            list?.remove(it)

                                            groupAdapter.listOfGroupUsers = list
                                        }

                                        binding.participantsCountAppCompatTextView.text = (groupAdapter.listOfGroupUsers?.size ?: 0).toString().plus(" ").plus(resources.getString(R.string.label_participants_postfix))

                                    }

                                    editGroupBottomSheet.show(supportFragmentManager, EditGroupBottomSheet.TAG)
                                }
                            }
                        }
                    }
                }

            }
        }

        chatConversationInfo?.let { it1 -> setData(it1) }

        binding.participantsRecyclerView.apply {
            adapter = groupAdapter
        }

        groupAdapter.listOfUsers = listofuser

        binding.backAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }


        binding.moreAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            val popupMenu = PopupMenu(this@GroupDetailsActivity, binding.moreAppCompatImageView)
            popupMenu.getMenuInflater().inflate(R.menu.edit_delete_menu, popupMenu.getMenu())
            popupMenu.setOnMenuItemClickListener(object : PopupMenu.OnMenuItemClickListener {
                override fun onMenuItemClick(menuItem: MenuItem): Boolean {
                    chatConversationInfo?.let {
                        return when (menuItem?.itemId) {

                            R.id.editGroup -> {
                                startActivityForResult(EditGroupActivity.getIntent(this@GroupDetailsActivity, it), 1998)
                                true
                            }
                            R.id.deleteGroup -> {
                                openDeleteGroup()
                                true
                            }
                            else -> false
                        }
                    }
                    return false
                }
            })
            popupMenu.show()
        }

    }

    private fun openDeleteGroup() {
        val builder = AlertDialog.Builder(this@GroupDetailsActivity)
        builder.setTitle(getString(R.string.label_delete_))
        builder.setMessage(getString(R.string.label_delete_are_you_sure_you_want_to_delete_groupchat))
        builder.setPositiveButton(getString(R.string.delete)) { dialogInterface, which ->
            chatConversationInfo?.let { groupViewModel.deleteChatGroup(it) }
            dialogInterface.dismiss()
        }
        builder.setNeutralButton(getString(R.string.label_cancel)) { dialogInterface, _ ->
            dialogInterface.dismiss()
        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }


    fun listenToViewModel() {
        groupViewModel.groupState.subscribeAndObserveOnMainThread {
            when (it) {
                is GroupViewModel.GroupViewState.GetGroupInfo -> {
                    binding.progressBar.visibility = View.GONE
                    chatConversationInfo = it.groupInfoResponse
                    setData(it.groupInfoResponse)
                }
                is GroupViewModel.GroupViewState.LoadingState -> {
                    binding.progressBar.visibility = if(it.isLoading) View.VISIBLE else View.GONE
                }
                is GroupViewModel.GroupViewState.LoadingGroupState -> {
                }
                is GroupViewModel.GroupViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is GroupViewModel.GroupViewState.DeleteGroupInfo -> {
                    setResult(RESULT_OK)
                    finish()
                }

                else -> {}
            }
        }.autoDispose()
    }

    private fun setData(groupInfoResponse: ChatConversationInfo) {
        Glide.with(this@GroupDetailsActivity)
            .load(groupInfoResponse.filePath)
            .circleCrop()
            .placeholder(resources.getDrawable(R.drawable.ic_chat_user_placeholder, null))
            .error(resources.getDrawable(R.drawable.ic_chat_user_placeholder, null))
            .into(binding.groupProfileRoundedImageView)

        binding.usernameAppCompatTextView.text = groupInfoResponse.groupName
        binding.descriptionAppCompatTextView.text = groupInfoResponse.groupDescription
        binding.participantsCountAppCompatTextView.text = (groupInfoResponse.users?.size ?: 0).toString().plus(" ").plus(resources.getString(R.string.label_participants_postfix))

        groupAdapter.listOfUsers = null
        groupAdapter.listOfGroupUsers = groupInfoResponse.users


        groupInfoResponse?.users?.find { it.userId.equals(loggedInUserCache.getLoggedInUser()?.loggedInUser?.id) }?.apply {
            binding.moreAppCompatImageView.visibility = if("1".equals(role)) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val message = data?.getParcelableArrayListExtra<FollowUser>("selectedUser")

                var list = groupAdapter.listOfGroupUsers ?: arrayListOf()
                message?.forEach {
                    var group = GroupUserInfo(
                        conversationId = chatConversationInfo?.conversationId ?: chatConversationInfo?.id ?: 0,
                        userId = it.id,
                        status = 1,
                        role = "0",
                        username = it.username,
                        profileUrl = it.avatarUrl
                    )

                    list.add(group)
                }

                groupAdapter.listOfGroupUsers = list

                binding.participantsCountAppCompatTextView.text = (list?.size ?: 0).toString().plus(" ").plus(resources.getString(R.string.label_participants_postfix))

            }
        } else if(requestCode == 1998 && resultCode == Activity.RESULT_OK) {
            var dataIn = data?.getParcelableExtra<ChatConversationInfo>(EditGroupActivity.CHAT_CONVERSATION_INFO)
            chatConversationInfo = dataIn

            chatConversationInfo?.conversationId = dataIn?.id ?: 0
            groupViewModel.getGroupInfo(chatConversationInfo?.id ?: 0)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onBackPressed() {
        var intent = Intent()
        intent.putExtra(GROUP_ID, chatConversationInfo)

        setResult(Activity.RESULT_OK, intent)

        super.onBackPressed()
    }
}