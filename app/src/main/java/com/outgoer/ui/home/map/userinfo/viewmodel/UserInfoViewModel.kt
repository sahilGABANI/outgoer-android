package com.outgoer.ui.home.map.userinfo.viewmodel

import com.outgoer.api.follow.FollowUserRepository
import com.outgoer.api.follow.model.AcceptRejectRequest
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import timber.log.Timber

class UserInfoViewModel(
    private val followUserRepository: FollowUserRepository
) : BaseViewModel() {

    fun followUnfollow(userId: Int) {
        followUserRepository.acceptRejectFollowRequest(AcceptRejectRequest(userId))
            .doOnSubscribe {}
            .subscribeOnIoAndObserveOnMainThread({

            }, { throwable ->
                throwable.printStackTrace()
                throwable.localizedMessage?.let {
                    Timber.e(it)
                }
            }).autoDispose()
    }
}