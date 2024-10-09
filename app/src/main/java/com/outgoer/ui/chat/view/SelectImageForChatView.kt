package com.outgoer.ui.chat.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewSelectImageForChatBinding
import com.outgoer.mediapicker.models.PhotoModel
import com.outgoer.mediapicker.models.VideoModel
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SelectImageForChatView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val imageClickSubject: PublishSubject<PhotoModel> = PublishSubject.create()
    val imageClick: Observable<PhotoModel> = imageClickSubject.hide()

    private val videoClickSubject: PublishSubject<VideoModel> = PublishSubject.create()
    val videoClick: Observable<VideoModel> = videoClickSubject.hide()

    private lateinit var binding: ViewSelectImageForChatBinding
    private var photoModel: PhotoModel? = null
    private var videoModel: VideoModel? = null

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_select_image_for_chat, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewSelectImageForChatBinding.bind(view)

        binding.apply {
            throttleClicks().subscribeAndObserveOnMainThread {

                videoModel?.let {
                    videoClickSubject.onNext(it)
                }

                photoModel?.let {
                    imageClickSubject.onNext(it)
                }

            }.autoDispose()
        }
    }

    fun bind(photoModel: PhotoModel) {
        this.photoModel = photoModel
        binding.apply {
            Glide.with(context)
                .load(photoModel.path)
                .placeholder(R.drawable.ic_place_holder_post)
                .centerCrop()
                .into(ivMedia)

            playAppCompatImageView.visibility = View.GONE
        }
    }

    fun bindVideo(videoModel: VideoModel) {
        this.videoModel = videoModel
        binding.apply {
            Glide.with(context)
                .load(videoModel.filePath)
                .placeholder(R.drawable.ic_place_holder_post)
                .centerCrop()
                .into(ivMedia)

            playAppCompatImageView.visibility = View.VISIBLE
        }
    }
}