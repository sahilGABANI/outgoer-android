package com.outgoer.ui.videorooms.view

import android.content.Context
import android.view.View
import com.outgoer.R
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewVideoRoomCreateNewBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class VideoRoomCreateNewView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val videoRoomCreateNewClickSubject: PublishSubject<Unit> = PublishSubject.create()
    val videoRoomCreateNewClick: Observable<Unit> = videoRoomCreateNewClickSubject.hide()

    private lateinit var binding: ViewVideoRoomCreateNewBinding

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_video_room_create_new, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = ViewVideoRoomCreateNewBinding.bind(view)

        binding.apply {
            llCreateNew.throttleClicks().subscribeAndObserveOnMainThread {
                videoRoomCreateNewClickSubject.onNext(Unit)
            }.autoDispose()
        }
    }

    fun bind() {
    }
}