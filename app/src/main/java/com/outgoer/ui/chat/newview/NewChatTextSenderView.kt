package com.outgoer.ui.chat.newview


import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.chat.model.ChatMessageInfo
import com.outgoer.api.post.model.MoreActionsForTextActionState
import com.outgoer.api.post.model.SpontyActionState
import com.outgoer.base.extension.getFormattedDateForChatMessageHeader
import com.outgoer.base.extension.getFormattedTimeForChatMessage
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.PopUpViewChatTextSenderBinding
import com.outgoer.databinding.ViewNewChatTextSenderBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class NewChatTextSenderView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private var binding: ViewNewChatTextSenderBinding? = null

    private val moreActionViewClicksSubject: PublishSubject<MoreActionsForTextActionState> = PublishSubject.create()
    val moreActionViewClicks: Observable<MoreActionsForTextActionState> = moreActionViewClicksSubject.hide()

    private lateinit var chatInfo: ChatMessageInfo
    private lateinit var popupMenu: PopupWindow
    private var highlightOverlay: View? = null

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_new_chat_text_sender, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewNewChatTextSenderBinding.bind(view)

        binding?.let {

            it.senderAppCompatTextView.setOnMentionClickListener { _, text ->
                println("Text: " + text)
                moreActionViewClicksSubject.onNext(MoreActionsForTextActionState.TaggedUser(text.toString(), chatInfo))
            }

            it.senderAppCompatTextView.setOnLongClickListener {view ->
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
            moreActionViewClicksSubject.onNext(MoreActionsForTextActionState.ReactedUsersView(chatInfo.id))
        }
    }

    private fun showCustomPopupMenu(longPressedView: View) {
        val parentView = (context as Activity).findViewById<ViewGroup>(android.R.id.content)
        val binding = PopUpViewChatTextSenderBinding.inflate(LayoutInflater.from(context), parentView, false)
        popupMenu = PopupWindow(
            binding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        if (::chatInfo.isInitialized) {
            with(binding) {
                senderAppCompatTextView.text = chatInfo.message.toString()
                timeTextView.text = getFormattedTimeForChatMessage(chatInfo.createdAt)

                fun setClickListener(view: View, action: MoreActionsForTextActionState) {
                    view.setOnClickListener {
                        moreActionViewClicksSubject.onNext(action)
                        popupMenu.dismiss()
                    }
                }

                // Set click listeners for actions
                setClickListener(replyLinearLayout, MoreActionsForTextActionState.ReplyMessage(chatInfo))
                setClickListener(forwardLinearLayout, MoreActionsForTextActionState.ForwardMessage(chatInfo))
                setClickListener(copyLinearLayout, MoreActionsForTextActionState.CopyMessage(chatInfo))
                setClickListener(deleteLinearLayout, MoreActionsForTextActionState.DeleteMessage(chatInfo))

                // Set click listeners for reactions
                setClickListener(likeAppCompatImageView, MoreActionsForTextActionState.ReactionOnMessage(chatInfo, "like"))
                setClickListener(loveAppCompatImageView, MoreActionsForTextActionState.ReactionOnMessage(chatInfo, "love"))
                setClickListener(laughingAppCompatImageView, MoreActionsForTextActionState.ReactionOnMessage(chatInfo, "laughing"))
                setClickListener(expressionAppCompatImageView, MoreActionsForTextActionState.ReactionOnMessage(chatInfo, "expression"))
                setClickListener(sadAppCompatImageView, MoreActionsForTextActionState.ReactionOnMessage(chatInfo, "sad"))
                setClickListener(prayAppCompatImageView, MoreActionsForTextActionState.ReactionOnMessage(chatInfo, "pray"))
            }
        }

        val marginBottom = context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._142sdp)
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

    fun bind(chatMessageInfo: ChatMessageInfo) {
        chatInfo = chatMessageInfo

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

            if((chatMessageInfo.storyId ?: 0) > 0) {
                replyToStory.isVisible =(chatMessageInfo.story != null)
                ivMediaView.isVisible = (chatMessageInfo.story != null)

                replyToStory.text = resources.getString(R.string.you_replied_to_their_story)

                Glide.with(context)
                    .load(if(chatMessageInfo.story?.image.isNullOrEmpty()) chatMessageInfo.story?.thumbnailUrl else chatMessageInfo.story?.image)
                    .placeholder(R.drawable.venue_placeholder)
                    .into(ivMediaView)
            } else {
                replyToStory.visibility = View.GONE
                ivMediaView.visibility = View.GONE
            }

            if (chatMessageInfo.showDate) {
                dateTextView.text = getFormattedDateForChatMessageHeader(context, chatMessageInfo.createdAt)
                dateTextView.visibility = View.VISIBLE
            } else {
                dateTextView.text = ""
                dateTextView.visibility = View.GONE
            }
            senderAppCompatTextView.text = chatMessageInfo.message

            timeTextView.text = getFormattedTimeForChatMessage(chatMessageInfo.createdAt)

            if (chatMessageInfo.isRead != null) {
                if (chatMessageInfo.isRead == 0) {
                    ivReadStatus.setImageResource(R.drawable.ic_chat_single_tick)
                    ivReadStatus.setColorFilter(ContextCompat.getColor(context, android.R.color.transparent))
                } else {
                    ivReadStatus.setImageResource(R.drawable.ic_chat_double_tick)
                    ivReadStatus.setColorFilter(ContextCompat.getColor(context, R.color.purple))
                }
            } else {
                ivReadStatus.setImageResource(R.drawable.ic_chat_single_tick)
                ivReadStatus.setColorFilter(ContextCompat.getColor(context, android.R.color.transparent))
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