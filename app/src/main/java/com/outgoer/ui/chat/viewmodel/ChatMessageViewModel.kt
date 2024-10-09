package com.outgoer.ui.chat.viewmodel

import android.content.Context
import com.outgoer.api.authentication.model.AwsInformation
import com.outgoer.api.aws.FileType
import com.outgoer.api.aws.FileUploader
import com.outgoer.api.aws.UploadFile
import com.outgoer.api.chat.ChatMessageRepository
import com.outgoer.api.chat.model.*
import com.outgoer.api.cloudflare.CloudFlareRepository
import com.outgoer.api.cloudflare.model.CloudFlareConfig
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.group.GroupRepository
import com.outgoer.api.group.model.GroupMemberRequest
import com.outgoer.api.group.model.GroupUserInfo
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.*
import com.outgoer.ui.comment.viewmodel.CommentViewState
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber
import java.io.File

class ChatMessageViewModel(
    private val cloudFlareRepository: CloudFlareRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val groupRepository: GroupRepository
) : BaseViewModel() {

    private val chatMessageStateSubject: PublishSubject<ChatMessageViewState> = PublishSubject.create()
    val messageViewState: Observable<ChatMessageViewState> = chatMessageStateSubject.hide()

    private var pageNumber = 1
    private var isLoadMore: Boolean = true
    private var isLoading: Boolean = false
    private var awsInformation: AwsInformation? = null
    private var allMessageList: MutableList<ChatMessageInfo> = mutableListOf()
    private var selectedTagUserInfo: MutableList<GroupUserInfo> = mutableListOf()

    init {
        loadAwsInformation()
        observeRoomJoined()
        observeNewMessage()
        observeOnlineStatus()
        observeMessageIsRead()
        observeTyping()
    }

    private fun loadAwsInformation() {

        awsInformation = AwsInformation(
            awsAccessKeyId = "AKIASTGZTNHR7GJD5H75",
            awsSecretAccessKey = "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",
            awsDefaultRegion = "us-east-1",
            awsBucket = "outgoer",
            awsBaseUrl = "https://outgoer.s3.amazonaws.com/"
        )
    }

    private fun observeRoomJoined() {
        chatMessageRepository.observeRoomJoined().subscribeOnIoAndObserveOnMainThread({
            chatMessageStateSubject.onNext(ChatMessageViewState.RoomConnected(it))
        }, {
            Timber.e(it)
            chatMessageStateSubject.onNext(ChatMessageViewState.RoomConnectionFail(it.localizedMessage ?: "Fail to join room"))
        }).autoDispose()
    }

    private fun observeNewMessage() {
        chatMessageRepository.observeNewMessage().subscribeOnIoAndObserveOnMainThread({
            chatMessageStateSubject.onNext(ChatMessageViewState.NewChatMessage(it))
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    fun joinRoom(joinRoomRequest: JoinRoomRequest) {
        chatMessageRepository.joinRoom(joinRoomRequest)
            .doOnSubscribe {
                chatMessageStateSubject.onNext(ChatMessageViewState.RoomConnecting)
            }
            .doOnError {
                chatMessageStateSubject.onNext(ChatMessageViewState.RoomConnectionFail("Fail to connect room"))
            }
            .subscribeOnIoAndObserveOnMainThread({

            }, {
                chatMessageStateSubject.onNext(ChatMessageViewState.RoomConnectionFail(it.localizedMessage ?: "Fail to connect room"))
            }).autoDispose()
    }

    fun leaveRoom(getMessageListRequest: GetMessageListRequest) {
        chatMessageRepository.leaveRoom(getMessageListRequest)
            .doOnSubscribe {
                chatMessageStateSubject.onNext(ChatMessageViewState.LeaveRoomConnecting)
            }
            .doOnError {
                chatMessageStateSubject.onNext(ChatMessageViewState.LeaveRoomConnectingFail("Fail to connect room"))
            }
            .subscribeOnIoAndObserveOnMainThread({

            }, {
                chatMessageStateSubject.onNext(ChatMessageViewState.LeaveRoomConnectingFail(it.localizedMessage ?: "Fail to connect room"))
            }).autoDispose()
    }

    fun sendNewMessage(chatSendMessageRequest: ChatSendMessageRequest) {
        if(selectedTagUserInfo.size > 0) {
            chatSendMessageRequest.mentions_ids = getSelectedTagMemberUserIds(
                selectedTagUserInfo,
                chatSendMessageRequest.message ?: ""
            )
        }

        chatMessageRepository.sendMessage(chatSendMessageRequest)
            .subscribeOnIoAndObserveOnMainThread({
                resetPagination(chatSendMessageRequest.conversationId ?: 0)
            }, {
                Timber.e(it)
            }).autoDispose()
        selectedTagUserInfo.clear()

    }

    fun forwardMessage(msgId: Int, receiverIds: String) {
        chatMessageRepository.forwardMessage(msgId, receiverIds)
            .subscribeOnIoAndObserveOnMainThread({

            }, {
                Timber.e(it)
            }).autoDispose()
    }

    private fun getMessageList(conversationId: Int) {
        chatMessageRepository.getChatMessageList(pageNumber, GetMessageListRequest(conversationId))
            .doOnSubscribe {
                chatMessageStateSubject.onNext(ChatMessageViewState.LoadingState(true))
            }
            .doAfterTerminate {
                chatMessageStateSubject.onNext(ChatMessageViewState.LoadingState(false))
                isLoading = false
            }
            .subscribeOnIoAndObserveOnMainThread({
                if (it != null) {
                    if(pageNumber == 1) {
                        allMessageList.clear()
                    }

                    allMessageList.addAll(it)

                    println("Api:allMessageList " + allMessageList.size)

                    chatMessageStateSubject.onNext(ChatMessageViewState.LoadChatMessageList(allMessageList))
                    if (it.isEmpty()) {
                        isLoadMore = false
                    }
                }
            }, {
                Timber.e(it)
            }).autoDispose()
    }

    fun loadMore(conversationId: Int) {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNumber++
                getMessageList(conversationId)
            }
        }
    }

    fun resetPagination(conversationId: Int) {
        pageNumber = 1
        isLoading = false
        isLoadMore = true
        getMessageList(conversationId)
    }

    fun getCloudFlareConfig() {
        cloudFlareRepository.getCloudFlareConfig()
            .doOnSubscribe {
                chatMessageStateSubject.onNext(ChatMessageViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                chatMessageStateSubject.onNext(ChatMessageViewState.LoadingState(false))
                if (response.success) {
                    val cloudFlareConfig = response.data
                    if (cloudFlareConfig != null) {
                        chatMessageStateSubject.onNext(ChatMessageViewState.GetCloudFlareConfig(cloudFlareConfig))
                    } else {
                        response.message?.let {
                            chatMessageStateSubject.onNext(ChatMessageViewState.CloudFlareConfigErrorMessage(it))
                        }
                    }
                }
            }, { throwable ->
                chatMessageStateSubject.onNext(ChatMessageViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    chatMessageStateSubject.onNext(ChatMessageViewState.CloudFlareConfigErrorMessage(it))
                }
            }).autoDispose()
    }

    fun uploadImageToCloudFlare(context: Context, cloudFlareConfig: CloudFlareConfig, imageFile: File, userId: Int) {
        val imageTempPathDir = context.getExternalFilesDir("OutgoerImages")?.path
        val fileName = getCommonPhotoFileName(userId)
        val imageCopyFile = File(imageTempPathDir + File.separator + fileName + ".jpg")
        val finalImageFile = imageFile.copyTo(imageCopyFile)

        val apiUrl = cloudFlareImageUploadBaseUrl.format(cloudFlareConfig.accountId)
        val authToken = "Bearer ".plus(cloudFlareConfig.apiToken)
        val filePart = MultipartBody.Part.createFormData(
            "file", finalImageFile.name, finalImageFile.asRequestBody("image/*".toMediaTypeOrNull())
        )
        cloudFlareRepository.uploadImageToCloudFlare(apiUrl, authToken, filePart)
            .doOnSubscribe {
                chatMessageStateSubject.onNext(ChatMessageViewState.UploadImageCloudFlareLoading(true))
            }.doAfterTerminate {
                chatMessageStateSubject.onNext(ChatMessageViewState.UploadImageCloudFlareLoading(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.success) {
                    val result = response.result
                    if (result != null) {
                        val variants = result.variants
                        if (!variants.isNullOrEmpty()) {
                            chatMessageStateSubject.onNext(ChatMessageViewState.UploadImageCloudFlareSuccess(variants.first()))
                        } else {
                            handleCloudFlareMediaUploadError(response.errors)
                        }
                    } else {
                        handleCloudFlareMediaUploadError(response.errors)
                    }
                } else {
                    handleCloudFlareMediaUploadError(response.errors)
                }
            }, { throwable ->
                chatMessageStateSubject.onNext(ChatMessageViewState.UploadImageCloudFlareLoading(false))
                throwable.localizedMessage?.let {
                    chatMessageStateSubject.onNext(ChatMessageViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun uploadVideoToCloudFlare(context: Context, cloudFlareConfig: CloudFlareConfig, videoFile: File, userId: Int) {
        val videoTempPathDir = context.getExternalFilesDir("OutgoerVideos")?.path
        val fileName = getCommonVideoFileName(userId)
        val videoCopyFile = File(videoTempPathDir + File.separator + fileName + ".mp4")
        val finalImageFile = videoFile.copyTo(videoCopyFile)

        val apiUrl = cloudFlareVideoUploadBaseUrl.format(cloudFlareConfig.accountId)
        val authToken = "Bearer ".plus(cloudFlareConfig.videoKey)
        val filePart = MultipartBody.Part.createFormData(
            "file", finalImageFile.name, finalImageFile.asRequestBody("image/*".toMediaTypeOrNull())
        )
        cloudFlareRepository.uploadVideoUsingTus(apiUrl, authToken, finalImageFile)
            .subscribeOn(Schedulers.io())
            .flatMap {
                cloudFlareRepository.getUploadVideoDetails(it, authToken)
            }
            .observeOn(AndroidSchedulers.mainThread()) // Observe on main thread if needed
            .subscribe({
                chatMessageStateSubject.onNext(ChatMessageViewState.UploadVideoCloudFlareSuccess(it.uid.toString(), it.thumbnail.toString(), ""))

            }, { throwable ->
                Timber.e(throwable)
                chatMessageStateSubject.onNext(ChatMessageViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    chatMessageStateSubject.onNext(ChatMessageViewState.ErrorMessage(it))
                }
            }).autoDispose()

    }

    private fun handleCloudFlareMediaUploadError(errors: List<String>?) {
        if (!errors.isNullOrEmpty()) {
            val error = errors.firstOrNull()
            if (error != null) {
                chatMessageStateSubject.onNext(ChatMessageViewState.ErrorMessage(error.toString()))
            }
        }
    }

    fun updateOnlineStatus(request: UpdateOnlineStatusRequest) {
        chatMessageRepository.updateOnlineStatus(request)
            .subscribeOnIoAndObserveOnMainThread({

            }, {
                Timber.e(it)
            }).autoDispose()
    }

    private fun observeOnlineStatus() {
        chatMessageRepository.observeOnlineStatus().subscribeOnIoAndObserveOnMainThread({
            chatMessageStateSubject.onNext(ChatMessageViewState.OnlineStatus(it.data))
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    fun sendMessageIsRead(request: SendMessageIsReadRequest) {
        chatMessageRepository.sendMessageIsRead(request)
            .subscribeOnIoAndObserveOnMainThread({

            }, {
                Timber.e(it)
            }).autoDispose()
    }

    private fun observeMessageIsRead() {
        chatMessageRepository.observeMessageIsRead().subscribeOnIoAndObserveOnMainThread({
            chatMessageStateSubject.onNext(ChatMessageViewState.UpdateMessageIsRead(it))
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    fun setUserOffline(request: SetUserOfflineRequest) {
        chatMessageRepository.setUserOffline(request)
            .subscribeOnIoAndObserveOnMainThread({

            }, {
                Timber.e(it)
            }).autoDispose()
    }

    fun deleteConversation(chatUserInfo: ChatConversationInfo) {
        chatMessageRepository.deleteChatConversation(chatUserInfo.conversationId)
            .subscribeOnIoAndObserveOnMainThread({
                Timber.e(it.message)
                if(it.success) {
                    chatMessageStateSubject.onNext((ChatMessageViewState.DeleteConversationInfo(chatUserInfo)))
                }
            }, { throwable ->
                Timber.e(throwable)
                throwable.localizedMessage?.let {
                    chatMessageStateSubject.onNext((ChatMessageViewState.ErrorMessage(it)))
                }
            }).autoDispose()
    }

    fun deleteChatGroup(chatUserInfo: ChatConversationInfo) {
        groupRepository.deleteGroupInfo(chatUserInfo.conversationId)
            .subscribeOnIoAndObserveOnMainThread({
                Timber.e(it.message)
                if(it.success) {
                    chatMessageStateSubject.onNext((ChatMessageViewState.DeleteGroupInfo(chatUserInfo)))
                }
            }, { throwable ->
                Timber.e(throwable)
                throwable.localizedMessage?.let {
                    chatMessageStateSubject.onNext((ChatMessageViewState.ErrorMessage(it)))
                }
            }).autoDispose()
    }

    fun deleteChatMessage(chatUserInfo: ChatMessageInfo) {
        chatMessageRepository.deleteChatMessage(chatUserInfo.id)
            .subscribeOnIoAndObserveOnMainThread({
                Timber.e(it.message)
                if(it.success) {
                    chatMessageStateSubject.onNext((ChatMessageViewState.DeleteMessage(chatUserInfo)))
                }
            }, { throwable ->
                Timber.e(throwable)
                throwable.localizedMessage?.let {
                    chatMessageStateSubject.onNext((ChatMessageViewState.ErrorMessage(it)))
                }
            }).autoDispose()
    }


    fun uploadVideo(
        audioFilePath: String? = null,
        chatSendMessageRequest: ChatSendMessageRequest
    ) {

        val listOfUploadingFile: MutableList<UploadFile> = mutableListOf()
        if (audioFilePath != null) {
            listOfUploadingFile.add(UploadFile(File(audioFilePath), FileType.music))
        }
        Timber.i(listOfUploadingFile.toString())

        awsInformation?.let { information ->
            FileUploader().uploadMultipleFile(
                OutgoerApplication.context,
                listOfUploadingFile,
                information
            ).map { fileList ->
                Timber.i("File Url %s", fileList.toString())

                chatSendMessageRequest.fileUrl = fileList.get(0).fileUrl
                chatMessageRepository.sendMessage(
                    chatSendMessageRequest
                )
            }.doOnSubscribe {
                chatMessageStateSubject.onNext(ChatMessageViewState.VideoUploadingState(true))
            }.doAfterTerminate {
                chatMessageStateSubject.onNext(ChatMessageViewState.VideoUploadingState(false))
            }.subscribeOnIoAndObserveOnMainThread({
                chatMessageStateSubject.onNext(
                    ChatMessageViewState.SuccessMessage(
                        "Audio Uploaded Successfully!"
                    )
                )
            }, {
                Timber.e(it)
                chatMessageStateSubject.onNext(
                    ChatMessageViewState.ErrorMessage(
                        it.localizedMessage ?: ""
                    )
                )
            }).autoDispose()
        } ?: run {
            chatMessageStateSubject.onNext(ChatMessageViewState.ErrorMessage("Server Error. Please try after sometime"))
        }
    }


    fun getGroupInfo(groupId: Int) {
        chatMessageStateSubject.onNext(ChatMessageViewState.LoadingState(true))
        groupRepository.getGroupInfo(groupId)
            .doOnSubscribe {
            }.doAfterTerminate {
//                groupStateSubjects.onNext(GroupViewState.LoadingState(false))
            }.subscribeOnIoAndObserveOnMainThread({ response ->
                chatMessageStateSubject.onNext(ChatMessageViewState.LoadingState(false))
                response.data?.let {
                    chatMessageStateSubject.onNext(ChatMessageViewState.GetGroupInfo(it))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    Timber.tag("<><>").e("CreateGroupRequest".plus(it))
                }
            }).autoDispose()
    }

    private fun observeMessageReaction() {
        chatMessageRepository.observeMessageReaction().subscribeOnIoAndObserveOnMainThread({
            chatMessageStateSubject.onNext(ChatMessageViewState.ReactionMessage(it))
        }, {
            Timber.e(it)
            chatMessageStateSubject.onNext(ChatMessageViewState.RoomConnectionFail(it.localizedMessage ?: "Fail to join room"))
        }).autoDispose()
    }


    private fun observeTyping() {
        chatMessageRepository.observeTyping().subscribeOnIoAndObserveOnMainThread({
            chatMessageStateSubject.onNext(ChatMessageViewState.TypingMessage(it))
        }, {
            Timber.e(it)
            chatMessageStateSubject.onNext(ChatMessageViewState.RoomConnectionFail(it.localizedMessage ?: "Fail to join room"))
        }).autoDispose()
    }


    fun addReactions(addReactionSocketEvent: AddReactionSocketEvent) {
        chatMessageRepository.addReactions(addReactionSocketEvent)
            .subscribeOnIoAndObserveOnMainThread({
                observeMessageReaction()
            }, {
                Timber.e(it)
            }).autoDispose()
    }

    fun removeReactions(removeReactionSocketEvent: RemoveReactionSocketEvent) {
        chatMessageRepository.removeReactions(removeReactionSocketEvent)
            .subscribeOnIoAndObserveOnMainThread({
                observeMessageReaction()
            }, {
                Timber.e(it)
            }).autoDispose()
    }

    fun typingMessage(request: MessageTypingSocketEvent) {
        chatMessageRepository.typingMessage(request)
            .subscribeOnIoAndObserveOnMainThread({

            }, {
                Timber.e(it)
            }).autoDispose()
    }

    private var pageReactionNumber = 1
    private var isReactionLoading: Boolean = false
    private var isReactionLoadMore: Boolean = true
    private var allReactionList: MutableList<Reaction> = mutableListOf()

    fun resetReactionPagination(messageId: Int) {
        pageReactionNumber = 1
        isReactionLoading = false
        isReactionLoadMore = true
        getChatReactionList(messageId, true)
    }

    private fun getChatReactionList(messageId: Int, reload: Boolean) {
        chatMessageRepository.getChatReactionList(pageReactionNumber, messageId)
            .doOnSubscribe {
                chatMessageStateSubject.onNext(ChatMessageViewState.LoadingReactionState(reload))
            }
            .doAfterTerminate {
                chatMessageStateSubject.onNext(ChatMessageViewState.LoadingReactionState(false))
                isLoading = false
            }
            .subscribeOnIoAndObserveOnMainThread({
                if (it != null) {
                    if(pageReactionNumber == 1) {
                        allReactionList.clear()
                    }
                    allReactionList.addAll(it)
                    chatMessageStateSubject.onNext(ChatMessageViewState.ChatReactionList(allReactionList))
                    if (it.isEmpty()) {
                        isReactionLoadMore = false
                    }
                }
            }, { throwable ->
                Timber.e(throwable)
                throwable.localizedMessage?.let {
                    chatMessageStateSubject.onNext((ChatMessageViewState.ErrorMessage(it)))
                }
            }).autoDispose()
    }

    fun loadReactionMore(messageId: Int) {
        if (!isReactionLoading) {
            isReactionLoading = true
            if (isReactionLoadMore) {
                pageReactionNumber++
                getChatReactionList(messageId, false)
            }
        }
    }



    fun searchTagUserClicked(
        initialDescriptionString: String,
        subString: String,
        followUser: GroupUserInfo
    ) {
        if (followUser !in selectedTagUserInfo) {
            selectedTagUserInfo.add(followUser)
            println("selectedTagUserInfo: " + selectedTagUserInfo.get(0).userId)
            println("selectedTagUserInfo: " + selectedTagUserInfo.get(0))
        }


        val remainString = initialDescriptionString.removePrefix(subString)
        if (subString.length == initialDescriptionString.length) {
            val lastIndexOfToken = initialDescriptionString.findLastAnyOf(listOf("@"))?.first ?: return
            Timber.i("lastIndexOfToken %s", lastIndexOfToken)
            val tempSubString = initialDescriptionString.substring(0, lastIndexOfToken)
            val descriptionString = "$tempSubString@${followUser.username}"
            chatMessageStateSubject.onNext(ChatMessageViewState.UpdateDescriptionText(descriptionString.plus(" ")))
        } else {
            val lastIndexOfToken = subString.findLastAnyOf(listOf("@"))?.first ?: return
            Timber.i("lastIndexOfToken %s", lastIndexOfToken)
            val tempSubString = subString.substring(0, lastIndexOfToken)
            val descriptionString = "$tempSubString @${followUser.username} $remainString"
            chatMessageStateSubject.onNext(ChatMessageViewState.UpdateDescriptionText(descriptionString.plus(" ")))
        }
    }





    private var pageNumberGMember = 1
    private var isLoadingGMember: Boolean = false
    private var isLoadMoreGMember: Boolean = true
    private var groupMemberList: MutableList<GroupUserInfo> = mutableListOf()

    fun resetGroupMemberPagination(groupMemberRequest: GroupMemberRequest) {
        pageNumberGMember = 1
        isLoadingGMember = false
        isLoadMoreGMember = true
        getGroupMemberList(groupMemberRequest)
    }

    private fun getGroupMemberList(groupMemberRequest: GroupMemberRequest) {
        groupRepository.getGroupMemberInfo(pageNumberGMember, groupMemberRequest)
            .doOnSubscribe {
                chatMessageStateSubject.onNext(ChatMessageViewState.LoadingReactionState(true))
            }
            .doAfterTerminate {
                chatMessageStateSubject.onNext(ChatMessageViewState.LoadingReactionState(false))
                isLoading = false
            }
            .subscribeOnIoAndObserveOnMainThread({
                if (it != null) {
                    if(pageNumberGMember == 1) {
                        groupMemberList.clear()
                    }

                    it?.data?.let { groupInfo ->
                        groupMemberList.addAll(groupInfo)
                    }

                    if(groupMemberRequest.search.isNullOrEmpty()) {
                        chatMessageStateSubject.onNext(ChatMessageViewState.InitialGroupMemberList(groupMemberList))
                    } else {
                        chatMessageStateSubject.onNext(ChatMessageViewState.GroupMemberList(groupMemberList))
                    }
                }
            }, { throwable ->
                Timber.e(throwable)
                throwable.localizedMessage?.let {
                    chatMessageStateSubject.onNext((ChatMessageViewState.ErrorMessage(it)))
                }
            }).autoDispose()
    }

    fun loadGroupMemberMore(groupMemberRequest: GroupMemberRequest) {
        if (!isLoadingGMember) {
            isLoadingGMember = true
            if (isLoadMoreGMember) {
                pageNumberGMember++
                getGroupMemberList(groupMemberRequest)
            }
        }
    }
}

sealed class ChatMessageViewState {
    data class LoadingState(val isLoading: Boolean) : ChatMessageViewState()
    data class VideoUploadingState(val isLoading: Boolean) : ChatMessageViewState()
    data class SuccessMessage(val successMessage: String) : ChatMessageViewState()
    data class ErrorMessage(val errorMessage: String) : ChatMessageViewState()
    object RoomConnecting : ChatMessageViewState()
    object LeaveRoomConnecting : ChatMessageViewState()
    data class TypingMessage(val messageTyping: MessageTypingSocketEvent) : ChatMessageViewState()
    data class ReactionMessage(val messageTyping: ChatMessageListener) : ChatMessageViewState()
    data class RoomConnected(val joinRoomRequest: JoinRoomRequest) : ChatMessageViewState()
    data class RoomConnectionFail(val errorMessage: String) : ChatMessageViewState()
    data class LeaveRoomConnectingFail(val errorMessage: String) : ChatMessageViewState()
    data class LoadChatMessageList(val listOfChatMessageInfo: List<ChatMessageInfo>) : ChatMessageViewState()
    data class LoadingReactionState(val isLoading: Boolean) : ChatMessageViewState()
    data class ChatReactionList(val listOfChatReactionInfo: List<Reaction>) : ChatMessageViewState()
    data class InitialGroupMemberList(val listGroupUser: List<GroupUserInfo>) : ChatMessageViewState()
    data class GroupMemberList(val listGroupUser: List<GroupUserInfo>) : ChatMessageViewState()
    data class NewChatMessage(val chatMessageInfo: ChatMessageInfo) : ChatMessageViewState()

    data class GetCloudFlareConfig(val cloudFlareConfig: CloudFlareConfig) : ChatMessageViewState()
    data class CloudFlareConfigErrorMessage(val errorMessage: String) : ChatMessageViewState()
    data class UploadImageCloudFlareLoading(val isLoading: Boolean) : ChatMessageViewState()
    data class UploadImageCloudFlareSuccess(val imageUrl: String) : ChatMessageViewState()
//    data class UploadVideoCloudFlareSuccess(val uid: String, val thumbnail: String) : ChatMessageViewState()
    data class UploadVideoCloudFlareSuccess(val uid: String, val thumbnail: String, val preview: String) : ChatMessageViewState()

    data class OnlineStatus(val isOnline: List<ChatOnlineStatus>?) : ChatMessageViewState()
    data class UpdateMessageIsRead(val chatMessageInfo: SendMessageIsReadRequest) : ChatMessageViewState()
    data class DeleteConversationInfo(val chatUserInfo: ChatConversationInfo) : ChatMessageViewState()
    data class DeleteGroupInfo(val chatUserInfo: ChatConversationInfo) : ChatMessageViewState()

    data class DeleteMessage(val chatUserInfo: ChatMessageInfo) : ChatMessageViewState()
    data class GetGroupInfo(val groupInfoResponse: ChatConversationInfo) : ChatMessageViewState()
    data class UpdateDescriptionText(val descriptionString: String) : ChatMessageViewState()

    object ReactionAdded: ChatMessageViewState()
}