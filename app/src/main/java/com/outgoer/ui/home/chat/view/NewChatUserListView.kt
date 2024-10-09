package com.outgoer.ui.home.chat.view

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.github.marlonlom.utilities.timeago.TimeAgoMessages
import com.outgoer.R
import com.outgoer.api.chat.model.ChatConversationActionState
import com.outgoer.api.chat.model.ChatConversationInfo
import com.outgoer.api.chat.model.MessageType
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewNewChatUserListBinding
import com.outgoer.utils.Utility
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class NewChatUserListView(context: Context) : ConstraintLayoutWithLifecycle(context) {
    private val chatConversationActionStateSubject: PublishSubject<ChatConversationActionState> = PublishSubject.create()
    val chatConversationActionState: Observable<ChatConversationActionState> = chatConversationActionStateSubject.hide()

    private var chatUserListBinding: ViewNewChatUserListBinding? = null
    private lateinit var chatConversationInfo: ChatConversationInfo

    var localeByLanguageTag: Locale = Locale.forLanguageTag("en")
    var calendar: Calendar = Calendar.getInstance()

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_new_chat_user_list, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        chatUserListBinding = ViewNewChatUserListBinding.bind(view)

        chatUserListBinding?.apply {
            throttleClicks().subscribeAndObserveOnMainThread {
                chatConversationActionStateSubject.onNext(ChatConversationActionState.ConversationClick(chatConversationInfo))
            }.autoDispose()
        }
    }

    fun bind(chatConversationInfo: ChatConversationInfo) {
        this.chatConversationInfo = chatConversationInfo

        chatUserListBinding?.apply {

            if(chatConversationInfo.chatType.equals(resources.getString(R.string.label_group))) {
                Glide.with(context)
                    .load(chatConversationInfo.filePath)
                    .placeholder(R.drawable.ic_group_user_placeholder)
                    .error(R.drawable.ic_group_user_placeholder)
                    .into(ivUserProfileImage)

                tvUserName.text = chatConversationInfo.groupName
                ivVerified.isVisible = false

            } else {
                Glide.with(context)
                    .load(chatConversationInfo.profileUrl)
                    .placeholder(R.drawable.ic_chat_user_placeholder)
                    .error(R.drawable.ic_chat_user_placeholder)
                    .into(ivUserProfileImage)

                tvUserName.text = chatConversationInfo.username
                ivVerified.isVisible = chatConversationInfo.profileVerified == 1
            }
            tvChatDateTime.text = chatConversationInfo.conversationUpdatedAt

            Timber.tag("ChatConversationInfo").i("chatConversationInfo.lastMessage: ${chatConversationInfo.lastMessage}")

            when (chatConversationInfo.fileType) {
                MessageType.Text, MessageType.REACTION_EMOJI -> {
                    tvLastMessage.visibility = View.VISIBLE
                    tvLastMessage.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0,0,0)
                    if (chatConversationInfo.fileType == MessageType.REACTION_EMOJI) {
                        tvLastMessage.text = context.resources.getString(R.string.reacted_message, chatConversationInfo.lastMessage)
                            .replaceFirstChar { it.uppercase() }
                    } else {
                        tvLastMessage.text = chatConversationInfo.lastMessage
                    }
                }
                MessageType.ChatStarted -> {
                    tvLastMessage.visibility = View.GONE
                }
                else  -> {
                    tvLastMessage.visibility = View.VISIBLE
                    tvLastMessage.text = chatConversationInfo.fileType?.name ?: ""

                    when (chatConversationInfo.fileType) {
                        MessageType.Image -> {
                            tvLastMessage.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_chat_file_type_photo,0,0,0)
                        }
                        MessageType.GIF -> {
                            tvLastMessage.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.gif_icons,0,0,0)
                        }
                        MessageType.Audio -> {
                            tvLastMessage.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.new_mic,0,0,0)
                        }
                        MessageType.Post -> {
                            tvLastMessage.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_chat_file_type_photo,0,0,0)
                        }
                        MessageType.Reel -> {
                            tvLastMessage.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_reels_icon_saved,0,0,0)
                        }
                        else -> {}
                    }
                }
            }

            if (chatConversationInfo.unreadCount != 0) {
                tvNewMsgCount.visibility = View.VISIBLE
                tvNewMsgCount.text = chatConversationInfo.unreadCount.toString()
            } else {
                tvNewMsgCount.visibility = View.GONE
                tvNewMsgCount.text = ""
            }

            val storyCount = (chatConversationInfo.storyCount ?: 0) > 0
            ivUserProfileImage.background = when {
                storyCount -> Utility.ringGradientColor
                else -> null
            }
/*            if(chatConversationInfo?.isLive?: 0 > 0) {
                ivUserProfileImage.background = resources.getDrawable(R.drawable.ring_gredient_color, null)
                liveAppCompatTextView.visibility = View.VISIBLE

            } else {
                if(chatConversationInfo?.reelCount ?: 0 ?: 0 > 0) {
                    ivUserProfileImage.background = resources.getDrawable(R.drawable.ring_gredient_color, null)
                    liveAppCompatTextView.visibility = View.GONE
                } else if(chatConversationInfo?.postCount ?: 0 > 0) {
                    ivUserProfileImage.background = resources.getDrawable(R.drawable.ring_gredient_color, null)
                    liveAppCompatTextView.visibility = View.GONE
                } else if(chatConversationInfo?.spontyCount ?: 0 > 0) {
                    ivUserProfileImage.background = resources.getDrawable(R.drawable.ring_blue_gredient_color, null)
                    liveAppCompatTextView.visibility = View.GONE
                } else {
                    ivUserProfileImage.background = null
                    liveAppCompatTextView.visibility = View.GONE
                }
            }*/

        }
    }

    override fun onDestroy() {
        chatUserListBinding = null
        super.onDestroy()
    }
}