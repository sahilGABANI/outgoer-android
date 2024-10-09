package com.outgoer.ui.otherprofile.viewmodel

import com.outgoer.api.post.PostRepository
import com.outgoer.api.post.model.PostInfo
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class OtherUserPostViewModel(
    private val postRepository: PostRepository,
) : BaseViewModel() {

    private val otherUserPostViewStateSubject: PublishSubject<OtherUserPostViewState> = PublishSubject.create()
    val otherUserPostViewState: Observable<OtherUserPostViewState> = otherUserPostViewStateSubject.hide()

    //-------------------Get User Post Pagination-------------------
    private var listOfUserPostData: MutableList<PostInfo> = mutableListOf()
    private var pageNumberUserPost: Int = 1
    private var isLoadMoreUserPost: Boolean = true
    private var isLoadingUserPost: Boolean = false

    fun resetUserPostPagination(userId: Int) {
        listOfUserPostData.clear()
        pageNumberUserPost = 1
        isLoadMoreUserPost = true
        isLoadingUserPost = false
        getUserPost(userId)
    }

    private fun getUserPost(userId: Int) {
        postRepository.getMyPost(pageNumberUserPost, userId)
            .doOnSubscribe {
                otherUserPostViewStateSubject.onNext(OtherUserPostViewState.LoadingState(true))
            }
            .doAfterTerminate {
                otherUserPostViewStateSubject.onNext(OtherUserPostViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.data?.let {
                    if (pageNumberUserPost == 1) {
                        listOfUserPostData = response.data.toMutableList()
                        otherUserPostViewStateSubject.onNext(OtherUserPostViewState.GetAllPostList(listOfUserPostData))
                    } else {
                        if (!it.isNullOrEmpty()) {
                            listOfUserPostData.addAll(it)
                            otherUserPostViewStateSubject.onNext(OtherUserPostViewState.GetAllPostList(listOfUserPostData))
                        } else {
                            isLoadMoreUserPost = false
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

    fun loadMoreUserPost(userId: Int) {
        if (!isLoadingUserPost) {
            isLoadingUserPost = true
            if (isLoadMoreUserPost) {
                pageNumberUserPost++
                getUserPost(userId)
            }
        }
    }
    //-------------------Get User Post Pagination-------------------

    sealed class OtherUserPostViewState {
        data class ErrorMessage(val errorMessage: String) : OtherUserPostViewState()
        data class SuccessMessage(val successMessage: String) : OtherUserPostViewState()
        data class LoadingState(val isLoading: Boolean) : OtherUserPostViewState()

        data class GetAllPostList(val postInfoList: List<PostInfo>) : OtherUserPostViewState()
    }
}