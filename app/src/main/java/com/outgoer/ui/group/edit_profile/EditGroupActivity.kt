package com.outgoer.ui.group.edit_profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import br.com.onimur.handlepathoz.HandlePathOz
import br.com.onimur.handlepathoz.HandlePathOzListener
import br.com.onimur.handlepathoz.model.PathOz
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.chat.model.ChatConversationInfo
import com.outgoer.api.cloudflare.model.CloudFlareConfig
import com.outgoer.api.group.model.UpdateGroupRequest
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityEditGroupBinding
import com.outgoer.ui.group.viewmodel.GroupViewModel
import com.outgoer.ui.postlocation.AddPostLocationActivity
import com.outgoer.utils.FileUtils
import java.io.File
import javax.inject.Inject

class EditGroupActivity : BaseActivity() {

    private lateinit var binding: ActivityEditGroupBinding

    private lateinit var handlePathOz: HandlePathOz
    private var selectedImagePath: String = ""
    private var chatConversationInfo: ChatConversationInfo? = null

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<GroupViewModel>
    private lateinit var groupViewModel: GroupViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var cloudFlareConfig: CloudFlareConfig? = null
    private var imageUrl: String? = null

    companion object {

        val CHAT_CONVERSATION_INFO = "CHAT_CONVERSATION_INFO"
        fun getIntent(context: Context, chatConversationInfo: ChatConversationInfo): Intent {
            var intent = Intent(context, EditGroupActivity::class.java)
            intent.putExtra(CHAT_CONVERSATION_INFO, chatConversationInfo)

            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)

        binding = ActivityEditGroupBinding.inflate(layoutInflater)
        groupViewModel = getViewModelFromFactory(viewModelFactory)

        setContentView(binding.root)

        initUI()
        groupViewModel.getCloudFlareConfig()
        listenToViewModel()
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
                is GroupViewModel.GroupViewState.GetGroupInfo -> {
                    chatConversationInfo = it.groupInfoResponse
                    setUserInfo()

                }
                is GroupViewModel.GroupViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is GroupViewModel.GroupViewState.UpdateGroup -> {
                    var intent = Intent()
                    intent.putExtra(CHAT_CONVERSATION_INFO, chatConversationInfo)

                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }
                is GroupViewModel.GroupViewState.UploadImageCloudFlareSuccess -> {
                    imageUrl = it.imageUrl
                    var selectedUserId: ArrayList<Int> = arrayListOf()


                    groupViewModel.updateGroup(
                        chatConversationInfo?.conversationId ?: 0,
                        UpdateGroupRequest(
                            groupName = binding.groupNameAppCompatEditText.text.toString(),
                            groupDescription = binding.descriptionAppCompatEditText.text.toString(),
                            groupPic = if (imageUrl.isNullOrEmpty()) chatConversationInfo?.filePath else imageUrl,
                        )
                    )
                }

                else -> {}
            }
        }.autoDispose()
    }

    private fun initUI() {
        handlePathOz = HandlePathOz(this, listener)

        intent?.let {
            chatConversationInfo =
                it.getParcelableExtra<ChatConversationInfo>(CHAT_CONVERSATION_INFO)

            groupViewModel.getGroupInfo(
                if (chatConversationInfo?.conversationId == 0) chatConversationInfo?.id
                    ?: 0 else chatConversationInfo?.conversationId ?: 0
            )

        }

        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }

        binding.groupProfileFrameLayout.throttleClicks().subscribeAndObserveOnMainThread {
            checkPermissionGranted(this@EditGroupActivity)
        }

        binding.continueMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            cloudFlareConfig?.let {

                if (selectedImagePath.isNullOrEmpty()) {
                    groupViewModel.updateGroup(
                        chatConversationInfo?.id ?: 0,
                        UpdateGroupRequest(
                            groupName = binding.groupNameAppCompatEditText.text.toString(),
                            groupDescription = binding.descriptionAppCompatEditText.text.toString(),
                            groupPic = if (imageUrl.isNullOrEmpty()) chatConversationInfo?.filePath else imageUrl,
                        )
                    )
                } else {
                    groupViewModel.uploadImageToCloudFlare(
                        this@EditGroupActivity,
                        it,
                        File(selectedImagePath),
                        loggedInUserCache.getUserId() ?: 0
                    )
                }
            }
        }
    }

    private fun setUserInfo() {
        chatConversationInfo?.let {
            Glide.with(this@EditGroupActivity)
                .load(it.filePath)
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .error(R.drawable.ic_chat_user_placeholder)
                .into(binding.ivMyProfile)

            binding.groupNameAppCompatEditText.setText(it.groupName ?: "")
            binding.descriptionAppCompatEditText.setText(it.groupDescription ?: "")
        }
    }

    private fun checkPermissionGranted(context: Context) {
        XXPermissions.with(context)
            .permission(Permission.READ_MEDIA_IMAGES)
            .request(object : OnPermissionCallback {

                override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                    if (all) {
                        FileUtils.openImagePicker(this@EditGroupActivity)
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
                    selectedImagePath = filePath
                    Glide.with(this@EditGroupActivity)
                        .load(filePath)
                        .circleCrop()
                        .placeholder(R.drawable.ic_chat_user_placeholder)
                        .error(R.drawable.ic_chat_user_placeholder)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .into(binding.ivMyProfile)
                }
            }
        }
    }


    override fun onBackPressed() {

        var intent = Intent()
        intent.putExtra(CHAT_CONVERSATION_INFO, chatConversationInfo)

        setResult(Activity.RESULT_OK, intent)
//        finish()
        super.onBackPressed()

    }
}