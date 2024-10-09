package com.outgoer.ui.home.chat.viewmodel

import com.google.gson.stream.MalformedJsonException
import com.outgoer.api.authentication.model.OutgoerUser
import com.outgoer.api.chat.ChatMessageRepository
import com.outgoer.api.chat.model.ChatConversationInfo
import com.outgoer.api.chat.model.ChatMessageInfo
import com.outgoer.api.chat.model.GetConversationListRequest
import com.outgoer.api.chat.model.GroupChatConversationInfo
import com.outgoer.api.follow.FollowUserRepository
import com.outgoer.api.follow.model.AcceptRejectRequest
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.profile.ProfileRepository
import com.outgoer.api.profile.model.LocationUpdateRequest
import com.outgoer.api.profile.model.NearByUserResponse
import com.outgoer.api.sponty.model.SpontyResponse
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import com.outgoer.ui.suggested.viewmodel.SuggestedUsersViewState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class ConversationViewModel(
    private val chatMessageRepository: ChatMessageRepository,
    private val profileRepository: ProfileRepository,
    private val followUserRepository: FollowUserRepository,
) : BaseViewModel() {

    init {
        observeOtherNewMessages()
        observeConnection()
        sendUserRoom()
    }

    private val chatMessageStateSubject: PublishSubject<ChatMessageViewState> = PublishSubject.create()
    val messageViewState: Observable<ChatMessageViewState> = chatMessageStateSubject.hide()

    private var pageNumberG = 1
    private var isLoadMoreG: Boolean = true
    private var isLoadingG: Boolean = false
    private var conversationListG: ArrayList<ChatConversationInfo> = arrayListOf()

    fun resetPagination(search: String) {
        pageNumberG = 1
        isLoadingG = false
        isLoadMoreG = true
        conversationListG.clear()
        getGroupConversationList(search)
    }

    fun loadMoreConversationList(search: String) {
        if (!isLoadingG) {
            isLoadingG = true
            if (isLoadMoreG) {
                pageNumberG++
                getGroupConversationList(search)
            }
        }
    }

    fun getGroupConversationList(search: String) {
        chatMessageRepository.getGroupChatConversationList(GetConversationListRequest(search, pageNumberG, PER_PAGE_20))
            .doOnSubscribe {
                chatMessageStateSubject.onNext(ChatMessageViewState.LoadingState(true))
            }
            .doAfterTerminate {
                chatMessageStateSubject.onNext(ChatMessageViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({
                isLoadingG = false
                conversationListG.addAll(it)
                chatMessageStateSubject.onNext(ChatMessageViewState.LoadGroupConversationDetail(conversationListG))

                if(it.size == 0) {
                    isLoadMoreG = false
                }
            }, { throwable ->
                isLoadingG = false
                if(throwable is MalformedJsonException) {
                } else {
                    throwable.localizedMessage?.let {
                        chatMessageStateSubject.onNext(ChatMessageViewState.ErrorMessage(it))
                    }
                }
            }).autoDispose()
    }

    private fun observeOtherNewMessages() {
        chatMessageRepository.observeOtherNewMessages().subscribeOnIoAndObserveOnMainThread({
            chatMessageStateSubject.onNext(ChatMessageViewState.OtherNewMessages(it))
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    private fun observeConnection() {
        chatMessageRepository.observeConnection().subscribeOnIoAndObserveOnMainThread({
            sendUserRoom()
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    private fun sendUserRoom() {
        chatMessageRepository.sendUserRoom()
            .subscribeOnIoAndObserveOnMainThread({
                Timber.e("sendUserRoom")
            }, {
                Timber.e(it)
            }).autoDispose()
    }

    private var listOfSuggestedUser: ArrayList<OutgoerUser> = arrayListOf()
    private var pageNo = 1
    private var isLoadMore = true
    private var isLoading = false
    private val PER_PAGE_20 = 20

    fun loadSuggestedUser(search:String) {
        profileRepository.getSuggestedUsersList(pageNo = pageNo, perPage = PER_PAGE_20,search).doOnSubscribe {
            chatMessageStateSubject.onNext(ChatMessageViewState.LoadingState(true))
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            isLoading = false
            response?.let {
                listOfSuggestedUser.addAll(it)
                chatMessageStateSubject.onNext(ChatMessageViewState.LoadSuggestedUserList(listOfSuggestedUser))

                if(it.size < PER_PAGE_20) {
                    isLoadMore = false
                    chatMessageStateSubject.onNext(ChatMessageViewState.DoneLoading(true))
                }
            }
        }, { throwable ->
            isLoading = false
            chatMessageStateSubject.onNext(ChatMessageViewState.LoadingState(false))
            Timber.e(throwable)
            throwable.localizedMessage?.let {
                chatMessageStateSubject.onNext((ChatMessageViewState.ErrorMessage(it)))
            }
        }).autoDispose()
    }

    fun loadMore() {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo++
                loadSuggestedUser("")
            } else {
                chatMessageStateSubject.onNext(ChatMessageViewState.DoneLoading(true))
            }
        }
    }

    fun followUnfollowUser(outgoerUser: OutgoerUser) {
        followUserRepository.acceptRejectFollowRequest(AcceptRejectRequest(outgoerUser.id))
            .subscribeOnIoAndObserveOnMainThread({
                Timber.e(it.message)
            }, { throwable ->
                Timber.e(throwable)
                throwable.localizedMessage?.let {
                    chatMessageStateSubject.onNext((ChatMessageViewState.ErrorMessage(it)))
                }
            }).autoDispose()
    }


    fun loadNearByUser(locationInfo: LocationUpdateRequest) {
        profileRepository.getNearByUsersList(locationInfo).doOnSubscribe {
            chatMessageStateSubject.onNext(ChatMessageViewState.LoadingState(true))
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            isLoading = false
            response?.let {
                if (!it.isNullOrEmpty()) {
                    chatMessageStateSubject.onNext(ChatMessageViewState.LoadNearByUserList(it))
                } else {
                    isLoadMore = false
                }
            }
        }, { throwable ->
            isLoading = false
            chatMessageStateSubject.onNext(ChatMessageViewState.LoadingState(false))
            Timber.e(throwable)
            throwable.localizedMessage?.let {
                chatMessageStateSubject.onNext((ChatMessageViewState.ErrorMessage(it)))
            }
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

}

sealed class ChatMessageViewState {
    data class LoadingState(val isLoading: Boolean) : ChatMessageViewState()
    data class SuccessMessage(val successMessage: String) : ChatMessageViewState()
    data class DoneLoading(val isDone: Boolean) : ChatMessageViewState()
    data class ErrorMessage(val errorMessage: String) : ChatMessageViewState()
    data class LoadMessageDetail(val messageList: List<ChatMessageInfo>) : ChatMessageViewState()
    data class LoadConversationDetail(val conversationList: List<ChatConversationInfo>) : ChatMessageViewState()
    data class LoadGroupConversationDetail(val conversationList: List<ChatConversationInfo>) : ChatMessageViewState()
    data class OtherNewMessages(val chatConversationInfo: ChatConversationInfo) : ChatMessageViewState()

    data class LoadSuggestedUserList(val listOfSuggestedUser: ArrayList<OutgoerUser>) : ChatMessageViewState()
    data class LoadNearByUserList(val listOfSuggestedUser: List<NearByUserResponse>) : ChatMessageViewState()
    data class ClearDetailsNeedLogin(val listOfSuggestedUser: List<NearByUserResponse>) : ChatMessageViewState()
    data class DeleteConversationInfo(val chatUserInfo: ChatConversationInfo) : ChatMessageViewState()

}