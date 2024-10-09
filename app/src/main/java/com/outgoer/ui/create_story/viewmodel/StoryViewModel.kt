package com.outgoer.ui.create_story.viewmodel

import android.content.Context
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.chat.ChatMessageRepository
import com.outgoer.api.chat.model.ChatMessageInfo
import com.outgoer.api.chat.model.ChatSendMessageRequest
import com.outgoer.api.chat.model.ConversationRequest
import com.outgoer.api.chat.model.JoinRoomRequest
import com.outgoer.api.cloudflare.CloudFlareRepository
import com.outgoer.api.cloudflare.model.CloudFlareConfig
import com.outgoer.api.event.EventRepository
import com.outgoer.api.event.model.CreateEventResponse
import com.outgoer.api.event.model.EventListData
import com.outgoer.api.hashtag.model.HashtagResponse
import com.outgoer.api.story.StoryRepository
import com.outgoer.api.story.model.MentionUser
import com.outgoer.api.story.model.StoryListResponse
import com.outgoer.api.story.model.StoryRequest
import com.outgoer.api.story.model.ViewStoryRequest
import com.outgoer.api.venue.model.RequestSearchVenue
import com.outgoer.api.venue.model.VenueMapInfo
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.cloudFlareImageUploadBaseUrl
import com.outgoer.base.extension.cloudFlareVideoUploadBaseUrl
import com.outgoer.base.extension.getCommonPhotoFileName
import com.outgoer.base.extension.getCommonVideoFileName
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import com.outgoer.ui.add_hashtag.viewmodel.HashtagViewModel
import com.outgoer.ui.chat.viewmodel.ChatMessageViewState
import com.outgoer.ui.otherprofile.viewmodel.OtherUserProfileViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber
import java.io.File

class StoryViewModel(private val storyRepository: StoryRepository,
                     private val cloudFlareRepository: CloudFlareRepository,
                     private val chatMessageRepository: ChatMessageRepository,
                     private val loginUserCache: LoggedInUserCache) : BaseViewModel() {

    private val storyViewStateSubjects: PublishSubject<StoryViewState> = PublishSubject.create()
    val storyViewState: Observable<StoryViewState> = storyViewStateSubjects.hide()

    init {
        observeRoomJoined()
        observeNewMessage()
    }

    private fun observeRoomJoined() {
        chatMessageRepository.observeRoomJoined().subscribeOnIoAndObserveOnMainThread({
            storyViewStateSubjects.onNext(StoryViewState.RoomConnected(it))
        }, {
            Timber.e(it)
            storyViewStateSubjects.onNext(StoryViewState.RoomConnectionFail(it.localizedMessage ?: "Fail to join room"))
        }).autoDispose()
    }

    private fun observeNewMessage() {
        chatMessageRepository.observeNewMessage().subscribeOnIoAndObserveOnMainThread({
            storyViewStateSubjects.onNext(StoryViewState.NewChatMessage(it))
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    fun sendNewMessage(chatSendMessageRequest: ChatSendMessageRequest) {
        chatMessageRepository.sendMessage(chatSendMessageRequest)
            .subscribeOnIoAndObserveOnMainThread({
            }, {
                Timber.e(it)
            }).autoDispose()
    }


    fun getCloudFlareConfig() {
        cloudFlareRepository.getCloudFlareConfig()
            .doOnSubscribe {
                storyViewStateSubjects.onNext(StoryViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                storyViewStateSubjects.onNext(StoryViewState.LoadingState(false))
                if (response.success) {
                    val cloudFlareConfig = response.data
                    if (cloudFlareConfig != null) {
                        storyViewStateSubjects.onNext(StoryViewState.GetCloudFlareConfig(cloudFlareConfig))
                    } else {
                        response.message?.let {
                            storyViewStateSubjects.onNext(StoryViewState.CloudFlareConfigErrorMessage(it))
                        }
                    }
                }
            }, { throwable ->
                storyViewStateSubjects.onNext(StoryViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    storyViewStateSubjects.onNext(StoryViewState.CloudFlareConfigErrorMessage(it))
                }
            }).autoDispose()
    }

    fun uploadImageToCloudFlare(context: Context, cloudFlareConfig: CloudFlareConfig, imageFile: File, selectedMediaType: String) {
        val imageTempPathDir = context.getExternalFilesDir("OutgoerImages")?.path
        val fileName = getCommonPhotoFileName(loginUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0)
        val imageCopyFile = File(imageTempPathDir + File.separator + fileName + ".jpg")
        val finalImageFile = imageFile.copyTo(imageCopyFile)

        val apiUrl = cloudFlareImageUploadBaseUrl.format(cloudFlareConfig.accountId)
        val authToken = "Bearer ".plus(cloudFlareConfig.apiToken)
        val filePart = MultipartBody.Part.createFormData(
            "file", finalImageFile.name, finalImageFile.asRequestBody("image/*".toMediaTypeOrNull())
        )
        cloudFlareRepository.uploadImageToCloudFlare(apiUrl, authToken, filePart)
            .doOnSubscribe {
                storyViewStateSubjects.onNext(StoryViewState.UploadMediaCloudFlareLoading(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.success) {
                    val result = response.result
                    if (result != null) {
                        val variants = result.variants
                        if (!variants.isNullOrEmpty()) {
                            storyViewStateSubjects.onNext(StoryViewState.UploadMediaCloudFlareSuccess(variants.first(), selectedMediaType))
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
                storyViewStateSubjects.onNext(StoryViewState.UploadMediaCloudFlareLoading(false))
                throwable.localizedMessage?.let {
                    storyViewStateSubjects.onNext(StoryViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun uploadVideoToCloudFlare(context: Context, cloudFlareConfig: CloudFlareConfig, videoFile: File, selectedMediaType: String) {
        val videoTempPathDir = context.getExternalFilesDir("OutgoerVideos")?.path
        val fileName = getCommonVideoFileName(loginUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0)
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
                storyViewStateSubjects.onNext(StoryViewState.UploadMediaCloudFlareVideoSuccess(it.uid.toString(), it.thumbnail.toString(), selectedMediaType))
            }, { throwable ->
                Timber.e(throwable)
                storyViewStateSubjects.onNext(StoryViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    storyViewStateSubjects.onNext(StoryViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    private fun handleCloudFlareMediaUploadError(errors: List<String>?) {
        if (!errors.isNullOrEmpty()) {
            val error = errors.firstOrNull()
            if (error != null) {
                storyViewStateSubjects.onNext(StoryViewState.ErrorMessage(error.toString()))
            }
        }
    }

    fun createStory(storyRequest: StoryRequest) {
        storyRepository.createStory(storyRequest)
            .doOnSubscribe {
                storyViewStateSubjects.onNext(StoryViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({
                it.data?.let { story ->
                    storyViewStateSubjects.onNext(StoryViewState.StoryDetailsResponse(story))
                }
            }, { throwable ->
                storyViewStateSubjects.onNext(StoryViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    storyViewStateSubjects.onNext(StoryViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun deleteStory(storyId: Int) {
        storyRepository.deleteStory(storyId)
            .doOnSubscribe {
                storyViewStateSubjects.onNext(StoryViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({
                it.message?.let { story ->
                    storyViewStateSubjects.onNext(StoryViewState.SuccessMessage(story))
                }
            }, { throwable ->
                storyViewStateSubjects.onNext(StoryViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    storyViewStateSubjects.onNext(StoryViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun viewStory(viewStoryRequest: ViewStoryRequest) {
        storyRepository.viewStory(viewStoryRequest)
            .doOnSubscribe {
                storyViewStateSubjects.onNext(StoryViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({
            }, { throwable ->
                storyViewStateSubjects.onNext(StoryViewState.LoadingState(false))
                throwable.localizedMessage?.let {
//                    storyViewStateSubjects.onNext(StoryViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    private var listOfMentionUsers: ArrayList<MentionUser> = arrayListOf()

    private var pageNumber: Int = 1
    private var isLoadMore: Boolean = true
    private var isLoading: Boolean = false

    fun resetPagination(viewStoryRequest: ViewStoryRequest) {
        listOfMentionUsers.clear()
        pageNumber = 1
        isLoadMore = true
        isLoading = false
        viewListStory(viewStoryRequest)
    }

    fun loadMore(viewStoryRequest: ViewStoryRequest) {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNumber++
                viewListStory(viewStoryRequest)
            }
        }
    }
    fun viewListStory(viewStoryRequest: ViewStoryRequest) {
        storyRepository.viewListStory(pageNumber, viewStoryRequest)
            .doOnSubscribe {
                storyViewStateSubjects.onNext(StoryViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({

                if(pageNumber == 1) {
                    listOfMentionUsers.clear()
                }
                storyViewStateSubjects.onNext(StoryViewState.LoadingState(false))

                it.data?.let { hashTagList ->
                    listOfMentionUsers.addAll(hashTagList)
                    storyViewStateSubjects.onNext(StoryViewState.ViewListUserInfo(listOfMentionUsers))
                }
            }, { throwable ->
                storyViewStateSubjects.onNext(StoryViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    storyViewStateSubjects.onNext(StoryViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getConversation(userId: Int) {
        chatMessageRepository.getConversationId(ConversationRequest(userId))
            .doOnSubscribe {
                storyViewStateSubjects.onNext(StoryViewState.LoadingState(true))
            }
            .doAfterTerminate {
                storyViewStateSubjects.onNext(StoryViewState.LoadingState(false))

            }
            .subscribeOnIoAndObserveOnMainThread({
                storyViewStateSubjects.onNext(
                    StoryViewState.GetConversation(
                        it.conversationId ?: 0
                    )
                )
            }, { throwable ->
                throwable.localizedMessage?.let {
                    storyViewStateSubjects.onNext(StoryViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

}

sealed class StoryViewState {
    data class ErrorMessage(val errorMessage: String) : StoryViewState()
    data class SuccessMessage(val successMessage: String) : StoryViewState()
    data class LoadingState(val isLoading: Boolean) : StoryViewState()
    data class StoryDetailsResponse(val storyList: StoryListResponse) : StoryViewState()
    data class GetCloudFlareConfig(val cloudFlareConfig: CloudFlareConfig) : StoryViewState()
    data class CloudFlareConfigErrorMessage(val errorMessage: String) : StoryViewState()
    data class UploadMediaCloudFlareLoading(val isLoading: Boolean) : StoryViewState()
    data class UploadMediaCloudFlareSuccess(val mediaUrl: String, val selectedMediaType: String) : StoryViewState()
    data class UploadMediaCloudFlareVideoSuccess(val uid: String, val mediaUrl: String, val selectedMediaType: String) : StoryViewState()

    data class RoomConnected(val joinRoomRequest: JoinRoomRequest) : StoryViewState()
    data class RoomConnectionFail(val errorMessage: String) : StoryViewState()

    data class NewChatMessage(val chatMessageInfo: ChatMessageInfo) : StoryViewState()
    data class ViewListUserInfo(val listOfMentionUser: ArrayList<MentionUser>) : StoryViewState()
    data class GetConversation(val conversationId: Int) : StoryViewState()
}
