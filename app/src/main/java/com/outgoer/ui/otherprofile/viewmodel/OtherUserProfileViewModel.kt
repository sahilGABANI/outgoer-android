package com.outgoer.ui.otherprofile.viewmodel

import com.outgoer.api.authentication.model.OutgoerUser
import com.outgoer.api.chat.ChatMessageRepository
import com.outgoer.api.chat.model.ConversationRequest
import com.outgoer.api.follow.FollowUserRepository
import com.outgoer.api.follow.model.AcceptRejectRequest
import com.outgoer.api.profile.ProfileRepository
import com.outgoer.api.profile.model.BlockUserRequest
import com.outgoer.api.profile.model.ReportUserRequest
import com.outgoer.api.reels.ReelsRepository
import com.outgoer.api.reels.model.ReelInfo
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class OtherUserProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val reelsRepository: ReelsRepository,
    private val followUserRepository: FollowUserRepository
) : BaseViewModel() {

    private val profileViewStatesSubject: PublishSubject<OtherUserProfileViewState> =
        PublishSubject.create()
    val profileViewStates: Observable<OtherUserProfileViewState> = profileViewStatesSubject.hide()

    fun getUserProfile(userId: Int) {
        profileRepository.getUserProfile(userId)
            .doOnSubscribe {
                profileViewStatesSubject.onNext(OtherUserProfileViewState.LoadingState(true))
            }
            .doAfterTerminate {
                profileViewStatesSubject.onNext(OtherUserProfileViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.success) {
                    response.data?.let {
                        profileViewStatesSubject.onNext(
                            OtherUserProfileViewState.OtherUserProfileData(
                                it
                            )
                        )
                    }
                } else {
                    response.message?.let {
                        profileViewStatesSubject.onNext(OtherUserProfileViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    profileViewStatesSubject.onNext(OtherUserProfileViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun blockUserProfile(blockUserRequest: BlockUserRequest) {
        profileRepository.blockUserProfile(blockUserRequest)
            .doOnSubscribe {
                profileViewStatesSubject.onNext(OtherUserProfileViewState.LoadingState(true))
            }
            .doAfterTerminate {
                profileViewStatesSubject.onNext(OtherUserProfileViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.success) {
                    response.message?.let {
                        profileViewStatesSubject.onNext(
                            OtherUserProfileViewState.SuccessMessage(
                                it
                            )
                        )
                    }
                } else {
                    response.message?.let {
                        profileViewStatesSubject.onNext(OtherUserProfileViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    profileViewStatesSubject.onNext(OtherUserProfileViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }


    fun reportUserVenue(reportUserRequest: ReportUserRequest) {
        profileRepository.reportUserVenue(reportUserRequest)
            .doOnSubscribe {
                profileViewStatesSubject.onNext(OtherUserProfileViewState.LoadingState(true))
            }
            .doAfterTerminate {
                profileViewStatesSubject.onNext(OtherUserProfileViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.success) {
                    response.message?.let {
                        profileViewStatesSubject.onNext(
                            OtherUserProfileViewState.SuccessMessage(
                                it
                            )
                        )
                    }
                } else {
                    response.message?.let {
                        profileViewStatesSubject.onNext(OtherUserProfileViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    profileViewStatesSubject.onNext(OtherUserProfileViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getConversation(userId: Int) {
        chatMessageRepository.getConversationId(ConversationRequest(userId))
            .doOnSubscribe {
                profileViewStatesSubject.onNext(OtherUserProfileViewState.LoadingState(true))
            }
            .doAfterTerminate {
                profileViewStatesSubject.onNext(OtherUserProfileViewState.LoadingState(false))

            }
            .subscribeOnIoAndObserveOnMainThread({
                profileViewStatesSubject.onNext(
                    OtherUserProfileViewState.GetConversation(
                        it.conversationId ?: 0
                    )
                )
            }, { throwable ->
                throwable.localizedMessage?.let {
                    profileViewStatesSubject.onNext(OtherUserProfileViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    private var pageNumber: Int = 1
    private var isLoadMore: Boolean = true
    private var isLoading: Boolean = false
    private var listOfReelsInfo: MutableList<ReelInfo> = mutableListOf()

    fun getUserReel(userId: Int) {
        reelsRepository.getMyReel(pageNumber, userId)
            .doOnSubscribe {
                isLoading = true
            }
            .doAfterTerminate {
                isLoading = false
            }
            .subscribeOnIoAndObserveOnMainThread({ reelsResponse ->
                reelsResponse?.data?.let {
                    if (pageNumber == 1) {
                        listOfReelsInfo = it.toMutableList()
                        profileViewStatesSubject.onNext(
                            OtherUserProfileViewState.GetUserReelInfo(
                                listOfReelsInfo
                            )
                        )
                    } else {
                        if (!it.isNullOrEmpty()) {
                            listOfReelsInfo.addAll(it)
                            profileViewStatesSubject.onNext(
                                OtherUserProfileViewState.GetUserReelInfo(
                                    listOfReelsInfo
                                )
                            )
                        } else {
                            isLoadMore = false
                        }
                    }
                }
            }, { throwable ->
                throwable.printStackTrace()
                throwable.localizedMessage?.let {
                    Timber.e(it)
                }
            }).autoDispose()
    }

    fun pullToRefresh(userId: Int) {
        pageNumber = 1
        isLoadMore = true
        isLoading = false
        listOfReelsInfo.clear()
        getUserReel(userId)
    }

    fun followUnfollow(userId: Int) {
        followUserRepository.acceptRejectFollowRequest(AcceptRejectRequest(userId))
            .doOnSubscribe {}
            .subscribeOnIoAndObserveOnMainThread({

            }, { throwable ->
                throwable.printStackTrace()
                throwable.localizedMessage?.let {
                    Timber.e(it)
                }
            }).autoDispose()
    }

    sealed class OtherUserProfileViewState {
        data class ErrorMessage(val errorMessage: String) : OtherUserProfileViewState()
        data class SuccessMessage(val successMessage: String) : OtherUserProfileViewState()
        data class LoadingState(val isLoading: Boolean) : OtherUserProfileViewState()
        data class OtherUserProfileData(val outgoerUser: OutgoerUser) : OtherUserProfileViewState()
        data class GetConversation(val conversationId: Int) : OtherUserProfileViewState()
        data class GetUserReelInfo(val listOfReelsInfo: List<ReelInfo>) :
            OtherUserProfileViewState()
    }
}