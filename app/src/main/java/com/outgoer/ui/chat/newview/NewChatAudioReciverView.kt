package com.outgoer.ui.chat.newview

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupWindow
import androidx.appcompat.widget.AppCompatSeekBar
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.chat.model.ChatMessageInfo
import com.outgoer.api.post.model.MoreActionsForTextActionState
import com.outgoer.base.extension.getFormattedDateForChatMessageHeader
import com.outgoer.base.extension.getFormattedTimeForChatMessage
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.PopUpViewChatAudioReceiverBinding
import com.outgoer.databinding.ViewNewChatAudioReceiverBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class NewChatAudioReciverView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val audioReceiverImgClickSubject: PublishSubject<ChatMessageInfo> =
        PublishSubject.create()
    val audioReceiverImgViewClick: Observable<ChatMessageInfo> = audioReceiverImgClickSubject.hide()


    private val chatAudioViewClicksSubject: PublishSubject<Triple<ChatMessageInfo, AppCompatSeekBar, FrameLayout>> =
        PublishSubject.create()
    val chatAudioViewClicks: Observable<Triple<ChatMessageInfo, AppCompatSeekBar, FrameLayout>> =
        chatAudioViewClicksSubject.hide()

    private val chatForDeleteViewClicksSubject: PublishSubject<ChatMessageInfo> =
        PublishSubject.create()
    val chatForDeleteViewClicks: Observable<ChatMessageInfo> = chatForDeleteViewClicksSubject.hide()

    private val moreActionViewClicksSubject: PublishSubject<MoreActionsForTextActionState> = PublishSubject.create()
    val moreActionViewClicks: Observable<MoreActionsForTextActionState> = moreActionViewClicksSubject.hide()

    private var binding: ViewNewChatAudioReceiverBinding? = null
    private var chatMessageInfo: ChatMessageInfo? = null
    private lateinit var popupMenu: PopupWindow
    private var highlightOverlay: View? = null

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_new_chat_audio_receiver, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewNewChatAudioReceiverBinding.bind(view)
        binding?.let {

            it.ivUserProfileImage.throttleClicks().subscribeAndObserveOnMainThread {
                audioReceiverImgClickSubject.onNext(chatMessageInfo!!)
            }

            it.audioLinearLayout.setOnLongClickListener {view ->
                highlightItem()
                showCustomPopupMenu(view)
                true
            }

            setClickListener(it.likeLinearLayout)
            setClickListener(it.loveLinearLayout)
            setClickListener(it.laughingLinearLayout)
            setClickListener(it.expressionLinearLayout)
            setClickListener(it.sadLinearLayout)
            setClickListener(it.prayLinearLayout)

            it.visualizer.setOnTouchListener(OnTouchListener { v, event -> true })
            it.playAudioAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread { i ->
                chatMessageInfo?.isPlay = !(chatMessageInfo?.isPlay ?: false)

                chatMessageInfo?.let { chatmessageinfo ->
                    chatAudioViewClicksSubject.onNext(
                        Triple(
                            chatmessageinfo,
                            it.visualizer,
                            it.actionFrameLayout
                        )
                    )

                    if (chatmessageinfo.isPlay) {
                        it.playAudioAppCompatImageView.setImageDrawable(
                            resources.getDrawable(
                                R.drawable.ic_pause_icon,
                                null
                            )
                        )
                    } else {
                        it.playAudioAppCompatImageView.setImageDrawable(
                            resources.getDrawable(
                                R.drawable.ic_play_icon,
                                null
                            )
                        )
                    }
                }
            }
        }
    }

    private fun setClickListener(view: View) {
        view.throttleClicks().subscribeAndObserveOnMainThread {
            chatMessageInfo?.let {
                moreActionViewClicksSubject.onNext(MoreActionsForTextActionState.ReactedUsersView(it.id))
            }
        }
    }

    private fun showCustomPopupMenu(longPressedView: View) {
        val parentView = (context as Activity).findViewById<ViewGroup>(android.R.id.content)
        val popupView = PopUpViewChatAudioReceiverBinding.inflate(LayoutInflater.from(context), parentView, false)
        popupMenu = PopupWindow(
            popupView.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        if (chatMessageInfo != null) {
            with(popupView) {
                if (chatMessageInfo?.isPlay == true) {
                    playAudioAppCompatImageView.setImageDrawable(
                        resources.getDrawable(
                            R.drawable.ic_pause_icon,
                            null
                        )
                    )
                } else {
                    playAudioAppCompatImageView.visibility = View.VISIBLE
                    progressbar.visibility = View.GONE
                    playAudioAppCompatImageView.setImageDrawable(
                        resources.getDrawable(
                            R.drawable.ic_play_icon,
                            null
                        )
                    )
                }

                if (!chatMessageInfo?.duration.isNullOrEmpty()) {

                    var duration = ""

                    if (!(chatMessageInfo?.duration ?: "").contains(":")) {
                        val minutes = ((chatMessageInfo?.duration?.toInt() ?: 0) % 3600) / 60
                        val seconds = (chatMessageInfo?.duration?.toInt() ?: 0) % 60
                        duration = String.format("%02d:%02d", minutes, seconds)
                        duration += "s"

                        timeAppCompatTextView.text = duration.toString()
                    } else {
                        timeAppCompatTextView.text = chatMessageInfo?.duration ?: ""
                    }
                }
                timeTextView.text = getFormattedTimeForChatMessage(chatMessageInfo?.createdAt)
                Glide.with(context)
                    .load(chatMessageInfo?.profileUrl)
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

                setClickListener(deleteLinearLayout, MoreActionsForTextActionState.DeleteMessage(chatMessageInfo!!))

                // Set click listeners for reactions
                setClickListener(likeAppCompatImageView, MoreActionsForTextActionState.ReactionOnMessage(chatMessageInfo!!, "like"))
                setClickListener(loveAppCompatImageView, MoreActionsForTextActionState.ReactionOnMessage(chatMessageInfo!!, "love"))
                setClickListener(laughingAppCompatImageView, MoreActionsForTextActionState.ReactionOnMessage(chatMessageInfo!!, "laughing"))
                setClickListener(expressionAppCompatImageView, MoreActionsForTextActionState.ReactionOnMessage(chatMessageInfo!!, "expression"))
                setClickListener(sadAppCompatImageView, MoreActionsForTextActionState.ReactionOnMessage(chatMessageInfo!!, "sad"))
                setClickListener(prayAppCompatImageView, MoreActionsForTextActionState.ReactionOnMessage(chatMessageInfo!!, "pray"))
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

    fun bind(chatMessage: ChatMessageInfo) {
        this.chatMessageInfo = chatMessage
        binding?.apply {
            likeLinearLayout.visibility = View.GONE
            loveLinearLayout.visibility = View.GONE
            laughingLinearLayout.visibility = View.GONE
            expressionLinearLayout.visibility = View.GONE
            sadLinearLayout.visibility = View.GONE
            prayLinearLayout.visibility = View.GONE
            chatMessage.reactionData?.let {
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

            if (chatMessage.showDate) {
                dateTextView.text =
                    getFormattedDateForChatMessageHeader(context, chatMessage.createdAt)
                dateTextView.visibility = View.VISIBLE
            } else {
                dateTextView.text = ""
                dateTextView.visibility = View.GONE
            }

            timeTextView.text = getFormattedTimeForChatMessage(chatMessage.createdAt)

            if (chatMessage.isPlay) {
                playAudioAppCompatImageView.setImageDrawable(
                    resources.getDrawable(
                        R.drawable.ic_pause_icon,
                        null
                    )
                )
            } else {
                playAudioAppCompatImageView.visibility = View.VISIBLE
                progressbar.visibility = View.GONE
                playAudioAppCompatImageView.setImageDrawable(
                    resources.getDrawable(
                        R.drawable.ic_play_icon,
                        null
                    )
                )
            }

            if (!chatMessage.duration.isNullOrEmpty()) {

                var duration = ""

                if (!(chatMessageInfo?.duration ?: "").contains(":")) {
                    val minutes = ((chatMessageInfo?.duration?.toInt() ?: 0) % 3600) / 60
                    val seconds = (chatMessageInfo?.duration?.toInt() ?: 0) % 60
                    duration = String.format("%02d:%02d", minutes, seconds)
                    duration += "s"

                    timeAppCompatTextView.text = duration.toString()
                } else {
                    timeAppCompatTextView.text = chatMessageInfo?.duration ?: ""
                }
            }

            Glide.with(context)
                .load(chatMessage.profileUrl)
                .error(R.drawable.ic_chat_user_placeholder)
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .circleCrop()
                .into(ivUserProfileImage)
            if (chatMessage.chatType == "group") {
               // receiverNameAppCompat.visibility = View.VISIBLE
                receiverNameAppCompat.text = chatMessage.username
            }
        }
    }

    private fun highlightItem() {
        val parent = rootView as ViewGroup
        highlightOverlay = View(context)
        highlightOverlay?.apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.parseColor("#9e000000"))
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
        chatMessageInfo = null
        binding = null
        if (::popupMenu.isInitialized) {
            popupMenu.dismiss()
        }
        super.onDestroy()
    }
}