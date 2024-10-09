package com.outgoer.ui.home.profile.viewmodel

import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.post.PostRepository
import com.outgoer.api.post.model.PostInfo
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class MyPostViewModel(
    private val postRepository: PostRepository,
    private val loginUserCache: LoggedInUserCache
) : BaseViewModel() {

    private val myPostViewStateSubject: PublishSubject<MyPostViewState> = PublishSubject.create()
    val myPostViewState: Observable<MyPostViewState> = myPostViewStateSubject.hide()

    //-------------------Get My Post Pagination-------------------
    private var listOfMyPostData: MutableList<PostInfo> = mutableListOf()
    private var pageNumberMyPost: Int = 1
    private var isLoadMoreMyPost: Boolean = true
    private var isLoadingMyPost: Boolean = false

    fun resetMyPostPagination() {
        listOfMyPostData.clear()
        pageNumberMyPost = 1
        isLoadMoreMyPost = true
        isLoadingMyPost = false
        getMyPost()
    }

    private fun getMyPost() {
        postRepository.getMyPost(pageNumberMyPost, loginUserCache.getUserId())
            .doOnSubscribe {
                myPostViewStateSubject.onNext(MyPostViewState.LoadingState(true))
            }
            .doAfterTerminate {
                myPostViewStateSubject.onNext(MyPostViewState.LoadingState(false))
                isLoadingMyPost = false
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.data.let {
                    if (pageNumberMyPost == 1) {
                        listOfMyPostData = it?.toMutableList() ?: mutableListOf()
                        myPostViewStateSubject.onNext(MyPostViewState.GetAllMyPostList(listOfMyPostData))
                    } else {
                        if (!it.isNullOrEmpty()) {
                            listOfMyPostData.addAll(it)
                            myPostViewStateSubject.onNext(MyPostViewState.GetAllMyPostList(listOfMyPostData))
                        } else {
                            isLoadMoreMyPost = false
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

    fun loadMoreMyPost() {
        if (!isLoadingMyPost) {
            isLoadingMyPost = true
            if (isLoadMoreMyPost) {
                pageNumberMyPost++
                getMyPost()
            }
        }
    }
    //-------------------Get My Post Pagination-------------------

    sealed class MyPostViewState {
        data class ErrorMessage(val errorMessage: String) : MyPostViewState()
        data class SuccessMessage(val successMessage: String) : MyPostViewState()
        data class LoadingState(val isLoading: Boolean) : MyPostViewState()

        data class GetAllMyPostList(val postInfoList: List<PostInfo>) : MyPostViewState()
    }
}