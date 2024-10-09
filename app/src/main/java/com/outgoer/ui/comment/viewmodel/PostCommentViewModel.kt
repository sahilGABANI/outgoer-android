package com.outgoer.ui.comment.viewmodel

import com.outgoer.api.follow.FollowUserRepository
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.follow.model.GetFollowersAndFollowingRequest
import com.outgoer.api.post.PostRepository
import com.outgoer.api.post.model.*
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.getSelectedTagUserIds
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import com.outgoer.ui.home.home.viewmodel.HomePageViewState
import com.outgoer.ui.home.home.viewmodel.HomeViewModel.Companion.homePageStateSubject
import com.outgoer.ui.postdetail.viewmodel.PostDetailViewModel.Companion.postDetailStateSubject
import com.outgoer.ui.postdetail.viewmodel.PostDetailViewState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class PostCommentViewModel(
    private val postRepository: PostRepository,
    private val followUserRepository: FollowUserRepository
) : BaseViewModel() {

    private val commentViewStateSubject: PublishSubject<CommentViewState> = PublishSubject.create()
    val commentViewState: Observable<CommentViewState> = commentViewStateSubject.hide()

    private var selectedTagUserInfo: MutableList<FollowUser> = mutableListOf()

    private var listOfCommentInfo: MutableList<CommentInfo> = mutableListOf()
    private var pageNumber: Int = 1
    private var isLoadMore: Boolean = true
    private var isLoading: Boolean = false

    private fun resetPagination(postId: Int) {
        listOfCommentInfo.clear()
        pageNumber = 1
        isLoadMore = true
        isLoading = false
        getListOfPostComments(postId)
    }

    fun loadMore(postId: Int) {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNumber++
                getListOfPostComments(postId)
            }
        }
    }

    fun getListOfPostComments(postId: Int) {
        postRepository.getListOfPostComments(pageNumber, PostUserAllCommentRequest(postId = postId))
            .doOnSubscribe {
                commentViewStateSubject.onNext(CommentViewState.LoadingState(true))
            }
            .doAfterTerminate {
                commentViewStateSubject.onNext(CommentViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    if (pageNumber == 1) {
                        listOfCommentInfo = response.toMutableList()
                        commentViewStateSubject.onNext(CommentViewState.LoadCommentInfo(listOfCommentInfo))
                        isLoading = false
                    } else {
                        if (!it.isNullOrEmpty()) {
                            listOfCommentInfo.addAll(it)
                            commentViewStateSubject.onNext(CommentViewState.LoadCommentInfo(listOfCommentInfo))
                            isLoading = false
                        } else {
                            isLoadMore = false
                        }
                    }
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    commentViewStateSubject.onNext(CommentViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun addComment(postId: Int, commentString: String) {
        val request = AddCommentRequest(
            postId = postId,
            comment = commentString,
            mentionIds = getSelectedTagUserIds(selectedTagUserInfo, commentString)
        )
        selectedTagUserInfo.clear()
        postRepository.addComment(request)
            .doOnSuccess {
                if (it != null) {
                    listOfCommentInfo.add(0, it)
                    commentViewStateSubject.onNext(CommentViewState.LoadCommentInfo(listOfCommentInfo, true))
                } else {
                    resetPagination(request.postId)
                }
            }
            .subscribeOnIoAndObserveOnMainThread({
                commentViewStateSubject.onNext(CommentViewState.SuccessMessage(it.toString()))
                homePageStateSubject.onNext(HomePageViewState.ReloadData(postId, true))
                postDetailStateSubject.onNext(PostDetailViewState.ReloadData)
            }, { throwable ->
                throwable.localizedMessage?.let {
                    commentViewStateSubject.onNext(CommentViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun addCommentReply(postId: Int, commentString: String, commentId: Int) {
        val request = AddCommentReplyRequest(
            postId = postId,
            replyMessage = commentString,
            commentId = commentId,
            mentionIds = getSelectedTagUserIds(selectedTagUserInfo, commentString)
        )
        selectedTagUserInfo.clear()
        postRepository.addCommentReply(request)
            .doOnSuccess {
                if (it != null) {
                    val mPos = listOfCommentInfo.indexOfFirst { cInfo ->
                        cInfo.id == request.commentId
                    }
                    if (mPos != -1) {
                        val reply = listOfCommentInfo[mPos].replies as ArrayList
                        reply.add(it)
                        listOfCommentInfo[mPos].replies = reply
                        commentViewStateSubject.onNext(CommentViewState.LoadCommentInfo(listOfCommentInfo))
                    } else {
                        resetPagination(request.postId)
                    }
                } else {
                    resetPagination(request.postId)
                }
            }
            .doOnSubscribe {
                commentViewStateSubject.onNext(CommentViewState.LoadingState(true))
            }
            .doAfterTerminate {
                commentViewStateSubject.onNext(CommentViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({
                commentViewStateSubject.onNext(CommentViewState.SuccessMessage(it.toString()))
            }, { throwable ->
                throwable.localizedMessage?.let {
                    commentViewStateSubject.onNext(CommentViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun addLikeToComment(commentInfo: CommentInfo) {
        postRepository.addLikeToComment(AddLikeToCommentRequest(commentInfo.id))
            .subscribeOnIoAndObserveOnMainThread({
                Timber.i("AddLikeToComment")
            }, { throwable ->
                throwable.localizedMessage?.let {
                    commentViewStateSubject.onNext(CommentViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun removeLikeFromComment(commentInfo: CommentInfo) {
        postRepository.removeLikeFromComment(RemoveLikeFromCommentRequest(commentInfo.id))
            .subscribeOnIoAndObserveOnMainThread({
                Timber.i("RemoveLikeFromComment")
            }, { throwable ->
                throwable.localizedMessage?.let {
                    commentViewStateSubject.onNext(CommentViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun deleteCommentOrReply(commentInfo: CommentInfo, replyComment: Boolean) {
        postRepository.deleteCommentOrReply(commentInfo.id,replyComment)
            .doOnSuccess {
                if (it != null) {
                    for (mainPos in 0 until listOfCommentInfo.size) {
                        val cInfo = listOfCommentInfo[mainPos]
                        if (cInfo.id == commentInfo.id) {
                            listOfCommentInfo.removeAt(mainPos)
                            commentViewStateSubject.onNext(CommentViewState.LoadCommentInfo(listOfCommentInfo))
                            return@doOnSuccess
                        } else {
                            val listOfCommentReplies =  arrayListOf<CommentInfo>()
                            cInfo.replies?.let { it1 -> listOfCommentReplies.addAll(it1) }
                            if (!listOfCommentReplies.isNullOrEmpty()) {
                                val childPos = listOfCommentReplies.indexOfFirst { replyInfo ->
                                    replyInfo.id == commentInfo.id
                                }
                                if (childPos != -1) {
                                    val reply = listOfCommentInfo[mainPos].replies as ArrayList
                                    reply.removeAt(childPos)
                                    listOfCommentInfo[mainPos].replies = reply
                                    commentViewStateSubject.onNext(CommentViewState.LoadCommentInfo(listOfCommentInfo))
                                    return@doOnSuccess
                                }
                            }
                        }
                    }
                    resetPagination(commentInfo.postId)
                } else {
                    resetPagination(commentInfo.postId)
                }
            }
            .subscribeOnIoAndObserveOnMainThread({
                commentViewStateSubject.onNext(CommentViewState.DeleteMessage(it.message.toString()))
                homePageStateSubject.onNext(HomePageViewState.ReloadData(commentInfo.postId, false))
                postDetailStateSubject.onNext(PostDetailViewState.ReloadData)
            }, { throwable ->
                throwable.localizedMessage?.let {
                    commentViewStateSubject.onNext(CommentViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun updateCommentOrReply(commentInfo: CommentInfo, commentString: String) {
        val request = UpdateCommentRequest(
            comment = commentString,
            mentionIds = getSelectedTagUserIds(selectedTagUserInfo, commentString)
        )
        selectedTagUserInfo.clear()
        postRepository.updateCommentOrReply(request, commentInfo.id)
            .doOnSuccess {
                if (it != null) {
                    for (mainPos in 0 until listOfCommentInfo.size) {
                        val cInfo = listOfCommentInfo[mainPos]
                        if (cInfo.id == commentInfo.id) {
                            listOfCommentInfo[mainPos].comment = it.comment
                            commentViewStateSubject.onNext(CommentViewState.LoadCommentInfo(listOfCommentInfo))
                            return@doOnSuccess
                        } else {
                            val listOfComment =  arrayListOf<CommentInfo>()
                            cInfo.replies?.let { it1 -> listOfComment.addAll(it1) }
                            if (!listOfComment.isNullOrEmpty()) {
                                val childPos = listOfComment.indexOfFirst { replyInfo ->
                                    replyInfo.id == commentInfo.id
                                }
                                if (childPos != -1) {
                                    commentInfo.comment = it.comment
                                    val reply = listOfCommentInfo[mainPos].replies as ArrayList
                                    reply.elementAt(childPos).comment = it.comment
                                    listOfCommentInfo[mainPos].replies = reply
                                    commentViewStateSubject.onNext(CommentViewState.LoadCommentInfo(listOfCommentInfo))
                                    return@doOnSuccess
                                }
                            }
                        }
                    }
                    resetPagination(commentInfo.postId)
                } else {
                    resetPagination(commentInfo.postId)
                }
            }
            .subscribeOnIoAndObserveOnMainThread({
                commentViewStateSubject.onNext(CommentViewState.EditComment(it))
            }, { throwable ->
                throwable.localizedMessage?.let {
                    commentViewStateSubject.onNext(CommentViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun clickOnEditText(commentText: String) {
        commentViewStateSubject.onNext(CommentViewState.UpdateEditTextView(commentText))
    }

    fun getInitialFollowersList(userId: Int) {
        followUserRepository.getAllFollowersList(1, GetFollowersAndFollowingRequest(userId, ""))
            .doOnSubscribe {
                commentViewStateSubject.onNext(CommentViewState.LoadingState(true))
            }
            .doAfterTerminate {
                commentViewStateSubject.onNext(CommentViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    commentViewStateSubject.onNext(CommentViewState.InitialFollowerList(it.toMutableList()))
                }
            }, { throwable ->
                commentViewStateSubject.onNext(CommentViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    commentViewStateSubject.onNext(CommentViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getFollowersList(userId: Int, searchText: String) {
        followUserRepository.getAllFollowersList(1, GetFollowersAndFollowingRequest(userId, searchText))
            .doOnSubscribe {
                commentViewStateSubject.onNext(CommentViewState.LoadingState(true))
            }
            .doAfterTerminate {
                commentViewStateSubject.onNext(CommentViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    commentViewStateSubject.onNext(CommentViewState.FollowerList(it.toMutableList()))
                }
            }, { throwable ->
                commentViewStateSubject.onNext(CommentViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    commentViewStateSubject.onNext(CommentViewState.ErrorMessage(it))
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
            commentViewStateSubject.onNext(CommentViewState.UpdateDescriptionText(descriptionString.plus(" ")))
        } else {
            val lastIndexOfToken = subString.findLastAnyOf(listOf("@"))?.first ?: return
            Timber.i("lastIndexOfToken %s", lastIndexOfToken)
            val tempSubString = subString.substring(0, lastIndexOfToken)
            val descriptionString = "$tempSubString @${followUser.username} $remainString"
            commentViewStateSubject.onNext(CommentViewState.UpdateDescriptionText(descriptionString.plus(" ")))
        }
    }
}

sealed class CommentViewState {
    data class SuccessMessage(val successMessage: String) : CommentViewState()
    data class ErrorMessage(val errorMessage: String) : CommentViewState()
    data class DeleteMessage(val deleteMessage: String) : CommentViewState()
    data class LoadingState(val isLoading: Boolean) : CommentViewState()
    data class LoadCommentInfo(val listOfComment: List<CommentInfo>, val scrollToTop: Boolean = false) : CommentViewState()
    data class EditComment(val commentInfo: CommentInfo) : CommentViewState()
    data class UpdateEditTextView(val commentText: String) : CommentViewState()
    data class InitialFollowerList(val listOfFollowers: List<FollowUser>) : CommentViewState()
    data class FollowerList(val listOfFollowers: List<FollowUser>) : CommentViewState()
    data class UpdateDescriptionText(val descriptionString: String) : CommentViewState()
}