package com.outgoer.ui.create_story.view

import android.content.Context
import android.graphics.Color
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.outgoer.R
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.StoryMediaViewBinding
import com.outgoer.ui.create_story.model.SelectedMedia
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.io.File


class StoryMediaView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val storySelectionActionStateSubject: PublishSubject<SelectedMedia> = PublishSubject.create()
    val storySelectionAction: Observable<SelectedMedia> = storySelectionActionStateSubject.hide()

    private var binding: StoryMediaViewBinding? = null

    private lateinit var filePath: SelectedMedia

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.story_media_view, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = StoryMediaViewBinding.bind(view)

        binding?.apply {
            storyRoundedImageView.throttleClicks().subscribeAndObserveOnMainThread {
                storySelectionActionStateSubject.onNext(filePath)
            }
        }
    }
    fun bind(actualPath: SelectedMedia) {

        this.filePath = actualPath
        binding?.apply {
            Timber.tag("PhotoEditorSDK").i("bind() -> actualPath.filePath: ${actualPath.filePath}")
            Glide.with(context)
                .load(File(actualPath.filePath))
                .placeholder(R.drawable.venue_placeholder)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(storyRoundedImageView)

            if(isSelected) {
                storyRoundedImageView.setBorderWidth(2f)
                storyRoundedImageView.setBorderColor(Color.WHITE)
            } else {
                storyRoundedImageView.setBorderWidth(0f)
            }
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}