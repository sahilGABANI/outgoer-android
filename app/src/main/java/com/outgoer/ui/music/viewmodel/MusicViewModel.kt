package com.outgoer.ui.music.viewmodel

import android.content.Context
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.cloudflare.CloudFlareRepository
import com.outgoer.api.cloudflare.model.CloudFlareConfig
import com.outgoer.api.hashtag.HashtagRepository
import com.outgoer.api.hashtag.model.HashtagResponse
import com.outgoer.api.music.MusicRepository
import com.outgoer.api.music.model.MusicCategoryResponse
import com.outgoer.api.music.model.MusicResponse
import com.outgoer.api.post.PostRepository
import com.outgoer.api.post.model.CreatePostRequest
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.cloudFlareImageUploadBaseUrl
import com.outgoer.base.extension.cloudFlareVideoUploadBaseUrl
import com.outgoer.base.extension.getCommonPhotoFileName
import com.outgoer.base.extension.getCommonVideoFileName
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import com.skydoves.viewmodel.lifecycle.viewModelLifecycleOwner
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber
import java.io.File

class MusicViewModel(
    private val musicRepository: MusicRepository
) : BaseViewModel() {

    private val musicStateSubject: PublishSubject<MusicInfoViewState> = PublishSubject.create()
    val musicState: Observable<MusicInfoViewState> = musicStateSubject.hide()

    private var listOfMusic: MutableList<MusicResponse> = mutableListOf()

    private var pageNumber: Int = 1
    private var isLoadMore: Boolean = true
    private var isLoading: Boolean = false

    fun getMusicCategory() {
        musicRepository.getMusicCategory()
            .doOnSubscribe {
                musicStateSubject.onNext(MusicInfoViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({
                musicStateSubject.onNext(MusicInfoViewState.LoadingState(false))

                it.data?.let { categoryList ->
                    musicStateSubject.onNext(MusicInfoViewState.GetMusicCategoryList(categoryList))
                }
            }, { throwable ->
                musicStateSubject.onNext(MusicInfoViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    musicStateSubject.onNext(MusicInfoViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun resetPagination(categoryId: Int, search: String? = null) {
        listOfMusic.clear()
        pageNumber = 1
        isLoadMore = true
        isLoading = false
        getMusicList(categoryId, search)
    }

    fun loadMore(categoryId: Int, search: String? = null) {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNumber++
                getMusicList(categoryId, search)
            }
        }
    }
    fun getMusicList(categoryId: Int, search: String? = null) {
        musicRepository.getMusicList(pageNumber, categoryId, search)
            .doOnSubscribe {
                musicStateSubject.onNext(MusicInfoViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({

                if(pageNumber == 1) {
                    listOfMusic.clear()
                }
                musicStateSubject.onNext(MusicInfoViewState.LoadingState(false))

                it.data?.let { musicList ->
                    listOfMusic.addAll(musicList)
                    musicStateSubject.onNext(MusicInfoViewState.GetMusicList(listOfMusic))
                }
            }, { throwable ->
                musicStateSubject.onNext(MusicInfoViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    musicStateSubject.onNext(MusicInfoViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    sealed class MusicInfoViewState {
        data class LoadingState(val isLoading: Boolean) : MusicInfoViewState()
        data class ErrorMessage(val errorMessage: String) : MusicInfoViewState()
        data class GetMusicList(val listOfMusic: MutableList<MusicResponse>) : MusicInfoViewState()
        data class GetMusicCategoryList(val listOfMusicCategory: MutableList<MusicCategoryResponse>) : MusicInfoViewState()
    }
}