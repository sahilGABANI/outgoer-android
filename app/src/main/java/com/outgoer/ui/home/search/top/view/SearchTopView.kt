package com.outgoer.ui.home.search.top.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.view.isVisible
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
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
import com.outgoer.databinding.ViewSearchTopBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SearchTopView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val searchTopClickSubject: PublishSubject<MyTagBookmarkInfo> = PublishSubject.create()
    val searchTopClick: Observable<MyTagBookmarkInfo> = searchTopClickSubject.hide()

    private lateinit var binding: ViewSearchTopBinding
    private lateinit var myTagBookmarkInfo: MyTagBookmarkInfo

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_search_top, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewSearchTopBinding.bind(view)

        binding.apply {
            throttleClicks().subscribeAndObserveOnMainThread {
                searchTopClickSubject.onNext(myTagBookmarkInfo)
            }.autoDispose()
        }
    }

    fun bind(myTagBookmarkInfo: MyTagBookmarkInfo) {
        this.myTagBookmarkInfo = myTagBookmarkInfo
        binding.apply {
            rlSearchView.isVisible  = true
            rlSavePostView.isVisible  = false
            val objectType = myTagBookmarkInfo.objectType

            progressImageLoading.visibility = View.VISIBLE


            if (!objectType.isNullOrEmpty()) {
                if (objectType == MediaObjectType.Reel.type) {

                    Glide.with(context)
                        .asGif()
                        .load(myTagBookmarkInfo.gifthumbnailUrl)
                        .listener(object: RequestListener<GifDrawable> {
                            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<GifDrawable>?, isFirstResource: Boolean): Boolean {
                                progressImageLoading.visibility = View.GONE
                                return false;
                            }

                            override fun onResourceReady(resource: GifDrawable?, model: Any?, target: Target<GifDrawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                                progressImageLoading.visibility = View.GONE
                                return false;
                            }

                        })
                        .into(ivMedia)

                    ivMediaTypeVideo.visibility = View.INVISIBLE
                    ivMultiplePost.visibility = View.INVISIBLE
                    ivReel.visibility = View.VISIBLE

                } else if (objectType == MediaObjectType.POST.type) {

                    val images = myTagBookmarkInfo.images
                    if (!images.isNullOrEmpty()) {
                        ivMediaTypeVideo.visibility = View.INVISIBLE
                        ivMultiplePost.visibility = View.INVISIBLE
                        ivReel.visibility = View.INVISIBLE

                        val photoUrl = if (myTagBookmarkInfo.type == 1) {
                            if (images.size > 1) {
                                ivMultiplePost.visibility = View.VISIBLE
                            }


                            Glide.with(context)
                                .load(images.first().image)
                                .listener(object : RequestListener<Drawable> {
                                    override fun onLoadFailed(p0: GlideException?, p1: Any?, p2: Target<Drawable>?, p3: Boolean): Boolean {
                                        progressImageLoading.visibility = View.GONE
                                        return false;
                                    }

                                    override fun onResourceReady(p0: Drawable?, p1: Any?, p2: Target<Drawable>?, p3: DataSource?, p4: Boolean): Boolean {
                                        progressImageLoading.visibility = View.GONE
                                        return false;
                                    }

                                })
                                .into(ivMedia)

                        } else {
                            ivMediaTypeVideo.visibility = View.VISIBLE


                            Glide.with(context)
                                .asGif()
                                .load(images.first().gifthumbnailUrl)
                                .listener(object: RequestListener<GifDrawable> {
                                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<GifDrawable>?, isFirstResource: Boolean): Boolean {
                                        progressImageLoading.visibility = View.GONE
                                        return false;
                                    }

                                    override fun onResourceReady(resource: GifDrawable?, model: Any?, target: Target<GifDrawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                                        progressImageLoading.visibility = View.GONE
                                        return false;
                                    }

                                })
                                .into(ivMedia)
                        }

                    } else {
                        Glide.with(context)
                            .load(R.drawable.ic_place_holder_post)
                            .into(ivMedia)

                        ivMediaTypeVideo.visibility = View.INVISIBLE
                        ivMultiplePost.visibility = View.INVISIBLE
                        ivReel.visibility = View.INVISIBLE
                    }
                }
            }
        }
    }
    fun bindSavePost(myTagBookmarkInfo: MyTagBookmarkInfo) {
        this.myTagBookmarkInfo = myTagBookmarkInfo
        binding.apply {
            rlSearchView.isVisible  = false
            rlSavePostView.isVisible  = true
            val objectType = myTagBookmarkInfo.objectType

            progressImagePostLoading.visibility = View.VISIBLE


            if (!objectType.isNullOrEmpty()) {
                 if (objectType == MediaObjectType.POST.type) {

                    val images = myTagBookmarkInfo.images
                    if (!images.isNullOrEmpty()) {
//                        ivMediaTypeVideo.visibility = View.INVISIBLE
                        ivMultiplePosts.visibility = View.INVISIBLE
                        ivReels.visibility = View.INVISIBLE

                        val photoUrl = if (myTagBookmarkInfo.type == 1) {
                            if (images.size > 1) {
                                ivMultiplePosts.visibility = View.VISIBLE
                            }


                            Glide.with(context)
                                .load(images.first().image)
                                .listener(object: RequestListener<Drawable> {
                                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                                        progressImagePostLoading.visibility = View.GONE
                                        return false;
                                    }

                                    override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                                        progressImagePostLoading.visibility = View.GONE
                                        return false;
                                    }

                                })
                                .into(ivMediaPost)
                        } else {
                            if (images.size > 1) {
                                ivMultiplePosts.visibility = View.VISIBLE
                            }else {
                                ivReels.visibility = View.VISIBLE
                            }
                            if (images.first().image.isNullOrEmpty()){


                                Glide.with(context)
                                    .asGif()
                                    .load(images.first().gifthumbnailUrl)
                                    .listener(object: RequestListener<GifDrawable> {
                                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<GifDrawable>?, isFirstResource: Boolean): Boolean {
                                            progressImagePostLoading.visibility = View.GONE
                                            return false;
                                        }

                                        override fun onResourceReady(resource: GifDrawable?, model: Any?, target: Target<GifDrawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                                            progressImagePostLoading.visibility = View.GONE
                                            return false;
                                        }

                                    })
                                    .into(ivMediaPost)
                            } else {

                                Glide.with(context)
                                    .load(images.first().image)
                                    .listener(object: RequestListener<Drawable> {
                                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                                            progressImagePostLoading.visibility = View.GONE
                                            return false;
                                        }

                                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                                            progressImagePostLoading.visibility = View.GONE
                                            return false;
                                        }

                                    })
                                    .into(ivMediaPost)
                            }
                        }

                    } else {
                        Glide.with(context)
                            .load(R.drawable.ic_place_holder_post)
                            .into(ivMediaPost)

//                        ivMediaTypeVideo.visibility = View.INVISIBLE
                        ivMultiplePost.visibility = View.INVISIBLE
                        ivReels.visibility = View.INVISIBLE
                    }
                }
            }
        }
    }
}