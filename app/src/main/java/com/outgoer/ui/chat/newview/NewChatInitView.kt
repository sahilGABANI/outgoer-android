package com.outgoer.ui.chat.newview


import android.content.Context
import android.view.View
import com.outgoer.R
import com.outgoer.api.chat.model.ChatMessageInfo
import com.outgoer.base.extension.getFormattedDateForChatMessageHeader
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewNewChatInitBinding
import timber.log.Timber

class NewChatInitView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private var binding: ViewNewChatInitBinding? = null

    private lateinit var chatInfo: ChatMessageInfo

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_new_chat_init, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewNewChatInitBinding.bind(view)

    }

    fun bind(chatMessageInfo: ChatMessageInfo) {
        chatInfo = chatMessageInfo

        binding?.apply {
            receiverAppCompatTextView.text = chatMessageInfo.message
            dateTextView.text = getFormattedDateForChatMessageHeader(context, chatMessageInfo.createdAt)
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}