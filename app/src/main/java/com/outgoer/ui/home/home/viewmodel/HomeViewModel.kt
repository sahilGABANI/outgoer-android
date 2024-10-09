package com.outgoer.ui.home.home.viewmodel

import com.outgoer.api.post.PostRepository
import com.outgoer.api.post.model.*
import com.outgoer.api.reels.ReelsRepository
import com.outgoer.api.reels.model.ReelInfo
import com.outgoer.api.sponty.SpontyRepository
import com.outgoer.api.sponty.model.AllJoinSpontyRequest
import com.outgoer.api.sponty.model.ReportSpontyRequest
import com.outgoer.api.sponty.model.SpontyActionRequest
import com.outgoer.api.sponty.model.SpontyActionResponse
import com.outgoer.api.story.StoryRepository
import com.outgoer.api.story.model.StoryListResponse
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import com.outgoer.ui.sponty.viewmodel.SpontyViewModel
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class HomeViewModel(
    private val postRepository: PostRepository,
    private val storyRepository: StoryRepository,
    private val spontyRepository: SpontyRepository
) : BaseViewModel() {

    companion object {
        val homePageStateSubject: PublishSubject<HomePageViewState> = PublishSubject.create()
        val homePageState: Observable<HomePageViewState> = homePageStateSubject.hide()
    }

    fun spontyReport(reportSpontyRequest: ReportSpontyRequest) {
        spontyRepository.spontyReport(reportSpontyRequest)
            .doOnSubscribe {
                homePageStateSubject.onNext(HomePageViewState.CommentLoadingState(true))
            }
            .doAfterTerminate {
                homePageStateSubject.onNext(HomePageViewState.CommentLoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.message?.let {
                    homePageStateSubject.onNext(HomePageViewState.SuccessReportMessage(it))
                }
            }, { throwable ->
                homePageStateSubject.onNext(HomePageViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    homePageStateSubject.onNext(HomePageViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }


    //-------------------Get All Post Pagination-------------------
    private var listOfStories: MutableList<StoryListResponse> = mutableListOf()
    private var pageNumberStory: Int = 1
    private var isLoadMoreStory: Boolean = true
    private var isLoadingStory: Boolean = false

    fun pullToRefreshStory(timeZone: String, isReload: Boolean) {
        pageNumberStory = 1
        isLoadMoreStory = true
        isLoadingStory = false
        listOfStories.clear()
        getAllStory(timeZone, isReload)
    }

    private fun getAllStory(timeZone: String, isReload: Boolean) {
        storyRepository.getListOfStories(pageNumber, timeZone)
            .doOnSubscribe {
                homePageStateSubject.onNext(HomePageViewState.LoadingState(isReload))
            }
            .doAfterTerminate {
                homePageStateSubject.onNext(HomePageViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.data?.let {


                    if (pageNumberStory == 1) {
                        listOfStories = response.data.toMutableList()
                        homePageStateSubject.onNext(HomePageViewState.GetAllStoryInfo(listOfStories))
                        isLoadingStory = false

                    } else {
                        if (!it.isNullOrEmpty()) {
                            listOfStories.addAll(it)
                            homePageStateSubject.onNext(
                                HomePageViewState.GetAllStoryInfo(
                                    listOfStories
                                )
                            )
                            isLoadingStory = false
                        } else {
                            isLoadingStory = false
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

    fun loadMoreStory(timeZone: String) {
        if (!isLoadingStory) {
            isLoadingStory = true
            if (isLoadMoreStory) {
                pageNumberStory++
                getAllStory(timeZone, false)
            }
        }
    }


    //-------------------Get All Post Pagination-------------------
    private var listOfPostData: MutableList<PostInfo> = mutableListOf()
    private var pageNumber: Int = 1
    private var isLoadMore: Boolean = true
    private var isLoading: Boolean = false

    fun getPostInfo(): MutableList<PostInfo> {
        return listOfPostData
    }
    fun pullToRefresh(isReload: Boolean) {
        pageNumber = 1
        isLoadMore = true
        isLoading = false
        listOfPostData.clear()
        getAllPost(isReload)
    }

    private fun getAllPost(isReload: Boolean) {
        postRepository.getAllPost(pageNumber)
            .doOnSubscribe {
                homePageStateSubject.onNext(HomePageViewState.LoadingState(isReload))
            }
            .doAfterTerminate {
                homePageStateSubject.onNext(HomePageViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.data?.let {
                    if (pageNumber == 1) {
                        listOfPostData = response.data.toMutableList()
                        homePageStateSubject.onNext(HomePageViewState.GetAllPostInfo(listOfPostData))
                        isLoading = false

                    } else {
                        if (!it.isNullOrEmpty()) {
                            listOfPostData.addAll(it)
                            homePageStateSubject.onNext(
                                HomePageViewState.GetAllPostInfo(
                                    listOfPostData
                                )
                            )
                            isLoading = false
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

    fun loadMore() {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNumber++
                getAllPost(false)
            }
        }
    }

    //-------------------Get All Post Pagination-------------------
    fun addPostLike(postInfo: PostInfo) {
        postRepository.addLikesToPost(AddLikesRequest(postInfo.id))
            .subscribeOnIoAndObserveOnMainThread({
                Timber.d("add Post Like Success")
            }, { throwable ->
                throwable.localizedMessage?.let {
                    homePageStateSubject.onNext(HomePageViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun removeLikeFromPost(postInfo: PostInfo) {
        postRepository.removeLikeFromPost(RemoveLikesRequest(postInfo.id))
            .subscribeOnIoAndObserveOnMainThread({
                Timber.d("remove Post Like Success")
            }, { throwable ->
                throwable.localizedMessage?.let {
                    homePageStateSubject.onNext(HomePageViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun addPostToBookmark(postInfo: PostInfo) {
        postRepository.addPostToBookmark(AddBookmarkRequest(postInfo.id))
            .subscribeOnIoAndObserveOnMainThread({
                Timber.d("add Post Bookmark Success")
            }, { throwable ->
                throwable.localizedMessage?.let {
                    homePageStateSubject.onNext(HomePageViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun removePostToBookmark(postInfo: PostInfo) {
        postRepository.removePostToBookmark(RemoveBookmarkRequest(postInfo.id))
            .subscribeOnIoAndObserveOnMainThread({
                Timber.d("remove Post Bookmark Success")
            }, { throwable ->
                throwable.localizedMessage?.let {
                    homePageStateSubject.onNext(HomePageViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun deletePost(postId: Int) {
        postRepository.deletePost(postId)
            .doOnSuccess {
//                resetPagination()
            }
            .subscribeOnIoAndObserveOnMainThread({
                homePageStateSubject.onNext(HomePageViewState.SuccessMessage(it.message.toString()))
            }, { throwable ->
                throwable.localizedMessage?.let {
                    homePageStateSubject.onNext(HomePageViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    private fun resetPagination() {
        listOfPostData.clear()
        pageNumber = 1
        isLoadMore = true
        isLoading = false
        getAllPost(false)
    }

    fun reportPost(postId: Int,reportId:Int) {
        postRepository.reportPost(ReportPostRequest(postId,reportId))
            .doOnSuccess {
              // homePageStateSubject.onNext(HomePageViewState.LoadingState(true))
            }
            .doAfterTerminate{
                //homePageStateSubject.onNext(HomePageViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({
                if(it.success) {
                    homePageStateSubject.onNext(HomePageViewState.SuccessMessage(it.message.toString()))
                } else {
                    homePageStateSubject.onNext(HomePageViewState.ErrorMessage(it.message.toString()))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    homePageStateSubject.onNext(HomePageViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun addRemoveSpontyLike(spontyActionRequest: SpontyActionRequest) {
        spontyRepository.addRemoveSpontyLike(spontyActionRequest)
            .doOnSubscribe {
//                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(true))
            }
            .doAfterTerminate {
//                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.data?.let {
                    homePageStateSubject.onNext(HomePageViewState.AddRemoveSpontyLike(it, spontyActionRequest.spontyId))
                }
            }, { throwable ->
                homePageStateSubject.onNext(HomePageViewState.LoadingState(false))
                homePageStateSubject?.let {
                    homePageStateSubject.onNext(HomePageViewState.ErrorMessage(throwable?.message ?: ""))
                }
            }).autoDispose()
    }


    fun addRemoveSponty(allJoinSpontyRequest: AllJoinSpontyRequest) {
        spontyRepository.addRemoveSponty(allJoinSpontyRequest)
            .doOnSubscribe {
                homePageStateSubject.onNext(HomePageViewState.LoadingState(true))
            }.doAfterTerminate {
                homePageStateSubject.onNext(HomePageViewState.LoadingState(false))
            }.subscribeOnIoAndObserveOnMainThread({ response ->
                response.joinStatus?.let {
                    homePageStateSubject.onNext(HomePageViewState.AddSpontyJoin(it))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    homePageStateSubject.onNext(HomePageViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }


}

//-------------------Get All Reel Pagination-------------------
sealed class HomePageViewState {
//    object ReloadData : HomePageViewState()

    data class AddSpontyJoin(val joinStatus: Int) : HomePageViewState()

    data class AddRemoveSpontyLike(val addSpontyLike: SpontyActionResponse, val spontyId: Int) : HomePageViewState()

    data class ReloadData(val postId: Int, val typeAdd: Boolean) : HomePageViewState()
    data class ErrorMessage(val errorMessage: String) : HomePageViewState()
    data class SuccessMessage(val successMessage: String) : HomePageViewState()
    data class LoadingState(val isLoading: Boolean) : HomePageViewState()
    data class GetAllPostInfo(val postInfoList: List<PostInfo>) : HomePageViewState()
    data class GetAllStoryInfo(val storyListInfo: List<StoryListResponse>) : HomePageViewState()
    data class SuccessReportMessage(val successMessage: String) : HomePageViewState()

    data class CommentLoadingState(val isLoading: Boolean) : HomePageViewState()

    data class FollowStatusUpdate(
        val reelInfoList: List<ReelInfo>?,
        val postInfoList: List<PostInfo>?
    ) : HomePageViewState()
}