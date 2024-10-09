package com.outgoer.ui.home.home.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import androidx.core.view.isVisible
import cn.jzvd.JZDataSource
import cn.jzvd.Jzvd
import com.ablanco.zoomy.DoubleTapListener
import com.ablanco.zoomy.LongPressListener
import com.ablanco.zoomy.TapListener
import com.ablanco.zoomy.Zoomy
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.outgoer.R
import com.outgoer.api.post.model.HomePagePostInfoState
import com.outgoer.api.post.model.PostImage
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewHomePagePostMediaBinding
import com.outgoer.videoplayer.JZMediaExoKotlin
import com.outgoer.videoplayer.VideoDoubleClick
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber


class HomePagePostMediaView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val doubleTapSubject: PublishSubject<Unit> = PublishSubject.create()
    val doubleTap: Observable<Unit> = doubleTapSubject.hide()

    private val playVideoSubject: PublishSubject<Unit> = PublishSubject.create()
    val playVideo: Observable<Unit> = playVideoSubject.hide()

    private val mediaPhotoViewClickSubject: PublishSubject<String> = PublishSubject.create()
    val mediaPhotoViewClick: Observable<String> = mediaPhotoViewClickSubject.hide()

    private val mediaVideoViewClickSubject: PublishSubject<HomePagePostInfoState.VideoViewClick> = PublishSubject.create()
    val mediaVideoViewClick: Observable<HomePagePostInfoState.VideoViewClick> = mediaVideoViewClickSubject.hide()

    private lateinit var postImage: PostImage
    private  var position: Int = 0


    private var binding: ViewHomePagePostMediaBinding? = null
    private val colorDrawable = ColorDrawable(Color.parseColor("#616161"))


    init {
        inflateUi()
    }

    @SuppressLint("ClickableViewAccessibility", "LogNotTimber")
    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_home_page_post_media, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        layoutParams.height = layoutParams.width
        binding = ViewHomePagePostMediaBinding.bind(view)


        RxBus.listen(RxEvent.StartVideo::class.java)
            .subscribeOnIoAndObserveOnMainThread({
                Timber.tag("HomePagePostMediaView").i("Get Start Video Callback For position :$position")
                startVideoPlaybackIfVisible(it.checkImage)
            }, {
                Timber.tag("HomePagePostMediaView").i("$it")
            }).autoDispose()

        binding?.apply {
            mutePlayerImageView.throttleClicks().subscribeAndObserveOnMainThread {
                mutePlayerImageView.isSelected = !mutePlayerImageView.isSelected
                if (mutePlayerImageView.isSelected) {
                    outgoerVideoPlayer.isVideMute = true
                    outgoerVideoPlayer.mute()
                    mutePlayerImageView.setImageResource(R.drawable.ic_post_mute)
                } else {
                    outgoerVideoPlayer.isVideMute = false
                    outgoerVideoPlayer.unMute()
                    mutePlayerImageView.setImageResource(R.drawable.ic_post_unmute)
                }
            }.autoDispose()

            outgoerVideoPlayer.posterImageView.throttleClicks().subscribeAndObserveOnMainThread {
                Log.e("inflateUi outgoerVideoPlayer", "${postImage.videoUrl}")
                postImage.videoUrl?.let { it1 -> mediaVideoViewClickSubject.onNext(HomePagePostInfoState.VideoViewClick(it1,postImage.thumbnailUrl)) }
            }.autoDispose()

            videoOutgoerFrameLayout.throttleClicks().subscribeAndObserveOnMainThread {
                Log.e("inflateUi outgoerVideoPlayer", "${postImage.videoUrl}")
                postImage.videoUrl?.let { it1 -> mediaVideoViewClickSubject.onNext(HomePagePostInfoState.VideoViewClick(it1,postImage.thumbnailUrl)) }
            }.autoDispose()

            outgoerVideoPlayer.setVideoDoubleClick(object : VideoDoubleClick {
                override fun onDoubleClick() {
                    postImage.videoUrl?.let { it1 -> doubleTapSubject.onNext(Unit) }
                }

                override fun onSingleClick() {

                }
            })

            photoView.throttleClicks().subscribeAndObserveOnMainThread {
                mediaPhotoViewClickSubject.onNext(postImage.image ?: "")
            }.autoDispose()

        }
    }

    fun bind(postMediaType: Int, postImage: PostImage, deviceHeight: Int, position: Int) {
        //Jzvd.goOnPlayOnPause()
        this.position = position
        this.postImage = postImage
        binding?.apply {


            initZoomyView()

            when (postMediaType) {
                1 -> {
                    setImageView()
                }

                2 -> {
                    setVideoView()
                }

                3 -> {
                    if (!postImage.image.isNullOrEmpty()) {
                        setImageView()
                    } else {
                        setVideoView()
                    }
                }
            }
        }
    }


    private fun setImageView() {
        binding?.photoView?.visibility = View.VISIBLE
        binding?.outgoerVideoPlayer?.visibility = View.GONE
        binding?.videoFrameLayout?.visibility = View.GONE
        binding?.mutePlayerImageView?.visibility = View.GONE
        binding?.progressImageProgressBar?.visibility = View.GONE
        binding?.photoView?.let {
            Glide.with(it.context)
                .load(postImage.image ?: "")
                .preload()

            Glide.with(it.context)
                .load(postImage.image ?: "")
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding?.progressImageProgressBar?.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding?.progressImageProgressBar?.visibility = View.GONE
                        return false
                    }
                })
                .into(it)
        }
    }

    fun setVideoView() {
        Timber.tag("SetVideoView").i("setVideoView() -> postImage.thumbnailUrl: ${postImage.thumbnailUrl}")
        Timber.tag("SetVideoView").i("setVideoView() -> postImage.videoUrl: ${postImage.videoUrl}")
        binding?.photoView?.visibility = View.GONE
        binding?.videoFrameLayout?.visibility = View.VISIBLE
        binding?.outgoerVideoPlayer?.visibility = View.VISIBLE
        binding?.mutePlayerImageView?.visibility = View.VISIBLE
        binding?.mutePlayerImageView?.isSelected = postImage.isMute
        if (postImage.isMute) {
            binding?.mutePlayerImageView?.setImageResource(R.drawable.ic_post_mute)
        } else {
            binding?.mutePlayerImageView?.setImageResource(R.drawable.ic_post_unmute)
        }
        Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE_FILL_PARENT)
        binding?.outgoerVideoPlayer?.posterImageView?.layoutParams?.height =
            ViewGroup.LayoutParams.MATCH_PARENT
        binding?.outgoerVideoPlayer?.posterImageView?.layoutParams?.width =
            ViewGroup.LayoutParams.MATCH_PARENT

        binding?.outgoerVideoPlayer?.posterImageView?.scaleType = ImageView.ScaleType.CENTER
        binding?.feedThumbnailView?.let {
            Glide.with(it.context).load(postImage.thumbnailUrl).diskCacheStrategy(DiskCacheStrategy.ALL).into(it)
        }
        binding?.outgoerVideoPlayer?.posterImageView?.let {
            Glide.with(it.context)
                .load(postImage.thumbnailUrl).diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding?.progressImageProgressBar?.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding?.progressImageProgressBar?.visibility = View.GONE
                        return false
                    }
                })
                .placeholder(colorDrawable)
                .into(it)
        }

        binding?.outgoerVideoPlayer?.apply {
            videoUrl = postImage.videoUrl.plus("?clientBandwidthHint=2.5")
            val jzDataSource = JZDataSource(this.videoUrl)
            jzDataSource.looping = true
            this.setUp(
                jzDataSource,
                Jzvd.SCREEN_TINY,
                JZMediaExoKotlin::class.java
            )
            Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE_FILL_SCROP)
        }

        binding?.outgoerVideoPlayer?.hideScreenProgress()
    }

    private fun initZoomyView() {
        val builder: Zoomy.Builder = Zoomy.Builder(context as Activity)
            .target(binding?.photoView)
            .interpolator(OvershootInterpolator())
            .tapListener(TapListener { v ->

                mediaPhotoViewClickSubject.onNext(postImage.image ?: "")
            })
            .longPressListener(LongPressListener { v ->
//                Toast.makeText(
//                    context, "Long press on "
//                            + v.tag, Toast.LENGTH_SHORT
//                ).show()

            }).doubleTapListener(DoubleTapListener { v ->
                postImage.image?.let { it1 -> doubleTapSubject.onNext(Unit) }
            })
            .animateZooming(false)

        builder.register()
    }

    override fun onDestroy() {
        super.onDestroy()
        playVideoSubject.onNext(Unit)
        binding = null
        Jzvd.releaseAllVideos()

    }

    override fun onResume() {
        Timber.tag("OppoMediaPlayer").i("onResume")
        super.onResume()
    }

    private fun startVideoPlaybackIfVisible(checkImage: Boolean) {
        if (binding?.outgoerVideoPlayer?.isVisible == true && binding?.outgoerVideoPlayer?.state != Jzvd.STATE_PLAYING) {
//            binding?.outgoerVideoPlayer?.startVideoAfterPreloading()
        } else {
            if (checkImage) {
                Jzvd.releaseAllVideos()
            }
        }
    }
}