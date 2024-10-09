package com.outgoer.ui.venue.viewmodel

import android.content.Context
import com.outgoer.api.cloudflare.CloudFlareRepository
import com.outgoer.api.cloudflare.model.CloudFlareConfig
import com.outgoer.api.createvenue.CreateVenueRepository
import com.outgoer.api.createvenue.model.CreateVenueResponse
import com.outgoer.api.venue.model.AddVenueGalleryRequest
import com.outgoer.api.venue.model.GetVenueDetailRequest
import com.outgoer.api.venue.model.RegisterVenueRequest
import com.outgoer.api.venue.model.VenueGalleryItem
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.cloudFlareImageUploadBaseUrl
import com.outgoer.base.extension.getCommonPhotoFileName
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber
import java.io.File

class CreateVenueViewModel(
    private val cloudFlareRepository: CloudFlareRepository,
    private val createVenueRepository: CreateVenueRepository
) : BaseViewModel() {

    private val groupStateSubjects: PublishSubject<GroupViewState> = PublishSubject.create()
    val groupState: Observable<GroupViewState> = groupStateSubjects.hide()

    fun createGroup(registerVenueRequest: RegisterVenueRequest) {
        createVenueRepository.createVenue(registerVenueRequest)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.data?.let {
                    groupStateSubjects.onNext(GroupViewState.CreateVenueSuccess(it, response.message ?: ""))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    groupStateSubjects.onNext(GroupViewState.ErrorMessage(it))

                }
            }).autoDispose()
    }

    fun updateVenue(registerVenueRequest: RegisterVenueRequest) {
        createVenueRepository.updateVenue(registerVenueRequest)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.data?.let {
                    groupStateSubjects.onNext(GroupViewState.UpdateVenueSuccess(it, response.message ?: ""))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    groupStateSubjects.onNext(GroupViewState.ErrorMessage(it))

                }
            }).autoDispose()
    }

    fun addPhotosToVenue(addVenueGalleryRequest: AddVenueGalleryRequest) {
        createVenueRepository.updateVenuePhotos(addVenueGalleryRequest)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.message?.let {
                    groupStateSubjects.onNext(GroupViewState.SuccessMessage(response.message ?: "", if(addVenueGalleryRequest.gallery.size > 0) addVenueGalleryRequest.gallery.get(0) else addVenueGalleryRequest.uid.get(0)))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    groupStateSubjects.onNext(GroupViewState.ErrorMessage(it))

                }
            }).autoDispose()
    }

    fun getPhotosOfVenue(getVenueDetailRequest: GetVenueDetailRequest) {
        createVenueRepository.getVenueGallery(getVenueDetailRequest)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.data?.let {
                    groupStateSubjects.onNext(GroupViewState.ListOfVenueGallery(it))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    groupStateSubjects.onNext(GroupViewState.ErrorMessage(it))

                }
            }).autoDispose()
    }


    fun removeVenuePhotoFromGallery(venueId: Int) {
        createVenueRepository.removeVenuePhotoFromGallery(venueId)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.message?.let {
                    groupStateSubjects.onNext(GroupViewState.SuccessDMessage(it, venueId))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    groupStateSubjects.onNext(GroupViewState.ErrorMessage(it))

                }
            }).autoDispose()
    }

    fun getCloudFlareConfig() {
        cloudFlareRepository.getCloudFlareConfig().doOnSubscribe {
            groupStateSubjects.onNext(GroupViewState.LoadingState(true))
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            groupStateSubjects.onNext(GroupViewState.LoadingState(false))

            if (response.success) {
                val cloudFlareConfig = response.data

                if (cloudFlareConfig != null) {
                    groupStateSubjects.onNext(
                        GroupViewState.GetCloudFlareConfig(
                            cloudFlareConfig
                        )
                    )
                } else {
                    response.message?.let {
                        groupStateSubjects.onNext(GroupViewState.CloudFlareConfigErrorMessage(it))
                    }
                }
            }
        }, { throwable ->
            groupStateSubjects.onNext(GroupViewState.LoadingState(false))
            throwable.localizedMessage?.let {
                groupStateSubjects.onNext(GroupViewState.CloudFlareConfigErrorMessage(it))
            }
        }).autoDispose()
    }


    fun uploadImageToCloudFlare(
        context: Context, cloudFlareConfig: CloudFlareConfig, imageFile: File, userId: Int
    ) {
        val imageTempPathDir = context.getExternalFilesDir("OutgoerImages")?.path
        val fileName = getCommonPhotoFileName(userId)
        val imageCopyFile = File(imageTempPathDir + File.separator + fileName + ".jpg")
        val finalImageFile = imageFile.copyTo(imageCopyFile)

        val apiUrl = cloudFlareImageUploadBaseUrl.format(cloudFlareConfig.accountId)
        val authToken = "Bearer ".plus(cloudFlareConfig.apiToken)
        val filePart = MultipartBody.Part.createFormData(
            "file", finalImageFile.name, finalImageFile.asRequestBody("image/*".toMediaTypeOrNull())
        )
        cloudFlareRepository.uploadImageToCloudFlare(apiUrl, authToken, filePart).doOnSubscribe {
            groupStateSubjects.onNext(GroupViewState.UploadImageCloudFlareLoading(true))
        }.doAfterTerminate {
            groupStateSubjects.onNext(GroupViewState.UploadImageCloudFlareLoading(false))
        }.subscribeOnIoAndObserveOnMainThread({ response ->

            if (response.success) {
                val result = response.result
                if (result != null) {
                    val variants = result.variants
                    if (!variants.isNullOrEmpty()) {
                        groupStateSubjects.onNext(
                            GroupViewState.UploadImageCloudFlareSuccess(
                                variants.first()
                            )
                        )
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
            groupStateSubjects.onNext(GroupViewState.UploadImageCloudFlareLoading(false))
            throwable.localizedMessage?.let {
                groupStateSubjects.onNext(GroupViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    private fun handleCloudFlareMediaUploadError(errors: List<String>?) {
        if (!errors.isNullOrEmpty()) {
            val error = errors.firstOrNull()
            if (error != null) {
                groupStateSubjects.onNext(GroupViewState.ErrorMessage(error.toString()))
            }
        }
    }

    sealed class GroupViewState {
        data class ErrorMessage(val errorMessage: String) : GroupViewState()
        data class SuccessMessage(val successMessage: String, val mediaUrl: String) : GroupViewState()
        data class SuccessDMessage(val successMessage: String, val venueId: Int) : GroupViewState()
        data class CreateVenueSuccess(val createVenueResponse: CreateVenueResponse, val successMessage: String) : GroupViewState()
        data class UpdateVenueSuccess(val createVenueResponse: CreateVenueResponse, val successMessage: String) : GroupViewState()

        data class LoadingState(val isLoading: Boolean) : GroupViewState()
        data class ListOfVenueGallery(val listofgallery: ArrayList<VenueGalleryItem>) : GroupViewState()

        data class GetCloudFlareConfig(val cloudFlareConfig: CloudFlareConfig) : GroupViewState()
        data class CloudFlareConfigErrorMessage(val errorMessage: String) : GroupViewState()
        data class UploadImageCloudFlareLoading(val isLoading: Boolean) : GroupViewState()
        data class UploadImageCloudFlareSuccess(val imageUrl: String) : GroupViewState()
    }
}