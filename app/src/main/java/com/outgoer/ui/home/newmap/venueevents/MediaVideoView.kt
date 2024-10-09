package com.outgoer.ui.home.newmap.venueevents

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.outgoer.R
import com.outgoer.api.event.model.EventData
import com.outgoer.api.post.model.VideoViewClick
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.PhotoMediaViewBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class MediaVideoView (context: Context) : ConstraintLayoutWithLifecycle(context){

    private lateinit var binding: PhotoMediaViewBinding

    private val mediaVideoViewClickSubject: PublishSubject<VideoViewClick> = PublishSubject.create()
    val mediaVideoViewClick: Observable<VideoViewClick> = mediaVideoViewClickSubject.hide()

    private lateinit var eventData: EventData
    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.photo_media_view, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = PhotoMediaViewBinding.bind(view)

        binding.apply {
            throttleClicks().subscribeAndObserveOnMainThread {
                eventData.videoUrl?.let { it1 -> mediaVideoViewClickSubject.onNext(VideoViewClick(it1,eventData.thumbnailUrl)) }
            }.autoDispose()
        }
    }

    fun bind(eventData: EventData) {
        this.eventData = eventData
        Log.e("eventData.videoUrl", "${eventData.videoUrl}")
        binding.apply {
            if (eventData.thumbnailUrl?.length==0)
            {
                mainRelativeLayout.isVisible=false
            }
            Glide.with(context)
                .load(eventData.thumbnailUrl)
                .placeholder(R.drawable.black_background)
                .listener(object: RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressImageLoading.visibility = View.GONE
                        return false;
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressImageLoading.visibility = View.GONE
                        return false;
                    }

                })
                .into(photosAppCompatImageView)
            playAppCompatImageView.isVisible=true
        }
    }
}