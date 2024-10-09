package com.outgoer.ui.block.view

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.post.model.BlockAccountPageState
import com.outgoer.api.profile.model.BlockUserResponse
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewBlockAccountsBinding
import com.outgoer.databinding.ViewPostLikesBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class BlockAccountView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val blockAccountViewClickSubject: PublishSubject<BlockAccountPageState> = PublishSubject.create()
    val blockAccountViewClick: Observable<BlockAccountPageState> = blockAccountViewClickSubject.hide()

    private var binding: ViewBlockAccountsBinding? = null
    private lateinit var blockUserResponse: BlockUserResponse

    init {
        inflateUi()
    }

    private fun inflateUi() {

        val view = View.inflate(context, R.layout.view_block_accounts, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewBlockAccountsBinding.bind(view)

        binding?.apply {
            ivUserProfile.throttleClicks().subscribeAndObserveOnMainThread {
                blockAccountViewClickSubject.onNext(
                    BlockAccountPageState.UserProfileClick(
                        blockUserResponse
                    )
                )
            }.autoDispose()

            btnFollow.throttleClicks().subscribeAndObserveOnMainThread {
                blockAccountViewClickSubject.onNext(
                    BlockAccountPageState.UnblockAccountClick(
                        blockUserResponse
                    )
                )
            }.autoDispose()
        }
    }

    fun bind(blockAccount: BlockUserResponse) {
        this.blockUserResponse = blockAccount

        binding?.apply {

            Glide.with(context)
                .load(blockAccount?.avatar ?: "")
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .error(R.drawable.ic_chat_user_placeholder)
                .into(ivUserProfile)

            tvUsername.text = blockAccount.name ?: ""
            tvDescription.text = blockAccount.username ?: ""
            ivVerified.isVisible = blockAccount.profileVerified == 1

            btnFollowing.isVisible = false
            btnFollow.isVisible = true
            btnFollow.text = resources.getString(R.string.label_unblock)
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}