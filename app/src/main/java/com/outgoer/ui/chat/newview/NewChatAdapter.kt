package com.outgoer.ui.chat.newview


import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.chat.model.ChatMessageInfo
import com.outgoer.api.chat.model.MessageType
import com.outgoer.api.post.model.MoreActionsForTextActionState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class NewChatAdapter(
    private val context: Context, private val loggedInUserCache: LoggedInUserCache, ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val audioReceiverImgClickSubject: PublishSubject<ChatMessageInfo> = PublishSubject.create()
    val audioReceiverImgViewClick: Observable<ChatMessageInfo> = audioReceiverImgClickSubject.hide()

    private val imageReceiverClickSubject: PublishSubject<ChatMessageInfo> = PublishSubject.create()
    val imageReceiverViewClick: Observable<ChatMessageInfo> = imageReceiverClickSubject.hide()

    private val gifReceiverClickSubject: PublishSubject<ChatMessageInfo> = PublishSubject.create()
    val gifReceiverViewClick: Observable<ChatMessageInfo> = gifReceiverClickSubject.hide()

    private val textReceiverImageClickSubject: PublishSubject<ChatMessageInfo> = PublishSubject.create()
    val textReceiverViewClick: Observable<ChatMessageInfo> = textReceiverImageClickSubject.hide()

    private val chatMediaViewClicksSubject: PublishSubject<ChatMessageInfo> = PublishSubject.create()
    val chatMediaViewClicks: Observable<ChatMessageInfo> = chatMediaViewClicksSubject.hide()

    private val chatForDeleteViewClicksSubject: PublishSubject<ChatMessageInfo> = PublishSubject.create()
    val chatForDeleteViewClicks: Observable<ChatMessageInfo> = chatForDeleteViewClicksSubject.hide()

    private val chatAudioFileViewClicksSubject: PublishSubject<ChatMessageInfo> = PublishSubject.create()
    val chatAudioFileViewClicks: Observable<ChatMessageInfo> = chatAudioFileViewClicksSubject.hide()

    private val chatAudioViewClicksSubject: PublishSubject<Triple<ChatMessageInfo, AppCompatSeekBar, FrameLayout>> = PublishSubject.create()
    val chatAudioViewClicks: Observable<Triple<ChatMessageInfo, AppCompatSeekBar, FrameLayout>> = chatAudioViewClicksSubject.hide()

    private val shareViewClicksSubject: PublishSubject<ChatMessageInfo> = PublishSubject.create()
    val shareViewClicks: Observable<ChatMessageInfo> = shareViewClicksSubject.hide()

    private val textReceiverLongClickSubject: PublishSubject<ChatMessageInfo> = PublishSubject.create()
    val textReceiverLongClick: Observable<ChatMessageInfo> = textReceiverLongClickSubject.hide()

    private val uniqueMessageInfoSet = mutableSetOf<ChatMessageInfo>()
    private var adapterItems = listOf<AdapterItem>()

    private val moreActionViewClicksSubject: PublishSubject<MoreActionsForTextActionState> = PublishSubject.create()
    val moreActionViewClicks: Observable<MoreActionsForTextActionState> = moreActionViewClicksSubject.hide()

    var listOfMessageInfo: ArrayList<ChatMessageInfo>? = null
        set(listOfImages) {
            field = listOfImages
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()
        uniqueMessageInfoSet.clear()
        val userId = loggedInUserCache.getUserId()
        listOfMessageInfo?.forEachIndexed { _, it ->
            if (uniqueMessageInfoSet.add(it)) {
                if ((it.message?.contains("created chat") == true)) {
                    adapterItems.add(AdapterItem.ChatCreated(it))
                } else {
                    if (it.senderId == userId) {
                        when (it.fileType) {
                            MessageType.Text, MessageType.Forward -> {
                                adapterItems.add(AdapterItem.ChatTextSenderViewItem(it))
                            }
                            MessageType.Image -> {
                                adapterItems.add(AdapterItem.ChatMediaSenderViewItem(it))
                            }
                            MessageType.Video -> {
                                adapterItems.add(AdapterItem.ChatVideoMediaSenderViewItem(it))
                            }
                            MessageType.Audio -> {
                                adapterItems.add(AdapterItem.ChatAudioMediaSenderViewItem(it))
                            }
                            MessageType.GIF -> {
                                adapterItems.add(AdapterItem.ChatGIFMediaSenderViewItem(it))
                            }
                            MessageType.Post -> {
                                adapterItems.add(AdapterItem.ChatShareSenderViewItem(it))
                            }
                            MessageType.Reel -> {
                                adapterItems.add(AdapterItem.ChatShareSenderViewItem(it))
                            }
                            MessageType.ChatStarted -> {
                                adapterItems.add(AdapterItem.ChatCreated(it))
                            }
                            MessageType.Reply -> {
                                adapterItems.add(AdapterItem.ChatTextReplySenderViewItem(it))
                            }
                            else -> {
                            }
                        }
                    } else {
                        when (it.fileType) {
                            MessageType.Text, MessageType.Forward -> {
                                adapterItems.add(AdapterItem.ChatTextReceiverViewItem(it))
                            }
                            MessageType.Image -> {
                                adapterItems.add(AdapterItem.ChatMediaReceiverViewItem(it))
                            }
                            MessageType.Video -> {
                                adapterItems.add(AdapterItem.ChatVideoMediaReceiverViewItem(it))
                            }
                            MessageType.Audio -> {
                                adapterItems.add(AdapterItem.ChatAudioMediaReceiverViewItem(it))
                            }
                            MessageType.GIF -> {
                                adapterItems.add(AdapterItem.ChatGIFMediaReceiverViewItem(it))
                            }
                            MessageType.Post -> {
                                adapterItems.add(AdapterItem.ChatShareReceiverViewItem(it))
                            }
                            MessageType.Reel -> {
                                adapterItems.add(AdapterItem.ChatShareReceiverViewItem(it))
                            }
                            MessageType.ChatStarted -> {
                                adapterItems.add(AdapterItem.ChatCreated(it))
                            }
                            MessageType.Reply -> {
                                adapterItems.add(AdapterItem.ChatTextReplyReceiverViewItem(it))
                            }
                            MessageType.Typing -> {
                                adapterItems.add(AdapterItem.ChatTypingReceiverViewItem(it))
                            }
                            else -> {
                            }
                        }
                    }
                }
            }
        }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    @SuppressLint("CheckResult")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.ChatCreatedViewItemType.ordinal -> {
                ImageThumbAdapterViewHolder(NewChatInitView(context))
            }
            ViewType.ChatAudioSenderViewItemType.ordinal -> {
                ImageThumbAdapterViewHolder(NewChatAudioSenderView(context).apply {
                    chatForDeleteViewClicks.subscribe { chatForDeleteViewClicksSubject.onNext(it) }
                    chatAudioViewClicks.subscribe { chatAudioViewClicksSubject.onNext(it) }
                    moreActionViewClicks.subscribe { moreActionViewClicksSubject.onNext(it) }
                })
            }
            ViewType.ChatAudioReceiverViewItemType.ordinal -> {
                ImageThumbAdapterViewHolder(NewChatAudioReciverView(context).apply {
                    audioReceiverImgViewClick.subscribe {
                        audioReceiverImgClickSubject.onNext(it) }
                    chatAudioViewClicks.subscribe { chatAudioViewClicksSubject.onNext(it) }
                    moreActionViewClicks.subscribe { moreActionViewClicksSubject.onNext(it) }
                })
            }
            ViewType.ChatGifSenderViewItemType.ordinal -> {
                ImageThumbAdapterViewHolder(NewChatGIFSenderView(context).apply {
                    chatForDeleteViewClicks.subscribe { chatForDeleteViewClicksSubject.onNext(it) }
                    chatMediaViewClicks.subscribe { chatMediaViewClicksSubject.onNext(it) }
                    moreActionViewClicks.subscribe { moreActionViewClicksSubject.onNext(it) }
                })
            }
            ViewType.ChatGifReceiverViewItemType.ordinal -> {
                ImageThumbAdapterViewHolder(NewChatGIFReciverView(context).apply {
                    chatMediaViewClicks.subscribe { chatMediaViewClicksSubject.onNext(it) }
                    moreActionViewClicks.subscribe { moreActionViewClicksSubject.onNext(it) }
                    gifReceiverViewClick.subscribe {
                        gifReceiverClickSubject.onNext(it)
                    }
                })
            }
            ViewType.ChatTextSenderViewItemType.ordinal -> {
                ImageThumbAdapterViewHolder(NewChatTextSenderView(context).apply {
//                    chatForDeleteViewClicks.subscribe { chatForDeleteViewClicksSubject.onNext(it) }
                    moreActionViewClicks.subscribe { moreActionViewClicksSubject.onNext(it) }
                })
            }
            ViewType.ChatTextReceiverViewItemType.ordinal -> {
                ImageThumbAdapterViewHolder(NewChatTextReceiverView(context).apply {
                    moreActionViewClicks.subscribe { moreActionViewClicksSubject.onNext(it) }
                })
            }
            ViewType.ChatImageSenderViewItemType.ordinal -> {
                ImageThumbAdapterViewHolder(NewChatImageSenderView(context).apply {
                    chatMediaViewClicks.subscribe { chatMediaViewClicksSubject.onNext(it) }
                    chatForDeleteViewClicks.subscribe { chatForDeleteViewClicksSubject.onNext(it) }
                    moreActionViewClicks.subscribe { moreActionViewClicksSubject.onNext(it) }
                })
            }
            ViewType.ChatImageReceiverViewItemType.ordinal -> {
                ImageThumbAdapterViewHolder(NewChatImageReceiverView(context).apply {
                    imageReceiverViewClick.subscribe { imageReceiverClickSubject.onNext(it) }
                    chatMediaViewClicks.subscribe { chatMediaViewClicksSubject.onNext(it) }
                    moreActionViewClicks.subscribe { moreActionViewClicksSubject.onNext(it) }
                })
            }

            ViewType.ChatVideoSenderViewItemType.ordinal -> {
                ImageThumbAdapterViewHolder(NewChatImageSenderView(context).apply {
                    chatMediaViewClicks.subscribe { chatMediaViewClicksSubject.onNext(it) }
                    moreActionViewClicks.subscribe { moreActionViewClicksSubject.onNext(it) }
                    chatForDeleteViewClicks.subscribe { chatForDeleteViewClicksSubject.onNext(it) }
                })
            }
            ViewType.ChatVideoReceiverViewItemType.ordinal -> {
                ImageThumbAdapterViewHolder(NewChatImageReceiverView(context).apply {
                    imageReceiverViewClick.subscribe { imageReceiverClickSubject.onNext(it) }
                    moreActionViewClicks.subscribe { moreActionViewClicksSubject.onNext(it) }
                    chatMediaViewClicks.subscribe { chatMediaViewClicksSubject.onNext(it) }
                })
            }

            ViewType.ChatShareSenderViewItemType.ordinal -> {
                ImageThumbAdapterViewHolder(NewChatShareSenderView(context).apply {
                    shareViewClicks.subscribe { shareViewClicksSubject.onNext(it) }
                    moreActionViewClicks.subscribe { moreActionViewClicksSubject.onNext(it) }
                    chatForDeleteViewClicks.subscribe { chatForDeleteViewClicksSubject.onNext(it) }
                })
            }
            ViewType.ChatShareReceiverViewItemType.ordinal -> {
                ImageThumbAdapterViewHolder(NewChatShareReceiverView(context).apply {
                    shareViewClicks.subscribe { shareViewClicksSubject.onNext(it) }
                    imageReceiverViewClick.subscribe { imageReceiverClickSubject.onNext(it) }
                    moreActionViewClicks.subscribe { moreActionViewClicksSubject.onNext(it) }
                    chatMediaViewClicks.subscribe { chatMediaViewClicksSubject.onNext(it) }
                })
            }
            ViewType.ChatTextReplySenderViewItemType.ordinal -> {
                ImageThumbAdapterViewHolder(NewChatTextReplySenderView(context).apply {
                    moreActionViewClicks.subscribe { moreActionViewClicksSubject.onNext(it) }
                })
            }
            ViewType.ChatTextReplyReceiverViewItemType.ordinal -> {
                ImageThumbAdapterViewHolder(NewChatTextReplyReceiverView(context).apply {
                    moreActionViewClicks.subscribe { moreActionViewClicksSubject.onNext(it) }
                })
            }
            ViewType.ChatTypingReceiverViewItemType.ordinal -> {
                ImageThumbAdapterViewHolder(NewChatTypingReceiverView(context))
            }
            else -> throw IllegalArgumentException("Unsupported ViewType")
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.ChatCreated -> {
                (holder.itemView as NewChatInitView).bind(adapterItem.chatMessage)
            }
            is AdapterItem.ChatAudioMediaReceiverViewItem -> {
                (holder.itemView as NewChatAudioReciverView).bind(adapterItem.chatMessage)
            }
            is AdapterItem.ChatAudioMediaSenderViewItem -> {
                (holder.itemView as NewChatAudioSenderView).bind(adapterItem.chatMessage)
            }
            is AdapterItem.ChatGIFMediaReceiverViewItem -> {
                (holder.itemView as NewChatGIFReciverView).bind(adapterItem.chatMessage)
            }
            is AdapterItem.ChatGIFMediaSenderViewItem -> {
                (holder.itemView as NewChatGIFSenderView).bind(adapterItem.chatMessage)
            }
            is AdapterItem.ChatMediaReceiverViewItem -> {
                (holder.itemView as NewChatImageReceiverView).bind(adapterItem.chatMessage)
            }
            is AdapterItem.ChatMediaSenderViewItem -> {
                (holder.itemView as NewChatImageSenderView).bind(adapterItem.chatMessage)
            }
            is AdapterItem.ChatVideoMediaReceiverViewItem -> {
                (holder.itemView as NewChatImageReceiverView).bind(adapterItem.chatMessage)
            }
            is AdapterItem.ChatVideoMediaSenderViewItem -> {
                (holder.itemView as NewChatImageSenderView).bind(adapterItem.chatMessage)
            }

            is AdapterItem.ChatTextReceiverViewItem -> {
                (holder.itemView as NewChatTextReceiverView).bind(adapterItem.chatMessage)
            }
            is AdapterItem.ChatTextSenderViewItem -> {
                (holder.itemView as NewChatTextSenderView).bind(adapterItem.chatMessage)
            }

            is AdapterItem.ChatShareSenderViewItem -> {
                (holder.itemView as NewChatShareSenderView).bind(adapterItem.chatMessage)
            }
            is AdapterItem.ChatShareReceiverViewItem -> {
                (holder.itemView as NewChatShareReceiverView).bind(adapterItem.chatMessage)
            }
            is AdapterItem.ChatTextReplySenderViewItem -> {
                (holder.itemView as NewChatTextReplySenderView).bind(adapterItem.chatMessage)
            }
            is AdapterItem.ChatTextReplyReceiverViewItem -> {
                (holder.itemView as NewChatTextReplyReceiverView).bind(adapterItem.chatMessage)
            }
            is AdapterItem.ChatTypingReceiverViewItem -> {
                (holder.itemView as NewChatTypingReceiverView).bind(adapterItem.chatMessage)
            }
        }
    }

    private class ImageThumbAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class ChatCreated(var chatMessage: ChatMessageInfo) :
            AdapterItem(ViewType.ChatCreatedViewItemType.ordinal)

        data class ChatTextSenderViewItem(var chatMessage: ChatMessageInfo) :
            AdapterItem(ViewType.ChatTextSenderViewItemType.ordinal)

        data class ChatTextReceiverViewItem(var chatMessage: ChatMessageInfo) :
            AdapterItem(ViewType.ChatTextReceiverViewItemType.ordinal)

        data class ChatMediaSenderViewItem(var chatMessage: ChatMessageInfo) :
            AdapterItem(ViewType.ChatImageSenderViewItemType.ordinal)

        data class ChatMediaReceiverViewItem(var chatMessage: ChatMessageInfo) :
            AdapterItem(ViewType.ChatImageReceiverViewItemType.ordinal)

        data class ChatGIFMediaSenderViewItem(var chatMessage: ChatMessageInfo) :
            AdapterItem(ViewType.ChatGifSenderViewItemType.ordinal)

        data class ChatGIFMediaReceiverViewItem(var chatMessage: ChatMessageInfo) :
            AdapterItem(ViewType.ChatGifReceiverViewItemType.ordinal)

        data class ChatAudioMediaSenderViewItem(var chatMessage: ChatMessageInfo) :
            AdapterItem(ViewType.ChatAudioSenderViewItemType.ordinal)

        data class ChatAudioMediaReceiverViewItem(var chatMessage: ChatMessageInfo) :
            AdapterItem(ViewType.ChatAudioReceiverViewItemType.ordinal)

        data class ChatVideoMediaSenderViewItem(var chatMessage: ChatMessageInfo) :
            AdapterItem(ViewType.ChatVideoSenderViewItemType.ordinal)

        data class ChatVideoMediaReceiverViewItem(var chatMessage: ChatMessageInfo) :
            AdapterItem(ViewType.ChatVideoReceiverViewItemType.ordinal)

        data class ChatShareSenderViewItem(var chatMessage: ChatMessageInfo) :
            AdapterItem(ViewType.ChatShareSenderViewItemType.ordinal)

        data class ChatShareReceiverViewItem(var chatMessage: ChatMessageInfo) :
            AdapterItem(ViewType.ChatShareReceiverViewItemType.ordinal)

        data class ChatTextReplySenderViewItem(var chatMessage: ChatMessageInfo) :
            AdapterItem(ViewType.ChatTextReplySenderViewItemType.ordinal)

        data class ChatTextReplyReceiverViewItem(var chatMessage: ChatMessageInfo) :
            AdapterItem(ViewType.ChatTextReplyReceiverViewItemType.ordinal)

        data class ChatTypingReceiverViewItem(var chatMessage: ChatMessageInfo) :
            AdapterItem(ViewType.ChatTypingReceiverViewItemType.ordinal)
    }

    private enum class ViewType {
        ChatCreatedViewItemType,
        ChatTextSenderViewItemType,
        ChatTextReceiverViewItemType,
        ChatImageSenderViewItemType,
        ChatImageReceiverViewItemType,
        ChatVideoSenderViewItemType,
        ChatVideoReceiverViewItemType,
        ChatGifSenderViewItemType,
        ChatGifReceiverViewItemType,
        ChatAudioSenderViewItemType,
        ChatAudioReceiverViewItemType,
        ChatShareSenderViewItemType,
        ChatShareReceiverViewItemType,
        ChatTextReplySenderViewItemType,
        ChatTextReplyReceiverViewItemType,
        ChatTypingReceiverViewItemType
    }
}