package com.outgoer.ui.editprofile.viewmodel

import android.content.Context
import com.outgoer.api.authentication.AuthenticationRepository
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.authentication.model.ChekUsernameRequest
import com.outgoer.api.cloudflare.CloudFlareRepository
import com.outgoer.api.cloudflare.model.CloudFlareConfig
import com.outgoer.api.follow.FollowUserRepository
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.follow.model.GetFollowersAndFollowingRequest
import com.outgoer.api.profile.ProfileRepository
import com.outgoer.api.profile.model.UpdateProfileRequest
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

class EditProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val cloudFlareRepository: CloudFlareRepository,
    private val loginUserCache: LoggedInUserCache,
    private val authenticationRepository: AuthenticationRepository,
    private val followUserRepository: FollowUserRepository,
) : BaseViewModel() {

    private val editProfileViewStatesSubject: PublishSubject<EditProfileViewState> = PublishSubject.create()
    val editProfileViewStates: Observable<EditProfileViewState> = editProfileViewStatesSubject.hide()
    private var selectedTagUserInfo: MutableList<FollowUser> = mutableListOf()

    fun uploadProfile(request: UpdateProfileRequest) {
        profileRepository.uploadProfile(request)
            .doOnSubscribe {
                editProfileViewStatesSubject.onNext(EditProfileViewState.LoadingState(true))
            }
            .doAfterTerminate {
                editProfileViewStatesSubject.onNext(EditProfileViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.success) {
                    response.message?.let {
                        editProfileViewStatesSubject.onNext(EditProfileViewState.SuccessMessage(it))
                    }
                } else {
                    response.message?.let {
                        editProfileViewStatesSubject.onNext(EditProfileViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    editProfileViewStatesSubject.onNext(EditProfileViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getCloudFlareConfig() {
        cloudFlareRepository.getCloudFlareConfig()
            .doOnSubscribe {
                editProfileViewStatesSubject.onNext(EditProfileViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                editProfileViewStatesSubject.onNext(EditProfileViewState.LoadingState(false))
                if (response.success) {
                    val cloudFlareConfig = response.data
                    if (cloudFlareConfig != null) {
                        editProfileViewStatesSubject.onNext(EditProfileViewState.GetCloudFlareConfig(cloudFlareConfig))
                    } else {
                        response.message?.let {
                            editProfileViewStatesSubject.onNext(EditProfileViewState.CloudFlareConfigErrorMessage(it))
                        }
                    }
                }
            }, { throwable ->
                editProfileViewStatesSubject.onNext(EditProfileViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    editProfileViewStatesSubject.onNext(EditProfileViewState.CloudFlareConfigErrorMessage(it))
                }
            }).autoDispose()
    }

    fun uploadImageToCloudFlare(context: Context, cloudFlareConfig: CloudFlareConfig, imageFile: File) {
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
                editProfileViewStatesSubject.onNext(EditProfileViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.success) {
                    val result = response.result
                    if (result != null) {
                        val variants = result.variants
                        if (!variants.isNullOrEmpty()) {
                            editProfileViewStatesSubject.onNext(EditProfileViewState.UploadImageCloudFlareSuccess(variants.first()))
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
                editProfileViewStatesSubject.onNext(EditProfileViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    editProfileViewStatesSubject.onNext(EditProfileViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    private fun handleCloudFlareMediaUploadError(errors: List<String>?) {
        if (!errors.isNullOrEmpty()) {
            val error = errors.firstOrNull()
            if (error != null) {
                editProfileViewStatesSubject.onNext(EditProfileViewState.ErrorMessage(error.toString()))
            }
        }
    }

    fun checkUsername(username: String) {
        authenticationRepository.checkUsername(ChekUsernameRequest(username))
            .doOnSubscribe {
                editProfileViewStatesSubject.onNext(EditProfileViewState.CheckUsernameLoading(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                editProfileViewStatesSubject.onNext(EditProfileViewState.CheckUsernameLoading(false))
                response.data?.usernameExist?.let {
                    editProfileViewStatesSubject.onNext(EditProfileViewState.CheckUsernameExist(it))
                }
            }, { throwable ->
                editProfileViewStatesSubject.onNext(EditProfileViewState.CheckUsernameLoading(false))
                throwable.localizedMessage?.let {
                    editProfileViewStatesSubject.onNext(EditProfileViewState.ErrorMessage(it))
                }
                Timber.d(throwable)
            }).autoDispose()
    }

    fun getFollowersList(userId: Int, searchText: String) {
        followUserRepository.getAllFollowersList(1, GetFollowersAndFollowingRequest(userId, searchText))
            .doOnSubscribe {
                editProfileViewStatesSubject.onNext(EditProfileViewState.LoadingState(true))
            }
            .doAfterTerminate {
                editProfileViewStatesSubject.onNext(EditProfileViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    editProfileViewStatesSubject.onNext(EditProfileViewState.FollowerList(it.toMutableList()))
                }
            }, { throwable ->
                editProfileViewStatesSubject.onNext(EditProfileViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    editProfileViewStatesSubject.onNext(EditProfileViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }


    fun searchTagUserClicked(
        initialDescriptionString: String,
        subString: String,
        followUser: FollowUser
    ) {
        if (followUser !in selectedTagUserInfo) {
            selectedTagUserInfo.add(followUser)
        }
        val remainString = initialDescriptionString.removePrefix(subString)
        if (subString.length == initialDescriptionString.length) {
            val lastIndexOfToken = initialDescriptionString.findLastAnyOf(listOf("@"))?.first ?: return
            Timber.i("lastIndexOfToken %s", lastIndexOfToken)
            val tempSubString = initialDescriptionString.substring(0, lastIndexOfToken)
            val descriptionString = "$tempSubString@${followUser.username}"
            editProfileViewStatesSubject.onNext(EditProfileViewState.UpdateDescriptionText(descriptionString.plus(" ")))
        } else {
            val lastIndexOfToken = subString.findLastAnyOf(listOf("@"))?.first ?: return
            Timber.i("lastIndexOfToken %s", lastIndexOfToken)
            val tempSubString = subString.substring(0, lastIndexOfToken)
            val descriptionString = "$tempSubString @${followUser.username} $remainString"
            editProfileViewStatesSubject.onNext(EditProfileViewState.UpdateDescriptionText(descriptionString.plus(" ")))
        }
    }
    fun getInitialFollowersList(userId: Int) {
        followUserRepository.getAllFollowersList(1, GetFollowersAndFollowingRequest(userId, ""))
            .doOnSubscribe {
                editProfileViewStatesSubject.onNext(EditProfileViewState.LoadingState(true))
            }
            .doAfterTerminate {
                editProfileViewStatesSubject.onNext(EditProfileViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    editProfileViewStatesSubject.onNext(EditProfileViewState.InitialFollowerList(it.toMutableList()))
                }
            }, { throwable ->
                editProfileViewStatesSubject.onNext(EditProfileViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    editProfileViewStatesSubject.onNext(EditProfileViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }
    sealed class EditProfileViewState {
        data class ErrorMessage(val errorMessage: String) : EditProfileViewState()
        data class SuccessMessage(val successMessage: String) : EditProfileViewState()
        data class LoadingState(val isLoading: Boolean) : EditProfileViewState()

        data class CloudFlareConfigErrorMessage(val errorMessage: String) : EditProfileViewState()
        data class GetCloudFlareConfig(val cloudFlareConfig: CloudFlareConfig) : EditProfileViewState()

        data class UploadImageCloudFlareSuccess(val imageUrl: String) : EditProfileViewState()

        data class CheckUsernameLoading(val isLoading: Boolean) : EditProfileViewState()
        data class CheckUsernameExist(val isUsernameExist: Int) : EditProfileViewState()
        data class InitialFollowerList(val listOfFollowers: List<FollowUser>) : EditProfileViewState()
        data class UpdateDescriptionText(val descriptionString: String) : EditProfileViewState()
        data class FollowerList(val listOfFollowers: List<FollowUser>) : EditProfileViewState()

    }
}