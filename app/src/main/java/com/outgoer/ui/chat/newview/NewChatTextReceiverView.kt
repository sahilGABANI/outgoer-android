package com.outgoer.ui.chat.newview


import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.chat.model.ChatMessageInfo
import com.outgoer.api.post.model.MoreActionsForTextActionState
import com.outgoer.base.extension.getFormattedDateForChatMessageHeader
import com.outgoer.base.extension.getFormattedTimeForChatMessage
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.PopUpViewChatTextReceiverBinding
import com.outgoer.databinding.ViewNewChatTextReceiverBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class NewChatTextReceiverView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val moreActionViewClicksSubject: PublishSubject<MoreActionsForTextActionState> = PublishSubject.create()
    val moreActionViewClicks: Observable<MoreActionsForTextActionState> = moreActionViewClicksSubject.hide()

    private var binding: ViewNewChatTextReceiverBinding? = null
    private lateinit var chatMessageInfo: ChatMessageInfo
    private lateinit var popupMenu: PopupWindow
    private var highlightOverlay: View? = null

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_new_chat_text_receiver, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewNewChatTextReceiverBinding.bind(view)


        binding?.apply {
            ivUserProfileImage.throttleClicks().subscribeAndObserveOnMainThread {
                moreActionViewClicksSubject.onNext(MoreActionsForTextActionState.UserProfileOpen(chatMessageInfo))
            }

            receiverAppCompatTextView.setOnLongClickListener {view ->
                highlightItem()
                showCustomPopupMenu(view)
                true
            }

            setClickListener(likeLinearLayout)
            setClickListener(loveLinearLayout)
            setClickListener(laughingLinearLayout)
            setClickListener(expressionLinearLayout)
            setClickListener(sadLinearLayout)
            setClickListener(prayLinearLayout)

            receiverAppCompatTextView.setOnMentionClickListener { _, text ->
                moreActionViewClicksSubject.onNext(MoreActionsForTextActionState.TaggedUser(text.toString(), chatMessageInfo))
            }
        }
    }

    private fun setClickListener(view: View) {
        view.throttleClicks().subscribeAndObserveOnMainThread {
            moreActionViewClicksSubject.onNext(MoreActionsForTextActionState.ReactedUsersView(chatMessageInfo.id))
        }
    }

    private fun showCustomPopupMenu(longPressedView: View) {
        val parentView = (context as Activity).findViewById<ViewGroup>(android.R.id.content)
        val popupView = PopUpViewChatTextReceiverBinding.inflate(LayoutInflater.from(context), parentView, false)
        popupMenu = PopupWindow(
            popupView.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        if (::chatMessageInfo.isInitialized) {
            with(popupView) {
                receiverAppCompatTextView.text = chatMessageInfo.message.toString()
                timeTextView.text = getFormattedTimeForChatMessage(chatMessageInfo.createdAt)
                Glide.with(context)
                    .load(chatMessageInfo.profileUrl)
                    .error(R.drawable.ic_chat_user_placeholder)
                    .placeholder(R.drawable.ic_chat_user_placeholder)
                    .circleCrop()
                    .into(ivUserProfileImage)

                fun setClickListener(view: View, action: MoreActionsForTextActionState) {
                    view.throttleClicks().subscribeAndObserveOnMainThread {
                        moreActionViewClicksSubject.onNext(action)
                        popupMenu.dismiss()
                    }
                }
                // Set click listeners for actions
                setClickListener(replyLinearLayout, MoreActionsForTextActionState.ReplyMessage(chatMessageInfo))
                setClickListener(forwardLinearLayout, MoreActionsForTextActionState.ForwardMessage(chatMessageInfo))
                setClickListener(copyLinearLayout, MoreActionsForTextActionState.CopyMessage(chatMessageInfo))
                setClickListener(deleteLinearLayout, MoreActionsForTextActionState.DeleteMessage(chatMessageInfo))

                // Set click listeners for reactions
                setClickListener(likeAppCompatImageView, MoreActionsForTextActionState.ReactionOnMessage(chatMessageInfo, "like"))
                setClickListener(loveAppCompatImageView, MoreActionsForTextActionState.ReactionOnMessage(chatMessageInfo, "love"))
                setClickListener(laughingAppCompatImageView, MoreActionsForTextActionState.ReactionOnMessage(chatMessageInfo, "laughing"))
                setClickListener(expressionAppCompatImageView, MoreActionsForTextActionState.ReactionOnMessage(chatMessageInfo, "expression"))
                setClickListener(sadAppCompatImageView, MoreActionsForTextActionState.ReactionOnMessage(chatMessageInfo, "sad"))
                setClickListener(prayAppCompatImageView, MoreActionsForTextActionState.ReactionOnMessage(chatMessageInfo, "pray"))
            }
        }

        val marginBottom = context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._142sdp)
        val marginLeft = context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._30sdp)
        val location = IntArray(2)
        longPressedView.getLocationOnScreen(location)
        val xPos = location[0] - marginLeft
        val yPos = location[1] - popupView.root.height - marginBottom
        popupMenu.showAtLocation(longPressedView, Gravity.NO_GRAVITY, xPos, yPos)
        popupMenu.setOnDismissListener {
            removeHighlight()
        }
    }

    fun bind(chatMessageInfo: ChatMessageInfo) {
        this.chatMessageInfo = chatMessageInfo
        binding?.apply {
            likeLinearLayout.visibility = View.GONE
            loveLinearLayout.visibility = View.GONE
            laughingLinearLayout.visibility = View.GONE
            expressionLinearLayout.visibility = View.GONE
            sadLinearLayout.visibility = View.GONE
            prayLinearLayout.visibility = View.GONE
            chatMessageInfo.reactionData?.let {
                if((it.reactionCounts?.like ?: 0) > 0) {
                    likeLinearLayout.visibility = View.VISIBLE
                    likeCountAppCompatTextView.text =( it.reactionCounts?.like ?: 0).toString()
                }
                if((it.reactionCounts?.love ?: 0) > 0) {
                    loveLinearLayout.visibility = View.VISIBLE
                    loveCountAppCompatTextView.text =( it.reactionCounts?.love ?: 0).toString()
                }
                if((it.reactionCounts?.laughing ?: 0) > 0) {
                    laughingLinearLayout.visibility = View.VISIBLE
                    laughingCountAppCompatTextView.text =( it.reactionCounts?.laughing ?: 0).toString()
                }
                if((it.reactionCounts?.expression ?: 0) > 0) {
                    expressionLinearLayout.visibility = View.VISIBLE
                    expressionCountAppCompatTextView.text =( it.reactionCounts?.expression ?: 0).toString()
                }
                if((it.reactionCounts?.sad ?: 0) > 0) {
                    sadLinearLayout.visibility = View.VISIBLE
                    sadCountAppCompatTextView.text =( it.reactionCounts?.sad ?: 0).toString()
                }
                if((it.reactionCounts?.pray ?: 0) > 0) {
                    prayLinearLayout.visibility = View.VISIBLE
                    prayCountAppCompatTextView.text =( it.reactionCounts?.pray ?: 0).toString()
                }
            }

            if (chatMessageInfo.showDate) {
                dateTextView.text =
                    getFormattedDateForChatMessageHeader(context, chatMessageInfo.createdAt)
                dateTextView.visibility = View.VISIBLE
            } else {
                dateTextView.text = ""
                dateTextView.visibility = View.GONE
            }

            if((chatMessageInfo.storyId ?: 0) > 0) {
                replyToStory.isVisible =(chatMessageInfo.story != null)
                ivMediaView.isVisible = (chatMessageInfo.story != null)
                replyToStory.text = resources.getString(R.string.replied_to_your_story, chatMessageInfo.username)

                Glide.with(context)
                    .load(if(chatMessageInfo.story?.image.isNullOrEmpty()) chatMessageInfo.story?.thumbnailUrl else chatMessageInfo.story?.image)
                    .placeholder(R.drawable.venue_placeholder)
                    .into(ivMediaView)
            } else {
                replyToStory.visibility = View.GONE
                ivMediaView.visibility = View.GONE
            }

            if (chatMessageInfo.createdAt.isNullOrEmpty()) {
                timeTextView.visibility = View.GONE
            } else {
                timeTextView.visibility = View.VISIBLE
                timeTextView.text = getFormattedTimeForChatMessage(chatMessageInfo.createdAt)
            }

            receiverAppCompatTextView.text = chatMessageInfo.message

            Glide.with(context)
                .load(chatMessageInfo.profileUrl)
                .error(R.drawable.ic_chat_user_placeholder)
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .circleCrop()
                .into(ivUserProfileImage)

            if (chatMessageInfo.chatType == "group") {
                // receiverNameAppCompat.visibility = View.VISIBLE
                receiverNameAppCompat.text = chatMessageInfo.username
            }
        }
    }

    private fun highlightItem() {
        val parent = rootView as ViewGroup
        highlightOverlay = View(context)
        highlightOverlay?.apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.parseColor("#88000000"))
            parent.addView(this)
        }
    }

    private fun removeHighlight() {
        val parent = rootView as ViewGroup
        highlightOverlay?.let {
            parent.removeView(it)
        }
    }

    override fun onDestroy() {
        binding = null
        if (::popupMenu.isInitialized) {
            popupMenu.dismiss()
        }
        super.onDestroy()
    }
}