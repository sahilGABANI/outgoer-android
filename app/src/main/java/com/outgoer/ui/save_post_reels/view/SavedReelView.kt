package com.outgoer.ui.save_post_reels.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.outgoer.R
import com.outgoer.api.post.model.MediaObjectType
import com.outgoer.api.post.model.MyTagBookmarkInfo
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewSavedReelsBinding
import com.outgoer.databinding.ViewSearchTopBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SavedReelView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val searchTopClickSubject: PublishSubject<MyTagBookmarkInfo> = PublishSubject.create()
    val searchTopClick: Observable<MyTagBookmarkInfo> = searchTopClickSubject.hide()

    private lateinit var binding: ViewSavedReelsBinding
    private lateinit var myTagBookmarkInfo: MyTagBookmarkInfo

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_saved_reels, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewSavedReelsBinding.bind(view)

        binding.apply {
            throttleClicks().subscribeAndObserveOnMainThread {
                searchTopClickSubject.onNext(myTagBookmarkInfo)
            }.autoDispose()
        }
    }

    fun bindSavePost(myTagBookmarkInfo: MyTagBookmarkInfo) {
        this.myTagBookmarkInfo = myTagBookmarkInfo
        binding.apply {
            val objectType = myTagBookmarkInfo.objectType
            progressImagePostLoading.visibility = View.VISIBLE
            if (!objectType.isNullOrEmpty()) {
                if (objectType == MediaObjectType.Reel.type) {
                    ivReels.visibility = View.VISIBLE
                    val photoUrl = myTagBookmarkInfo.gifthumbnailUrl
                    Glide.with(context).asGif().load(photoUrl).listener(object : RequestListener<GifDrawable> {
                        override fun onLoadFailed(
                            e: GlideException?, model: Any?, target: Target<GifDrawable>?, isFirstResource: Boolean
                        ): Boolean {
                            progressImagePostLoading.visibility = View.GONE
                            return false;
                        }

                        override fun onResourceReady(
                            resource: GifDrawable?, model: Any?, target: Target<GifDrawable>?, dataSource: DataSource?, isFirstResource: Boolean
                        ): Boolean {
                            progressImagePostLoading.visibility = View.GONE
                            return false;
                        }

                    }).into(ivMediaPost)
                }
            }
        }
    }
}