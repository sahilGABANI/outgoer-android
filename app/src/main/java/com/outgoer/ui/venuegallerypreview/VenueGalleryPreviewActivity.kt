package com.outgoer.ui.venuegallerypreview

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import cn.jzvd.JZDataSource
import cn.jzvd.Jzvd
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.venue.model.VenueGalleryItem
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showToast
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivityVenueGalleryPreviewBinding
import com.outgoer.ui.venuegallerypreview.viewmodel.VenueGalleryPreviewViewModel
import com.outgoer.videoplayer.JZMediaExoKotlin
import javax.inject.Inject
import kotlin.properties.Delegates

class VenueGalleryPreviewActivity : BaseActivity() {

    companion object {
        private const val INTENT_EXTRA_VENUE_GALLERY_ITEM = "INTENT_EXTRA_VENUE_GALLERY_ITEM"
        fun getIntent(context: Context, venueGalleryItem: VenueGalleryItem): Intent {
            val intent = Intent(context, VenueGalleryPreviewActivity::class.java)
            intent.putExtra(INTENT_EXTRA_VENUE_GALLERY_ITEM, venueGalleryItem)
            return intent
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<VenueGalleryPreviewViewModel>
    private lateinit var venueGalleryPreviewViewModel: VenueGalleryPreviewViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var loggedInUserId by Delegates.notNull<Int>()

    private lateinit var binding: ActivityVenueGalleryPreviewBinding
    private lateinit var venueGalleryItem: VenueGalleryItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityVenueGalleryPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        OutgoerApplication.component.inject(this)
        venueGalleryPreviewViewModel = getViewModelFromFactory(viewModelFactory)

        loadDataFromIntent()
    }

    private fun loadDataFromIntent() {
        intent?.let {
            if (it.hasExtra(INTENT_EXTRA_VENUE_GALLERY_ITEM)) {
                val venueGalleryItem = it.getParcelableExtra<VenueGalleryItem>(INTENT_EXTRA_VENUE_GALLERY_ITEM)
                if (venueGalleryItem != null) {
                    this.venueGalleryItem = venueGalleryItem
                    listenToViewModel()
                    listenToViewEvent()
                }
            } else {
                onBackPressed()
            }
        } ?: onBackPressed()
    }

    private fun listenToViewModel() {
        venueGalleryPreviewViewModel.venueGalleryPreviewState.subscribeAndObserveOnMainThread {
            when (it) {
                is VenueGalleryPreviewViewModel.VenueGalleryPreviewViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is VenueGalleryPreviewViewModel.VenueGalleryPreviewViewState.LoadingState -> {
                    if (it.isLoading) {
                        binding.progressBar.visibility = View.VISIBLE
                    } else {
                        binding.progressBar.visibility = View.INVISIBLE
                    }
                }
                is VenueGalleryPreviewViewModel.VenueGalleryPreviewViewState.DeleteVenueMediaSuccess -> {
                    finish()
                }

                else -> {}
            }
        }.autoDispose()
    }

    private fun listenToViewEvent() {
        loggedInUserId = loggedInUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0

        if (venueGalleryItem.userId == loggedInUserId) {
            binding.ivMore.visibility = View.VISIBLE
        } else {
            binding.ivMore.visibility = View.GONE
        }

        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()

        binding.ivMore.throttleClicks().subscribeAndObserveOnMainThread {
            val bottomSheetFragment = VenuePreviewMoreOptionBottomSheet()
            bottomSheetFragment.moreOptionItemClick.subscribeAndObserveOnMainThread {
                bottomSheetFragment.dismissBottomSheet()
                venueGalleryPreviewViewModel.deleteVenueMedia(venueGalleryItem.id)
            }.autoDispose()
            bottomSheetFragment.show(supportFragmentManager, VenuePreviewMoreOptionBottomSheet::class.java.name)
        }.autoDispose()

        binding.ivMutePlayer.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.outgoerVideoPlayer.isVideMute) {
                binding.outgoerVideoPlayer.isVideMute = false
                binding.outgoerVideoPlayer.unMute()
                binding.ivMutePlayer.setImageResource(R.drawable.ic_post_unmute)
            } else {
                binding.outgoerVideoPlayer.isVideMute = true
                binding.outgoerVideoPlayer.mute()
                binding.ivMutePlayer.setImageResource(R.drawable.ic_post_mute)
            }
        }

        if (venueGalleryItem.type == 1) {
            binding.ivPreview.visibility = View.VISIBLE
            binding.outgoerVideoPlayer.visibility = View.GONE
            binding.ivMutePlayer.visibility = View.GONE

            Glide.with(this)
                .load(venueGalleryItem.media)
                .placeholder(R.color.color08163C)
                .into(binding.ivPreview)

        } else if (venueGalleryItem.type == 2) {
            binding.ivPreview.visibility = View.GONE
            binding.outgoerVideoPlayer.visibility = View.VISIBLE
            binding.ivMutePlayer.visibility = View.VISIBLE

            binding.outgoerVideoPlayer.apply {
                binding.ivMutePlayer.setImageResource(R.drawable.ic_post_unmute)

                if (!venueGalleryItem.thumbnailUrl.isNullOrEmpty()) {
                    Glide.with(this@VenueGalleryPreviewActivity)
                        .load(venueGalleryItem.thumbnailUrl)
                        .placeholder(R.color.color08163C)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .into(posterImageView)
                }

                val jzDataSource = JZDataSource(venueGalleryItem.videoUrl.plus("?clientBandwidthHint=2.5"))
                jzDataSource.looping = true
                this.setUp(
                    jzDataSource,
                    Jzvd.SCREEN_NORMAL,
                    JZMediaExoKotlin::class.java
                )
//                Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE_FILL_SCROP)
                startVideoAfterPreloading()
            }
        }
    }

    override fun onPause() {
        Jzvd.goOnPlayOnPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        Jzvd.goOnPlayOnResume()
    }

    override fun onDestroy() {
        Jzvd.releaseAllVideos()
        super.onDestroy()
    }
}