package com.outgoer.ui.livestreamuser.view

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.outgoer.R
import com.outgoer.api.live.model.LiveEventSendOrReadComment
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewLiveEventCommentBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class LiveEventCommentView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val liveStreamCommentViewClicksSubject: PublishSubject<LiveEventSendOrReadComment> = PublishSubject.create()
    val liveStreamCommentViewClicks: Observable<LiveEventSendOrReadComment> = liveStreamCommentViewClicksSubject.hide()

    private lateinit var binding: ViewLiveEventCommentBinding
    private lateinit var liveEventSendOrReadComment: LiveEventSendOrReadComment

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_live_event_comment, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewLiveEventCommentBinding.bind(view)

        binding.apply {
            userNameTextView.throttleClicks().subscribeAndObserveOnMainThread {
                liveStreamCommentViewClicksSubject.onNext(liveEventSendOrReadComment)
            }
        }
    }

    fun bind(liveEventSendOrReadComment: LiveEventSendOrReadComment) {
        this.liveEventSendOrReadComment = liveEventSendOrReadComment
        binding.apply {
            userNameTextView.text = liveEventSendOrReadComment.username ?: ""
            userCommentTextView.text = liveEventSendOrReadComment.comment ?: ""
            ivVerified.isVisible = liveEventSendOrReadComment.profileVerified == 1
        }
    }
}