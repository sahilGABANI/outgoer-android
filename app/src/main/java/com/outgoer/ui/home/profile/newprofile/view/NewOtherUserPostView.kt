package com.outgoer.ui.home.profile.newprofile.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.outgoer.R
import com.outgoer.api.post.model.PostInfo
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewNewMyPostBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class NewOtherUserPostView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val postViewClickSubject: PublishSubject<PostInfo> = PublishSubject.create()
    val postViewClick: Observable<PostInfo> = postViewClickSubject.hide()

    private lateinit var binding: ViewNewMyPostBinding
    private lateinit var postInfo: PostInfo

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_new_my_post, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewNewMyPostBinding.bind(view)

        binding.apply {
            throttleClicks().subscribeAndObserveOnMainThread {
                postViewClickSubject.onNext(postInfo)
            }.autoDispose()
        }
    }

    fun bind(postInfo: PostInfo) {
        this.postInfo = postInfo
        binding.apply {
            val images = postInfo.images
            if (!images.isNullOrEmpty()) {
                val photoUrl = if (postInfo.type == 1) {
                    ivMediaTypeVideo.visibility = View.INVISIBLE
                    if (images.size > 1) {
                        ivMultipleMedia.visibility = View.VISIBLE
                    } else {
                        ivMultipleMedia.visibility = View.INVISIBLE
                    }

                    Glide.with(context)
                        .load(images.first().image)
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
                        .into(ivMedia)
                } else {
                    ivMediaTypeVideo.visibility = View.VISIBLE
                    ivMultipleMedia.visibility = View.INVISIBLE


                    Glide.with(context)
                        .asGif()
                        .load(images.first().gifthumbnailUrl)
                        .placeholder(R.drawable.black_background)
                        .listener(object: RequestListener<GifDrawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<GifDrawable>?,
                                isFirstResource: Boolean
                            ): Boolean {
                                progressImageLoading.visibility = View.GONE
                                return false;
                            }

                            override fun onResourceReady(
                                resource: GifDrawable?,
                                model: Any?,
                                target: Target<GifDrawable>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ): Boolean {
                                progressImageLoading.visibility = View.GONE
                                return false;
                            }

                        })
                        .into(ivMedia)
                }

                progressImageLoading.visibility = View.VISIBLE
            } else {
                Glide.with(context)
                    .load(R.drawable.ic_place_holder_post)
                    .into(ivMedia)

                ivMultipleMedia.visibility = View.INVISIBLE
                ivMediaTypeVideo.visibility = View.INVISIBLE
            }

        }
    }
}