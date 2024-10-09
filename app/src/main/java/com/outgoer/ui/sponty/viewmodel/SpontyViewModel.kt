package com.outgoer.ui.sponty.viewmodel

import android.content.Context
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.cloudflare.CloudFlareRepository
import com.outgoer.api.cloudflare.model.CloudFlareConfig
import com.outgoer.api.follow.FollowUserRepository
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.follow.model.GetFollowersAndFollowingRequest
import com.outgoer.api.post.model.CommentInfo
import com.outgoer.api.post.model.UpdateCommentRequest
import com.outgoer.api.sponty.SpontyRepository
import com.outgoer.api.sponty.model.*
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.*
import com.outgoer.ui.comment.viewmodel.CommentViewState
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber
import java.io.File

class SpontyViewModel(
    private val spontyRepository: SpontyRepository, private val followUserRepository: FollowUserRepository,
    private val cloudFlareRepository: CloudFlareRepository,
    private val loginUserCache: LoggedInUserCache
) : BaseViewModel() {

    private val spontyDataStateSubject: PublishSubject<SpontyDataState> = PublishSubject.create()
    val spontyDataState: Observable<SpontyDataState> = spontyDataStateSubject.hide()

    private var pageNumber = 1
    private var isLoadMore: Boolean = true
    private var isLoading: Boolean = false
    private var listofsponty: ArrayList<SpontyResponse> = arrayListOf()
    private var selectedTagUserInfo: MutableList<FollowUser> = mutableListOf()
    private var listOfCommentInfo: MutableList<SpontyCommentResponse> = mutableListOf()

    fun resetPagination(isReload: Boolean) {
        pageNumber = 1
        isLoading = false
        isLoadMore = true
        listofsponty.clear()
        getAllSponty(isReload)
    }

    fun loadMore() {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNumber++
                getAllSponty(false)
            }
        }
    }

    fun getAllSponty(isReload: Boolean) {
        spontyRepository.getAllSponty(pageNumber)
            .doOnSubscribe {
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(isReload))
            }.doAfterTerminate {
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(false))
                isLoading = false
            }.subscribeOnIoAndObserveOnMainThread({ response ->
                response.data?.let {
                    listofsponty.addAll(it)
                    spontyDataStateSubject.onNext(SpontyDataState.ListofSponty(listofsponty))
                }

                if (response?.data?.isNullOrEmpty() == true) {
                    isLoadMore = false
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    spontyDataStateSubject.onNext(SpontyDataState.ErrorMessage(it))
                }
            }).autoDispose()
    }


    fun getAllNearBySponty() {
        spontyRepository.getNearbyAllSponty()
            .doOnSubscribe {
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(true))
            }.doAfterTerminate {
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(false))
            }.subscribeOnIoAndObserveOnMainThread({ response ->
                response.data?.let {
                    listofsponty.addAll(it)
                    spontyDataStateSubject.onNext(SpontyDataState.NearByListofSponty(listofsponty))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    spontyDataStateSubject.onNext(SpontyDataState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun createSponty(createSpontyRequest: CreateSpontyRequest) {
        val mentionId = arrayListOf<Int>()
        selectedTagUserInfo.forEach {
            mentionId.add(it.id)
        }

        createSpontyRequest.descriptionTag = mentionId.joinToString(separator = ",")
        spontyRepository.createSponty(createSpontyRequest)
            .doOnSubscribe {
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(true))
            }.doAfterTerminate {
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(false))
            }.subscribeOnIoAndObserveOnMainThread({ response ->
                response.data?.let {
                    spontyDataStateSubject.onNext(SpontyDataState.SpontyInfo(it))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    spontyDataStateSubject.onNext(SpontyDataState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun addRemoveSponty(allJoinSpontyRequest: AllJoinSpontyRequest) {
        spontyRepository.addRemoveSponty(allJoinSpontyRequest)
            .doOnSubscribe {
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(true))
            }.doAfterTerminate {
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(false))
            }.subscribeOnIoAndObserveOnMainThread({ response ->
                response.joinStatus?.let {
                    spontyDataStateSubject.onNext(SpontyDataState.AddSpontyJoin(it))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    spontyDataStateSubject.onNext(SpontyDataState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getSpecificSpontyInfo(spontyId: Int) {
        spontyRepository.getSpecificSpontyInfo(spontyId)
            .doOnSubscribe {
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(true))
            }.doAfterTerminate {
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(false))
            }.subscribeOnIoAndObserveOnMainThread({ response ->
                response.data?.let {
                    spontyDataStateSubject.onNext(SpontyDataState.SpecificSpontyInfo(it))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    spontyDataStateSubject.onNext(SpontyDataState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getAllJoinSponty(allJoinSpontyRequest: AllJoinSpontyRequest) {
        spontyRepository.getAllJoinSponty(allJoinSpontyRequest)
            .doOnSubscribe {
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(true))
            }.doAfterTerminate {
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(false))
            }.subscribeOnIoAndObserveOnMainThread({ response ->
                response.data?.let {
                    spontyDataStateSubject.onNext(SpontyDataState.SpontyJoinInfo(it))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    spontyDataStateSubject.onNext(SpontyDataState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getFollowersList(userId: Int, searchText: String) {
        followUserRepository.getAllFollowersList(1, GetFollowersAndFollowingRequest(userId, searchText))
            .doOnSubscribe {
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(true))
            }
            .doAfterTerminate {
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    spontyDataStateSubject.onNext(SpontyDataState.FollowerList(it.toMutableList()))
                }
            }, { throwable ->
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(false))
                throwable.localizedMessage?.let {
                    spontyDataStateSubject.onNext(SpontyDataState.ErrorMessage(it))
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
            spontyDataStateSubject.onNext(SpontyDataState.UpdateDescriptionText(descriptionString.plus(" ")))
        } else {
            val lastIndexOfToken = subString.findLastAnyOf(listOf("@"))?.first ?: return
            Timber.i("lastIndexOfToken %s", lastIndexOfToken)
            val tempSubString = subString.substring(0, lastIndexOfToken)
            val descriptionString = "$tempSubString @${followUser.username} $remainString"
            spontyDataStateSubject.onNext(SpontyDataState.UpdateDescriptionText(descriptionString.plus(" ")))
        }
    }

    fun getInitialFollowersList(userId: Int) {
        followUserRepository.getAllFollowersList(1, GetFollowersAndFollowingRequest(userId, ""))
            .doOnSubscribe {
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(true))
            }
            .doAfterTerminate {
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    spontyDataStateSubject.onNext(SpontyDataState.InitialFollowerList(it.toMutableList()))
                }
            }, { throwable ->
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(false))
                throwable.localizedMessage?.let {
                    spontyDataStateSubject.onNext(SpontyDataState.ErrorMessage(it))
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
                    spontyDataStateSubject.onNext(SpontyDataState.AddRemoveSpontyLike(it, spontyActionRequest.spontyId))
                }
            }, { throwable ->
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(false))
                throwable.localizedMessage?.let {
                    spontyDataStateSubject.onNext(SpontyDataState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getAllSpontyLike(spontyActionRequest: SpontyActionRequest) {
        spontyRepository.getAllLikes(spontyActionRequest)
            .doOnSubscribe {
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(true))
            }
            .doAfterTerminate {
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.data?.let {
                    spontyDataStateSubject.onNext(SpontyDataState.GetAllSpontyLikes(it))
                }
            }, { throwable ->
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(false))
                throwable.localizedMessage?.let {
                    spontyDataStateSubject.onNext(SpontyDataState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getAllSpontyComments(spontyActionRequest: SpontyActionRequest) {
        spontyRepository.getAllComments(spontyActionRequest)
            .doOnSubscribe {
                spontyDataStateSubject.onNext(SpontyDataState.CommentLoadingState(true))
            }
            .doAfterTerminate {
                spontyDataStateSubject.onNext(SpontyDataState.CommentLoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.data?.let {
                    listOfCommentInfo = response.data
                    spontyDataStateSubject.onNext(SpontyDataState.GetAllComments(it))
                }
            }, { throwable ->
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(false))
                throwable.localizedMessage?.let {
                    spontyDataStateSubject.onNext(SpontyDataState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun addSpontyComments(addSpontyCommentRequest: AddSpontyCommentRequest) {
        var mentionId = arrayListOf<Int>()
        selectedTagUserInfo.forEach {
            mentionId.add(it.id)
        }
        addSpontyCommentRequest.mentionIds = mentionId.joinToString(separator = ",")

        selectedTagUserInfo.clear()
        spontyRepository.addSpontyComment(addSpontyCommentRequest)
            .doOnSubscribe {
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(true))
            }
            .doAfterTerminate {
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.data?.let {
                    spontyDataStateSubject.onNext(SpontyDataState.AddComments(it))
                }
            }, { throwable ->
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(false))
                throwable.localizedMessage?.let {
                    spontyDataStateSubject.onNext(SpontyDataState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun clickOnEditText(commentText: String) {
        spontyDataStateSubject.onNext(SpontyDataState.UpdateEditTextView(commentText))
    }
    fun addSpontyCommentsLike(addSpontyCommentRequest: SpontyCommentActionRequest) {
        selectedTagUserInfo.clear()
        spontyRepository.addSpontyCommentsLike(addSpontyCommentRequest)
            .doOnSubscribe {
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(true))
            }
            .doAfterTerminate {
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->

            }, { throwable ->
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(false))
                throwable.localizedMessage?.let {
                    spontyDataStateSubject.onNext(SpontyDataState.ErrorMessage(it))
                }
            }).autoDispose()
    }


    fun addSpontyReplyComments(postId: Int, commentString: String, commentId: Int) {
        var mentionId = arrayListOf<Int>()
        selectedTagUserInfo.forEach {
            mentionId.add(it.id)
        }
//        addSpontyCommentRequest.mentionIds = mentionId.joinToString(separator = ",")

        val request = AddSpontyCommentReplyRequest(
            spontyId = postId,
            comment = commentString,
            commentId = commentId,
            mentionIds = mentionId.joinToString(separator = ",")
        )
        selectedTagUserInfo.clear()
        spontyRepository.addSpontyReplyComments(request)
            .doOnSuccess {
                if (it != null) {
                    val mPos = listOfCommentInfo.indexOfFirst { cInfo ->
                        cInfo.id == request.commentId
                    }
                    if (mPos != -1) {
                        val reply = listOfCommentInfo[mPos].replies as ArrayList
                        it.data?.let { it1 -> reply.add(it1) }
                        listOfCommentInfo[mPos].replies = reply
                        spontyDataStateSubject.onNext(SpontyDataState.GetAllComments(listOfCommentInfo as ArrayList<SpontyCommentResponse>))
                    }
                }
            }
            .doOnSubscribe {
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(true))
            }
            .doAfterTerminate {
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(false))
            }

            .subscribeOnIoAndObserveOnMainThread({ response ->
                spontyDataStateSubject.onNext(SpontyDataState.SuccessMessage(response.toString()))
            }, { throwable ->
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(false))
                throwable.localizedMessage?.let {
                    spontyDataStateSubject.onNext(SpontyDataState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun addSpontyUpdateComments(commentString: String, commentId: SpontyCommentResponse) {
        var mentionId = arrayListOf<Int>()
        selectedTagUserInfo.forEach {
            mentionId.add(it.id)
        }
//        addSpontyCommentRequest.mentionIds = mentionId.joinToString(separator = ",")
        val request = UpdateCommentRequest(
            comment = commentString,
            mentionIds = mentionId.joinToString(separator = ",")
        )
        selectedTagUserInfo.clear()
        spontyRepository.addSpontyUpdateComments(request,commentId.id)
            .doOnSuccess {
                if (it != null) {
                    for (mainPos in 0 until listOfCommentInfo.size) {
                        val cInfo = listOfCommentInfo[mainPos]
                        if (cInfo.id == commentId.id) {
                            listOfCommentInfo[mainPos].comment = it.data?.comment
                            spontyDataStateSubject.onNext(SpontyDataState.GetAllComments(listOfCommentInfo as ArrayList<SpontyCommentResponse>))
                            return@doOnSuccess
                        } else {
                            val listOfComment =  arrayListOf<SpontyCommentResponse>()
                            cInfo.replies?.let { it1 -> listOfComment.addAll(it1) }
                            if (!listOfComment.isNullOrEmpty()) {
                                val childPos = listOfComment.indexOfFirst { replyInfo ->
                                    replyInfo.id == commentId.id
                                }
                                if (childPos != -1) {
                                    commentId.comment = it.data?.comment
                                    val reply = listOfCommentInfo[mainPos].replies as ArrayList
                                    reply.elementAt(childPos).comment = it.data?.comment
                                    listOfCommentInfo[mainPos].replies = reply
                                    spontyDataStateSubject.onNext(SpontyDataState.GetAllComments(listOfCommentInfo as ArrayList<SpontyCommentResponse>))
                                    return@doOnSuccess
                                }
                            }
                        }
                    }
                }
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                spontyDataStateSubject.onNext(SpontyDataState.EditComment(response.toString()))
            }, { throwable ->
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(false))
                throwable.localizedMessage?.let {
                    spontyDataStateSubject.onNext(SpontyDataState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun removeComments(commentId: Int, comment: SpontyCommentResponse) {
        spontyRepository.removeComment(commentId)
            .doOnSuccess {
                if (it != null) {
                    for (mainPos in 0 until listOfCommentInfo.size) {
                        val cInfo = listOfCommentInfo[mainPos]
                        if (cInfo.id == comment.id) {
                            listOfCommentInfo.removeAt(mainPos)
                            spontyDataStateSubject.onNext(SpontyDataState.GetAllComments(listOfCommentInfo as ArrayList<SpontyCommentResponse>))
                            return@doOnSuccess
                        } else {
                            val listOfCommentReplies =  arrayListOf<SpontyCommentResponse>()
                            cInfo.replies?.let { it1 -> listOfCommentReplies.addAll(it1) }
                            if (!listOfCommentReplies.isNullOrEmpty()) {
                                val childPos = listOfCommentReplies.indexOfFirst { replyInfo ->
                                    replyInfo.id == comment.id
                                }
                                if (childPos != -1) {
                                    val reply = listOfCommentInfo[mainPos].replies as ArrayList
                                    reply.removeAt(childPos)
                                    listOfCommentInfo[mainPos].replies = reply
                                    spontyDataStateSubject.onNext(SpontyDataState.GetAllComments(listOfCommentInfo as ArrayList<SpontyCommentResponse>))
                                    return@doOnSuccess
                                }
                            }
                        }
                    }
                }
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                spontyDataStateSubject.onNext(SpontyDataState.EditComment(response.toString()))
            }, { throwable ->
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(false))
                throwable.localizedMessage?.let {
                    spontyDataStateSubject.onNext(SpontyDataState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun removeSponty(commentId: Int) {
        spontyRepository.removeSponty(commentId)
            .doOnSubscribe {
                spontyDataStateSubject.onNext(SpontyDataState.CommentLoadingState(true))
            }
            .doAfterTerminate {
                spontyDataStateSubject.onNext(SpontyDataState.CommentLoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.message?.let {
                    spontyDataStateSubject.onNext(SpontyDataState.SuccessCommentMessage(it, commentId))
                }
            }, { throwable ->
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(false))
                throwable.localizedMessage?.let {
                    spontyDataStateSubject.onNext(SpontyDataState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun spontyReport(reportSpontyRequest: ReportSpontyRequest) {
        spontyRepository.spontyReport(reportSpontyRequest)
            .doOnSubscribe {
                spontyDataStateSubject.onNext(SpontyDataState.CommentLoadingState(true))
            }
            .doAfterTerminate {
                spontyDataStateSubject.onNext(SpontyDataState.CommentLoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.message?.let {
                    spontyDataStateSubject.onNext(SpontyDataState.SuccessReportMessage(it))
                }
            }, { throwable ->
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(false))
                throwable.localizedMessage?.let {
                    spontyDataStateSubject.onNext(SpontyDataState.ErrorMessage(it))
                }
            }).autoDispose()
    }


    fun getCloudFlareConfig() {
        cloudFlareRepository.getCloudFlareConfig()
            .doOnSubscribe {
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(false))
                if (response.success) {
                    val cloudFlareConfig = response.data
                    if (cloudFlareConfig != null) {
                        spontyDataStateSubject.onNext(SpontyDataState.GetCloudFlareConfig(cloudFlareConfig))
                    } else {
                        response.message?.let {
                            spontyDataStateSubject.onNext(SpontyDataState.CloudFlareConfigErrorMessage(it))
                        }
                    }
                }
            }, { throwable ->
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(false))
                throwable.localizedMessage?.let {
                    spontyDataStateSubject.onNext(SpontyDataState.CloudFlareConfigErrorMessage(it))
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
                spontyDataStateSubject.onNext(SpontyDataState.UploadMediaCloudFlareLoading(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.success) {
                    val result = response.result
                    if (result != null) {
                        val variants = result.variants
                        if (!variants.isNullOrEmpty()) {
                            spontyDataStateSubject.onNext(SpontyDataState.UploadMediaCloudFlareSuccess(variants.first(), selectedMediaType))
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
                spontyDataStateSubject.onNext(SpontyDataState.UploadMediaCloudFlareLoading(false))
                throwable.localizedMessage?.let {
                    spontyDataStateSubject.onNext(SpontyDataState.ErrorMessage(it))
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
                spontyDataStateSubject.onNext(
                    SpontyDataState.UploadMediaCloudFlareVideoSuccess(
                        it.uid.toString(),
                        it.thumbnail.toString(),
                        selectedMediaType
                    )
                )
            }, { throwable ->
                Timber.e(throwable)
                spontyDataStateSubject.onNext(SpontyDataState.LoadingState(false))
                throwable.localizedMessage?.let {
                    spontyDataStateSubject.onNext(SpontyDataState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    private fun handleCloudFlareMediaUploadError(errors: List<String>?) {
        if (!errors.isNullOrEmpty()) {
            val error = errors.firstOrNull()
            if (error != null) {
                spontyDataStateSubject.onNext(SpontyDataState.ErrorMessage(error.toString()))
            }
        }
    }

    sealed class SpontyDataState {

        data class GetCloudFlareConfig(val cloudFlareConfig: CloudFlareConfig) : SpontyDataState()
        data class CloudFlareConfigErrorMessage(val errorMessage: String) : SpontyDataState()
        data class UploadMediaCloudFlareLoading(val isLoading: Boolean) : SpontyDataState()
        data class UploadMediaCloudFlareSuccess(val mediaUrl: String, val selectedMediaType: String) : SpontyDataState()
        data class UploadMediaCloudFlareVideoSuccess(val uid: String, val mediaUrl: String, val selectedMediaType: String) : SpontyDataState()

        data class ErrorMessage(val errorMessage: String) : SpontyDataState()
        data class SuccessMessage(val successMessage: String) : SpontyDataState()
        data class SuccessReportMessage(val successMessage: String) : SpontyDataState()
        data class EditComment(val successMessage: String) : SpontyDataState()
        data class LoadingState(val isLoading: Boolean) : SpontyDataState()
        data class CommentLoadingState(val isLoading: Boolean) : SpontyDataState()
        data class ListofSponty(val spontyData: ArrayList<SpontyResponse>) : SpontyDataState()
        data class NearByListofSponty(val spontyData: ArrayList<SpontyResponse>) : SpontyDataState()
        data class SpontyInfo(val createSponty: SpontyResponse) : SpontyDataState()
        data class SpecificSpontyInfo(val specificSponty: SpontyResponse) : SpontyDataState()
        data class SpontyJoinInfo(val spontyJoinInfo: List<SpontyJoinResponse>) : SpontyDataState()
        data class AddSpontyJoin(val joinStatus: Int) : SpontyDataState()
        data class InitialFollowerList(val listOfFollowers: List<FollowUser>) : SpontyDataState()
        data class FollowerList(val listOfFollowers: List<FollowUser>) : SpontyDataState()
        data class UpdateDescriptionText(val descriptionString: String) : SpontyDataState()
        data class AddRemoveSpontyLike(val addSpontyLike: SpontyActionResponse, val spontyId: Int) : SpontyDataState()
        data class GetAllSpontyLikes(val listOfSpontyLikes: ArrayList<SpontyActionResponse>) : SpontyDataState()
        data class GetAllComments(val listOfSpontyLikes: ArrayList<SpontyCommentResponse>) : SpontyDataState()
        data class AddComments(val comments: SpontyCommentResponse) : SpontyDataState()
        data class UpdateEditTextView(val comments: String) : SpontyDataState()
        data class SuccessCommentMessage(val successMessage: String, val commentId: Int) : SpontyDataState()
    }
}