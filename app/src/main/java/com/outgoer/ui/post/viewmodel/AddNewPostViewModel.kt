package com.outgoer.ui.post.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.cloudflare.CloudFlareRepository
import com.outgoer.api.cloudflare.model.CloudFlareConfig

import com.outgoer.api.follow.FollowUserRepository
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.follow.model.GetFollowersAndFollowingRequest
import com.outgoer.api.post.PostRepository
import com.outgoer.api.post.model.CreatePostRequest
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.*

import com.outgoer.ui.sponty.viewmodel.SpontyViewModel
import com.skydoves.viewmodel.lifecycle.viewModelLifecycleOwner
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.net.URL

class AddNewPostViewModel(
    private val cloudFlareRepository: CloudFlareRepository,
    private val postRepository: PostRepository,
    private val followUserRepository: FollowUserRepository,
    private val loginUserCache: LoggedInUserCache
) : BaseViewModel() {

    private val addNewPostStateSubjects: PublishSubject<AddNewPostViewState> = PublishSubject.create()
    val addNewPostState: Observable<AddNewPostViewState> = addNewPostStateSubjects.hide()

    private var selectedTagUserInfo: MutableList<FollowUser> = mutableListOf()

    fun getCloudFlareConfig() {
        cloudFlareRepository.getCloudFlareConfig()
            .doOnSubscribe {
                addNewPostStateSubjects.onNext(AddNewPostViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                addNewPostStateSubjects.onNext(AddNewPostViewState.LoadingState(false))
                if (response.success) {
                    val cloudFlareConfig = response.data
                    if (cloudFlareConfig != null) {
                        addNewPostStateSubjects.onNext(AddNewPostViewState.GetCloudFlareConfig(cloudFlareConfig))
                    } else {
                        response.message?.let {
                            addNewPostStateSubjects.onNext(AddNewPostViewState.CloudFlareConfigErrorMessage(it))
                        }
                    }
                }
            }, { throwable ->
                addNewPostStateSubjects.onNext(AddNewPostViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    addNewPostStateSubjects.onNext(AddNewPostViewState.CloudFlareConfigErrorMessage(it))
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
                addNewPostStateSubjects.onNext(AddNewPostViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.success) {
                    val result = response.result
                    if (result != null) {
                        val variants = result.variants
                        if (!variants.isNullOrEmpty()) {
                            addNewPostStateSubjects.onNext(AddNewPostViewState.UploadImageCloudFlareSuccess(variants.first()))
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
                addNewPostStateSubjects.onNext(AddNewPostViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    addNewPostStateSubjects.onNext(AddNewPostViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun stopClient() {
        cloudFlareRepository.stopClient()
    }

    fun uploadVideoToCloudFlare(context: Context, cloudFlareConfig: CloudFlareConfig, videoFile: File) {
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
                addNewPostStateSubjects.onNext(
                    AddNewPostViewState.UploadVideoCloudFlareSuccess(
                        it.uid.toString(),
                        it.thumbnail.toString()
                    )
                )
            }, { throwable ->
                Timber.e(throwable)
                addNewPostStateSubjects.onNext(AddNewPostViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    addNewPostStateSubjects.onNext(AddNewPostViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    private fun handleCloudFlareMediaUploadError(errors: List<String>?) {
        if (!errors.isNullOrEmpty()) {
            val error = errors.firstOrNull()
            if (error != null) {
                addNewPostStateSubjects.onNext(AddNewPostViewState.ErrorMessage(error.toString()))
            }
        }
    }

    fun descriptionTagsInfo(): String {
        val mentionId = arrayListOf<Int>()
        selectedTagUserInfo.forEach {
            mentionId.add(it.id)
        }

        return mentionId.joinToString(separator = ",")
    }

    fun createPost(request: CreatePostRequest) {
        request.descriptionTag = descriptionTagsInfo()

        postRepository.createPost(request)
            .doOnSubscribe {
                addNewPostStateSubjects.onNext(AddNewPostViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                addNewPostStateSubjects.onNext(AddNewPostViewState.LoadingState(false))
                if (response.success) {
                    addNewPostStateSubjects.onNext(AddNewPostViewState.CreatePostSuccessMessage)
                }
            }, { throwable ->
                addNewPostStateSubjects.onNext(AddNewPostViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    addNewPostStateSubjects.onNext(AddNewPostViewState.CloudFlareConfigErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getLiveDataInfo() {
        cloudFlareRepository.getLiveData()?.observe(viewModelLifecycleOwner, Observer {
            addNewPostStateSubjects.onNext(AddNewPostViewState.ProgressDisplay(it))
        } )
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
            addNewPostStateSubjects.onNext(AddNewPostViewState.UpdateDescriptionText(descriptionString.plus(" ")))
        } else {
            val lastIndexOfToken = subString.findLastAnyOf(listOf("@"))?.first ?: return
            Timber.i("lastIndexOfToken %s", lastIndexOfToken)
            val tempSubString = subString.substring(0, lastIndexOfToken)
            val descriptionString = "$tempSubString @${followUser.username} $remainString"
            addNewPostStateSubjects.onNext(AddNewPostViewState.UpdateDescriptionText(descriptionString.plus(" ")))
        }
    }

    fun getFollowersList(userId: Int, searchText: String) {
        followUserRepository.getAllFollowersList(1, GetFollowersAndFollowingRequest(userId, searchText))
            .doOnSubscribe {
                addNewPostStateSubjects.onNext(AddNewPostViewState.LoadingState(true))
            }
            .doAfterTerminate {
                addNewPostStateSubjects.onNext(AddNewPostViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    addNewPostStateSubjects.onNext(AddNewPostViewState.FollowerList(it.toMutableList()))
                }
            }, { throwable ->
                addNewPostStateSubjects.onNext(AddNewPostViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    addNewPostStateSubjects.onNext(AddNewPostViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getInitialFollowersList(userId: Int) {
        followUserRepository.getAllFollowersList(1, GetFollowersAndFollowingRequest(userId, ""))
            .doOnSubscribe {
                addNewPostStateSubjects.onNext(AddNewPostViewState.LoadingState(true))
            }
            .doAfterTerminate {
                addNewPostStateSubjects.onNext(AddNewPostViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    addNewPostStateSubjects.onNext(AddNewPostViewState.InitialFollowerList(it.toMutableList()))
                }
            }, { throwable ->
                addNewPostStateSubjects.onNext(AddNewPostViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    addNewPostStateSubjects.onNext(AddNewPostViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    sealed class AddNewPostViewState {
        data class ErrorMessage(val errorMessage: String) : AddNewPostViewState()
        data class SuccessMessage(val successMessage: String) : AddNewPostViewState()
        data class LoadingState(val isLoading: Boolean) : AddNewPostViewState()

        data class CloudFlareConfigErrorMessage(val errorMessage: String) : AddNewPostViewState()
        data class GetCloudFlareConfig(val cloudFlareConfig: CloudFlareConfig) : AddNewPostViewState()

        data class UploadImageCloudFlareSuccess(val imageUrl: String) : AddNewPostViewState()
        data class UploadVideoCloudFlareSuccess(val videoId: String, val thumbnail: String) :
            AddNewPostViewState()

        object CreatePostSuccessMessage : AddNewPostViewState()

        data class ProgressDisplay(val progressInfo: Double) : AddNewPostViewState()
        data class UpdateDescriptionText(val descriptionString: String) : AddNewPostViewState()
        data class InitialFollowerList(val listOfFollowers: List<FollowUser>) : AddNewPostViewState()
        data class FollowerList(val listOfFollowers: List<FollowUser>) : AddNewPostViewState()

    }
}