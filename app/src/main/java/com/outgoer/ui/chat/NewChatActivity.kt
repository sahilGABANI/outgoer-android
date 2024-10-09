package com.outgoer.ui.chat

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.abedelazizshe.lightcompressorlibrary.config.Configuration
import com.abedelazizshe.lightcompressorlibrary.config.SaveLocation
import com.abedelazizshe.lightcompressorlibrary.config.SharedStorageConfiguration
import com.bumptech.glide.Glide
import com.giphy.sdk.core.models.Media
import com.giphy.sdk.ui.GPHContentType
import com.giphy.sdk.ui.GPHSettings
import com.giphy.sdk.ui.Giphy
import com.giphy.sdk.ui.themes.GPHCustomTheme
import com.giphy.sdk.ui.themes.GPHTheme
import com.giphy.sdk.ui.themes.GridType
import com.giphy.sdk.ui.views.GiphyDialogFragment
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.jakewharton.rxbinding3.widget.editorActions
import com.jakewharton.rxbinding3.widget.textChanges
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.chat.model.*
import com.outgoer.api.cloudflare.model.CloudFlareConfig
import com.outgoer.api.group.model.GroupMemberRequest
import com.outgoer.api.group.model.GroupUserInfo
import com.outgoer.api.post.model.MoreActionsForTextActionState
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.base.view.CustomProgressDialog
import com.outgoer.databinding.ActivityNewChatBinding
import com.outgoer.ui.chat.newview.NewChatAdapter
import com.outgoer.ui.chat.reaction_bottom_sheet.ReactionBottomSheetView
import com.outgoer.ui.chat.view.GroupUserTagAdapter
import com.outgoer.ui.chat.viewmodel.ChatMessageViewModel
import com.outgoer.ui.chat.viewmodel.ChatMessageViewState
import com.outgoer.ui.comment.viewmodel.CommentViewState
import com.outgoer.ui.commenttagpeople.view.CommentTagPeopleAdapter
import com.outgoer.ui.group.audio.AudioRecordBottomSheet
import com.outgoer.ui.group.details.GroupDetailsActivity
import com.outgoer.ui.home.home.SharePostReelBottomSheet
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import com.outgoer.ui.postdetail.PostDetailActivity
import com.outgoer.ui.progress_dialog.ProgressDialogFragment
import com.outgoer.ui.reelsdetail.ReelsDetailActivity
import io.reactivex.Observable
import kotlinx.coroutines.launch
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent.setEventListener
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlin.math.absoluteValue


class NewChatActivity : BaseActivity(), GiphyDialogFragment.GifSelectionListener {

    companion object {
        private const val RC_SELECT_IMAGE = 10001
        private const val REQUEST_CODE = 1887
        const val INTENT_EXTRA_SELECTED_IMAGE_PATH = "INTENT_EXTRA_SELECTED_IMAGE_PATH"
        const val INTENT_EXTRA_SELECTED_VIDEO_PATH = "INTENT_EXTRA_SELECTED_VIDEO_PATH"

        private const val CHAT_CONVERSATION_INFO = "CHAT_CONVERSATION_INFO"
        fun getIntent(context: Context, chatConversationInfo: ChatConversationInfo): Intent {
            val intent = Intent(context, NewChatActivity::class.java)
            intent.putExtra(CHAT_CONVERSATION_INFO, chatConversationInfo)
            return intent
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ChatMessageViewModel>
    private lateinit var chatMessageViewModel: ChatMessageViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var loggedInUserId: Int = 0
    private var loggedInName: String = ""
    private var loggedInUserName: String = ""
    private var loggedInUserAvatar: String = ""
    private lateinit var binding: ActivityNewChatBinding
    private lateinit var newChatAdapter: NewChatAdapter
    private var messageList: MutableList<ChatMessageInfo> = mutableListOf()
    private var allMessageList: ArrayList<ChatMessageInfo> = arrayListOf()
    private var countdownTimer: CountDownTimer? = null
    private var receiverId = -1
    private var conversationId = -1
    private lateinit var chatConversationInfo: ChatConversationInfo
    private var cloudFlareConfig: CloudFlareConfig? = null
    private var imageUploadDialog: Dialog? = null
    var mediaPlayer: MediaPlayer? = null
    var icLogoPlaceholder : Drawable? = null
    private var isReload: Boolean = false
    private var replyChatInfo: ChatMessageInfo? = null
    private var yPosition: Int = 0
    private var isReply: Boolean = false
    private var isTyping: Boolean = true
    private lateinit var groupUserTagAdapter: GroupUserTagAdapter
    private var initialListOfFollower: ArrayList<GroupUserInfo> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        OutgoerApplication.component.inject(this)
        binding = ActivityNewChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        chatMessageViewModel = getViewModelFromFactory(viewModelFactory)
        loadDataFromIntent()
        updateTextEditListener()
        icLogoPlaceholder = ContextCompat.getDrawable(this, R.drawable.ic_chat_user_placeholder)
    }

    private fun manageComment() {
        if (binding.chatMessageEditTextView.text.isNullOrEmpty()) {
            return
        }
        hideKeyboard(binding.chatMessageEditTextView)

        binding.chatMessageEditTextView.setText("")
    }

    private fun updateTextEditListener() {
        println("chatConversationInfo.chatType: " + chatConversationInfo.chatType)
        if(chatConversationInfo.chatType.equals("group")) {

            chatMessageViewModel.resetGroupMemberPagination(
                GroupMemberRequest(
                    binding.chatMessageEditTextView.text.toString(),
                    conversationId
                )
            )

            binding.chatMessageEditTextView.textChanges()
                .subscribeAndObserveOnMainThread {
                    if (it.isEmpty()) {
                        binding.rlFollowerList.visibility = View.GONE
                    } else {
                        val lastChar = it.last().toString()
                        if (lastChar.contains("@")) {
                            groupUserTagAdapter.groupuserList = initialListOfFollower
                            binding.rlFollowerList.visibility = View.VISIBLE
                        } else {
                            val wordList = it.split(" ")
                            val lastWord = wordList.last()
                            if (lastWord.contains("@")) {
                                chatMessageViewModel.resetGroupMemberPagination(
                                    GroupMemberRequest(
                                        search = lastWord.replace("@", ""),
                                        conversationId
                                    )
                                )

                            } else {
                                binding.rlFollowerList.visibility = View.GONE
                            }
                        }
                    }
                }.autoDispose()


            binding.chatMessageEditTextView.editorActions()
                .filter { action -> action == EditorInfo.IME_ACTION_SEND }
                .subscribeAndObserveOnMainThread {
                    manageComment()
                }.autoDispose()


            groupUserTagAdapter = GroupUserTagAdapter(this@NewChatActivity).apply {
                groupUserClick.subscribeAndObserveOnMainThread { followUser ->
                    val cursorPosition: Int = binding.chatMessageEditTextView.selectionStart
                    val descriptionString = binding.chatMessageEditTextView.text.toString()
                    val subString = descriptionString.subSequence(0, cursorPosition).toString()
                    chatMessageViewModel.searchTagUserClicked(
                        binding.chatMessageEditTextView.text.toString(),
                        subString,
                        followUser
                    )
                }.autoDispose()
            }

            binding.rlFollowerList.apply {
                layoutManager = LinearLayoutManager(this@NewChatActivity)
                adapter = groupUserTagAdapter
            }
        }


    }

    private fun loadDataFromIntent() {

        loggedInUserId = loggedInUserCache.getUserId() ?: 0
        loggedInName = loggedInUserCache.getLoggedInUser()?.loggedInUser?.name ?: ""
        loggedInUserName = loggedInUserCache.getLoggedInUser()?.loggedInUser?.username ?: ""
        loggedInUserAvatar = loggedInUserCache.getLoggedInUser()?.loggedInUser?.avatar ?: ""

        chatConversationInfo = intent.getParcelableExtra(CHAT_CONVERSATION_INFO)!!

        receiverId = chatConversationInfo.receiverId
        conversationId = chatConversationInfo.conversationId

        if (chatConversationInfo.chatType.equals(resources.getString(R.string.label_group))) {
            binding.tvUserCount.visibility = View.VISIBLE
            chatMessageViewModel.getGroupInfo(chatConversationInfo.conversationId)
        } else {
            binding.tvUserCount.visibility = View.GONE
        }

        binding.audioAppCompatImageView.visibility = View.VISIBLE
        binding.messageSendImageView.visibility = View.GONE


        setEventListener(this, KeyboardVisibilityEventListener {
            if (it) {
                binding.audioAppCompatImageView.visibility = View.GONE
                binding.messageSendImageView.visibility = View.VISIBLE
            } else {
                if (binding.chatMessageEditTextView.text.toString().length > 0) {
                    binding.audioAppCompatImageView.visibility = View.GONE
                    binding.messageSendImageView.visibility = View.VISIBLE
                } else {
                    binding.audioAppCompatImageView.visibility = View.VISIBLE
                    binding.messageSendImageView.visibility = View.GONE
                }
            }
        })


        if ((conversationId == 0 && resources.getString(R.string.label_group)
                .equals(chatConversationInfo.chatType)) || (receiverId == 0 && resources.getString(R.string.label_chat)
                .equals(chatConversationInfo.chatType))
        ) {
            showToast("Please try again")
        }

        icLogoPlaceholder =
            if (chatConversationInfo.chatType.equals(resources.getString(R.string.label_group))) ContextCompat.getDrawable(
                this,
                R.drawable.ic_group_user_placeholder
            ) else {
                ContextCompat.getDrawable(this, R.drawable.ic_chat_user_placeholder)
            }


        listenToViewModel()
        listenToViewEvents()
        chatMessageViewModel.getCloudFlareConfig()
    }

    @SuppressLint("StringFormatMatches")
    private fun listenToViewModel() {
        chatMessageViewModel.messageViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is ChatMessageViewState.GetCloudFlareConfig -> {
                    cloudFlareConfig = it.cloudFlareConfig
                }
                is ChatMessageViewState.CloudFlareConfigErrorMessage -> {
                    showLongToast(it.errorMessage)
                    onBackPressed()
                }
                is ChatMessageViewState.UploadImageCloudFlareLoading -> {
                    if (it.isLoading) {
                        imageUploadDialog = CustomProgressDialog.showAVLProgressDialog(this)
                    } else {
                        CustomProgressDialog.hideAVLProgressDialog(imageUploadDialog)
                    }
                }
                is ChatMessageViewState.ReactionMessage -> {
                    val info = newChatAdapter.listOfMessageInfo
                    info?.find { data -> data.id == it.messageTyping.id }?.reactionData = it.messageTyping.reactionData
                    newChatAdapter.listOfMessageInfo = info
                }
                is ChatMessageViewState.UploadImageCloudFlareSuccess -> {
                    chatMessageViewModel.sendNewMessage(
                        ChatSendMessageRequest(
                            senderId = loggedInUserId,
                            receiverId = if (chatConversationInfo.chatType.equals(
                                    resources.getString(
                                        R.string.label_group
                                    )
                                )
                            ) 0 else receiverId,
                            conversationId = conversationId,
                            message = "",
                            fileSize = "",
                            file = "",
                            fileType = MessageType.Image,
                            fileUrl = it.imageUrl,
                            profileUrl = if (chatConversationInfo.chatType.equals(
                                    resources.getString(
                                        R.string.label_group
                                    )
                                )
                            ) chatConversationInfo.filePath else loggedInUserAvatar,
                            name = loggedInName,
                            chatType = chatConversationInfo.chatType,
                            groupName = if (chatConversationInfo.chatType.equals(
                                    resources.getString(
                                        R.string.label_group
                                    )
                                )
                            ) chatConversationInfo.groupName else "",
                            thumbnail = "",
                            username = loggedInUserName
                        )
                    )
                }
                is ChatMessageViewState.UploadVideoCloudFlareSuccess -> {
                    chatMessageViewModel.sendNewMessage(
                        ChatSendMessageRequest(
                            senderId = loggedInUserId,
                            receiverId = if (chatConversationInfo.chatType.equals(
                                    resources.getString(
                                        R.string.label_group
                                    )
                                )
                            ) 0 else receiverId,
                            conversationId = conversationId,
                            message = "",
                            fileType = MessageType.Video,
                            fileSize = "",
                            file = "",
                            videoUrl = it.uid,
                            thumbnail = it.thumbnail,
                            profileUrl = if (chatConversationInfo.chatType.equals(
                                    resources.getString(
                                        R.string.label_group
                                    )
                                )
                            ) chatConversationInfo.filePath else loggedInUserAvatar,
                            name = loggedInName,
                            chatType = chatConversationInfo.chatType,
                            groupName = if (chatConversationInfo.chatType.equals(
                                    resources.getString(
                                        R.string.label_group
                                    )
                                )
                            ) chatConversationInfo.groupName else "",
                            username = loggedInUserName
                        )
                    )
                }
                is ChatMessageViewState.ErrorMessage -> {
                    showLongToast(it.toString())
                }
                is ChatMessageViewState.LoadChatMessageList -> {
                    allMessageList.clear()
                    allMessageList.addAll(it.listOfChatMessageInfo)
                    groupByDate(allMessageList.distinct(), "callHere")
                    hideShowNoData()
                }
                is ChatMessageViewState.NewChatMessage -> {
                    isTyping = true
                    if (it.chatMessageInfo.conversationId == chatConversationInfo.conversationId) {
                        allMessageList.add(0, it.chatMessageInfo)
                        groupByDate(allMessageList, "called by me")

//                        binding.chatRecyclerView.scrollToPosition(0)
                    }

                    if ((loggedInUserId != it.chatMessageInfo.senderId) && (it.chatMessageInfo.conversationId == chatConversationInfo.conversationId)) {
                        val request = SendMessageIsReadRequest(
                            messageIds = it.chatMessageInfo.id.toString(),
                            senderId = loggedInUserId,
                            receiverId = receiverId,
                            conversationId = conversationId,
                        )
                        chatMessageViewModel.sendMessageIsRead(request)
                    }
                    hideShowNoData()
                }
                is ChatMessageViewState.LoadingState -> {

                }
                is ChatMessageViewState.SuccessMessage -> {

                }
                is ChatMessageViewState.RoomConnected -> {
                    val request = UpdateOnlineStatusRequest(
                        receiverId = receiverId,
                        conversationId = conversationId,
                    )
                    chatMessageViewModel.updateOnlineStatus(request)
                }
                ChatMessageViewState.RoomConnecting -> {

                }
                is ChatMessageViewState.RoomConnectionFail -> {

                }
                is ChatMessageViewState.OnlineStatus -> {
                    if (chatConversationInfo.chatType.equals(
                            resources.getString(
                                R.string.label_chat
                            )
                        )
                    ) {
                        it.isOnline?.find { it.receiverId == receiverId }.apply {
                            if (this?.isOnline == true) {
                                binding.ivOnline.setImageResource(R.drawable.ic_new_chat_status_online)
                                binding.tvUserCount.isVisible = true
                                binding.tvUserCount.text = resources.getString(R.string.label_online)
                            } else {
                                binding.ivOnline.setImageResource(R.drawable.ic_chat_status_offline)
                                binding.tvUserCount.isVisible = true
                                binding.tvUserCount.text = resources.getString(R.string.label_offline)
                            }
                        }
                    }
                }
                is ChatMessageViewState.UpdateMessageIsRead -> {
                    val messageIds = it.chatMessageInfo.messageIds
                    if (messageIds.isNotEmpty()) {
                        val messageIdsList = messageIds.split(",").map { it.toInt() }
                        for (i in 0 until messageList.size) {
                            val message = messageList[i]
                            for (messageId in messageIdsList) {
                                if (message.id == messageId) {
                                    messageList[i].isRead = 1
                                }
                            }
                        }
                        newChatAdapter.listOfMessageInfo = messageList as ArrayList<ChatMessageInfo>
                    }
                }
                is ChatMessageViewState.DeleteMessage -> {
                    val info = newChatAdapter.listOfMessageInfo
                    info?.remove(it.chatUserInfo)
                    newChatAdapter.listOfMessageInfo = info
                }
                is ChatMessageViewState.DeleteConversationInfo -> {
                    finish()
                }
                is ChatMessageViewState.DeleteGroupInfo -> {
                    finish()
                }
                is ChatMessageViewState.GetGroupInfo -> {
                    binding.tvUserCount.text = (it.groupInfoResponse.users?.size ?: 0).toString().plus(" ").plus(resources.getString(R.string.label_participants_postfix))
                }
                is ChatMessageViewState.TypingMessage -> {
                    if (conversationId == it.messageTyping.conversationId && isTyping) {
                        isTyping = false
                        val info = newChatAdapter.listOfMessageInfo
                        info?.add(0, ChatMessageInfo(
                            id = 0,
                            senderId = receiverId,
                            fileType = MessageType.Typing,
                            profileUrl = if (chatConversationInfo.chatType.equals(resources.getString(R.string.label_group))) {
                                chatConversationInfo.filePath
                            } else {
                                chatConversationInfo.profileUrl
                            },
                            message = "",
                            reactionData = null
                        ))
                        newChatAdapter.listOfMessageInfo = info
                    }
                }
                is ChatMessageViewState.GroupMemberList -> {
                    mentionTagPeopleViewVisibility(!it.listGroupUser.isNullOrEmpty())
                    groupUserTagAdapter.groupuserList = it.listGroupUser
                }
                is ChatMessageViewState.InitialGroupMemberList -> {
                    initialListOfFollower.addAll(it.listGroupUser)
//                    mentionTagPeopleViewVisibility(!it.listGroupUser.isNullOrEmpty())
                }
                is ChatMessageViewState.UpdateDescriptionText -> {
                    mentionTagPeopleViewVisibility(false)
                    binding.chatMessageEditTextView.setText(it.descriptionString)
                    binding.chatMessageEditTextView.setSelection(binding.chatMessageEditTextView.text.toString().length)
                }
                else -> {}
            }
        }
    }

    private fun mentionTagPeopleViewVisibility(isVisibility: Boolean) {
        if (isVisibility && binding.rlFollowerList.visibility == View.GONE) {
            binding.rlFollowerList.visibility = View.VISIBLE
        } else if (!isVisibility && binding.rlFollowerList.visibility == View.VISIBLE) {
            binding.rlFollowerList.visibility = View.GONE
        }
    }

    private fun listenToViewEvents() {
        val profilePath =
            if (chatConversationInfo.chatType.equals(resources.getString(R.string.label_group))) {
                chatConversationInfo.filePath
            } else {
                chatConversationInfo.profileUrl
            }

        val uName =
            if (chatConversationInfo.chatType.equals(resources.getString(R.string.label_group))) {
                chatConversationInfo.groupName
            } else {
                if (chatConversationInfo.username.isNullOrEmpty()) chatConversationInfo.name else chatConversationInfo.username
            }

        Glide.with(this).load(profilePath).placeholder(icLogoPlaceholder)
            .error(icLogoPlaceholder).into(binding.ivUserProfileImage)

        binding.tvUsername.text = uName

        binding.groupInfoLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
            if (chatConversationInfo.chatType.equals(resources.getString(R.string.label_group))) {
                startActivityForResult(
                    GroupDetailsActivity.getIntentWithData(
                        this@NewChatActivity,
                        chatConversationInfo
                    ), REQUEST_CODE
                )
            } else {
                startActivityWithDefaultAnimation(
                    NewOtherUserProfileActivity.getIntent(
                        this, receiverId
                    )
                )

            }
        }

        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()

        binding.ivCloseReply.throttleClicks().subscribeAndObserveOnMainThread {
            isReply = false
            binding.cardChatView.visibility = View.GONE
        }.autoDispose()

        binding.gifImageView.throttleClicks().subscribeAndObserveOnMainThread {
            Giphy.configure(this, GIF_API_KEY)


            GPHCustomTheme.channelColor = 0xffD8D8D8.toInt()
            GPHCustomTheme.backgroundColor = 0x86121212.toInt()
            GPHCustomTheme.activeImageColor = 0xff2666ca.toInt()
            GPHCustomTheme.searchBackgroundColor = 0xFF4E4E4E.toInt()
            GPHCustomTheme.searchQueryColor = 0xffffffff.toInt()
            GPHCustomTheme.suggestionBackgroundColor = Color.TRANSPARENT

            val settings = GPHSettings(GridType.waterfall, GPHTheme.Custom)
            settings.enableDynamicText = true
            settings.mediaTypeConfig = arrayOf(GPHContentType.gif, GPHContentType.sticker, GPHContentType.text, GPHContentType.emoji)

            val dialog = GiphyDialogFragment.newInstance(settings)
            dialog.show(supportFragmentManager, "gifs_dialog")

//            GiphyDialogFragment.newInstance().show(supportFragmentManager, "giphy_dialog")
        }

        binding.audioAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            XXPermissions.with(this@NewChatActivity)
                .permission(Permission.RECORD_AUDIO)
                .request(object : OnPermissionCallback {

                    override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                        if (all) {
                            val audioRecordBottomSheet =
                                AudioRecordBottomSheet.newInstanceWithData(chatConversationInfo)
                            audioRecordBottomSheet.show(
                                supportFragmentManager,
                                AudioRecordBottomSheet.TAG
                            )
                        } else {
                            showToast(getString(R.string.msg_permission_denied) + "I1")
                        }
                    }

                    override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                        super.onDenied(permissions, never)
                        showToast(getString(R.string.msg_permission_denied))
                        XXPermissions.startPermissionActivity(this@NewChatActivity, permissions);
                    }
                })
        }

        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            binding.swipeRefreshLayout.isRefreshing = false
            isReload = true
            chatMessageViewModel.resetPagination(conversationId)
        }.autoDispose()

        newChatAdapter = NewChatAdapter(this, loggedInUserCache).apply {

            textReceiverLongClick.subscribeAndObserveOnMainThread {

            }

            shareViewClicks.subscribeAndObserveOnMainThread {
                if(MessageType.Post.equals(it.fileType) && it.postId ?: 0 > 0) {
                    startActivity(PostDetailActivity.getIntent(this@NewChatActivity, it.postId ?: 0, showComments = false))
                } else {
                    startActivity(ReelsDetailActivity.getIntent(this@NewChatActivity, it.reelId ?: 0, showComments = false))
                }
            }

            chatAudioFileViewClicks.subscribeAndObserveOnMainThread {
            }
            audioReceiverImgViewClick.subscribeAndObserveOnMainThread {
                startActivityWithDefaultAnimation(
                    NewOtherUserProfileActivity.getIntent(this@NewChatActivity, it.senderId!!)
                )
            }
            gifReceiverViewClick.subscribeAndObserveOnMainThread {

                startActivityWithDefaultAnimation(
                    NewOtherUserProfileActivity.getIntent(this@NewChatActivity, it.senderId!!)
                )
            }
            textReceiverViewClick.subscribeAndObserveOnMainThread {
                startActivityWithDefaultAnimation(
                    NewOtherUserProfileActivity.getIntent(this@NewChatActivity, it.senderId!!)
                )
            }
            imageReceiverViewClick.subscribeAndObserveOnMainThread {
                startActivityWithDefaultAnimation(
                    NewOtherUserProfileActivity.getIntent(this@NewChatActivity, it.senderId!!)
                )
            }
            chatAudioViewClicks.subscribeAndObserveOnMainThread { pairChatInfo ->

                val newInfo = pairChatInfo.first

                val playerUrl = newInfo.fileUrl?.replace("m4a", "mp3") ?: ""

                listOfMessageInfo?.forEach {
                    it.isPlay = if (it.id == newInfo.id) newInfo.isPlay else false
                }

                if (newInfo.isPlay) {

                    mediaPlayer?.apply {
                        stop()
                        release()
                    }

                    pairChatInfo.third.getChildAt(1).visibility = View.VISIBLE
                    pairChatInfo.third.getChildAt(0).visibility = View.GONE


                    mediaPlayer = MediaPlayer()
                    mediaPlayer?.apply {
                        setDataSource(newInfo.fileUrl)
                        prepareAsync()


                        setOnPreparedListener {
                            pairChatInfo.third.getChildAt(1).visibility = View.GONE
                            pairChatInfo.third.getChildAt(0).visibility = View.VISIBLE
                            pairChatInfo.second.setMax(mediaPlayer?.duration ?: 0);

                            this.start()

                            countdownTimer = object : CountDownTimer((mediaPlayer?.duration ?: 0).toLong(), 1000){
                                override fun onTick(millisUntilFinished: Long) {
                                    if (mediaPlayer != null) {
                                        val mCurrentPosition: Int = mediaPlayer?.currentPosition?:0
                                        newChatAdapter.listOfMessageInfo?.find { it.id == newInfo.id }?.apply {
                                            if(isPlay) {
                                                pairChatInfo.second.setProgress(mCurrentPosition)
                                            } else {
                                                pairChatInfo.second.setProgress(0)
                                            }
                                        }
                                    }
                                }

                                override fun onFinish() {
                                    pairChatInfo.second.setProgress(mediaPlayer?.duration ?: 0)

                                }
                            }.start()
                        }
                        setOnCompletionListener { m ->
                            if (!m.isPlaying) {
                                pairChatInfo.second.setProgress(0)
                                listOfMessageInfo?.forEach {
                                    it.isPlay = false
                                }
                                newChatAdapter.listOfMessageInfo = listOfMessageInfo

                                countdownTimer?.cancel()
                                countdownTimer = null
                                stop()
                            }
                        }
                    }
                } else {
                    pairChatInfo.second.setProgress(0)
                    mediaPlayer?.apply {
                        if (isPlaying) {
                            pause()
                        }
                        stop()
                        release()
                    }
                    mediaPlayer = null
                }

                newChatAdapter.listOfMessageInfo = listOfMessageInfo
            }
            chatMediaViewClicks.subscribeAndObserveOnMainThread {
                val mediaType = when (it.fileType) {
                    MessageType.Image -> {
                        DisplayActivity.INTENT_EXTRA_MEDIA_IMAGE
                    }
                    MessageType.Video -> {
                        DisplayActivity.INTENT_EXTRA_MEDIA_VIDEO
                    }
                    MessageType.GIF -> {
                        DisplayActivity.INTENT_EXTRA_MEDIA_VIDEO
                    }
                    else -> {
                        null
                    }
                }

                if (MessageType.Video.equals(it.fileType)) {
                    Observable.timer(2000, TimeUnit.MILLISECONDS)
                        .subscribeAndObserveOnMainThread { d ->
                            startActivityWithFadeInAnimation(
                                DisplayActivity.launchActivity(
                                    this@NewChatActivity,
                                    it.videoUrl,
                                    mediaType,
                                    it.thumbnail
                                )
                            )
                        }.autoDispose()
                } else {
                    startActivityWithFadeInAnimation(
                        DisplayActivity.launchActivity(
                            this@NewChatActivity,
                            it.fileUrl,
                            mediaType
                        )
                    )
                }
            }

            chatForDeleteViewClicks.subscribeAndObserveOnMainThread {
                chatMessageViewModel.deleteChatMessage(it)
            }

            moreActionViewClicks.subscribeAndObserveOnMainThread {
                when(it) {
                    is MoreActionsForTextActionState.TaggedUser -> {
                        val loggedInUserId = loggedInUserCache.getLoggedInUser()?.loggedInUser?.id
                        val clickedText = it.clickedText
                        val tagsList = it.commentInfo.mentions ?: arrayListOf()

                        if (!tagsList.isNullOrEmpty()) {
                            val tag = tagsList.firstOrNull { cInfo ->
                                cInfo.user?.username == clickedText
                            }
                            if (tag != null) {
                                if (loggedInUserId != tag.userId) {
                                    if (tag.user?.userType == MapVenueUserType.VENUE_OWNER.type) {
                                        if (loggedInUserCache.getUserId() == tag.userId) {
                                            RxBus.publish(RxEvent.OpenVenueUserProfile)
                                        } else {
                                            startActivityWithDefaultAnimation(
                                                NewVenueDetailActivity.getIntent(
                                                    this@NewChatActivity,
                                                    0,
                                                    tag.userId
                                                )
                                            )
                                        }
                                    } else {
                                        startActivityWithDefaultAnimation(
                                            NewOtherUserProfileActivity.getIntent(
                                                this@NewChatActivity,
                                                tag.userId
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    is MoreActionsForTextActionState.ReplyMessage -> {
                        replyChatInfo = it.chatMessageInfo
                        isReply = true
                        showReplyView(it.chatMessageInfo.username, it.chatMessageInfo.message)
                    }
                    is MoreActionsForTextActionState.ForwardMessage -> {
                        val sharePostReelBottomSheet = SharePostReelBottomSheet.newInstance(true)
                        sharePostReelBottomSheet.forwardClick.throttleClicks().subscribeAndObserveOnMainThread {res ->
                            sharePostReelBottomSheet.dismiss()
                            val selectedUser = res.joinToString()
                            chatMessageViewModel.forwardMessage(it.chatMessageInfo.id, selectedUser)
                        }.autoDispose()
                        sharePostReelBottomSheet.show(supportFragmentManager, SharePostReelBottomSheet.Companion::class.java.name)
                    }
                    is MoreActionsForTextActionState.CopyMessage -> {
                        copyToClipboard(it.chatMessageInfo.message)
                    }
                    is MoreActionsForTextActionState.DeleteMessage -> {
                        chatMessageViewModel.deleteChatMessage(it.chatMessageInfo)
                    }

                    is MoreActionsForTextActionState.ReactionOnMessage -> {
                        chatMessageViewModel.addReactions(AddReactionSocketEvent(
                            it.chatMessageInfo.conversationId,
                            loggedInUserId,
                            it.chatMessageInfo.id,
                            it.reactionType,
                            loggedInUserCache.getLoggedInUser()?.loggedInUser?.name,
                            loggedInUserCache.getLoggedInUser()?.loggedInUser?.username,
                            groupName = null,
                            it.chatMessageInfo.receiverId,
                            it.chatMessageInfo.profileUrl
                        ))
                    }
                    is MoreActionsForTextActionState.UserProfileOpen -> {
                        startActivityWithDefaultAnimation(
                            NewOtherUserProfileActivity.getIntent(this@NewChatActivity, it.chatMessageInfo.senderId ?: 0)
                        )
                    }
                    is MoreActionsForTextActionState.ReactedUsersView -> {
                        val reactionBottomSheetView = ReactionBottomSheetView.newInstance(it.messageId, loggedInUserId)
                        reactionBottomSheetView.reactionClick.subscribeAndObserveOnMainThread { reactionRes ->
                            chatMessageViewModel.removeReactions(RemoveReactionSocketEvent(
                                conversationId = conversationId,
                                senderId = loggedInUserId,
                                messageId = reactionRes.messageId,
                                reactionType = reactionRes.reactionType
                            ))
                        }.autoDispose()
                        reactionBottomSheetView.show(supportFragmentManager, ReactionBottomSheetView.Companion::class.java.name)
                    }
                }
            }
        }

        binding.chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@NewChatActivity, RecyclerView.VERTICAL, true)
            adapter = newChatAdapter

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, state: Int) {
                    super.onScrollStateChanged(recyclerView, state)
                    if (state == RecyclerView.SCROLL_STATE_IDLE) {
                        val layoutManager = recyclerView.layoutManager ?: return
                        chatMessageViewModel.loadMore(conversationId)
                    }
                }
            })
        }

        binding.chatRecyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                val childView: View? = rv.findChildViewUnder(e.x, e.y)
                if (childView != null && e.action == MotionEvent.ACTION_UP) {
                    val location = IntArray(2)
                    childView.getLocationOnScreen(location)


                    val xOnScreen = location[0]
                    val yOnScreen = location[1]

                    yPosition = yOnScreen

                    println("Screen : " + xOnScreen + " : " + yOnScreen)

                }
                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
            }

        })

        val verticalScrollOffset = AtomicInteger(0)
        binding.chatRecyclerView.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            val y = oldBottom - bottom
            if (y.absoluteValue > 0) {
                binding.chatRecyclerView.post {
                    if (y > 0 || verticalScrollOffset.get().absoluteValue >= y.absoluteValue) {
                        binding.chatRecyclerView.scrollBy(0, y)
                    } else {
                        binding.chatRecyclerView.scrollBy(0, verticalScrollOffset.get())
                    }
                }
            }
        }

        binding.cameraSelectionImageView.throttleClicks().subscribeAndObserveOnMainThread {
            checkPermissions(this)
        }.autoDispose()

        binding.messageSendImageView.throttleClicks().subscribeAndObserveOnMainThread {
            if(!binding.chatMessageEditTextView.text.toString().isNullOrEmpty())
                sendTextMessage()
        }.autoDispose()

        binding.chatMessageEditTextView.editorActions()
            .filter { action -> action == EditorInfo.IME_ACTION_SEND }
            .subscribeAndObserveOnMainThread {
                sendTextMessage()
            }.autoDispose()

        binding.chatMessageEditTextView.textChanges()
            .skipInitialValue()
            .subscribeOnIoAndObserveOnMainThread({
                if (it.isNotEmpty()) {
                    chatMessageViewModel.typingMessage(MessageTypingSocketEvent(conversationId, loggedInUserName))
                }
            }, {
                Timber.e(it)
            }).autoDispose()

        chatMessageViewModel.joinRoom(
            JoinRoomRequest(
                senderId = loggedInUserId,
                receiverId = receiverId,
                conversationId = conversationId,
                chatType = chatConversationInfo.chatType ?: ""
            )
        )
    }

    private fun showReplyView(username: String?, message: String?) {
        binding.cardChatView.visibility = View.VISIBLE
        binding.replyUsername.text = username
        binding.messageReply.text = message
    }

    private fun copyToClipboard(message: String?) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Text", message)
        clipboard.setPrimaryClip(clip)
    }

    private fun groupByDate(msgList: List<ChatMessageInfo>, info: String) {
        messageList.clear()
        Timber.tag("ChatMessageInfo").d("msgList: $msgList")
        val filterList: MutableList<ChatMessageInfo> = mutableListOf()
        val groups = msgList.groupBy { getChatMessageHeaderDateForGroup(it.createdAt) }

        for (date: String in groups.keys) {
            var groupItems: List<ChatMessageInfo>? = groups[date]

            val index = groupItems?.size?.minus(1)
            if (index != null) {
                val loopIndex = index - 1
                if (!groupItems.isNullOrEmpty()) {
                    for (i in 0..loopIndex) {
                        groupItems[i].showDate = false
                    }
                }

                groupItems = groupItems?.distinct()
                groupItems?.get(groupItems.size - 1)?.showDate = true

                if (groupItems != null) {
                    filterList.addAll(groupItems)
                }
            }
        }

        messageList.addAll(filterList)
        Timber.tag("ChatMessageInfo").d("messageList: $messageList")
        newChatAdapter.listOfMessageInfo = messageList as ArrayList<ChatMessageInfo>
    }

    private fun sendTextMessage() {
        isReload = true
        if (isReply) {
            if (replyChatInfo != null) {
                chatMessageViewModel.sendNewMessage(
                    ChatSendMessageRequest(
                        senderId = loggedInUserId,
                        receiverId = if (chatConversationInfo.chatType.equals(resources.getString(R.string.label_group))) 0 else receiverId,
                        conversationId = conversationId,
                        fileType = MessageType.Reply,
                        fileUrl = "",
                        file = "",
                        fileSize = "",
                        message = replyChatInfo?.message,
                        profileUrl = if (chatConversationInfo.chatType.equals(resources.getString(R.string.label_group))) chatConversationInfo.filePath else loggedInUserAvatar,
                        name = loggedInName,
                        chatType = chatConversationInfo.chatType,
                        thumbnail = "",
                        groupName = if (chatConversationInfo.chatType.equals(resources.getString(R.string.label_group))) chatConversationInfo.groupName else "",
                        username = loggedInUserName,
                        replyId = replyChatInfo?.id,
                        replyName = replyChatInfo?.username,
                        replyMessage = binding.chatMessageEditTextView.text.toString()
                    )
                )
            }
            isReply = false
            binding.cardChatView.visibility = View.GONE
        } else {
            chatMessageViewModel.sendNewMessage(
                ChatSendMessageRequest(
                    senderId = loggedInUserId,
                    receiverId = if (chatConversationInfo.chatType.equals(resources.getString(R.string.label_group))) 0 else receiverId,
                    conversationId = conversationId,
                    fileType = MessageType.Text,
                    fileUrl = "",
                    file = "",
                    fileSize = "",
                    message = binding.chatMessageEditTextView.text.toString(),
                    profileUrl = if (chatConversationInfo.chatType.equals(resources.getString(R.string.label_group))) chatConversationInfo.filePath else loggedInUserAvatar,
                    name = loggedInName,
                    chatType = chatConversationInfo.chatType,
                    thumbnail = "",
                    groupName = if (chatConversationInfo.chatType.equals(resources.getString(R.string.label_group))) chatConversationInfo.groupName else "",
                    username = loggedInUserName
                )
            )
        }
        binding.chatMessageEditTextView.setText("")
    }

    private fun hideShowNoData() {
        if (messageList.isNotEmpty()) {
            binding.llNoData.visibility = View.GONE
        } else {

            if(chatConversationInfo.chatType != resources.getString(R.string.label_group)) {
                Glide.with(this)
                    .load(chatConversationInfo.profileUrl)
                    .placeholder(icLogoPlaceholder)
                    .circleCrop()
                    .into(binding.ivEmptyChat)
                binding.tvEmptyUsername.text = chatConversationInfo.name
                binding.llNoData.visibility = View.VISIBLE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        chatMessageViewModel.resetPagination(conversationId)
    }

    override fun onPause() {
        super.onPause()
        if (conversationId != 0 && receiverId != 0) {
            chatMessageViewModel.setUserOffline(
                SetUserOfflineRequest(
                    senderId = loggedInUserId
                )
            )
        }
    }

    private fun checkPermissions(context: Context) {
        XXPermissions.with(context).permission(
            listOf(
                Permission.CAMERA,
                Permission.READ_MEDIA_IMAGES,
                Permission.READ_MEDIA_VIDEO

            )
        ).request(object : OnPermissionCallback {

            override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                if (all) {
                    if (cloudFlareConfig == null) {
                        chatMessageViewModel.getCloudFlareConfig()
                    }
                    startActivityForResultWithDefaultAnimation(
                        ChatSelectImageActivity.getIntent(
                            context
                        ), RC_SELECT_IMAGE
                    )
                } else {
                    showToast(getString(R.string.msg_some_permission_denied))
                }
            }

            override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                super.onDenied(permissions, never)

                XXPermissions.startPermissionActivity(this@NewChatActivity, permissions);
                showToast(getString(R.string.msg_permission_denied))
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SELECT_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    if (data.hasExtra(INTENT_EXTRA_SELECTED_VIDEO_PATH)) {
                        val selectedVideoFilePath =
                            data.getStringExtra(INTENT_EXTRA_SELECTED_VIDEO_PATH)
                        if (!selectedVideoFilePath.isNullOrEmpty()) {
                            compressVideoFile(selectedVideoFilePath)
                        }
                    } else if (data.hasExtra(INTENT_EXTRA_SELECTED_IMAGE_PATH)) {
                        val selectedImageFilePath =
                            data.getStringExtra(INTENT_EXTRA_SELECTED_IMAGE_PATH)
                        if (!selectedImageFilePath.isNullOrEmpty()) {
                            cloudFlareConfig?.let {
                                chatMessageViewModel.uploadImageToCloudFlare(
                                    this, it, File(selectedImageFilePath), loggedInUserId
                                )
                            }
                        }
                    }

                }
            }
        } else if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            finish()
        }
    }

    private fun compressVideoFile(selectedVideoFilePath: String) {
        val videoUris = listOf(Uri.fromFile(File(selectedVideoFilePath)))
        lifecycleScope.launch {
            VideoCompressor.start(
                context = applicationContext,
                videoUris,
                isStreamable = false,
                sharedStorageConfiguration = SharedStorageConfiguration(
                    saveAt = SaveLocation.movies,
                    subFolderName = "outgoer"
                ),
                configureWith = Configuration(
                    quality = VideoQuality.HIGH,
                    videoNames = videoUris.map { uri -> uri.pathSegments.last() },
                    isMinBitrateCheckEnabled = true,
                ),
                listener = object : CompressionListener {
                    override fun onProgress(index: Int, percent: Float) {

                    }

                    override fun onStart(index: Int) {

                    }

                    override fun onSuccess(index: Int, size: Long, path: String?) {
                        cloudFlareConfig?.let {
                            chatMessageViewModel.uploadVideoToCloudFlare(
                                this@NewChatActivity, it, File(selectedVideoFilePath), loggedInUserId
                            )
                        }
                        var progressDialogFragment = ProgressDialogFragment.newInstance()
                        progressDialogFragment.progressState.subscribeAndObserveOnMainThread {
                            progressDialogFragment.dismiss()
                        }
                        progressDialogFragment.show(supportFragmentManager, ProgressDialogFragment.javaClass.name)
                    }

                    override fun onFailure(index: Int, failureMessage: String) {
                        Timber.wtf(failureMessage)
                    }

                    override fun onCancelled(index: Int) {
                        Timber.wtf("compression has been cancelled")
                        // make UI changes, cleanup, etc
                    }
                },
            )
        }
    }

    private fun showPopup(v: View, chatMessageInfo: ChatMessageInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            PopupMenu(this@NewChatActivity, v, Gravity.END, 0, R.style.MyPopupMenu)
        } else {
            PopupMenu(this@NewChatActivity, v)
        }.apply {
            setOnMenuItemClickListener(object : PopupMenu.OnMenuItemClickListener {
                override fun onMenuItemClick(item: MenuItem?): Boolean {
                    return when (item?.itemId) {

                        R.id.deleteGroup -> {
                            chatMessageViewModel.deleteChatMessage(chatMessageInfo)
                            true
                        }
                        else -> false
                    }
                }

            })
            if (chatConversationInfo.chatType.equals(resources.getString(R.string.label_group))) {
                inflate(R.menu.edit_delete_menu)
            } else {
                inflate(R.menu.delete_menu)
            }
            show()
        }
    }

    private fun openDeleteGroup() {
        val builder = AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialogCustom))
        builder.setTitle(getString(R.string.label_delete_))
        builder.setMessage(getString(R.string.label_delete_are_you_sure_you_want_to_delete))
        builder.setPositiveButton(getString(R.string.delete)) { dialogInterface, which ->
            chatMessageViewModel.deleteChatGroup(chatConversationInfo)
            dialogInterface.dismiss()
        }
        builder.setNeutralButton(getString(R.string.label_cancel)) { dialogInterface, _ ->
            dialogInterface.dismiss()
        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }
    private fun openDeleteGroupChat() {
        val builder = AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialogCustom))
        builder.setTitle(getString(R.string.label_delete_))
        builder.setMessage(getString(R.string.label_delete_are_you_sure_you_want_to_delete_chat))
        builder.setPositiveButton(getString(R.string.delete)) { dialogInterface, which ->
            chatMessageViewModel.deleteConversation(chatConversationInfo)
            dialogInterface.dismiss()
        }
        builder.setNeutralButton(getString(R.string.label_cancel)) { dialogInterface, _ ->
            dialogInterface.dismiss()
        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    override fun onDestroy() {
        chatMessageViewModel.leaveRoom(GetMessageListRequest(conversationId))

        mediaPlayer?.apply {
            stop()
            release()
        }
        mediaPlayer = null
        super.onDestroy()
    }

    override fun didSearchTerm(term: String) {
    }

    override fun onDismissed(selectedContentType: GPHContentType) {
    }

    override fun onGifSelected(
        media: Media,
        searchTerm: String?,
        selectedContentType: GPHContentType
    ) {

        chatMessageViewModel.sendNewMessage(
            ChatSendMessageRequest(
                senderId = loggedInUserId,
                receiverId = if (chatConversationInfo.chatType.equals(resources.getString(R.string.label_group))) 0 else receiverId,
                message = "",
                conversationId = conversationId,
                fileType = MessageType.GIF,
                fileSize = "",
                file = "",
                fileUrl = media.images.original?.mp4Url ?: media.images.originalStill?.gifUrl,
                profileUrl = if (chatConversationInfo.chatType.equals(resources.getString(R.string.label_group))) chatConversationInfo.filePath else loggedInUserAvatar,
                name = loggedInName,
                chatType = chatConversationInfo.chatType,
                thumbnail = media.images.originalStill?.gifUrl ?: "",
                groupName = if (chatConversationInfo.chatType.equals(resources.getString(R.string.label_group))) chatConversationInfo.groupName else "",
                username = loggedInUserName
            )
        )
    }
}