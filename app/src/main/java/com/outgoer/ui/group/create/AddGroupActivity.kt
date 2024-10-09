package com.outgoer.ui.group.create

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import br.com.onimur.handlepathoz.HandlePathOz
import br.com.onimur.handlepathoz.HandlePathOzListener
import br.com.onimur.handlepathoz.model.PathOz
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.chat.model.ChatConversationInfo
import com.outgoer.api.cloudflare.model.CloudFlareConfig
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.group.model.CreateGroupRequest
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityAddGroupBinding
import com.outgoer.ui.chat.NewChatActivity
import com.outgoer.ui.group.view.GroupAdapter
import com.outgoer.ui.group.viewmodel.GroupViewModel
import com.outgoer.utils.FileUtils
import java.io.File
import javax.inject.Inject

class AddGroupActivity : BaseActivity() {

    private lateinit var binding: ActivityAddGroupBinding

    private lateinit var groupAdapter: GroupAdapter
    private var selectedlistofuser: ArrayList<FollowUser> = arrayListOf()

    private lateinit var handlePathOz: HandlePathOz
    private var selectedImagePath: String = ""
    private var imageUrl: String? = null

    private var chatConversationInfo: ChatConversationInfo? = null

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<GroupViewModel>
    private lateinit var groupViewModel: GroupViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var cloudFlareConfig: CloudFlareConfig? = null

    companion object {
        val SELECTED_USER_INFO = "SELECTED_USER_INFO"
        fun getIntent(context: Context): Intent {
            return Intent(context, AddGroupActivity::class.java)
        }

        fun getIntentWithData(context: Context, users: ArrayList<FollowUser>): Intent {
            val intent = Intent(context, AddGroupActivity::class.java)
            intent.putExtra(SELECTED_USER_INFO, users)

            return intent
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)
        binding = ActivityAddGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        groupViewModel = getViewModelFromFactory(viewModelFactory)

        groupViewModel.getCloudFlareConfig()
        initUI()
        listenToViewModel()
    }

    private fun initUI() {
        handlePathOz = HandlePathOz(this, listener)

        intent?.let {
            selectedlistofuser =
                it.getParcelableArrayListExtra<FollowUser>(SELECTED_USER_INFO) as ArrayList<FollowUser>
        }

        binding.backAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }

        binding.cameraRoundedImageView.throttleClicks().subscribeAndObserveOnMainThread {
            checkPermissionGranted(this@AddGroupActivity)
        }

        binding.continueMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.groupNameAppCompatEditText.text.toString().isNullOrEmpty()) {
                showToast(resources.getString(R.string.label_enter_group_name))
            } else if (selectedImagePath.isNullOrEmpty()) {
                var selectedUserId: ArrayList<Int> = arrayListOf()

                selectedlistofuser.forEach {
                    selectedUserId.add(it.id)
                }
                groupViewModel.createGroup(
                    CreateGroupRequest(
                        groupName = binding.groupNameAppCompatEditText.text.toString(),
                        groupDescription = binding.descriptionAppCompatEditText.text.toString(),
                        groupPic = if (imageUrl.isNullOrEmpty()) "" else imageUrl,
                        selectedUserId.joinToString(",")
                    )
                )
            } else {
                cloudFlareConfig?.let {
                    groupViewModel.uploadImageToCloudFlare(
                        this@AddGroupActivity,
                        it,
                        File(selectedImagePath),
                        loggedInUserCache.getUserId() ?: 0
                    )
                }


            }
        }

        groupAdapter = GroupAdapter(this@AddGroupActivity).apply {
            closeItemClick.subscribeAndObserveOnMainThread {
                selectedlistofuser.remove(it)
                groupAdapter.listOfUsers = listOfUsers
            }
        }

        binding.participantsRecyclerView.apply {
            adapter = groupAdapter
        }

        groupAdapter.isFirstOne = true
        groupAdapter.listOfUsers = selectedlistofuser


    }

    fun listenToViewModel() {
        groupViewModel.groupState.subscribeAndObserveOnMainThread {
            when (it) {
                is GroupViewModel.GroupViewState.GetCloudFlareConfig -> {
                    cloudFlareConfig = it.cloudFlareConfig
                }
                is GroupViewModel.GroupViewState.LoadingState -> {
                    binding.progressBar.visibility = if (it.isLoading) View.VISIBLE else View.GONE
                    binding.continueMaterialButton.visibility =
                        if (!it.isLoading) View.VISIBLE else View.GONE
                }
                is GroupViewModel.GroupViewState.UploadImageCloudFlareLoading -> {
                    binding.progressBar.visibility = if (it.isLoading) View.VISIBLE else View.GONE
                    binding.continueMaterialButton.visibility =
                        if (!it.isLoading) View.VISIBLE else View.GONE
                }
                is GroupViewModel.GroupViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is GroupViewModel.GroupViewState.SuccessConversation -> {
                    Firebase.messaging.subscribeToTopic("conversation_group_${it.message}")
                        .addOnCompleteListener { task ->
                            var msg = "Subscribed"
                            if (!task.isSuccessful) {
                                msg = "Subscribe failed"
                            }
                            Log.d("Add Group", msg)
                           // Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                        }

                    chatConversationInfo = it.chatInfo
                    chatConversationInfo?.let { info ->
                        startActivity(NewChatActivity.getIntent(this@AddGroupActivity, info))
                    }
                    finish()

                }
                is GroupViewModel.GroupViewState.UploadImageCloudFlareSuccess -> {
                    imageUrl = it.imageUrl
                    var selectedUserId: ArrayList<Int> = arrayListOf()

                    selectedlistofuser.forEach {
                        selectedUserId.add(it.id)
                    }
                    groupViewModel.createGroup(
                        CreateGroupRequest(
                            groupName = binding.groupNameAppCompatEditText.text.toString(),
                            groupDescription = binding.descriptionAppCompatEditText.text.toString(),
                            groupPic = if (imageUrl.isNullOrEmpty()) "" else imageUrl,
                            selectedUserId.joinToString(",")
                        )
                    )
                }

                else -> {}
            }
        }.autoDispose()
    }

    private fun checkPermissionGranted(context: Context) {
        XXPermissions.with(context)
            .permission(Permission.READ_MEDIA_IMAGES)
            .permission(Permission.READ_MEDIA_VIDEO)
            .request(object : OnPermissionCallback {

                override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                    if (all) {
                        FileUtils.openImagePicker(this@AddGroupActivity)
                    } else {
                        showToast(getString(R.string.msg_permission_denied))
                    }
                }

                override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                    super.onDenied(permissions, never)
                    showToast(getString(R.string.msg_permission_denied))
                }
            })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == FileUtils.PICK_IMAGE) and (resultCode == Activity.RESULT_OK)) {
            data?.data?.also {
                handlePathOz.getRealPath(it)
            }
        }
    }

    private val listener = object : HandlePathOzListener.SingleUri {
        override fun onRequestHandlePathOz(pathOz: PathOz, tr: Throwable?) {
            if (tr != null) {
                showToast(getString(R.string.error_in_finding_file_path))
            } else {
                val filePath = pathOz.path
                if (filePath.isNotEmpty()) {
                    binding.cameraRoundedImageView.setPadding(0, 0, 0, 0)
                    selectedImagePath = filePath
                    Glide.with(this@AddGroupActivity)
                        .load(filePath)
                        .circleCrop()
                        .placeholder(R.drawable.ic_chat_user_placeholder)
                        .error(R.drawable.ic_chat_user_placeholder)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .into(binding.cameraRoundedImageView)
                }
            }
        }
    }
}