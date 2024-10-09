package com.outgoer.ui.home.newReels.comment.viewmodel

import com.outgoer.api.follow.FollowUserRepository
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.follow.model.GetFollowersAndFollowingRequest
import com.outgoer.api.reels.ReelsRepository
import com.outgoer.api.reels.model.*
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.getSelectedTagUserIds
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import com.outgoer.ui.home.newReels.viewmodel.ReelsViewModel.Companion.reelsViewStateSubject
import com.outgoer.ui.home.newReels.viewmodel.ReelsViewState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class ReelsCommentViewModel(
    private val reelsRepository: ReelsRepository,
    private val followUserRepository: FollowUserRepository
) : BaseViewModel() {

    private val reelsCommentViewStateSubject: PublishSubject<ReelsCommentViewState> = PublishSubject.create()
    val reelsCommentViewState: Observable<ReelsCommentViewState> = reelsCommentViewStateSubject.hide()

    private var selectedTagUserInfo: MutableList<FollowUser> = mutableListOf()

    private var listOfCommentInfo: MutableList<ReelCommentInfo> = mutableListOf()
    private var pageNumber: Int = 1
    private var isLoadMore: Boolean = true
    private var isLoading: Boolean = false

    private fun resetPagination(reelId: Int) {
        listOfCommentInfo.clear()
        pageNumber = 1
        isLoadMore = true
        isLoading = false
        getAllReelComments(reelId)
    }

    fun loadMore(reelId: Int) {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNumber++
                getAllReelComments(reelId)
            }
        }
    }

    fun getAllReelComments(reelId: Int) {
        reelsRepository.getAllReelComments(pageNumber, GetAllReelCommentsRequest(reelId = reelId))
            .doOnSubscribe {
                reelsCommentViewStateSubject.onNext(ReelsCommentViewState.LoadingState(true))
            }
            .doAfterTerminate {
                reelsCommentViewStateSubject.onNext(ReelsCommentViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    if (pageNumber == 1) {
                        listOfCommentInfo = response.toMutableList()
                        reelsCommentViewStateSubject.onNext(ReelsCommentViewState.LoadCommentInfo(listOfCommentInfo))
                        isLoading = false
                    } else {
                        if (!it.isNullOrEmpty()) {
                            listOfCommentInfo.addAll(it)
                            reelsCommentViewStateSubject.onNext(ReelsCommentViewState.LoadCommentInfo(listOfCommentInfo))
                            isLoading = false
                        } else {
                            isLoadMore = false
                        }
                    }
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    reelsCommentViewStateSubject.onNext(ReelsCommentViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun addComment(reelInfo: ReelInfo, commentString: String) {
        val request = AddReelCommentRequest(
            reelId = reelInfo.id,
            comment = commentString,
            mentionIds = getSelectedTagUserIds(selectedTagUserInfo, commentString)
        )
        selectedTagUserInfo.clear()
        reelsRepository.addComment(request)
            .doOnSuccess {
                if (it != null) {
                    listOfCommentInfo.add(0, it)
                    reelInfo.totalComments = reelInfo.totalComments?.plus(1)
                    reelsCommentViewStateSubject.onNext(ReelsCommentViewState.UpdateCommentReelInfo(reelInfo))
                    reelsCommentViewStateSubject.onNext(ReelsCommentViewState.LoadCommentInfo(listOfCommentInfo, true))
                } else {
                    resetPagination(request.reelId)
                }
            }
            .subscribeOnIoAndObserveOnMainThread({
                reelsViewStateSubject.onNext(ReelsViewState.RefreshData)
                reelsCommentViewStateSubject.onNext(ReelsCommentViewState.SuccessMessage(it.toString()))
            }, { throwable ->
                throwable.localizedMessage?.let {
                    reelsCommentViewStateSubject.onNext(ReelsCommentViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun addCommentReply(reelId: Int, commentString: String, commentId: Int) {
        val request = AddReelCommentReplyRequest(
            reelsId = reelId,
            replyMessage = commentString,
            commentId = commentId,
            mentionIds = getSelectedTagUserIds(selectedTagUserInfo, commentString)
        )
        selectedTagUserInfo.clear()
        reelsRepository.addCommentReply(request)
            .doOnSuccess {
                if (it != null) {
                    val mPos = listOfCommentInfo.indexOfFirst { cInfo ->
                        cInfo.id == request.commentId
                    }
                    if (mPos != -1) {
                        val reply = listOfCommentInfo[mPos].replies as ArrayList
                       reply.add(it)
                        reelsCommentViewStateSubject.onNext(ReelsCommentViewState.LoadCommentInfo(listOfCommentInfo))
                    } else {
                        resetPagination(request.reelsId)
                    }
                } else {
                    resetPagination(request.reelsId)
                }
            }
            .doOnSubscribe {
                reelsCommentViewStateSubject.onNext(ReelsCommentViewState.LoadingState(true))
            }
            .doAfterTerminate {
                reelsCommentViewStateSubject.onNext(ReelsCommentViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({
                reelsCommentViewStateSubject.onNext(ReelsCommentViewState.SuccessMessage(it.toString()))
            }, { throwable ->
                throwable.localizedMessage?.let {
                    reelsCommentViewStateSubject.onNext(ReelsCommentViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun addLikeToComment(reelCommentInfo: ReelCommentInfo) {
        reelsRepository.addLikeToComment(AddLikeToReelCommentRequest(reelCommentInfo.id))
            .subscribeOnIoAndObserveOnMainThread({
                Timber.i("CommentLikeStatus %s", it.reelsCommentLike)
            }, { throwable ->
                throwable.localizedMessage?.let {
                    reelsCommentViewStateSubject.onNext(ReelsCommentViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun removeLikeFromComment(reelCommentInfo: ReelCommentInfo) {
        reelsRepository.removeLikeFromComment(RemoveLikeFromReelCommentRequest(reelCommentInfo.id))
            .subscribeOnIoAndObserveOnMainThread({
                Timber.i("Remove Comment Like")
            }, { throwable ->
                throwable.localizedMessage?.let {
                    reelsCommentViewStateSubject.onNext(ReelsCommentViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun deleteCommentOrReply(reelCommentInfo: ReelCommentInfo) {
        reelsRepository.deleteCommentOrReply(reelCommentInfo.id)
            .doOnSuccess {
                if (it != null) {
                    for (mainPos in 0 until listOfCommentInfo.size) {
                        val cInfo = listOfCommentInfo[mainPos]
                        if (cInfo.id == reelCommentInfo.id) {
                            listOfCommentInfo.removeAt(mainPos)
                            reelsCommentViewStateSubject.onNext(ReelsCommentViewState.LoadCommentInfo(listOfCommentInfo))
                            return@doOnSuccess
                        } else {
                            val replies = cInfo.replies
                            if (!replies.isNullOrEmpty()) {
                                val childPos = replies.indexOfFirst { replyInfo ->
                                    replyInfo.id == reelCommentInfo.id
                                }
                                if (childPos != -1) {
                                    val reply = listOfCommentInfo[mainPos].replies as ArrayList
                                    reply.removeAt(childPos)
                                    reelsCommentViewStateSubject.onNext(ReelsCommentViewState.LoadCommentInfo(listOfCommentInfo))
                                    return@doOnSuccess
                                }
                            }
                        }
                    }
                    resetPagination(reelCommentInfo.reelId)
                } else {
                    resetPagination(reelCommentInfo.reelId)
                }
            }
            .subscribeOnIoAndObserveOnMainThread({
                reelsViewStateSubject.onNext(ReelsViewState.RefreshData)
                reelsCommentViewStateSubject.onNext(ReelsCommentViewState.DeleteMessage(it.message.toString()))
            }, { throwable ->
                throwable.localizedMessage?.let {
                    reelsCommentViewStateSubject.onNext(ReelsCommentViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun updateCommentOrReply(reelCommentInfo: ReelCommentInfo, commentString: String) {
        val request = UpdateReelCommentRequest(
            comment = commentString,
            mentionIds = getSelectedTagUserIds(selectedTagUserInfo, commentString)
        )
        selectedTagUserInfo.clear()
        reelsRepository.updateCommentOrReply(request, reelCommentInfo.id)
            .doOnSuccess {
                if (it != null) {
                    for (mainPos in 0 until listOfCommentInfo.size) {
                        val cInfo = listOfCommentInfo[mainPos]
                        if (cInfo.id == reelCommentInfo.id) {
                            listOfCommentInfo[mainPos].comment = it.comment
                            reelsCommentViewStateSubject.onNext(ReelsCommentViewState.LoadCommentInfo(listOfCommentInfo))
                            return@doOnSuccess
                        } else {
                            val replies = cInfo.replies
                            if (!replies.isNullOrEmpty()) {
                                val childPos = replies.indexOfFirst { replyInfo ->
                                    replyInfo.id == reelCommentInfo.id
                                }
                                if (childPos != -1) {
                                    reelCommentInfo.comment = it.comment
                                    val reply = listOfCommentInfo[mainPos].replies as ArrayList
                                    reply.elementAt(childPos).comment = it.comment
                                    reelsCommentViewStateSubject.onNext(ReelsCommentViewState.LoadCommentInfo(listOfCommentInfo))
                                    return@doOnSuccess
                                }
                            }
                        }
                    }
                    resetPagination(reelCommentInfo.reelId)
                } else {
                    resetPagination(reelCommentInfo.reelId)
                }
            }
            .subscribeOnIoAndObserveOnMainThread({
                reelsCommentViewStateSubject.onNext(ReelsCommentViewState.EditComment(it))
            }, { throwable ->
                throwable.localizedMessage?.let {
                    reelsCommentViewStateSubject.onNext(ReelsCommentViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun clickOnEditText(commentText: String) {
        reelsCommentViewStateSubject.onNext(ReelsCommentViewState.UpdateEditTextView(commentText))
    }

    fun getInitialFollowersList(userId: Int) {
        followUserRepository.getAllFollowersList(1, GetFollowersAndFollowingRequest(userId, ""))
            .doOnSubscribe {
                reelsCommentViewStateSubject.onNext(ReelsCommentViewState.LoadingState(true))
            }
            .doAfterTerminate {
                reelsCommentViewStateSubject.onNext(ReelsCommentViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    reelsCommentViewStateSubject.onNext(ReelsCommentViewState.InitialFollowerList(it.toMutableList()))
                }
            }, { throwable ->
                reelsCommentViewStateSubject.onNext(ReelsCommentViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    reelsCommentViewStateSubject.onNext(ReelsCommentViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getFollowersList(userId: Int, searchText: String) {
        followUserRepository.getAllFollowersList(1, GetFollowersAndFollowingRequest(userId, searchText))
            .doOnSubscribe {
                reelsCommentViewStateSubject.onNext(ReelsCommentViewState.LoadingState(true))
            }
            .doAfterTerminate {
                reelsCommentViewStateSubject.onNext(ReelsCommentViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    reelsCommentViewStateSubject.onNext(ReelsCommentViewState.FollowerList(it.toMutableList()))
                }
            }, { throwable ->
                reelsCommentViewStateSubject.onNext(ReelsCommentViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    reelsCommentViewStateSubject.onNext(ReelsCommentViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun searchTagUserClicked(
        initialDescriptionString: String,
        subString: String,
        followUser: FollowUser
    ) {
        if (followUser !in selectedTagUserInfo) {
            selectedTagUserInfo.add(followUser)
        }
        val remainString = initialDescriptionString.removePrefix(subString)
        if (subString.length == initialDescriptionString.length) {
            val lastIndexOfToken = initialDescriptionString.findLastAnyOf(listOf("@"))?.first ?: return
            Timber.i("lastIndexOfToken %s", lastIndexOfToken)
            val tempSubString = initialDescriptionString.substring(0, lastIndexOfToken)
            val descriptionString = "$tempSubString@${followUser.username}"
            reelsCommentViewStateSubject.onNext(ReelsCommentViewState.UpdateDescriptionText(descriptionString.plus(" ")))
        } else {
            val lastIndexOfToken = subString.findLastAnyOf(listOf("@"))?.first ?: return
            Timber.i("lastIndexOfToken %s", lastIndexOfToken)
            val tempSubString = subString.substring(0, lastIndexOfToken)
            val descriptionString = "$tempSubString @${followUser.username} $remainString"
            reelsCommentViewStateSubject.onNext(ReelsCommentViewState.UpdateDescriptionText(descriptionString.plus(" ")))
        }
    }
}

sealed class ReelsCommentViewState {
    data class SuccessMessage(val successMessage: String) : ReelsCommentViewState()
    data class ErrorMessage(val errorMessage: String) : ReelsCommentViewState()
    data class DeleteMessage(val deleteMessage: String) : ReelsCommentViewState()
    data class LoadingState(val isLoading: Boolean) : ReelsCommentViewState()
    data class LoadCommentInfo(val listOfComment: List<ReelCommentInfo>, val scrollToTop: Boolean = false) : ReelsCommentViewState()
    data class EditComment(val reelCommentInfo: ReelCommentInfo) : ReelsCommentViewState()
    data class UpdateEditTextView(val commentText: String) : ReelsCommentViewState()
    data class UpdateCommentReelInfo(val reelInfo: ReelInfo?) : ReelsCommentViewState()
    data class InitialFollowerList(val listOfFollowers: List<FollowUser>) : ReelsCommentViewState()
    data class FollowerList(val listOfFollowers: List<FollowUser>) : ReelsCommentViewState()
    data class UpdateDescriptionText(val descriptionString: String) : ReelsCommentViewState()
}