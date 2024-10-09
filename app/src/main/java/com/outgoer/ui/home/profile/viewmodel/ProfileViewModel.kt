package com.outgoer.ui.home.profile.viewmodel

import android.content.Context
import com.outgoer.api.authentication.AuthenticationRepository
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.authentication.model.OutgoerUser
import com.outgoer.api.cloudflare.CloudFlareRepository
import com.outgoer.api.cloudflare.model.CloudFlareConfig
import com.outgoer.api.createvenue.CreateVenueRepository
import com.outgoer.api.createvenue.model.CreateVenueResponse
import com.outgoer.api.post.model.PeopleForTag
import com.outgoer.api.profile.ProfileRepository
import com.outgoer.api.profile.model.BlockUserListRequest
import com.outgoer.api.profile.model.BlockUserRequest
import com.outgoer.api.profile.model.BlockUserResponse
import com.outgoer.api.profile.model.DeviceAccountRequest
import com.outgoer.api.profile.model.SetVisibilityRequest
import com.outgoer.api.profile.model.SwitchDeviceAccountRequest
import com.outgoer.api.profile.model.UpdateProfileRequest
import com.outgoer.api.reels.ReelsRepository
import com.outgoer.api.reels.model.ReelInfo
import com.outgoer.api.venue.VenueRepository
import com.outgoer.api.venue.model.CheckInOutRequest
import com.outgoer.api.venue.model.GetVenueDetailRequest
import com.outgoer.api.venue.model.RegisterVenueRequest
import com.outgoer.api.venue.model.VenueDetail
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.cloudFlareImageUploadBaseUrl
import com.outgoer.base.extension.getCommonPhotoFileName
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import com.outgoer.base.network.model.NewOutgoerCommonResponse
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber
import java.io.File

class ProfileViewModel(
    private val cloudFlareRepository: CloudFlareRepository,
    private val profileRepository: ProfileRepository,
    private val authenticationRepository: AuthenticationRepository,
    private val reelsRepository: ReelsRepository,
    private val loginUserCache: LoggedInUserCache,
    private val createVenueRepository: CreateVenueRepository,
    private val venueRepository: VenueRepository
) : BaseViewModel() {

    private val profileViewStatesSubject: PublishSubject<ProfileViewState> = PublishSubject.create()
    val profileViewStates: Observable<ProfileViewState> = profileViewStatesSubject.hide()

    fun getVenueDetail(venueId: Int) {
        createVenueRepository.getVenueDetail(GetVenueDetailRequest(venueId = venueId))
            .doOnSubscribe {
                profileViewStatesSubject.onNext(ProfileViewState.LoadingState(true))
            }
            .doAfterTerminate {
                profileViewStatesSubject.onNext(ProfileViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    profileViewStatesSubject.onNext(ProfileViewState.LoadVenueDetail(it))
                }
            }, { throwable ->
                profileViewStatesSubject.onNext(ProfileViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    profileViewStatesSubject.onNext(ProfileViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun setVisibility(setVisibilityRequest: SetVisibilityRequest) {
        profileRepository.setVisibility(setVisibilityRequest)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.data?.let {
                    profileViewStatesSubject.onNext(ProfileViewState.MyProfileData(it))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    Timber.tag("<><>").e("CreateGroupRequest".plus(it))
                    profileViewStatesSubject.onNext(ProfileViewState.ErrorMessage(it))

                }
            }).autoDispose()
    }

    fun updateVenue(registerVenueRequest: RegisterVenueRequest) {
        createVenueRepository.updateVenue(registerVenueRequest)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.data?.let {
                    profileViewStatesSubject.onNext(ProfileViewState.UpdateVenueSuccess(it, response.message ?: ""))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    Timber.tag("<><>").e("CreateGroupRequest".plus(it))
                    profileViewStatesSubject.onNext(ProfileViewState.ErrorMessage(it))

                }
            }).autoDispose()
    }


    fun myProfile() {
        profileRepository.myProfile()
            .doOnSubscribe {
                profileViewStatesSubject.onNext(ProfileViewState.LoadingState(true))
            }
            .doAfterTerminate {
                profileViewStatesSubject.onNext(ProfileViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.success) {
                    response.data?.let {
                        profileViewStatesSubject.onNext(ProfileViewState.MyProfileData(it))
                    }
                } else {
                    response.message?.let {
                        profileViewStatesSubject.onNext(ProfileViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    profileViewStatesSubject.onNext(ProfileViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun logout(deviceId: String) {
        authenticationRepository.logout(deviceId)
            .doOnSubscribe {
                profileViewStatesSubject.onNext(ProfileViewState.LoadingState(true))
            }
            .doAfterTerminate {
                profileViewStatesSubject.onNext(ProfileViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ loginResponse ->

                if(loginResponse.accessToken.isNullOrEmpty()) {
                    profileViewStatesSubject.onNext(ProfileViewState.LogoutSuccess)
                } else {
                    loginResponse.data?.let {
                        profileViewStatesSubject.onNext(ProfileViewState.SwitchProfileData(it))
                    }
                }

            }, { _ ->
                profileViewStatesSubject.onNext(ProfileViewState.LogoutSuccess)
            }).autoDispose()
    }


    fun logoutAll(deviceId: String) {
        authenticationRepository.logoutAllUsers(deviceId)
            .doOnSubscribe {
                profileViewStatesSubject.onNext(ProfileViewState.LoadingState(true))
            }
            .doAfterTerminate {
                profileViewStatesSubject.onNext(ProfileViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ loginResponse ->
                loginResponse?.let {
                    profileViewStatesSubject.onNext(ProfileViewState.ConversationSuccessMessage(it))
                }
                profileViewStatesSubject.onNext(ProfileViewState.LogoutSuccess)
            }, { _ ->
                profileViewStatesSubject.onNext(ProfileViewState.LogoutSuccess)
            }).autoDispose()
    }

    private var pageNumber: Int = 1
    private var isLoadMore: Boolean = true
    private var isLoading: Boolean = false
    private var listOfReelsInfo: MutableList<ReelInfo> = mutableListOf()

    fun getMyReel(loggedInUserId: Int) {
        reelsRepository.getMyReel(pageNumber, loggedInUserId)
            .doOnSubscribe {
                isLoading = true
            }
            .doAfterTerminate {
                isLoading = false
            }
            .subscribeOnIoAndObserveOnMainThread({ reelsResponse ->
                reelsResponse?.data?.let {
                    if (pageNumber == 1) {
                        listOfReelsInfo = it.toMutableList()
                        profileViewStatesSubject.onNext(ProfileViewState.GetMyReelInfo(listOfReelsInfo))
                    } else {
                        if (!it.isNullOrEmpty()) {
                            listOfReelsInfo.addAll(it)
                            profileViewStatesSubject.onNext(ProfileViewState.GetMyReelInfo(listOfReelsInfo))
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

    fun loadMoreMyReel(loggedInUserId: Int) {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNumber++
                getMyReel(loggedInUserId)
            }
        }
    }

    fun pullToRefresh(loggedInUserId: Int) {
        pageNumber = 1
        isLoadMore = true
        isLoading = false
        listOfReelsInfo.clear()
        getMyReel(loggedInUserId)
    }

    fun deactivate() {
            authenticationRepository.deactivate()
                .doOnSubscribe {
                    profileViewStatesSubject.onNext(ProfileViewState.LoadingState(true))
                }
                .doAfterTerminate {
                    profileViewStatesSubject.onNext(ProfileViewState.LoadingState(false))
                }
                .subscribeOnIoAndObserveOnMainThread({ response ->
                    response.message?.let {
                        profileViewStatesSubject.onNext(ProfileViewState.SuccessMessage(it))
                    }
                    profileViewStatesSubject.onNext(ProfileViewState.DeactivateProfile)
                }, {  throwable ->
                    throwable.printStackTrace()
                    throwable.localizedMessage?.let {
                        profileViewStatesSubject.onNext(ProfileViewState.ErrorMessage(it))
                    }
                }).autoDispose()
        }


    fun getCloudFlareConfig() {
        cloudFlareRepository.getCloudFlareConfig()
            .doOnSubscribe {
                profileViewStatesSubject.onNext(ProfileViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                profileViewStatesSubject.onNext(ProfileViewState.LoadingState(false))
                if (response.success) {
                    val cloudFlareConfig = response.data
                    if (cloudFlareConfig != null) {
                        profileViewStatesSubject.onNext(ProfileViewState.GetCloudFlareConfig(cloudFlareConfig))
                    } else {
                        response.message?.let {
                            profileViewStatesSubject.onNext(ProfileViewState.CloudFlareConfigErrorMessage(it))
                        }
                    }
                }
            }, { throwable ->
                profileViewStatesSubject.onNext(ProfileViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    profileViewStatesSubject.onNext(ProfileViewState.CloudFlareConfigErrorMessage(it))
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
                profileViewStatesSubject.onNext(ProfileViewState.UploadMediaCloudFlareLoading(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.success) {
                    profileViewStatesSubject.onNext(ProfileViewState.UploadMediaCloudFlareLoading(false))
                    val result = response.result
                    if (result != null) {
                        val variants = result.variants
                        if (!variants.isNullOrEmpty()) {
                            profileViewStatesSubject.onNext(ProfileViewState.UploadMediaCloudFlareSuccess(variants.first()))
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
                profileViewStatesSubject.onNext(ProfileViewState.UploadMediaCloudFlareLoading(false))
                throwable.localizedMessage?.let {
                    profileViewStatesSubject.onNext(ProfileViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    private fun handleCloudFlareMediaUploadError(errors: List<String>?) {
        if (!errors.isNullOrEmpty()) {
            val error = errors.firstOrNull()
            if (error != null) {
                profileViewStatesSubject.onNext(ProfileViewState.ErrorMessage(error.toString()))
            }
        }
    }

    fun uploadProfile(request: UpdateProfileRequest) {
        profileRepository.uploadProfile(request)
            .doOnSubscribe {
                profileViewStatesSubject.onNext(ProfileViewState.UploadMediaCloudFlareLoading(true))
            }
            .doAfterTerminate {
                profileViewStatesSubject.onNext(ProfileViewState.UploadMediaCloudFlareLoading(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.success) {
                    response.message?.let {
                        profileViewStatesSubject.onNext(ProfileViewState.SuccessMessage(it))
                    }
                    response.data?.let {data ->
                        profileViewStatesSubject.onNext(ProfileViewState.MyProfileData(data))
                    }
                } else {
                    response.message?.let {
                        profileViewStatesSubject.onNext(ProfileViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    profileViewStatesSubject.onNext(ProfileViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }



    private var pageNumberB: Int = 1
    private var isLoadMoreB: Boolean = true
    private var isLoadingB: Boolean = false
    private var listOfBlockAccounts: MutableList<BlockUserResponse> = mutableListOf()

    fun getBlockAccount() {
        profileRepository.blockAccounts(BlockUserListRequest(pageNumberB))
            .doOnSubscribe {
                isLoadingB = true
                profileViewStatesSubject.onNext(ProfileViewState.LoadingState(true))
            }
            .doAfterTerminate {
                isLoadingB = false
                profileViewStatesSubject.onNext(ProfileViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ blockResponse ->
                blockResponse?.data?.let {
                    if (pageNumberB == 1) {
                        listOfBlockAccounts = it.toMutableList()
                        profileViewStatesSubject.onNext(ProfileViewState.GetListOfBlockedUsers(listOfBlockAccounts))
                    } else {
                        if (!it.isNullOrEmpty()) {
                            listOfBlockAccounts.addAll(it)
                            profileViewStatesSubject.onNext(ProfileViewState.GetListOfBlockedUsers(listOfBlockAccounts))
                        } else {
                            isLoadMoreB = false
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

    fun loadMoreBlockAccount() {
        if (!isLoadingB) {
            isLoadingB = true
            if (isLoadMoreB) {
                pageNumberB++
                getBlockAccount()
            }
        }
    }

    fun pullToRefreshBlockAccount() {
        pageNumberB = 1
        isLoadMoreB = true
        isLoadingB = false
        listOfBlockAccounts.clear()
        getBlockAccount()
    }


    fun blockUserProfile(blockUserRequest: BlockUserRequest) {
        profileRepository.blockUserProfile(blockUserRequest)
            .doOnSubscribe {
                profileViewStatesSubject.onNext(ProfileViewState.LoadingState(true))
            }
            .doAfterTerminate {
                profileViewStatesSubject.onNext(ProfileViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.success) {
                    response.message?.let {
                        profileViewStatesSubject.onNext(
                            ProfileViewState.SuccessMessage(
                                it
                            )
                        )
                    }
                } else {
                    response.message?.let {
                        profileViewStatesSubject.onNext(ProfileViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    profileViewStatesSubject.onNext(ProfileViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }


    fun getDeviceAccount(request: DeviceAccountRequest) {
        profileRepository.getDeviceAccount(request)
            .doOnSubscribe {
                profileViewStatesSubject.onNext(ProfileViewState.LoadingState(true))
            }
            .doAfterTerminate {
                profileViewStatesSubject.onNext(ProfileViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.success) {
                    response.data?.let {
                        profileViewStatesSubject.onNext(ProfileViewState.GetSavedAccount(it))
                    }
                } else {
                    response.message?.let {
                        profileViewStatesSubject.onNext(ProfileViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    profileViewStatesSubject.onNext(ProfileViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }


    fun switchAccount(request: SwitchDeviceAccountRequest) {
        profileRepository.switchAccount(request)
            .doOnSubscribe {
                profileViewStatesSubject.onNext(ProfileViewState.LoadingState(true))
            }
            .doAfterTerminate {
                profileViewStatesSubject.onNext(ProfileViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.success) {
                    response.data?.let {
                        profileViewStatesSubject.onNext(ProfileViewState.SwitchedUserProfileData(it))
                    }
                } else {
                    response.message?.let {
                        profileViewStatesSubject.onNext(ProfileViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    profileViewStatesSubject.onNext(ProfileViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun checkInOutVenue(checkInOutRequest: CheckInOutRequest) {
        venueRepository.checkInOutVenue(checkInOutRequest)
            .doOnSubscribe {}
            .subscribeOnIoAndObserveOnMainThread({
                it.message?.let { message ->
                    profileViewStatesSubject.onNext(ProfileViewState.SuccessMessage(message))
                    profileViewStatesSubject.onNext(ProfileViewState.SuccessCheckOut)
                }
            }, { throwable ->
                Timber.e(throwable)
            }).autoDispose()
    }

    sealed class ProfileViewState {
        data class LoadVenueDetail(val venueDetail: VenueDetail) : ProfileViewState()

        data class ErrorMessage(val errorMessage: String) : ProfileViewState()
        data class SuccessMessage(val successMessage: String) : ProfileViewState()
        data class ConversationSuccessMessage(val successMessage: NewOutgoerCommonResponse) : ProfileViewState()
        data class LoadingState(val isLoading: Boolean) : ProfileViewState()

        data class GetListOfBlockedUsers(val listOfBlockedUsers: MutableList<BlockUserResponse>) : ProfileViewState()
        data class GetMyReelInfo(val listOfReelsInfo: List<ReelInfo>) : ProfileViewState()
        data class MyProfileData(val outgoerUser: OutgoerUser) : ProfileViewState()
        data class SwitchProfileData(val outgoerUser: OutgoerUser) : ProfileViewState()

        object LogoutSuccess : ProfileViewState()
        object DeactivateProfile : ProfileViewState()


        data class GetCloudFlareConfig(val cloudFlareConfig: CloudFlareConfig) : ProfileViewState()
        data class CloudFlareConfigErrorMessage(val errorMessage: String) : ProfileViewState()
        data class UploadMediaCloudFlareLoading(val isLoading: Boolean) : ProfileViewState()
        data class UploadMediaCloudFlareSuccess(val mediaUrl: String) : ProfileViewState()

        data class UpdateVenueSuccess(val createVenueResponse: CreateVenueResponse, val successMessage: String) : ProfileViewState()
        data class GetSavedAccount(val savedAccountList: ArrayList<PeopleForTag>) : ProfileViewState()
        data class SwitchedUserProfileData(val switchedAccount: OutgoerUser) : ProfileViewState()

        object SuccessCheckOut: ProfileViewState()
    }
}