package com.outgoer.ui.reelsdetail.viewmodel

import com.outgoer.api.follow.FollowUserRepository
import com.outgoer.api.follow.model.AcceptRejectRequest
import com.outgoer.api.post.PostRepository
import com.outgoer.api.post.model.ReportPostRequest
import com.outgoer.api.reels.ReelsRepository
import com.outgoer.api.reels.model.*
import com.outgoer.base.BaseViewModel
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import com.outgoer.ui.postdetail.viewmodel.PostDetailViewModel
import com.outgoer.ui.postdetail.viewmodel.PostDetailViewState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class ReelsDetailViewModel(
    private val reelsRepository: ReelsRepository,
    private val followUserRepository: FollowUserRepository,
) : BaseViewModel() {

    private val reelsDetailViewStateSubject: PublishSubject<ReelsDetailViewState> = PublishSubject.create()
    val reelsDetailViewState: Observable<ReelsDetailViewState> = reelsDetailViewStateSubject.hide()

    private var pageNumber: Int = 1
    private var isLoadMore: Boolean = true
    private var isLoading: Boolean = false
    private var listOfReelsInfo: MutableList<ReelInfo> = mutableListOf()

    fun getMyReel(userId: Int?) {
        reelsRepository.getMyReel(pageNumber, userId)
            .doOnSubscribe {
                isLoading = true
                reelsDetailViewStateSubject.onNext(ReelsDetailViewState.LoadingState(true))
            }
            .doAfterTerminate {
                isLoading = false
                reelsDetailViewStateSubject.onNext(ReelsDetailViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ reelsResponse ->
                reelsResponse?.data.let {
                    if (pageNumber == 1) {
                        listOfReelsInfo = it?.toMutableList() ?: mutableListOf()
                        reelsDetailViewStateSubject.onNext(ReelsDetailViewState.GetAllReelsInfo(listOfReelsInfo))
                    } else {
                        if (!it.isNullOrEmpty()) {
                            listOfReelsInfo.addAll(it)
                            reelsDetailViewStateSubject.onNext(ReelsDetailViewState.GetAllReelsInfo(listOfReelsInfo))
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

    fun loadMore(userId: Int?) {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNumber++
                getMyReel(userId)
            }
        }
    }

    fun pullToRefresh(userId: Int?) {
        pageNumber = 1
        isLoadMore = true
        isLoading = false
        listOfReelsInfo.clear()
        getMyReel(userId)
    }

    fun addLikeToReel(reelInfo: ReelInfo) {
        reelsRepository.addLikeToReel(AddReelLikeRequest(reelInfo.id))
            .subscribeOnIoAndObserveOnMainThread({
                Timber.d("add Reel Like Success")
            }, { throwable ->
                throwable.localizedMessage?.let {
                    reelsDetailViewStateSubject.onNext(ReelsDetailViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun removeLikeFromReel(reelInfo: ReelInfo) {
        reelsRepository.removeLikeFromReel(RemoveReelLikeRequest(reelInfo.id))
            .subscribeOnIoAndObserveOnMainThread({
                Timber.d("remove Reel Like Success")
            }, { throwable ->
                throwable.localizedMessage?.let {
                    reelsDetailViewStateSubject.onNext(ReelsDetailViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun addReelToBookmark(reelInfo: ReelInfo) {
        reelsRepository.addReelToBookmark(AddBookmarkToReelRequest(reelInfo.id))
            .subscribeOnIoAndObserveOnMainThread({
                Timber.d("add Reel Bookmark Success")
            }, { throwable ->
                throwable.localizedMessage?.let {
                    reelsDetailViewStateSubject.onNext(ReelsDetailViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun removeReelToBookmark(reelInfo: ReelInfo) {
        reelsRepository.removeReelToBookmark(RemoveBookmarkFromReelRequest(reelInfo.id))
            .subscribeOnIoAndObserveOnMainThread({
                Timber.d("remove Reel Bookmark Success")
            }, { throwable ->
                throwable.localizedMessage?.let {
                    reelsDetailViewStateSubject.onNext(ReelsDetailViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun followUnfollowUser(listOfDataItems: List<ReelInfo>?, reelInfo: ReelInfo) {
        listOfDataItems?.forEach {
            if (reelInfo.userId == it.userId) {
                it.followStatus = reelInfo.followStatus
            }
        }
        reelsDetailViewStateSubject.onNext(ReelsDetailViewState.FollowStatusUpdate(listOfDataItems))
        followUserRepository.acceptRejectFollowRequest(AcceptRejectRequest(reelInfo.userId))
            .subscribeOnIoAndObserveOnMainThread({
                Timber.d("Follow User Success")
            }, { throwable ->
                listOfDataItems?.forEach {
                    if (reelInfo.userId == it.userId) {
                        it.followStatus = !reelInfo.followStatus
                    }
                }
                reelsDetailViewStateSubject.onNext(ReelsDetailViewState.FollowStatusUpdate(listOfDataItems))
                throwable.localizedMessage?.let {
                    reelsDetailViewStateSubject.onNext(ReelsDetailViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun deleteReel(reelId: Int) {
        reelsRepository.deleteReel(reelId)
            .subscribeOnIoAndObserveOnMainThread({
                RxBus.publish(RxEvent.RefreshHomePagePost)
                RxBus.publish(RxEvent.RefreshMyProfile)
                reelsDetailViewStateSubject.onNext(ReelsDetailViewState.SuccessMessage(it.message.toString()))
            }, { throwable ->
                throwable.localizedMessage?.let {
                    reelsDetailViewStateSubject.onNext(ReelsDetailViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getReelById(reelId: Int) {
        reelsRepository.getReelById(reelId)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.data?.let {
                    reelsDetailViewStateSubject.onNext(ReelsDetailViewState.GetReelInfo(it))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    reelsDetailViewStateSubject.onNext(ReelsDetailViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun reportReels(reelId: Int,reportId:Int) {
        reelsRepository.reportReel(ReportReelRequest(reelId,reportId))
            .doOnSuccess {
                reelsDetailViewStateSubject.onNext(ReelsDetailViewState.LoadingState(true))
            }
            .doAfterTerminate{
                reelsDetailViewStateSubject.onNext(ReelsDetailViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({
                if(it.success) {
                    reelsDetailViewStateSubject.onNext(ReelsDetailViewState.SuccessMessage(it.message.toString()))
                } else {
                    reelsDetailViewStateSubject.onNext(ReelsDetailViewState.ErrorMessage(it.message.toString()))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    reelsDetailViewStateSubject.onNext(ReelsDetailViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    sealed class ReelsDetailViewState {
        data class ErrorMessage(val errorMessage: String) : ReelsDetailViewState()
        data class SuccessMessage(val successMessage: String) : ReelsDetailViewState()
        data class LoadingState(val isLoading: Boolean) : ReelsDetailViewState()
        data class DeleteReelsState(val successMessage: String) : ReelsDetailViewState()

        data class GetAllReelsInfo(val listOfReelsInfo: List<ReelInfo>) : ReelsDetailViewState()

        data class FollowStatusUpdate(val listOfReelsInfo: List<ReelInfo>?) : ReelsDetailViewState()
        data class GetReelInfo(val reelInfo: ReelInfo) : ReelsDetailViewState()
    }
}