package com.outgoer.ui.home.newReels.viewmodel

import android.content.Context
import android.os.CountDownTimer
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.cloudflare.CloudFlareRepository
import com.outgoer.api.cloudflare.model.CloudFlareConfig
import com.outgoer.api.reels.ReelsRepository
import com.outgoer.api.reels.model.CreateReelRequest
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.cloudFlareVideoUploadBaseUrl
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
import androidx.lifecycle.Observer


class AddNewReelViewModel(
    private val cloudFlareRepository: CloudFlareRepository,
    private val reelsRepository: ReelsRepository,
    private val loginUserCache: LoggedInUserCache
) : BaseViewModel() {

    private val addNewReelStateSubject: PublishSubject<AddNewReelViewState> = PublishSubject.create()
    val addNewReelState: Observable<AddNewReelViewState> = addNewReelStateSubject.hide()

    fun getCloudFlareConfig() {
        cloudFlareRepository.getCloudFlareConfig()
            .doOnSubscribe {
                addNewReelStateSubject.onNext(AddNewReelViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                addNewReelStateSubject.onNext(AddNewReelViewState.LoadingState(false))
                if (response.success) {
                    val cloudFlareConfig = response.data
                    if (cloudFlareConfig != null) {
                        addNewReelStateSubject.onNext(AddNewReelViewState.GetCloudFlareConfig(cloudFlareConfig))
                    } else {
                        response.message?.let {
                            addNewReelStateSubject.onNext(AddNewReelViewState.CloudFlareConfigErrorMessage(it))
                        }
                    }
                }
            }, { throwable ->
                addNewReelStateSubject.onNext(AddNewReelViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    addNewReelStateSubject.onNext(AddNewReelViewState.CloudFlareConfigErrorMessage(it))
                }
            }).autoDispose()
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
                addNewReelStateSubject.onNext(
                    AddNewReelViewState.UploadVideoCloudFlareSuccess(
                        it.uid.toString(),
                        it.thumbnail.toString()
                    )
                )
            }, { throwable ->
                Timber.e(throwable)
                addNewReelStateSubject.onNext(AddNewReelViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    addNewReelStateSubject.onNext(AddNewReelViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    private fun handleCloudFlareMediaUploadError(errors: List<String>?) {
        if (!errors.isNullOrEmpty()) {
            val error = errors.firstOrNull()
            if (error != null) {
                addNewReelStateSubject.onNext(AddNewReelViewState.ErrorMessage(error.toString()))
            }
        }
    }

    fun createReel(request: CreateReelRequest) {
        reelsRepository.createReel(request)
            .doOnSubscribe {
                addNewReelStateSubject.onNext(AddNewReelViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.success) {
                    object : CountDownTimer(5000, 1000) {
                        override fun onTick(millisUntilFinished: Long) {
                        }

                        override fun onFinish() {
                            addNewReelStateSubject.onNext(AddNewReelViewState.LoadingState(false))
                            addNewReelStateSubject.onNext(AddNewReelViewState.CreateReelSuccessMessage)
                        }
                    }.start()


                }
            }, { throwable ->
                addNewReelStateSubject.onNext(AddNewReelViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    addNewReelStateSubject.onNext(AddNewReelViewState.CloudFlareConfigErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getLiveDataInfo() {

        cloudFlareRepository.getLiveData()?.observe(viewModelLifecycleOwner, Observer {
            addNewReelStateSubject.onNext(AddNewReelViewState.ProgressDisplay(it))
        } )
    }

    fun stopClient() {
        cloudFlareRepository.stopClient()
    }

    sealed class AddNewReelViewState {
        data class ProgressDisplay(val progressInfo: Double) : AddNewReelViewState()

        data class ErrorMessage(val errorMessage: String) : AddNewReelViewState()
        data class SuccessMessage(val successMessage: String) : AddNewReelViewState()
        data class LoadingState(val isLoading: Boolean) : AddNewReelViewState()

        data class CloudFlareConfigErrorMessage(val errorMessage: String) : AddNewReelViewState()
        data class GetCloudFlareConfig(val cloudFlareConfig: CloudFlareConfig) : AddNewReelViewState()

        data class UploadVideoCloudFlareSuccess(val videoId: String, val thumbnail: String) : AddNewReelViewState()
        object CreateReelSuccessMessage : AddNewReelViewState()
    }
}