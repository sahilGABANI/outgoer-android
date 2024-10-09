package com.outgoer.ui.addvenuemedia.viewmodel

import android.content.Context
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.cloudflare.CloudFlareRepository
import com.outgoer.api.cloudflare.model.CloudFlareConfig
import com.outgoer.api.createvenue.CreateVenueRepository
import com.outgoer.api.createvenue.model.CreateVenueResponse
import com.outgoer.api.venue.VenueRepository
import com.outgoer.api.venue.model.AddVenueMediaListRequest
import com.outgoer.api.venue.model.RegisterVenueRequest
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber
import java.io.File

class AddVenueMediaViewModel(
    private val cloudFlareRepository: CloudFlareRepository,
    private val venueRepository: VenueRepository,
    private val loginUserCache: LoggedInUserCache,
    private val createVenueRepository :CreateVenueRepository
) : BaseViewModel() {

    private val addVenueMediaStateSubject: PublishSubject<AddVenueMediaViewState> = PublishSubject.create()
    val addVenueMediaState: Observable<AddVenueMediaViewState> = addVenueMediaStateSubject.hide()

    fun getCloudFlareConfig() {
        cloudFlareRepository.getCloudFlareConfig()
            .doOnSubscribe {
                addVenueMediaStateSubject.onNext(AddVenueMediaViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                addVenueMediaStateSubject.onNext(AddVenueMediaViewState.LoadingState(false))
                if (response.success) {
                    val cloudFlareConfig = response.data
                    if (cloudFlareConfig != null) {
                        addVenueMediaStateSubject.onNext(AddVenueMediaViewState.GetCloudFlareConfig(cloudFlareConfig))
                    } else {
                        response.message?.let {
                            addVenueMediaStateSubject.onNext(AddVenueMediaViewState.CloudFlareConfigErrorMessage(it))
                        }
                    }
                }
            }, { throwable ->
                addVenueMediaStateSubject.onNext(AddVenueMediaViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    addVenueMediaStateSubject.onNext(AddVenueMediaViewState.CloudFlareConfigErrorMessage(it))
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
                addVenueMediaStateSubject.onNext(AddVenueMediaViewState.UploadMediaCloudFlareLoading(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.success) {
                    addVenueMediaStateSubject.onNext(AddVenueMediaViewState.UploadMediaCloudFlareLoading(false))
                    val result = response.result
                    if (result != null) {
                        val variants = result.variants
                        if (!variants.isNullOrEmpty()) {
                            addVenueMediaStateSubject.onNext(AddVenueMediaViewState.UploadMediaCloudFlareSuccess(variants.first(), selectedMediaType))
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
                addVenueMediaStateSubject.onNext(AddVenueMediaViewState.UploadMediaCloudFlareLoading(false))
                throwable.localizedMessage?.let {
                    addVenueMediaStateSubject.onNext(AddVenueMediaViewState.ErrorMessage(it))
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
                addVenueMediaStateSubject.onNext(
                    AddVenueMediaViewState.UploadMediaCloudFlareSuccess(
                        it.uid.toString(),
                        selectedMediaType,
                        it.thumbnail.toString()
                    )
                )
            }, { throwable ->
                Timber.e(throwable)
                addVenueMediaStateSubject.onNext(AddVenueMediaViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    addVenueMediaStateSubject.onNext(AddVenueMediaViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    private fun handleCloudFlareMediaUploadError(errors: List<String>?) {
        if (!errors.isNullOrEmpty()) {
            val error = errors.firstOrNull()
            if (error != null) {
                addVenueMediaStateSubject.onNext(AddVenueMediaViewState.ErrorMessage(error.toString()))
            }
        }
    }

    fun addVenueMedia(request: AddVenueMediaListRequest) {
        venueRepository.addVenueMedia(request)
            .doOnSubscribe {
                addVenueMediaStateSubject.onNext(AddVenueMediaViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({
                addVenueMediaStateSubject.onNext(AddVenueMediaViewState.AddVenueMediaSuccess)
            }, { throwable ->
                addVenueMediaStateSubject.onNext(AddVenueMediaViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    addVenueMediaStateSubject.onNext(AddVenueMediaViewState.CloudFlareConfigErrorMessage(it))
                }
            }).autoDispose()
    }

    fun updateVenue(registerVenueRequest: RegisterVenueRequest) {
        createVenueRepository.updateVenue(registerVenueRequest)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.data?.let {
                    addVenueMediaStateSubject.onNext(AddVenueMediaViewState.UpdateVenueSuccess(it, response.message ?: ""))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    Timber.tag("<><>").e("Update Venue Error 1 ".plus(it))
                    Timber.tag("<><>").e("Update Venue Error 2 ".plus(throwable.message))
                    Timber.tag("<><>").e("Update Venue Error 3 ".plus(throwable.cause))
                    addVenueMediaStateSubject.onNext(AddVenueMediaViewState.ErrorMessage(it))

                }
            }).autoDispose()
    }


    fun checkVenue(registerVenueRequest: RegisterVenueRequest) {
        createVenueRepository.checkVenue(registerVenueRequest)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.let {
                    addVenueMediaStateSubject.onNext(AddVenueMediaViewState.CheckVenue(it.success, it.message ?: ""))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    Timber.tag("<><>").e("Update Venue Error 1 ".plus(it))
                    Timber.tag("<><>").e("Update Venue Error 2 ".plus(throwable.message))
                    Timber.tag("<><>").e("Update Venue Error 3 ".plus(throwable.cause))
                    addVenueMediaStateSubject.onNext(AddVenueMediaViewState.ErrorMessage(it))

                }
            }).autoDispose()
    }

    sealed class AddVenueMediaViewState {
        data class LoadingState(val isLoading: Boolean) : AddVenueMediaViewState()
        data class SuccessMessage(val successMessage: String) : AddVenueMediaViewState()
        data class ErrorMessage(val errorMessage: String) : AddVenueMediaViewState()
        data class UpdateVenueSuccess(val createVenueResponse: CreateVenueResponse, val successMessage: String) : AddVenueMediaViewState()
        data class CheckVenue(val createVenueResponse: Boolean, val successMessage: String) : AddVenueMediaViewState()

        data class GetCloudFlareConfig(val cloudFlareConfig: CloudFlareConfig) : AddVenueMediaViewState()
        data class CloudFlareConfigErrorMessage(val errorMessage: String) : AddVenueMediaViewState()
        data class UploadMediaCloudFlareLoading(val isLoading: Boolean) : AddVenueMediaViewState()
        data class UploadMediaCloudFlareSuccess(val mediaUrl: String, val selectedMediaType: String, val thumbnail: String? = null) : AddVenueMediaViewState()
        data class UploadImageMediaCloudFlareSuccess(val mediaUrl: String, val selectedMediaType: String) : AddVenueMediaViewState()
        object AddVenueMediaSuccess : AddVenueMediaViewState()
    }
}