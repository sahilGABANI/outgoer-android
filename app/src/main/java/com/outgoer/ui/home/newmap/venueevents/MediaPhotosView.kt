package com.outgoer.ui.home.newmap.venueevents

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.outgoer.R
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.PhotoMediaViewBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class MediaPhotosView (context: Context) : ConstraintLayoutWithLifecycle(context){

    private lateinit var binding: PhotoMediaViewBinding

    private val mediaPhotoViewClickSubject: PublishSubject<String> = PublishSubject.create()
    val mediaPhotoViewClick: Observable<String> = mediaPhotoViewClickSubject.hide()

    private lateinit var imageUrl: String

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.photo_media_view, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = PhotoMediaViewBinding.bind(view)

        binding.apply {
            throttleClicks().subscribeAndObserveOnMainThread {
                mediaPhotoViewClickSubject.onNext(imageUrl)
            }.autoDispose()
        }
    }

    fun bind(imageUrl: String) {
        this.imageUrl = imageUrl
        binding.apply {
            Glide.with(context)
                .load(imageUrl)
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
        }
    }
}