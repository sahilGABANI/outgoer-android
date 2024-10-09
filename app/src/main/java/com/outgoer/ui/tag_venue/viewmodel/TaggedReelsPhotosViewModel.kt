package com.outgoer.ui.tag_venue.viewmodel

import com.outgoer.api.follow.FollowUserRepository
import com.outgoer.api.follow.model.AcceptRejectRequest
import com.outgoer.api.post.PostRepository
import com.outgoer.api.post.model.*
import com.outgoer.api.reels.ReelsRepository
import com.outgoer.api.reels.model.*
import com.outgoer.api.sponty.SpontyRepository
import com.outgoer.api.sponty.model.AllJoinSpontyRequest
import com.outgoer.api.sponty.model.SpontyActionRequest
import com.outgoer.api.sponty.model.SpontyActionResponse
import com.outgoer.api.sponty.model.SpontyResponse
import com.outgoer.api.tagged_post_reels.TaggedPostReelsRepository
import com.outgoer.api.tagged_post_reels.model.TaggedPostReelsRequest
import com.outgoer.api.tagged_post_reels.model.TaggedPostReelsViewRequest
import com.outgoer.api.tagged_post_reels.model.TaggedPostReelsViewResponse
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class TaggedReelsPhotosViewModel(private val venueTaggedRepository: TaggedPostReelsRepository, private val reelsRepository: ReelsRepository,
                                 private val postRepository: PostRepository,
                                 private val spontyRepository: SpontyRepository,
                                 private val followUserRepository: FollowUserRepository) : BaseViewModel() {

    private val venueTaggedStateSubject: PublishSubject<VenueTaggedViewState> = PublishSubject.create()
    val venueTaggedState: Observable<VenueTaggedViewState> = venueTaggedStateSubject.hide()

    private var pageNo = 1
    private var isLoading = false
    private var isLoadMore = true

    fun loadMoreVenuePostReelList(TaggedPostReelsRequest: TaggedPostReelsRequest) {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo += 1
                getVenuePostReelList(TaggedPostReelsRequest)

            }
        }
    }

    fun resetPaginationVenuePostReelList(taggedPostReelsRequest: TaggedPostReelsRequest) {
        pageNo = 1
        isLoading = false
        isLoadMore = true
        getVenuePostReelList(taggedPostReelsRequest)
    }

    fun getVenuePostReelList(taggedPostReelsRequest: TaggedPostReelsRequest) {
        if(taggedPostReelsRequest.tabType.equals("1")) {
            venueTaggedRepository.getVenueTaggedReel(pageNo, taggedPostReelsRequest)
                .doOnSubscribe {
                    venueTaggedStateSubject.onNext(VenueTaggedViewState.LoadingState(true))
                }
                .doAfterTerminate {
                    isLoading = false
                    venueTaggedStateSubject.onNext(VenueTaggedViewState.LoadingState(false))
                }
                .subscribeOnIoAndObserveOnMainThread({ response ->

                    response.data?.let {
                        venueTaggedStateSubject.onNext(VenueTaggedViewState.ListOfReelInfo(it as ArrayList<ReelInfo>))
                    }
                }, { throwable ->
                    venueTaggedStateSubject.onNext(VenueTaggedViewState.LoadingState(false))
                    throwable.localizedMessage?.let {
                        venueTaggedStateSubject.onNext(VenueTaggedViewState.ErrorMessage(it))
                    }
                }).autoDispose()
        } else if(taggedPostReelsRequest.tabType.equals("2")) {
            venueTaggedRepository.getVenueTaggedPost(pageNo, taggedPostReelsRequest)
                .doOnSubscribe {
                    venueTaggedStateSubject.onNext(VenueTaggedViewState.LoadingState(true))
                }
                .doAfterTerminate {
                    isLoading = false
                    venueTaggedStateSubject.onNext(VenueTaggedViewState.LoadingState(false))
                }
                .subscribeOnIoAndObserveOnMainThread({ response ->

                    response.data?.let {
                        venueTaggedStateSubject.onNext(VenueTaggedViewState.ListOfPostInfo(it as ArrayList<PostInfo>))
                    }
                }, { throwable ->
                    venueTaggedStateSubject.onNext(VenueTaggedViewState.LoadingState(false))
                    throwable.localizedMessage?.let {
                        venueTaggedStateSubject.onNext(VenueTaggedViewState.ErrorMessage(it))
                    }
                }).autoDispose()
        } else if(taggedPostReelsRequest.tabType.equals("3")) {
            venueTaggedRepository.getVenueTaggedSponty(pageNo, taggedPostReelsRequest)
                .doOnSubscribe {
                    venueTaggedStateSubject.onNext(VenueTaggedViewState.LoadingState(true))
                }
                .doAfterTerminate {
                    isLoading = false
                    venueTaggedStateSubject.onNext(VenueTaggedViewState.LoadingState(false))
                }
                .subscribeOnIoAndObserveOnMainThread({ response ->

                    response.data?.let {
                        venueTaggedStateSubject.onNext(VenueTaggedViewState.ListOfSpontyInfo(it as ArrayList<SpontyResponse>))
                    }
                }, { throwable ->
                    venueTaggedStateSubject.onNext(VenueTaggedViewState.LoadingState(false))
                    throwable.localizedMessage?.let {
                        venueTaggedStateSubject.onNext(VenueTaggedViewState.ErrorMessage(it))
                    }
                }).autoDispose()
        }
    }


    fun addLikeToReel(reelInfo: ReelInfo) {
        reelsRepository.addLikeToReel(AddReelLikeRequest(reelInfo.id))
            .subscribeOnIoAndObserveOnMainThread({
                Timber.d("add Reel Like Success")
            }, { throwable ->
                throwable.localizedMessage?.let {
                    venueTaggedStateSubject.onNext(VenueTaggedViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun removeLikeFromReel(reelInfo: ReelInfo) {
        reelsRepository.removeLikeFromReel(RemoveReelLikeRequest(reelInfo.id))
            .subscribeOnIoAndObserveOnMainThread({
                Timber.d("remove Reel Like Success")
            }, { throwable ->
                throwable.localizedMessage?.let {
                    venueTaggedStateSubject.onNext(VenueTaggedViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun addReelToBookmark(reelInfo: ReelInfo) {
        reelsRepository.addReelToBookmark(AddBookmarkToReelRequest(reelInfo.id))
            .subscribeOnIoAndObserveOnMainThread({
                Timber.d("add Reel Bookmark Success")
            }, { throwable ->
                throwable.localizedMessage?.let {
                    venueTaggedStateSubject.onNext(VenueTaggedViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun removeReelToBookmark(reelInfo: ReelInfo) {
        reelsRepository.removeReelToBookmark(RemoveBookmarkFromReelRequest(reelInfo.id))
            .subscribeOnIoAndObserveOnMainThread({
                Timber.d("remove Reel Bookmark Success")
            }, { throwable ->
                throwable.localizedMessage?.let {
                    venueTaggedStateSubject.onNext(VenueTaggedViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun followUnfollowUser(listOfDataItems: List<ReelInfo>?, reelInfo: ReelInfo) {
        listOfDataItems?.forEach {
            if (reelInfo.userId == it.userId) {
                it.followStatus = reelInfo.followStatus
            }
        }
        venueTaggedStateSubject.onNext(VenueTaggedViewState.FollowStatusUpdate(listOfDataItems))
        followUserRepository.acceptRejectFollowRequest(AcceptRejectRequest(reelInfo.userId))
            .subscribeOnIoAndObserveOnMainThread({
                Timber.d("Follow User Success")
            }, { throwable ->
                listOfDataItems?.forEach {
                    if (reelInfo.userId == it.userId) {
                        it.followStatus = !reelInfo.followStatus
                    }
                }
                venueTaggedStateSubject.onNext(VenueTaggedViewState.FollowStatusUpdate(listOfDataItems))
                throwable.localizedMessage?.let {
                    venueTaggedStateSubject.onNext(VenueTaggedViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun deleteReel(reelId: Int, tabType: String, venueId: Int) {
        reelsRepository.deleteReel(reelId)
            .doOnSuccess {
                resetPaginationVenuePostReelList(TaggedPostReelsRequest(tabType, venueId))
            }
            .subscribeOnIoAndObserveOnMainThread({
                venueTaggedStateSubject.onNext(VenueTaggedViewState.SuccessMessage(it.message.toString()))
            }, { throwable ->
                throwable.localizedMessage?.let {
                    venueTaggedStateSubject.onNext(VenueTaggedViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }


    fun addPostLike(postInfo: PostInfo) {
        postRepository.addLikesToPost(AddLikesRequest(postInfo.id))
            .subscribeOnIoAndObserveOnMainThread({
                Timber.d("add Post Like Success")
            }, { throwable ->
                throwable.localizedMessage?.let {
                    venueTaggedStateSubject.onNext(VenueTaggedViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun removeLikeFromPost(postInfo: PostInfo) {
        postRepository.removeLikeFromPost(RemoveLikesRequest(postInfo.id))
            .subscribeOnIoAndObserveOnMainThread({
                Timber.d("remove Post Like Success")
            }, { throwable ->
                throwable.localizedMessage?.let {
                    venueTaggedStateSubject.onNext(VenueTaggedViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun addPostToBookmark(postInfo: PostInfo) {
        postRepository.addPostToBookmark(AddBookmarkRequest(postInfo.id))
            .subscribeOnIoAndObserveOnMainThread({
                Timber.d("add Post Bookmark Success")
            }, { throwable ->
                throwable.localizedMessage?.let {
                    venueTaggedStateSubject.onNext(VenueTaggedViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun removePostToBookmark(postInfo: PostInfo) {
        postRepository.removePostToBookmark(RemoveBookmarkRequest(postInfo.id))
            .subscribeOnIoAndObserveOnMainThread({
                Timber.d("remove Post Bookmark Success")
            }, { throwable ->
                throwable.localizedMessage?.let {
                    venueTaggedStateSubject.onNext(VenueTaggedViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun deletePost(postId: Int, venueId: Int) {
        postRepository.deletePost(postId)
            .doOnSuccess {
                resetPaginationVenuePostReelList(TaggedPostReelsRequest("2", venueId))
            }
            .subscribeOnIoAndObserveOnMainThread({
                venueTaggedStateSubject.onNext(VenueTaggedViewState.SuccessMessage(it.message.toString()))
            }, { throwable ->
                throwable.localizedMessage?.let {
                    venueTaggedStateSubject.onNext(VenueTaggedViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }


    fun addRemoveSpontyLike(spontyActionRequest: SpontyActionRequest) {
        spontyRepository.addRemoveSpontyLike(spontyActionRequest)
            .doOnSubscribe {
                venueTaggedStateSubject.onNext(VenueTaggedViewState.LoadingState(true))
            }
            .doAfterTerminate {
                venueTaggedStateSubject.onNext(VenueTaggedViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.data?.let {
                    venueTaggedStateSubject.onNext(VenueTaggedViewState.AddRemoveSpontyLike(it, spontyActionRequest.spontyId))
                }
            }, { throwable ->
                venueTaggedStateSubject.onNext(VenueTaggedViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    venueTaggedStateSubject.onNext(VenueTaggedViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun addRemoveSponty(allJoinSpontyRequest: AllJoinSpontyRequest)  {
        spontyRepository.addRemoveSponty(allJoinSpontyRequest)
            .doOnSubscribe {
                venueTaggedStateSubject.onNext(VenueTaggedViewState.LoadingState(true))
            }.doAfterTerminate {
                venueTaggedStateSubject.onNext(VenueTaggedViewState.LoadingState(false))
            }.subscribeOnIoAndObserveOnMainThread({ response ->
                response.joinStatus?.let {
                    venueTaggedStateSubject.onNext(VenueTaggedViewState.AddSpontyJoin(it))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    venueTaggedStateSubject.onNext(VenueTaggedViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getTaggedViewChange(taggedPostReelsViewRequest: TaggedPostReelsViewRequest)  {
        venueTaggedRepository.getTaggedViewChange(taggedPostReelsViewRequest)
            .doOnSubscribe {
                venueTaggedStateSubject.onNext(VenueTaggedViewState.LoadingState(true))
            }.doAfterTerminate {
                venueTaggedStateSubject.onNext(VenueTaggedViewState.LoadingState(false))
            }.subscribeOnIoAndObserveOnMainThread({ response ->
                response.data?.let {
                    venueTaggedStateSubject.onNext(VenueTaggedViewState.GetTaggedVeune(it))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    venueTaggedStateSubject.onNext(VenueTaggedViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun removeSponty(commentId: Int) {
        spontyRepository.removeSponty(commentId)
            .doOnSubscribe {
                venueTaggedStateSubject.onNext(VenueTaggedViewState.LoadingState(true))
            }
            .doAfterTerminate {
                venueTaggedStateSubject.onNext(VenueTaggedViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.message?.let {
                    venueTaggedStateSubject.onNext(VenueTaggedViewState.SuccessCommentMessage(it, commentId))
                }
            }, { throwable ->
                venueTaggedStateSubject.onNext(VenueTaggedViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    venueTaggedStateSubject.onNext(VenueTaggedViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }
}

sealed class VenueTaggedViewState {
    data class ErrorMessage(val errorMessage: String) : VenueTaggedViewState()
    data class SuccessMessage(val successMessage: String) : VenueTaggedViewState()
    data class LoadingState(val isLoading: Boolean) : VenueTaggedViewState()

    data class ListOfReelInfo(val listofreel: ArrayList<ReelInfo>) : VenueTaggedViewState()
    data class ListOfPostInfo(val listofpost: ArrayList<PostInfo>) : VenueTaggedViewState()
    data class ListOfSpontyInfo(val listofpost: ArrayList<SpontyResponse>) : VenueTaggedViewState()

    data class FollowStatusUpdate(val listOfReelsInfo: List<ReelInfo>?) : VenueTaggedViewState()
    data class AddRemoveSpontyLike(val addSpontyLike: SpontyActionResponse, val spontyId: Int) : VenueTaggedViewState()
    data class AddSpontyJoin(val joinStatus: Int) : VenueTaggedViewState()
    data class GetTaggedVeune(val taggedPostReelsViewResponse: TaggedPostReelsViewResponse) : VenueTaggedViewState()
    data class SuccessCommentMessage(val successMessage: String, val commentId: Int) : VenueTaggedViewState()

}