package com.outgoer.ui.chat.viewmodel

import com.outgoer.api.chat.ChatMessageRepository
import com.outgoer.api.chat.model.ConversationRequest
import com.outgoer.api.follow.FollowUserRepository
import com.outgoer.api.follow.model.AcceptRejectRequest
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.follow.model.GetFollowersAndFollowingRequest
import com.outgoer.api.profile.ProfileRepository
import com.outgoer.api.profile.model.LocationUpdateRequest
import com.outgoer.api.profile.model.NearByUserResponse
import com.outgoer.api.search.SearchRepository
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import com.outgoer.ui.otherprofile.viewmodel.OtherUserProfileViewModel
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class CreateNewMessageViewModel(
    private val profileRepository: ProfileRepository,
    private val followUserRepository: FollowUserRepository,
    private val searchRepository: SearchRepository,
    private val chatMessageRepository : ChatMessageRepository
) : BaseViewModel() {

    private val createNewMessageStateSubject: PublishSubject<CreateMessageViewState> = PublishSubject.create()
    val createNewMessageState: Observable<CreateMessageViewState> = createNewMessageStateSubject.hide()

    private var listOfFollowing: ArrayList<FollowUser> = arrayListOf()
    private var searchText = ""
    private var pageNo = 0
    private var isLoading = false
    private var isLoadMore = true

    private var searchpageNo: Int = 1
    private var searchisLoadMore: Boolean = true
    private var searchisLoading: Boolean = false
    private var searchTextGlobal = ""
    private var listOfSearchAccountData: MutableList<FollowUser> = mutableListOf()

    fun searchFollowingList(userId: Int, searchText: String) {
        this.searchText = searchText
        pageNo = 1
        isLoading = false
        isLoadMore = true
        listOfFollowing.clear()
        getFollowingList(userId)
    }

    fun loadMoreFollowingList(userId: Int) {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo += 1
                getFollowingList(userId)
            }
        }
    }

    private fun getFollowingList(userId: Int) {
        followUserRepository.getAllFollowingList(pageNo, GetFollowersAndFollowingRequest(userId, searchText))
            .doOnSubscribe {
                createNewMessageStateSubject.onNext(CreateMessageViewState.LoadingState(true))
            }
            .doAfterTerminate {
                createNewMessageStateSubject.onNext(CreateMessageViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    if (pageNo == 1) {
                        listOfFollowing.addAll(response)
                        createNewMessageStateSubject.onNext(CreateMessageViewState.FollowingList(listOfFollowing))
                        isLoading = false
                    } else {
                        if (!it.isNullOrEmpty()) {
                            listOfFollowing.addAll(it)
                            createNewMessageStateSubject.onNext(CreateMessageViewState.FollowingList(listOfFollowing))
                            isLoading = false
                        } else {
                            isLoadMore = false
                        }
                    }
                }
            }, { throwable ->
                createNewMessageStateSubject.onNext(CreateMessageViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    createNewMessageStateSubject.onNext(CreateMessageViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun loadNearByUser(locationInfo: LocationUpdateRequest) {
        profileRepository.getNearByUsersList(locationInfo).doOnSubscribe {
            createNewMessageStateSubject.onNext(CreateMessageViewState.LoadingState(true))
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            response?.let {
                if (!it.isNullOrEmpty()) {
                    createNewMessageStateSubject.onNext(CreateMessageViewState.LoadNearByUserList(it))
                }
            }
        }, { throwable ->
            createNewMessageStateSubject.onNext(CreateMessageViewState.LoadingState(false))
            Timber.e(throwable)
            throwable.localizedMessage?.let {
                createNewMessageStateSubject.onNext((CreateMessageViewState.ErrorMessage(it)))
            }
        }).autoDispose()
    }

    fun acceptRejectFollowRequest(userId: Int) {
        followUserRepository.acceptRejectFollowRequest(AcceptRejectRequest(userId))
            .doOnSubscribe {
                createNewMessageStateSubject.onNext(CreateMessageViewState.LoadingState(true))
            }
            .doAfterTerminate {
                createNewMessageStateSubject.onNext(CreateMessageViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({
                Timber.d(it.message)
            }, { throwable ->
                createNewMessageStateSubject.onNext(CreateMessageViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    createNewMessageStateSubject.onNext(CreateMessageViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun searchText(search: String) {
        listOfSearchAccountData.clear()
        searchTextGlobal = search
        searchpageNo = 1
        searchisLoadMore = true
        searchisLoading = false
        searchAccounts()
    }

    fun loadMoreSearchAccount() {
        if (!searchisLoading) {
            searchisLoading = true
            if (searchisLoadMore) {
                searchpageNo += 1
                searchAccounts()
            }
        }
    }

    private fun searchAccounts() {
        searchRepository.searchAccounts(searchpageNo, searchTextGlobal).doOnSubscribe {
            createNewMessageStateSubject.onNext(CreateMessageViewState.LoadingState(true))
        }.doAfterTerminate {
            createNewMessageStateSubject.onNext(CreateMessageViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            searchisLoading = false
            response?.let {
                if (searchpageNo == 1) {
                    listOfSearchAccountData = response.toMutableList()
                    createNewMessageStateSubject.onNext(CreateMessageViewState.SearchAccountList(listOfSearchAccountData))
                } else {
                    if (!it.isNullOrEmpty()) {
                        listOfSearchAccountData.addAll(it)
                        createNewMessageStateSubject.onNext(CreateMessageViewState.SearchAccountList(listOfSearchAccountData))
                    } else {
                        searchisLoadMore = false
                    }
                }
            }
        }, { throwable ->
            createNewMessageStateSubject.onNext(CreateMessageViewState.LoadingState(false))
            throwable.localizedMessage?.let {
                createNewMessageStateSubject.onNext(CreateMessageViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun getConversation(userId: Int) {
        chatMessageRepository.getConversationId(ConversationRequest(userId))
            .doOnSubscribe {
                createNewMessageStateSubject.onNext(CreateMessageViewState.LoadingState(true))
            }
            .doAfterTerminate {
                createNewMessageStateSubject.onNext(CreateMessageViewState.LoadingState(false))

            }
            .subscribeOnIoAndObserveOnMainThread({
                createNewMessageStateSubject.onNext(
                    CreateMessageViewState.GetConversation(
                        it.conversationId ?: 0
                    )
                )
            }, { throwable ->
                throwable.localizedMessage?.let {
                    createNewMessageStateSubject.onNext(CreateMessageViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }
}

sealed class CreateMessageViewState {
    data class LoadingState(val isLoading: Boolean) : CreateMessageViewState()
    data class SuccessMessage(val successMessage: String) : CreateMessageViewState()
    data class ErrorMessage(val errorMessage: String) : CreateMessageViewState()

    data class LoadNearByUserList(val listOfSuggestedUser: List<NearByUserResponse>) : CreateMessageViewState()
    data class FollowingList(val listOfFollowing: ArrayList<FollowUser>) : CreateMessageViewState()
    data class SearchAccountList(val listOfSearchAccountData: List<FollowUser>) : CreateMessageViewState()

    data class GetConversation(val conversationId: Int) : CreateMessageViewState()

}