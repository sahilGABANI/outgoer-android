package com.outgoer.ui.save_post_reels.viewmodel

import com.outgoer.api.post.PostRepository
import com.outgoer.api.post.model.MyTagBookmarkInfo
import com.outgoer.api.post.model.PostInfo
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import com.outgoer.ui.otherprofile.viewmodel.OtherUserPostViewModel
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class SavedPostReelViewModel(private val postRepository: PostRepository) : BaseViewModel() {

    private val savedPostReelsViewStateSubject: PublishSubject<SavedPostReelState> = PublishSubject.create()
    val savedPostReelsViewState: Observable<SavedPostReelState> = savedPostReelsViewStateSubject.hide()

    //-------------------Get User Post Pagination-------------------
    private var listOfUserPostData: MutableList<MyTagBookmarkInfo> = mutableListOf()
    private var pageNumberUserPost: Int = 1
    private var isLoadMoreUserPost: Boolean = true
    private var isLoadingUserPost: Boolean = false
    private fun getUserPost(userId: Int,type :String) {
        postRepository.getMyBookmark(pageNumberUserPost, userId,type)
            .doOnSubscribe {
                savedPostReelsViewStateSubject.onNext(SavedPostReelState.LoadingState(true))
            }
            .doAfterTerminate {
                savedPostReelsViewStateSubject.onNext(SavedPostReelState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.data?.let {
                    if (pageNumberUserPost == 1) {
                        listOfUserPostData = response.data.toMutableList()
                        savedPostReelsViewStateSubject.onNext(SavedPostReelState.GetAllPostList(listOfUserPostData))
                    } else {
                        if (!it.isNullOrEmpty()) {
                            listOfUserPostData.addAll(it)
                            savedPostReelsViewStateSubject.onNext(SavedPostReelState.GetAllPostList(listOfUserPostData))
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

    fun loadBookMark(userId: Int,type: String) {
        if (!isLoadingUserPost) {
            isLoadingUserPost = true
            if (isLoadMoreUserPost) {
                pageNumberUserPost++
                getUserPost(userId,type)
            }
        }
    }


    fun resetBookMark(userId: Int,type :String) {
        listOfUserPostData.clear()
        pageNumberUserPost = 1
        isLoadMoreUserPost = true
        isLoadingUserPost = false
        getUserPost(userId,type)
    }

}

sealed class SavedPostReelState {
    data class ErrorMessage(val errorMessage: String) : SavedPostReelState()
    data class SuccessMessage(val successMessage: String) : SavedPostReelState()
    data class LoadingState(val isLoading: Boolean) :SavedPostReelState()

    data class GetAllPostList(val postInfoList: List<MyTagBookmarkInfo>) : SavedPostReelState()
}