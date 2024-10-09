package com.outgoer.ui.newvenuedetail.view


import android.app.AlertDialog
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.ContextThemeWrapper
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.outgoer.R
import com.outgoer.api.venue.model.VenueGalleryItem
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewVenueDetailPhotosBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class VenueDetailPhotosView(context: Context) : ConstraintLayoutWithLifecycle(context)  {

    private val venueDetailGalleryViewClickSubject: PublishSubject<VenueGalleryItem> = PublishSubject.create()
    val venueDetailGalleryViewClick: Observable<VenueGalleryItem> = venueDetailGalleryViewClickSubject.hide()

    private val deleteViewClickSubject: PublishSubject<VenueGalleryItem> = PublishSubject.create()
    val deleteViewClick: Observable<VenueGalleryItem> = deleteViewClickSubject.hide()

    private lateinit var binding: ViewVenueDetailPhotosBinding
    private lateinit var venueGalleryItem: VenueGalleryItem

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_venue_detail_photos, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = ViewVenueDetailPhotosBinding.bind(view)

        binding.ivMedia.throttleClicks().subscribeAndObserveOnMainThread {
            venueDetailGalleryViewClickSubject.onNext(venueGalleryItem)
        }.autoDispose()

        binding.deleteAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            val builder = AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialogCustom))
            builder.setTitle(context.getString(R.string.label_delete_))
            builder.setMessage(context.getString(R.string.label_are_you_sure_you_want_to_delete))
            builder.setPositiveButton(context.getString(R.string.delete)) { dialogInterface, which ->
                deleteViewClickSubject.onNext(venueGalleryItem)
                dialogInterface.dismiss()
            }
            builder.setNeutralButton(context.getString(R.string.label_cancel)) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            val alertDialog: AlertDialog = builder.create()
            alertDialog.setCancelable(false)
            alertDialog.show()
        }
    }

    fun bind(venueGalleryItem: VenueGalleryItem, isMyVenue: Boolean) {
        this.venueGalleryItem = venueGalleryItem
        binding.apply {

            println("venueGalleryItem.gifthumbnailUrl: " + venueGalleryItem.gifthumbnailUrl)
            if(!venueGalleryItem.gifthumbnailUrl.isNullOrEmpty()) {
                Glide.with(context)
                    .asGif()
                    .load(venueGalleryItem.gifthumbnailUrl)
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
                    .error(R.drawable.black_background)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .into(binding.ivMedia)
            } else {
                Glide.with(context)
                    .load(venueGalleryItem.media)
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
                    .error(R.drawable.black_background)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .into(binding.ivMedia)
            }

            binding.deleteAppCompatImageView.visibility = if(isMyVenue) View.VISIBLE else View.GONE
        }
    }
}