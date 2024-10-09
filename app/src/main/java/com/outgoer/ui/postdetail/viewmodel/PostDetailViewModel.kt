package com.outgoer.ui.postdetail.viewmodel

import com.outgoer.api.post.PostRepository
import com.outgoer.api.post.model.*
import com.outgoer.base.BaseViewModel
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import com.outgoer.ui.home.home.viewmodel.HomePageViewState
import com.outgoer.ui.home.home.viewmodel.HomeViewModel
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class PostDetailViewModel(
    private val postRepository: PostRepository
) : BaseViewModel() {

    companion object{
        val postDetailStateSubject: PublishSubject<PostDetailViewState> = PublishSubject.create()
        val postDetailState: Observable<PostDetailViewState> = postDetailStateSubject.hide()
    }

    fun addPostLike(postInfo: PostInfo) {
        postRepository.addLikesToPost(AddLikesRequest(postInfo.id))
            .subscribeOnIoAndObserveOnMainThread({
                Timber.d("add Post Like Success")
                RxBus.publish(RxEvent.RefreshHomePagePost)
                RxBus.publish(RxEvent.RefreshMyProfile)
            }, { throwable ->
                throwable.localizedMessage?.let {
                    postDetailStateSubject.onNext(PostDetailViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun removeLikeFromPost(postInfo: PostInfo) {
        postRepository.removeLikeFromPost(RemoveLikesRequest(postInfo.id))
            .subscribeOnIoAndObserveOnMainThread({
                Timber.d("remove Post Like Success")
                RxBus.publish(RxEvent.RefreshHomePagePost)
                RxBus.publish(RxEvent.RefreshMyProfile)
            }, { throwable ->
                throwable.localizedMessage?.let {
                    postDetailStateSubject.onNext(PostDetailViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun addPostToBookmark(postInfo: PostInfo) {
        postRepository.addPostToBookmark(AddBookmarkRequest(postInfo.id))
            .subscribeOnIoAndObserveOnMainThread({
                Timber.d("add Post Bookmark Success")
                RxBus.publish(RxEvent.RefreshHomePagePost)
                RxBus.publish(RxEvent.RefreshMyProfile)
            }, { throwable ->
                throwable.localizedMessage?.let {
                    postDetailStateSubject.onNext(PostDetailViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun removePostToBookmark(postInfo: PostInfo) {
        postRepository.removePostToBookmark(RemoveBookmarkRequest(postInfo.id))
            .subscribeOnIoAndObserveOnMainThread({
                Timber.d("remove Post Bookmark Success")
                RxBus.publish(RxEvent.RefreshHomePagePost)
                RxBus.publish(RxEvent.RefreshMyProfile)
            }, { throwable ->
                throwable.localizedMessage?.let {
                    postDetailStateSubject.onNext(PostDetailViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun deletePost(postId: Int) {
        postRepository.deletePost(postId)
            .subscribeOnIoAndObserveOnMainThread({
                RxBus.publish(RxEvent.RefreshHomePagePost)
                RxBus.publish(RxEvent.RefreshMyProfile)
                postDetailStateSubject.onNext(PostDetailViewState.SuccessMessage(it.message.toString()))
            }, { throwable ->
                throwable.localizedMessage?.let {
                    postDetailStateSubject.onNext(PostDetailViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getPostById(postId: Int) {
        postRepository.getPostById(postId)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.data?.let {
                    postDetailStateSubject.onNext(PostDetailViewState.GetPostInfo(it))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    postDetailStateSubject.onNext(PostDetailViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun reportPost(postId: Int,reportId:Int) {
        postRepository.reportPost(ReportPostRequest(postId,reportId))
            .doOnSuccess {
                postDetailStateSubject.onNext(PostDetailViewState.LoadingState(true))
            }
            .doAfterTerminate{
                postDetailStateSubject.onNext(PostDetailViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({
                if(it.success) {
                    postDetailStateSubject.onNext(PostDetailViewState.SuccessMessage(it.message.toString()))
                } else {
                    postDetailStateSubject.onNext(PostDetailViewState.ErrorMessage(it.message.toString()))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    postDetailStateSubject.onNext(PostDetailViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }
}

sealed class PostDetailViewState {
    data class ErrorMessage(val errorMessage: String) : PostDetailViewState()
    data class SuccessMessage(val successMessage: String) : PostDetailViewState()
    data class LoadingState(val isLoading: Boolean) : PostDetailViewState()
    data class GetPostInfo(val postInfo: PostInfo) : PostDetailViewState()
    object ReloadData: PostDetailViewState()
}