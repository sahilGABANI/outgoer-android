package com.outgoer.ui.home.profile.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.post.model.MediaObjectType
import com.outgoer.api.post.model.MyTagBookmarkInfo
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewMyBookmarkBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class UserBookmarkView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val bookmarkPostViewClickSubject: PublishSubject<MyTagBookmarkInfo> = PublishSubject.create()
    val bookmarkPostViewClick: Observable<MyTagBookmarkInfo> = bookmarkPostViewClickSubject.hide()

    private lateinit var binding: ViewMyBookmarkBinding
    private lateinit var myTagBookmarkInfo: MyTagBookmarkInfo

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_my_bookmark, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewMyBookmarkBinding.bind(view)

        binding.apply {
            throttleClicks().subscribeAndObserveOnMainThread {
                bookmarkPostViewClickSubject.onNext(myTagBookmarkInfo)
            }.autoDispose()
        }
    }

    fun bind(myTagBookmarkInfo: MyTagBookmarkInfo) {
        this.myTagBookmarkInfo = myTagBookmarkInfo
        binding.apply {
            val objectType = myTagBookmarkInfo.objectType
            if (!objectType.isNullOrEmpty()) {
                if (objectType == MediaObjectType.Reel.type) {

                    Glide.with(context)
                        .load(myTagBookmarkInfo.thumbnailUrl)
                        .placeholder(R.drawable.venue_placeholder)
                        .into(binding.ivMedia)

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
                            images.first().image
                        } else {
                            ivMediaTypeVideo.visibility = View.VISIBLE
                            images.first().thumbnailUrl
                        }

                        Glide.with(context)
                            .load(photoUrl)
                            .placeholder(R.drawable.venue_placeholder)
                            .into(ivMedia)

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
}