package com.outgoer.ui.tag.viewmodel

import com.outgoer.api.chat.model.ChatMessageInfo
import com.outgoer.api.chat.model.SharePostReelsRequest
import com.outgoer.api.follow.FollowUserRepository
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.follow.model.GetFollowersAndFollowingRequest
import com.outgoer.api.post.PostRepository
import com.outgoer.api.post.model.PeopleForTag
import com.outgoer.api.post.model.PeopleForTagRequest
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import com.outgoer.ui.followdetail.viewmodel.FollowersViewState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class AddTagViewModel(
    private val postRepository: PostRepository,
    private  val followRepository: FollowUserRepository
) : BaseViewModel() {

    private val addTagStateSubject: PublishSubject<AddTagViewState> = PublishSubject.create()
    val addTagState: Observable<AddTagViewState> = addTagStateSubject.hide()

    private var listOfTagData: MutableList<PeopleForTag> = mutableListOf()
    private var searchText = ""
    private var pageNo = 1
    private var isLoading = false
    private var loadMore = true

    fun searchTagPeople(searchText: String) {
        this.searchText = searchText
        pageNo = 1
        isLoading = false
        loadMore = true
        getPeopleForTag()
    }

    fun loadMoreTagPeople() {
        if (!isLoading) {
            isLoading = true
            if (loadMore) {
                pageNo += 1
                getPeopleForTag()
            }
        }
    }

    private fun getPeopleForTag() {
        postRepository.getPeopleForTag(pageNo, PeopleForTagRequest(searchText))
            .doOnSubscribe {
                addTagStateSubject.onNext(AddTagViewState.LoadingState(true))
            }
            .doAfterTerminate {
                addTagStateSubject.onNext(AddTagViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ tagResponse ->
                tagResponse?.data?.let {
                    if (pageNo == 1) {
                        listOfTagData = it.toMutableList()
                        addTagStateSubject.onNext(AddTagViewState.ListOfPeopleForTag(listOfTagData))
                    } else {
                        if (it.isNotEmpty()) {
                            listOfTagData.addAll(it)
                            addTagStateSubject.onNext(AddTagViewState.ListOfPeopleForTag(listOfTagData))
                        } else {
                            isLoading = false
                        }
                    }
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    addTagStateSubject.onNext(AddTagViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getShareReelsPostToChat(sharePostReelsRequest: SharePostReelsRequest) {
        postRepository.getShareReelsPostToChat(sharePostReelsRequest)
            .doOnSubscribe {
                addTagStateSubject.onNext(AddTagViewState.LoadingState(true))
            }
            .doAfterTerminate {
                addTagStateSubject.onNext(AddTagViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ shareChat ->
                shareChat?.data?.let {
                    addTagStateSubject.onNext(AddTagViewState.ShareReelsPostToChat(it))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    addTagStateSubject.onNext(AddTagViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }



    private var listOfFollowers: MutableList<FollowUser> = mutableListOf()
    private var searchTextF = ""
    private var pageNoF = 1
    private var isLoadingF = false
    private var isLoadMoreF = true

    fun searchFollowersList(userId: Int, searchText: String) {
        this.searchTextF = searchText
        pageNoF = 1
        isLoadingF = false
        isLoadMoreF = true
        getFollowersList(userId)
    }

    fun loadMoreFollowersList(userId: Int) {
        if (!isLoadingF) {
            isLoadingF = true
            if (isLoadMoreF) {
                pageNoF += 1
                getFollowersList(userId)
            }
        }
    }

    private fun getFollowersList(userId: Int) {
        followRepository.getAllFollowersList(pageNoF, GetFollowersAndFollowingRequest(userId, searchTextF))
            .doOnSubscribe {
                addTagStateSubject.onNext(AddTagViewState.LoadingState(true))
            }
            .doAfterTerminate {
                addTagStateSubject.onNext(AddTagViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    if (pageNoF == 1) {
                        listOfFollowers.clear()
                        listOfFollowers.addAll(response)
                        addTagStateSubject.onNext(AddTagViewState.FollowerList(listOfFollowers))
                        isLoadingF = false
                    } else {
                        if (!it.isNullOrEmpty()) {
                            listOfFollowers.addAll(it)
                            addTagStateSubject.onNext(AddTagViewState.FollowerList(listOfFollowers))
                            isLoadingF = false
                        } else {
                            isLoadMoreF = false
                        }
                    }
                }
            }, { throwable ->
                addTagStateSubject.onNext(AddTagViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    addTagStateSubject.onNext(AddTagViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    sealed class AddTagViewState {
        data class ErrorMessage(val errorMessage: String) : AddTagViewState()
        data class SuccessMessage(val successMessage: String) : AddTagViewState()
        data class LoadingState(val isLoading: Boolean) : AddTagViewState()
        data class ListOfPeopleForTag(val ListOfPeopleForTag: List<PeopleForTag>) : AddTagViewState()
        data class ShareReelsPostToChat(val listofchat: List<ChatMessageInfo>) : AddTagViewState()
        data class FollowerList(val listOfFollowers: List<FollowUser>) : AddTagViewState()
    }
}