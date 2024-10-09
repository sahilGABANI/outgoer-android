package com.outgoer.ui.createevent.view


import android.app.AlertDialog
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.ContextThemeWrapper
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.outgoer.R
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.EventAdsMediaViewBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class EventAdsView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val addMediaActionStateSubject: PublishSubject<String> = PublishSubject.create()
    val addMediaActionState: Observable<String> = addMediaActionStateSubject.hide()

    private val mediaActionStateSubject: PublishSubject<String> = PublishSubject.create()
    val mediaActionState: Observable<String> = mediaActionStateSubject.hide()

    private val deleteActionStateSubject: PublishSubject<String> = PublishSubject.create()
    val deleteActionState: Observable<String> = deleteActionStateSubject.hide()

    private var binding: EventAdsMediaViewBinding? = null

    private lateinit var mediaUrl: String

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.event_ads_media_view, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = EventAdsMediaViewBinding.bind(view)

        binding?.apply {
            addLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
                addMediaActionStateSubject.onNext("")
            }.autoDispose()

            mediaLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
                mediaActionStateSubject.onNext(mediaUrl)
            }.autoDispose()

            deleteAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {

                val builder = AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialogCustom))
                builder.setTitle(context.getString(R.string.label_delete_))
                builder.setMessage(context.getString(R.string.label_are_you_sure_you_want_to_delete))
                builder.setPositiveButton(context.getString(R.string.delete)) { dialogInterface, which ->
                    deleteActionStateSubject.onNext(mediaUrl)
                    dialogInterface.dismiss()
                }
                builder.setNeutralButton(context.getString(R.string.label_cancel)) { dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
                val alertDialog: AlertDialog = builder.create()
                alertDialog.setCancelable(false)
                alertDialog.show()
            }.autoDispose()
        }
    }

    fun bindForAdd(media: String) {
        binding?.apply {
            mediaLinearLayout.visibility = View.GONE
            addLinearLayout.visibility = View.VISIBLE
        }
    }


    fun bind(media: String, isAds: Boolean) {
        this.mediaUrl = media
        binding?.apply {
            mediaLinearLayout.visibility = View.VISIBLE
            addLinearLayout.visibility = View.GONE
            videoAppCompatImageView.visibility = if(isAds || media.contains("thumbnails")) View.VISIBLE else View.GONE

            Glide.with(context)
                .setDefaultRequestOptions(RequestOptions().timeout(5000))
                .load(media)
                .placeholder(R.drawable.venue_placeholder)
                .error(R.drawable.venue_placeholder)
                .listener(object : RequestListener<Drawable> {
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
                .into(mediaRoundedImageView)
        }
    }


    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}