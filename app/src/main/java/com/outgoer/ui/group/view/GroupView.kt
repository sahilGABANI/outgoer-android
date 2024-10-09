package com.outgoer.ui.group.view

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.group.model.GroupUserInfo
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.GroupListItemBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class GroupView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val groupItemClickStateSubject: PublishSubject<FollowUser> = PublishSubject.create()
    val groupItemClick: Observable<FollowUser> = groupItemClickStateSubject.hide()

    private val groupItemClickedStateSubject: PublishSubject<GroupUserInfo> = PublishSubject.create()
    val groupItemClicked: Observable<GroupUserInfo> = groupItemClickedStateSubject.hide()

    private val closeItemClickStateSubject: PublishSubject<FollowUser> = PublishSubject.create()
    val closeItemClick: Observable<FollowUser> = closeItemClickStateSubject.hide()

    private var binding: GroupListItemBinding? = null
    private lateinit var userInfo: FollowUser
    private lateinit var groupUserInfo: GroupUserInfo

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.group_list_item, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = GroupListItemBinding.bind(view)

        binding?.apply {
            userInfoRelativeLayout.throttleClicks().subscribeAndObserveOnMainThread {
                if(::userInfo.isInitialized) {
                    groupItemClickStateSubject.onNext(userInfo)
                } else {
                    groupItemClickedStateSubject.onNext(groupUserInfo)
                }
            }.autoDispose()

            closeAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
                closeItemClickStateSubject.onNext(userInfo)
            }
        }
    }

    fun bind(user: FollowUser, isClose: Boolean) {
        this.userInfo = user
        binding?.let {
            Glide.with(context)
                .load(user.avatar)
                .centerCrop()
                .placeholder(resources.getDrawable(R.drawable.ic_chat_user_placeholder, null))
                .error(resources.getDrawable(R.drawable.ic_chat_user_placeholder, null))
                .into(it.ivUserProfileImage)

            it.tvUserName.text = user.name
            if (user.isAdmin) {
                it.selectedAppCompatImageView.visibility = View.GONE
                it.closeAppCompatImageView.visibility = View.GONE
                it.adminAppCompatTextView.visibility = View.VISIBLE
            } else {
                it.closeAppCompatImageView.visibility = if (isClose) View.VISIBLE else View.GONE
                it.selectedAppCompatImageView.visibility =
                    if (!isClose && user.isSelected) View.VISIBLE else View.GONE
            }

            it.ivVerified.isVisible = user.profileVerified == 1
        }
    }

    fun bindNew(user: GroupUserInfo, isClose: Boolean) {
        groupUserInfo = user
        binding?.let {
            Glide.with(context)
                .load(user.profileUrl)
                .centerCrop()
                .placeholder(resources.getDrawable(R.drawable.ic_chat_user_placeholder, null))
                .error(resources.getDrawable(R.drawable.ic_chat_user_placeholder, null))
                .into(it.ivUserProfileImage)

            it.tvUserName.text = user.username

            if (user.role == "1") {
                it.selectedAppCompatImageView.visibility = View.GONE
                it.closeAppCompatImageView.visibility = View.GONE
                it.adminAppCompatTextView.visibility = View.VISIBLE
            } else {
                it.adminAppCompatTextView.visibility = View.GONE
                it.closeAppCompatImageView.visibility = if (isClose) View.VISIBLE else View.GONE
                it.selectedAppCompatImageView.visibility =
                    if (!isClose && user.isSelected) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}
