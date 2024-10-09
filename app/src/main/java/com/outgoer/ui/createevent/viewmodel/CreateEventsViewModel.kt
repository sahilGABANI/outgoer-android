package com.outgoer.ui.createevent.viewmodel

import android.content.Context
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.cloudflare.CloudFlareRepository
import com.outgoer.api.cloudflare.model.CloudFlareConfig
import com.outgoer.api.event.EventRepository
import com.outgoer.api.event.model.CreateEventResponse
import com.outgoer.api.event.model.EventListData
import com.outgoer.api.event.model.GooglePlaces
import com.outgoer.api.venue.model.RequestSearchVenue
import com.outgoer.api.venue.model.VenueMapInfo
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.*
import com.outgoer.ui.chat.viewmodel.ChatMessageViewState
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber
import java.io.File

class CreateEventsViewModel(private val eventRepository: EventRepository, private val cloudFlareRepository: CloudFlareRepository, private val loginUserCache: LoggedInUserCache) : BaseViewModel() {

    private val eventsViewStateSubjects: PublishSubject<EventViewState> = PublishSubject.create()
    val eventsViewState: Observable<EventViewState> = eventsViewStateSubjects.hide()

    private var pageNumber = 1
    private var isLoadMore: Boolean = true
    private var isLoading: Boolean = false
    private var listofvenues: ArrayList<VenueMapInfo> = arrayListOf()

    fun getCloudFlareConfig() {
        cloudFlareRepository.getCloudFlareConfig()
            .doOnSubscribe {
                eventsViewStateSubjects.onNext(EventViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                eventsViewStateSubjects.onNext(EventViewState.LoadingState(false))
                if (response.success) {
                    val cloudFlareConfig = response.data
                    if (cloudFlareConfig != null) {
                        eventsViewStateSubjects.onNext(EventViewState.GetCloudFlareConfig(cloudFlareConfig))
                    } else {
                        response.message?.let {
                            eventsViewStateSubjects.onNext(EventViewState.CloudFlareConfigErrorMessage(it))
                        }
                    }
                }
            }, { throwable ->
                eventsViewStateSubjects.onNext(EventViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    eventsViewStateSubjects.onNext(EventViewState.CloudFlareConfigErrorMessage(it))
                }
            }).autoDispose()
    }

    fun uploadImageToCloudFlare(context: Context, cloudFlareConfig: CloudFlareConfig, imageFile: File, selectedMediaType: String) {
        val imageTempPathDir = context.getExternalFilesDir("OutgoerImages")?.path
        val fileName = getCommonPhotoFileName(loginUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0)
        val imageCopyFile = File(imageTempPathDir + File.separator + fileName + ".jpg")
        val finalImageFile = imageFile.copyTo(imageCopyFile)

        val apiUrl = cloudFlareImageUploadBaseUrl.format(cloudFlareConfig.accountId)
        val authToken = "Bearer ".plus(cloudFlareConfig.apiToken)
        val filePart = MultipartBody.Part.createFormData(
            "file", finalImageFile.name, finalImageFile.asRequestBody("image/*".toMediaTypeOrNull())
        )
        cloudFlareRepository.uploadImageToCloudFlare(apiUrl, authToken, filePart)
            .doOnSubscribe {
                eventsViewStateSubjects.onNext(EventViewState.UploadMediaCloudFlareLoading(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.success) {
                    val result = response.result
                    if (result != null) {
                        val variants = result.variants
                        if (!variants.isNullOrEmpty()) {
                            eventsViewStateSubjects.onNext(EventViewState.UploadMediaCloudFlareSuccess(variants.first(), selectedMediaType))
                        } else {
                            handleCloudFlareMediaUploadError(response.errors)
                        }
                    } else {
                        handleCloudFlareMediaUploadError(response.errors)
                    }
                } else {
                    handleCloudFlareMediaUploadError(response.errors)
                }
            }, { throwable ->
                eventsViewStateSubjects.onNext(EventViewState.UploadMediaCloudFlareLoading(false))
                throwable.localizedMessage?.let {
                    eventsViewStateSubjects.onNext(EventViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun uploadVideoToCloudFlare(context: Context, cloudFlareConfig: CloudFlareConfig, videoFile: File, selectedMediaType: String) {
        val videoTempPathDir = context.getExternalFilesDir("OutgoerVideos")?.path
        val fileName = getCommonVideoFileName(loginUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0)
        val videoCopyFile = File(videoTempPathDir + File.separator + fileName + ".mp4")
        val finalImageFile = videoFile.copyTo(videoCopyFile)

        val apiUrl = cloudFlareVideoUploadBaseUrl.format(cloudFlareConfig.accountId)
        val authToken = "Bearer ".plus(cloudFlareConfig.videoKey)
        val filePart = MultipartBody.Part.createFormData(
            "file", finalImageFile.name, finalImageFile.asRequestBody("image/*".toMediaTypeOrNull())
        )
        cloudFlareRepository.uploadVideoUsingTus(apiUrl, authToken, finalImageFile)
            .subscribeOn(Schedulers.io())
            .flatMap {
                cloudFlareRepository.getUploadVideoDetails(it, authToken)
            }
            .observeOn(AndroidSchedulers.mainThread()) // Observe on main thread if needed
            .subscribe({

                println("it.playback?.hls: " + it.playback?.hls)
                eventsViewStateSubjects.onNext(EventViewState.UploadMediaCloudFlareVideoSuccess(it.uid.toString(), it.thumbnail.toString(), it.playback?.hls ?: "", selectedMediaType))
            }, { throwable ->
                Timber.e(throwable)
                eventsViewStateSubjects.onNext(EventViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    eventsViewStateSubjects.onNext(EventViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    private fun handleCloudFlareMediaUploadError(errors: List<String>?) {
        if (!errors.isNullOrEmpty()) {
            val error = errors.firstOrNull()
            if (error != null) {
                eventsViewStateSubjects.onNext(EventViewState.ErrorMessage(error.toString()))
            }
        }
    }
    
    fun createEvents(createEventResponse: CreateEventResponse) {
        eventRepository.createEvents(createEventResponse)
            .doOnSubscribe {
                eventsViewStateSubjects.onNext(EventViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({
                it?.let {
                    eventsViewStateSubjects.onNext(EventViewState.EventDetails(it))
                }
            }, { throwable ->
                eventsViewStateSubjects.onNext(EventViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    eventsViewStateSubjects.onNext(EventViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }



    fun resetPagination() {
        pageNumber = 1
        isLoading = false
        isLoadMore = true
        listofvenues.clear()
        getAllVenues()
    }

    fun loadMore() {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNumber++
                getAllVenues()
            }
        }
    }

    fun getAllVenues()  {
        eventRepository.getEventsList(pageNumber)
            .doOnSubscribe {
                eventsViewStateSubjects.onNext(EventViewState.LoadingState(true))
            }.doAfterTerminate {
                eventsViewStateSubjects.onNext(EventViewState.LoadingState(false))
                isLoading = false
            }.subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    listofvenues.addAll(it)
                    eventsViewStateSubjects.onNext(EventViewState.VenueMapList(listofvenues))
                }

                if (response?.isNullOrEmpty() == true) {
                    isLoadMore = false
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    eventsViewStateSubjects.onNext(EventViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    private var listOfVenueInfo: MutableList<VenueMapInfo> = mutableListOf()
    private var searchText = ""
    private var pageNo = 1
    private var visLoading = false
    private var visLoadMore = true

    fun searchVenueList(searchText: String) {
        this.searchText = searchText
        pageNo = 1
        visLoading = false
        visLoadMore = true
        getVenueList()
    }

    fun loadMoreVenueList() {
        if (!visLoading) {
            visLoading = true
            if (visLoadMore) {
                pageNo += 1
                getVenueList()
            }
        }
    }

    private fun getVenueList() {
        val request = RequestSearchVenue(
            search = searchText
        )
        eventRepository.searchEventVenue(pageNo, request)
            .doOnSubscribe {
                eventsViewStateSubjects.onNext(EventViewState.LoadingState(true))
            }
            .doAfterTerminate {
                eventsViewStateSubjects.onNext(EventViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    if (pageNo == 1) {
                        listOfVenueInfo = it.toMutableList()
                        eventsViewStateSubjects.onNext(EventViewState.VenueInfoList(listOfVenueInfo))
                        visLoading = false
                    } else {
                        if (!it.isNullOrEmpty()) {
                            listOfVenueInfo.addAll(it)
                            eventsViewStateSubjects.onNext(EventViewState.VenueInfoList(listOfVenueInfo))
                            visLoading = false
                        } else {
                            visLoadMore = false
                        }
                    }
                }
            }, { throwable ->
                eventsViewStateSubjects.onNext(EventViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    eventsViewStateSubjects.onNext(EventViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getNearGooglePlaces(searchText: String? = null)  {
        eventRepository.getNearGooglePlaces(searchText)
            .doOnSubscribe {
                eventsViewStateSubjects.onNext(EventViewState.LoadingState(true))
            }.doAfterTerminate {
                eventsViewStateSubjects.onNext(EventViewState.LoadingState(false))
                isLoading = false
            }.subscribeOnIoAndObserveOnMainThread({ response ->
                response.data?.let {
                    eventsViewStateSubjects.onNext(EventViewState.ListOfGoogleMap(it))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    eventsViewStateSubjects.onNext(EventViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

}

sealed class EventViewState {
    data class ErrorMessage(val errorMessage: String) : EventViewState()
    data class SuccessMessage(val successMessage: String) : EventViewState()
    data class LoadingState(val isLoading: Boolean) : EventViewState()
    data class EventDetails(val event: EventListData) : EventViewState()
    data class VenueMapList(val event: ArrayList<VenueMapInfo>) : EventViewState()
    data class ListOfGoogleMap(val event: ArrayList<GooglePlaces>) : EventViewState()

    data class GetCloudFlareConfig(val cloudFlareConfig: CloudFlareConfig) : EventViewState()
    data class CloudFlareConfigErrorMessage(val errorMessage: String) : EventViewState()
    data class UploadMediaCloudFlareLoading(val isLoading: Boolean) : EventViewState()
    data class UploadMediaCloudFlareSuccess(val mediaUrl: String, val selectedMediaType: String) : EventViewState()
    data class UploadMediaCloudFlareVideoSuccess(val uid: String, val mediaUrl: String, val videoUrl: String, val selectedMediaType: String) : EventViewState()
    data class VenueInfoList(val listOfVenueInfo: List<VenueMapInfo>) : EventViewState()
}
