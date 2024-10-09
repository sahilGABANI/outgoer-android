package com.outgoer.ui.home.profile.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.post.model.PostInfo
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewMyPostBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class UserPostView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val postViewClickSubject: PublishSubject<PostInfo> = PublishSubject.create()
    val postViewClick: Observable<PostInfo> = postViewClickSubject.hide()

    private lateinit var binding: ViewMyPostBinding
    private lateinit var postInfo: PostInfo

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_my_post, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewMyPostBinding.bind(view)

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
                    images.first().image
                } else {
                    ivMediaTypeVideo.visibility = View.VISIBLE
                    ivMultipleMedia.visibility = View.INVISIBLE
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

                ivMultipleMedia.visibility = View.INVISIBLE
                ivMediaTypeVideo.visibility = View.INVISIBLE
            }
        }
    }
}