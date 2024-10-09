package com.outgoer.ui.posttags.viewmodel

import com.outgoer.api.follow.FollowUserRepository
import com.outgoer.api.follow.model.AcceptRejectRequest
import com.outgoer.api.post.PostRepository
import com.outgoer.api.post.model.PostTagsItem
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class PostTaggedPeopleViewModel(
    private val postRepository: PostRepository,
    private val followUserRepository: FollowUserRepository
) : BaseViewModel() {

    private val postTaggedPeopleViewStateSubject: PublishSubject<PostTaggedPeopleViewState> = PublishSubject.create()
    val postTaggedPeopleViewState: Observable<PostTaggedPeopleViewState> = postTaggedPeopleViewStateSubject.hide()

    fun getPostTaggedPeople(postId: Int) {
        postRepository.getPostTaggedPeople(postId).doOnSubscribe {
            postTaggedPeopleViewStateSubject.onNext(PostTaggedPeopleViewState.LoadingState(true))
        }.doAfterTerminate {
            postTaggedPeopleViewStateSubject.onNext(PostTaggedPeopleViewState.LoadingState(false))
        }.doAfterSuccess {
            postTaggedPeopleViewStateSubject.onNext(PostTaggedPeopleViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            response?.data?.let {
                postTaggedPeopleViewStateSubject.onNext(PostTaggedPeopleViewState.GetTaggedPeopleList(it))
            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                postTaggedPeopleViewStateSubject.onNext(PostTaggedPeopleViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun followUnfollow(postTagsItem: PostTagsItem) {
        followUserRepository.acceptRejectFollowRequest(AcceptRejectRequest(postTagsItem.userId ?: 0))
            .doOnSubscribe {
                postTaggedPeopleViewStateSubject.onNext(PostTaggedPeopleViewState.LoadingState(true))
            }
            .doAfterTerminate {
                postTaggedPeopleViewStateSubject.onNext(PostTaggedPeopleViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({
                postTaggedPeopleViewStateSubject.onNext(PostTaggedPeopleViewState.SuccessMessage(it.toString()))
            }, {
                postTaggedPeopleViewStateSubject.onNext((PostTaggedPeopleViewState.ErrorMessage(it.message.toString())))
            }).autoDispose()
    }
}

sealed class PostTaggedPeopleViewState {
    data class SuccessMessage(val successMessage: String) : PostTaggedPeopleViewState()
    data class ErrorMessage(val errorMessage: String) : PostTaggedPeopleViewState()
    data class LoadingState(val isLoading: Boolean) : PostTaggedPeopleViewState()
    data class GetTaggedPeopleList(val listOfTaggedPeople: List<PostTagsItem>) : PostTaggedPeopleViewState()
}