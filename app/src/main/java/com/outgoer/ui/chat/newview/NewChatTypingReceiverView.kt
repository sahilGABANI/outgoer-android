package com.outgoer.ui.chat.newview

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.chat.model.ChatMessageInfo
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewNewChatTypingReceiverBinding

class NewChatTypingReceiverView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private var binding: ViewNewChatTypingReceiverBinding? = null
    private lateinit var chatMessageInfo: ChatMessageInfo

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_new_chat_typing_receiver, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewNewChatTypingReceiverBinding.bind(view)
    }

    fun bind(chatMessageInfo: ChatMessageInfo) {
        this.chatMessageInfo = chatMessageInfo
        binding?.apply {
            Glide.with(context)
                .load(chatMessageInfo.profileUrl)
                .error(R.drawable.ic_chat_user_placeholder)
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .circleCrop()
                .into(ivUserProfileImage)
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}