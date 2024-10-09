package com.outgoer.ui.chat.newview

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupWindow
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import com.outgoer.R
import com.outgoer.api.chat.model.ChatMessageInfo
import com.outgoer.api.post.model.MoreActionsForTextActionState
import com.outgoer.base.extension.getFormattedDateForChatMessageHeader
import com.outgoer.base.extension.getFormattedTimeForChatMessage
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.PopUpViewChatAudioSenderBinding
import com.outgoer.databinding.PopUpViewChatTextSenderBinding
import com.outgoer.databinding.ViewNewChatAudioSenderBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject


class NewChatAudioSenderView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val chatAudioViewClicksSubject: PublishSubject<Triple<ChatMessageInfo, AppCompatSeekBar, FrameLayout>> =
        PublishSubject.create()
    val chatAudioViewClicks: Observable<Triple<ChatMessageInfo, AppCompatSeekBar, FrameLayout>> =
        chatAudioViewClicksSubject.hide()

    private val chatAudioFileViewClicksSubject: PublishSubject<ChatMessageInfo> =
        PublishSubject.create()
    val chatAudioFileViewClicks: Observable<ChatMessageInfo> = chatAudioFileViewClicksSubject.hide()

    private val chatForDeleteViewClicksSubject: PublishSubject<ChatMessageInfo> =
        PublishSubject.create()
    val chatForDeleteViewClicks: Observable<ChatMessageInfo> = chatForDeleteViewClicksSubject.hide()

    private val moreActionViewClicksSubject: PublishSubject<MoreActionsForTextActionState> = PublishSubject.create()
    val moreActionViewClicks: Observable<MoreActionsForTextActionState> = moreActionViewClicksSubject.hide()

    private var binding: ViewNewChatAudioSenderBinding? = null
    private lateinit var chatMessageInfo: ChatMessageInfo
    private lateinit var popupMenu: PopupWindow
    private var highlightOverlay: View? = null

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_new_chat_audio_sender, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewNewChatAudioSenderBinding.bind(view)
        binding?.let {
            it.visualizer.setOnTouchListener(OnTouchListener { v, event -> true })
            it.playAudioAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread { i ->
                chatMessageInfo.isPlay = !chatMessageInfo.isPlay
                chatAudioViewClicksSubject.onNext(
                    Triple(
                        chatMessageInfo,
                        it.visualizer,
                        it.actionFrameLayout
                    )
                )

                if (chatMessageInfo.isPlay) {
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
        }
    }

    private fun setClickListener(view: View) {
        view.throttleClicks().subscribeAndObserveOnMainThread {
            moreActionViewClicksSubject.onNext(MoreActionsForTextActionState.ReactedUsersView(chatMessageInfo.id))
        }
    }

    private fun showCustomPopupMenu(longPressedView: View) {
        val parentView = (context as Activity).findViewById<ViewGroup>(android.R.id.content)
        val binding = PopUpViewChatAudioSenderBinding.inflate(LayoutInflater.from(context), parentView, false)
        popupMenu = PopupWindow(
            binding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        if (::chatMessageInfo.isInitialized) {
            with(binding) {
                if (chatMessageInfo.isPlay) {
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

                if (!chatMessageInfo.duration.isNullOrEmpty()) {
                    var duration = ""
                    val minutes = ((chatMessageInfo.duration?.toInt() ?: 0) % 3600) / 60
                    val seconds = (chatMessageInfo.duration?.toInt() ?: 0) % 60
                    duration = String.format("%02d:%02d", minutes, seconds)
                    duration += "s"
                    timeAppCompatTextView.text = duration.toString()
                }
                timeTextView.text = getFormattedTimeForChatMessage(chatMessageInfo.createdAt)

                fun setClickListener(view: View, action: MoreActionsForTextActionState) {
                    view.setOnClickListener {
                        moreActionViewClicksSubject.onNext(action)
                        popupMenu.dismiss()
                    }
                }

                // Set click listeners for actions
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

        val marginBottom = context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._50sdp)
        val marginLeft = context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._25sdp)
        val marginRight = context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._25sdp)

        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        val location = IntArray(2)
        longPressedView.getLocationOnScreen(location)
        val xPos = location[0] + marginLeft
        val yPos = location[1] - binding.root.height - marginBottom
        val adjustedXPos = minOf(xPos, screenWidth - binding.root.measuredWidth - marginRight)
        popupMenu.showAtLocation(longPressedView, Gravity.NO_GRAVITY, adjustedXPos, yPos)
        popupMenu.setOnDismissListener {
            removeHighlight()
        }
    }

    private fun showPopup(v: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            PopupMenu(context, v, Gravity.END, 0, R.style.MyPopupMenu)
        } else {
            PopupMenu(context, v)
        }.apply {
            setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
                when (item?.itemId) {
                    R.id.delete -> {
                        chatForDeleteViewClicksSubject.onNext(chatMessageInfo)
                        true
                    }
                    else -> false
                }
            })
            inflate(R.menu.delete_menu)
            show()
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

            chatAudioFileViewClicksSubject.onNext(chatMessageInfo)
            if (chatMessageInfo.showDate) {
                dateTextView.text =
                    getFormattedDateForChatMessageHeader(context, chatMessageInfo.createdAt)
                dateTextView.visibility = View.VISIBLE
            } else {
                dateTextView.text = ""
                dateTextView.visibility = View.GONE
            }

            timeTextView.text = getFormattedTimeForChatMessage(chatMessageInfo.createdAt)

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

            if (!chatMessageInfo.duration.isNullOrEmpty()) {

                var duration = ""

                val minutes = ((chatMessageInfo.duration?.toInt() ?: 0) % 3600) / 60
                val seconds = (chatMessageInfo.duration?.toInt() ?: 0) % 60

                duration = String.format("%02d:%02d", minutes, seconds)
                duration += "s"

                timeAppCompatTextView.text = duration.toString()
            }


            if (chatMessageInfo.isRead != null) {
                if (chatMessageInfo.isRead == 0) {
                    ivReadStatus.setImageResource(R.drawable.ic_chat_single_tick)
                    ivReadStatus.setColorFilter(
                        ContextCompat.getColor(
                            context,
                            android.R.color.transparent
                        )
                    );
                } else {
                    ivReadStatus.setImageResource(R.drawable.ic_chat_double_tick)
                    ivReadStatus.setColorFilter(ContextCompat.getColor(context, R.color.purple))
                }
            } else {
                ivReadStatus.setImageResource(R.drawable.ic_chat_single_tick)
                ivReadStatus.setColorFilter(
                    ContextCompat.getColor(
                        context,
                        android.R.color.transparent
                    )
                );
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
        binding = null
        if (::popupMenu.isInitialized) {
            popupMenu.dismiss()
        }
        super.onDestroy()
    }
}