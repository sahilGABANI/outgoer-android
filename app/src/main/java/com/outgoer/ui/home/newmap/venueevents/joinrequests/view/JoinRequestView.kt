package com.outgoer.ui.home.newmap.venueevents.joinrequests.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.event.model.RequestResponseList
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.JoinRequestItemBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class JoinRequestView(
    context: Context
) : ConstraintLayoutWithLifecycle(context) {

    private val approveActionStateSubject: PublishSubject<RequestResponseList> = PublishSubject.create()
    val approveActionState: Observable<RequestResponseList> = approveActionStateSubject.hide()

    private val rejectActionStateSubject: PublishSubject<RequestResponseList> = PublishSubject.create()
    val rejectActionState: Observable<RequestResponseList> = rejectActionStateSubject.hide()

    private var binding: JoinRequestItemBinding? = null
    private lateinit var requestResponse: RequestResponseList

    init {
        inflateUi()
    }

    private fun inflateUi() {

        val view = View.inflate(context, R.layout.join_request_item, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        binding = JoinRequestItemBinding.bind(view)
        binding?.apply {

            approveMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
                approveActionStateSubject.onNext(requestResponse)
            }.autoDispose()

            closeAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
                rejectActionStateSubject.onNext(requestResponse)
            }.autoDispose()
        }
    }

    fun bind(eventRes: RequestResponseList) {
        this.requestResponse = eventRes
        binding?.apply {
            tvUsername.text = eventRes.name

            Glide.with(context)
                .load(eventRes.avatar)
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .into(ivUserProfile)

            llFollowStatus.visibility = if (eventRes.status == 1) View.GONE else View.VISIBLE

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}