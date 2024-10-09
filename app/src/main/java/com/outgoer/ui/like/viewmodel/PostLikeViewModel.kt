package com.outgoer.ui.like.viewmodel

import com.outgoer.api.follow.FollowUserRepository
import com.outgoer.api.follow.model.AcceptRejectRequest
import com.outgoer.api.post.PostRepository
import com.outgoer.api.post.model.PostLikesUser
import com.outgoer.api.post.model.PostUserAllLikesRequest
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class PostLikeViewModel(
    private val postRepository: PostRepository,
    private val followUserRepository: FollowUserRepository
) : BaseViewModel() {

    private val postLikeViewStateSubjects: PublishSubject<PostLikeViewState> = PublishSubject.create()
    val postLikeViewState: Observable<PostLikeViewState> = postLikeViewStateSubjects.hide()

    //-------------------Get All Post Likes Pagination-------------------
    private var listOfPostLike: MutableList<PostLikesUser> = mutableListOf()
    private var searchText = ""
    private var pageNo = 1
    private var isLoading = false
    private var isLoadMore = true

    fun searchPostLikeUser(postId: Int, searchText: String) {
        this.searchText = searchText
        pageNo = 1
        isLoading = false
        isLoadMore = true
        listOfPostLike.clear()
        getPostUserAllLikes(postId)
    }

    fun loadMorePostLikeUser(postId: Int) {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo += 1
                getPostUserAllLikes(postId)
            }
        }
    }

    private fun getPostUserAllLikes(postId: Int) {
        postRepository.getPostUserAllLikes(pageNo, PostUserAllLikesRequest(postId = postId, search = searchText))
            .doOnSubscribe {
                postLikeViewStateSubjects.onNext(PostLikeViewState.LoadingState(true))
            }
            .doAfterTerminate {
                postLikeViewStateSubjects.onNext(PostLikeViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                isLoading = false
                response?.data.let {
                    if (pageNo == 1) {
                        listOfPostLike = it?.toMutableList() ?: mutableListOf()
                        postLikeViewStateSubjects.onNext(PostLikeViewState.PostLikesUserList(listOfPostLike))
                    } else {
                        if (!it.isNullOrEmpty()) {
                            listOfPostLike.addAll(it)
                            postLikeViewStateSubjects.onNext(PostLikeViewState.PostLikesUserList(listOfPostLike))
                        } else {
                            isLoadMore = false
                        }
                    }
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    postLikeViewStateSubjects.onNext(PostLikeViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun sendFollowRequest(postLikesUser: PostLikesUser) {
        followUserRepository.acceptRejectFollowRequest(AcceptRejectRequest(postLikesUser.user.id))
            .subscribeOnIoAndObserveOnMainThread({
                Timber.d("Follow - Unfollow Success")
            }, { throwable ->
                throwable.localizedMessage?.let {
                    postLikeViewStateSubjects.onNext(PostLikeViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    sealed class PostLikeViewState {
        data class ErrorMessage(val errorMessage: String) : PostLikeViewState()
        data class SuccessMessage(val successMessage: String) : PostLikeViewState()
        data class LoadingState(val isLoading: Boolean) : PostLikeViewState()
        data class PostLikesUserList(val postLikesUserList: List<PostLikesUser>) : PostLikeViewState()
    }
}