package com.outgoer.ui.chat.newview

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.bumptech.glide.Glide
import com.makeramen.roundedimageview.RoundedImageView
import com.outgoer.R
import com.outgoer.api.chat.model.ChatMessageInfo
import com.outgoer.api.chat.model.MessageType
import com.outgoer.api.post.model.MoreActionsForTextActionState
import com.outgoer.base.extension.getFormattedDateForChatMessageHeader
import com.outgoer.base.extension.getFormattedTimeForChatMessage
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.PopUpViewChatGifReceiverBinding
import com.outgoer.databinding.PopUpViewChatTextReceiverBinding
import com.outgoer.databinding.ViewNewChatGifReceiverBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class NewChatGIFReciverView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val gifReceiverImageClickSubject: PublishSubject<ChatMessageInfo> = PublishSubject.create()
    val gifReceiverViewClick: Observable<ChatMessageInfo> = gifReceiverImageClickSubject.hide()

    private val chatMediaViewClicksSubject: PublishSubject<ChatMessageInfo> = PublishSubject.create()
    val chatMediaViewClicks: Observable<ChatMessageInfo> = chatMediaViewClicksSubject.hide()

    private val chatForDeleteViewClicksSubject: PublishSubject<ChatMessageInfo> = PublishSubject.create()
    val chatForDeleteViewClicks: Observable<ChatMessageInfo> = chatForDeleteViewClicksSubject.hide()

    private val moreActionViewClicksSubject: PublishSubject<MoreActionsForTextActionState> = PublishSubject.create()
    val moreActionViewClicks: Observable<MoreActionsForTextActionState> = moreActionViewClicksSubject.hide()

    private var binding: ViewNewChatGifReceiverBinding? = null
    private lateinit var chatMessageInfo: ChatMessageInfo
    private lateinit var popupMenu: PopupWindow
    private var highlightOverlay: View? = null

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_new_chat_gif_receiver, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewNewChatGifReceiverBinding.bind(view)
        binding?.let {

            it.ivUserProfileImage.throttleClicks().subscribeAndObserveOnMainThread {
                gifReceiverImageClickSubject.onNext(chatMessageInfo)
            }

            it.ivMediaView.throttleClicks().subscribeAndObserveOnMainThread {
                chatMediaViewClicksSubject.onNext(chatMessageInfo)
            }
//            it.playAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
//                chatMediaViewClicksSubject.onNext(chatMessageInfo)
//            }
            it.timingConstraintLayout.throttleClicks().subscribeAndObserveOnMainThread {
                chatForDeleteViewClicksSubject.onNext(chatMessageInfo)
            }
            it.ivMediaView.setOnLongClickListener {view ->
                showCustomPopupMenu(view)
                highlightItem()
                true
            }

            setClickListener(it.likeLinearLayout)
            setClickListener(it.loveLinearLayout)
            setClickListener(it.laughingLinearLayout)
            setClickListener(it.expressionLinearLayout)
            setClickListener(it.sadLinearLayout)
            setClickListener(it.prayLinearLayout)
        }
    }

    private fun setClickListener(view: View) {
        view.throttleClicks().subscribeAndObserveOnMainThread {
            moreActionViewClicksSubject.onNext(MoreActionsForTextActionState.ReactedUsersView(chatMessageInfo.id))
        }
    }

    private fun showCustomPopupMenu(longPressedView: View) {
        val parentView = (context as Activity).findViewById<ViewGroup>(android.R.id.content)
        val popupView = PopUpViewChatGifReceiverBinding.inflate(LayoutInflater.from(context), parentView, false)
        popupMenu = PopupWindow(
            popupView.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        if (::chatMessageInfo.isInitialized) {
            with(popupView) {
                timeTextView.text = getFormattedTimeForChatMessage(chatMessageInfo.createdAt)

                Glide.with(context)
                    .asGif()
                    .load(chatMessageInfo.thumbnail)
                    .placeholder(R.drawable.ic_chat_user_placeholder)
                    .centerCrop()
                    .into(ivMediaView)

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

                setClickListener(deleteLinearLayout, MoreActionsForTextActionState.DeleteMessage(chatMessageInfo))

                setClickListener(likeAppCompatImageView, MoreActionsForTextActionState.ReactionOnMessage(chatMessageInfo, "like"))
                setClickListener(loveAppCompatImageView, MoreActionsForTextActionState.ReactionOnMessage(chatMessageInfo, "love"))
                setClickListener(laughingAppCompatImageView, MoreActionsForTextActionState.ReactionOnMessage(chatMessageInfo, "laughing"))
                setClickListener(expressionAppCompatImageView, MoreActionsForTextActionState.ReactionOnMessage(chatMessageInfo, "expression"))
                setClickListener(sadAppCompatImageView, MoreActionsForTextActionState.ReactionOnMessage(chatMessageInfo, "sad"))
                setClickListener(prayAppCompatImageView, MoreActionsForTextActionState.ReactionOnMessage(chatMessageInfo, "pray"))
            }
        }

        val marginBottom = context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._50sdp)
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
                dateTextView.text = getFormattedDateForChatMessageHeader(context, chatMessageInfo.createdAt)
                dateTextView.visibility = View.VISIBLE
            } else {
                dateTextView.text = ""
                dateTextView.visibility = View.GONE
            }

            Glide.with(context)
                .asGif()
                .load(chatMessageInfo.thumbnail)
                .placeholder(R.drawable.ic_logo_placeholder)
                .centerCrop()
                .into(ivMediaView)

            timeTextView.text = getFormattedTimeForChatMessage(chatMessageInfo.createdAt)

            Glide.with(context)
                .load(chatMessageInfo.profileUrl)
                .error(R.drawable.ic_chat_user_placeholder)
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .circleCrop()
                .into(ivUserProfileImage)
            if (chatMessageInfo.chatType == "group") {
                receiverNameAppCompat.visibility =  View.VISIBLE
                receiverNameAppCompat.text = chatMessageInfo.username
            }
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