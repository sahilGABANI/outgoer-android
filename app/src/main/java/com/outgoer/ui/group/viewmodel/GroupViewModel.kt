package com.outgoer.ui.group.viewmodel

import android.content.Context
import com.outgoer.api.chat.model.ChatConversationInfo
import com.outgoer.api.cloudflare.CloudFlareRepository
import com.outgoer.api.cloudflare.model.CloudFlareConfig
import com.outgoer.api.group.GroupRepository
import com.outgoer.api.group.model.CreateGroupRequest
import com.outgoer.api.group.model.GroupInfoResponse
import com.outgoer.api.group.model.ManageGroupRequest
import com.outgoer.api.group.model.UpdateGroupRequest
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

class GroupViewModel(
    private val cloudFlareRepository: CloudFlareRepository,
    private val groupRepository: GroupRepository
) : BaseViewModel() {

    private val groupStateSubjects: PublishSubject<GroupViewState> = PublishSubject.create()
    val groupState: Observable<GroupViewState> = groupStateSubjects.hide()

    fun createGroup(createGroupRequest: CreateGroupRequest) {
        groupRepository.createGroup(createGroupRequest)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if(response.success) {
                    response.data?.let {data ->
                        groupStateSubjects.onNext(GroupViewState.SuccessConversation(response.message.toString() , data))
                    }
                    groupStateSubjects.onNext(GroupViewState.SuccessMessage(response.message.toString()))
                }

            }, { throwable ->
                throwable.localizedMessage?.let {
                    Timber.tag("<><>").e("CreateGroupRequest".plus(it))
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

    fun updateGroup(groupId: Int, updateGroupRequest: UpdateGroupRequest) {
        groupRepository.updateGroup(groupId, updateGroupRequest)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.data?.let {
                    groupStateSubjects.onNext(GroupViewState.UpdateGroup(it))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    Timber.tag("<><>").e("CreateGroupRequest".plus(it))
                    groupStateSubjects.onNext(GroupViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getGroupInfo(groupId: Int) {
        groupStateSubjects.onNext(GroupViewState.LoadingGroupState(true))
        groupRepository.getGroupInfo(groupId)
            .doOnSubscribe {
            }.doAfterTerminate {
//                groupStateSubjects.onNext(GroupViewState.LoadingState(false))
            }.subscribeOnIoAndObserveOnMainThread({ response ->
                groupStateSubjects.onNext(GroupViewState.LoadingGroupState(false))
                response.data?.let {
                    groupStateSubjects.onNext(GroupViewState.GetGroupInfo(it))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    Timber.tag("<><>").e("CreateGroupRequest".plus(it))
                }
            }).autoDispose()
    }


    fun deleteChatGroup(chatUserInfo: ChatConversationInfo) {
        groupRepository.deleteGroupInfo(chatUserInfo.id ?: 0)
            .subscribeOnIoAndObserveOnMainThread({
                Timber.e(it.message)
                if (it.success) {
                    groupStateSubjects.onNext((GroupViewState.DeleteGroupInfo(chatUserInfo)))
                }
            }, { throwable ->
                Timber.e(throwable)
                throwable.localizedMessage?.let {
                    groupStateSubjects.onNext((GroupViewState.ErrorMessage(it)))
                }
            }).autoDispose()
    }


    fun setGroupAdmin(manageGroupRequest: ManageGroupRequest) {
        groupRepository.setGroupAdmin(manageGroupRequest)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.message?.let {
                    groupStateSubjects.onNext(GroupViewState.AdminSuccessMessage(it))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    Timber.tag("<><>").e("CreateGroupRequest".plus(it))
                }
            }).autoDispose()
    }

    fun removeGroupUser(manageGroupRequest: ManageGroupRequest) {
        groupRepository.removeGroupUser(manageGroupRequest)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.message?.let {
                    groupStateSubjects.onNext(GroupViewState.RemoveUserSuccessMessage(it))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    Timber.tag("<><>").e("CreateGroupRequest".plus(it))
                    groupStateSubjects.onNext(GroupViewState.ErrorMessage(it ?: ""))
                }
            }).autoDispose()
    }

    fun removeGroupAdminUser(manageGroupRequest: ManageGroupRequest) {
        groupRepository.removeGroupAdminUser(manageGroupRequest)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.message?.let {
                    groupStateSubjects.onNext(GroupViewState.RemoveAdminSuccessMessage(it))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    Timber.tag("<><>").e("CreateGroupRequest".plus(it))
                }
            }).autoDispose()
    }

    fun addGroupUser(manageGroupRequest: ManageGroupRequest) {
        groupRepository.addGroupUser(manageGroupRequest)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.message?.let {
                    groupStateSubjects.onNext(GroupViewState.AddUserToSuccessMessage(it))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    Timber.tag("<><>").e("CreateGroupRequest".plus(it))
                }
            }).autoDispose()
    }


    sealed class GroupViewState {
        data class ErrorMessage(val errorMessage: String) : GroupViewState()
        data class SuccessMessage(val successMessage: String) : GroupViewState()
        data class AdminSuccessMessage(val successMessage: String) : GroupViewState()
        data class RemoveUserSuccessMessage(val successMessage: String) : GroupViewState()
        data class RemoveAdminSuccessMessage(val successMessage: String) : GroupViewState()
        data class AddUserToSuccessMessage(val successMessage: String) : GroupViewState()
        data class SuccessConversation(val message:String,val chatInfo : ChatConversationInfo) : GroupViewState()
        data class LoadingState(val isLoading: Boolean) : GroupViewState()
        data class UpdateGroup(val groupInfoResponse: GroupInfoResponse) : GroupViewState()
        data class GetGroupInfo(val groupInfoResponse: ChatConversationInfo) : GroupViewState()
        data class LoadingGroupState(val isLoading: Boolean) : GroupViewState()

        data class DeleteGroupInfo(val groupInfoResponse: ChatConversationInfo) : GroupViewState()
        data class GetCloudFlareConfig(val cloudFlareConfig: CloudFlareConfig) : GroupViewState()
        data class CloudFlareConfigErrorMessage(val errorMessage: String) : GroupViewState()
        data class UploadImageCloudFlareLoading(val isLoading: Boolean) : GroupViewState()
        data class UploadImageCloudFlareSuccess(val imageUrl: String) : GroupViewState()
    }
}