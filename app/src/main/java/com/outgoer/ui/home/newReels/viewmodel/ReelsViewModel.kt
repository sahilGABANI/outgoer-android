package com.outgoer.ui.home.newReels.viewmodel

import com.outgoer.api.follow.FollowUserRepository
import com.outgoer.api.follow.model.AcceptRejectRequest
import com.outgoer.api.post.PostRepository
import com.outgoer.api.post.model.ReportPostRequest
import com.outgoer.api.reels.ReelsRepository
import com.outgoer.api.reels.model.*
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import com.outgoer.ui.home.home.viewmodel.HomePageViewState
import com.outgoer.ui.home.home.viewmodel.HomeViewModel
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class ReelsViewModel(
    private val reelsRepository: ReelsRepository,
    private val followUserRepository: FollowUserRepository,
) : BaseViewModel() {

    companion object {
        val reelsViewStateSubject: PublishSubject<ReelsViewState> = PublishSubject.create()
        val reelsViewState: Observable<ReelsViewState> = reelsViewStateSubject.hide()
    }

    private var pageNumber: Int = 1
    private var isLoadMore: Boolean = true
    private var isLoading: Boolean = false
    private var listOfReelsInfo: MutableList<ReelInfo> = mutableListOf()

    fun getAllReels(tabType: Int) {
        reelsRepository.getAllReels(pageNumber, tabType)
            .doOnSubscribe {
                isLoading = true
                reelsViewStateSubject.onNext(ReelsViewState.LoadingState(true))
            }
            .doAfterTerminate {
                isLoading = false
                reelsViewStateSubject.onNext(ReelsViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ reelsResponse ->
                reelsResponse?.data?.let {
                    if (pageNumber == 1) {
                        listOfReelsInfo = it.toMutableList()
                        reelsViewStateSubject.onNext(ReelsViewState.GetAllReelsInfo(listOfReelsInfo))
                    } else {
                        if (!it.isNullOrEmpty()) {
                            listOfReelsInfo.addAll(it)
                            reelsViewStateSubject.onNext(
                                ReelsViewState.GetAllReelsInfo(
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

    fun loadMore(tabType: Int) {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNumber++
                getAllReels(tabType)
            }
        }
    }

    fun pullToRefresh(tabType: Int, isHome: Boolean = false) {
        if(isHome) {
            if(pageNumber == 1) {
                listOfReelsInfo.clear()
            }

            listOfReelsInfo.addAll(reelsRepository.listOfReelsInfo)
            reelsViewStateSubject.onNext(ReelsViewState.GetAllReelsInfo(listOfReelsInfo))
        } else {
            pageNumber = 1
            isLoadMore = true
            isLoading = false
            listOfReelsInfo.clear()
            getAllReels(tabType)
        }
    }

    fun addLikeToReel(reelInfo: ReelInfo) {
        reelsRepository.addLikeToReel(AddReelLikeRequest(reelInfo.id))
            .subscribeOnIoAndObserveOnMainThread({
                Timber.d("add Reel Like Success")
            }, { throwable ->
                throwable.localizedMessage?.let {
                    reelsViewStateSubject.onNext(ReelsViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun removeLikeFromReel(reelInfo: ReelInfo) {
        reelsRepository.removeLikeFromReel(RemoveReelLikeRequest(reelInfo.id))
            .subscribeOnIoAndObserveOnMainThread({
                Timber.d("remove Reel Like Success")
            }, { throwable ->
                throwable.localizedMessage?.let {
                    reelsViewStateSubject.onNext(ReelsViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun addReelToBookmark(reelInfo: ReelInfo) {
        reelsRepository.addReelToBookmark(AddBookmarkToReelRequest(reelInfo.id))
            .subscribeOnIoAndObserveOnMainThread({
                Timber.d("add Reel Bookmark Success")
            }, { throwable ->
                throwable.localizedMessage?.let {
                    reelsViewStateSubject.onNext(ReelsViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun removeReelToBookmark(reelInfo: ReelInfo) {
        reelsRepository.removeReelToBookmark(RemoveBookmarkFromReelRequest(reelInfo.id))
            .subscribeOnIoAndObserveOnMainThread({
                Timber.d("remove Reel Bookmark Success")
            }, { throwable ->
                throwable.localizedMessage?.let {
                    reelsViewStateSubject.onNext(ReelsViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun followUnfollowUser(listOfDataItems: List<ReelInfo>?, reelInfo: ReelInfo) {
        listOfDataItems?.forEach {
            if (reelInfo.userId == it.userId) {
                it.followStatus = reelInfo.followStatus
            }
        }
        reelsViewStateSubject.onNext(ReelsViewState.FollowStatusUpdate(listOfDataItems))
        followUserRepository.acceptRejectFollowRequest(AcceptRejectRequest(reelInfo.userId))
            .subscribeOnIoAndObserveOnMainThread({
                Timber.d("Follow User Success")
            }, { throwable ->
                listOfDataItems?.forEach {
                    if (reelInfo.userId == it.userId) {
                        it.followStatus = !reelInfo.followStatus
                    }
                }
                reelsViewStateSubject.onNext(ReelsViewState.FollowStatusUpdate(listOfDataItems))
                throwable.localizedMessage?.let {
                    reelsViewStateSubject.onNext(ReelsViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun deleteReel(reelId: Int, tabType: Int) {
        reelsRepository.deleteReel(reelId)
            .doOnSuccess {
                pullToRefresh(tabType)
            }
            .subscribeOnIoAndObserveOnMainThread({
                reelsViewStateSubject.onNext(ReelsViewState.SuccessMessage(it.message.toString()))
            }, { throwable ->
                throwable.localizedMessage?.let {
                    reelsViewStateSubject.onNext(ReelsViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getReelByHashTag(tagId: Int) {
        reelsRepository.getReelsByHashTag(pageNumber, tagId)
            .doOnSubscribe {
                isLoading = true
                reelsViewStateSubject.onNext(ReelsViewState.LoadingState(true))
            }
            .doAfterTerminate {
                isLoading = false
                reelsViewStateSubject.onNext(ReelsViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ reelsResponse ->
                reelsResponse?.data?.let {
                    if (pageNumber == 1) {
                        listOfReelsInfo = it.toMutableList()
                        reelsViewStateSubject.onNext(
                            ReelsViewState.GetReelsByTagInfo(
                                listOfReelsInfo
                            )
                        )
                    } else {
                        if (!it.isNullOrEmpty()) {
                            listOfReelsInfo.addAll(it)
                            reelsViewStateSubject.onNext(
                                ReelsViewState.GetReelsByTagInfo(
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

    fun pullToRefreshReelsByHashTag(tagId: Int) {
        pageNumber = 1
        isLoadMore = true
        isLoading = false
        listOfReelsInfo.clear()
        getReelByHashTag(tagId)
    }

    fun reportReels(reelsId: Int, reportId:Int) {
        reelsRepository.reportReel(ReportReelRequest(reelsId,reportId))
            .doOnSuccess {
                reelsViewStateSubject.onNext(ReelsViewState.LoadingState(true))
            }
            .doAfterTerminate{
                reelsViewStateSubject.onNext(ReelsViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({
                if(it.success) {
                    reelsViewStateSubject.onNext(ReelsViewState.SuccessMessage(it.message.toString()))
                } else {
                    reelsViewStateSubject.onNext(ReelsViewState.ErrorMessage(it.message.toString()))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    reelsViewStateSubject.onNext(ReelsViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }
}

sealed class ReelsViewState {
    data class ErrorMessage(val errorMessage: String) : ReelsViewState()
    data class SuccessMessage(val successMessage: String) : ReelsViewState()
    data class LoadingState(val isLoading: Boolean) : ReelsViewState()
    data class GetAllReelsInfo(val listOfReelsInfo: List<ReelInfo>) : ReelsViewState()
    data class GetReelsByTagInfo(val listOfReelsInfo: List<ReelInfo>) : ReelsViewState()
    data class FollowStatusUpdate(val listOfReelsInfo: List<ReelInfo>?) : ReelsViewState()
    object RefreshData : ReelsViewState()
}