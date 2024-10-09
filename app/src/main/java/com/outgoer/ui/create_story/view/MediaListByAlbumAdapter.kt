package com.outgoer.ui.create_story.view

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.outgoer.R
import com.outgoer.mediapicker.custom.PressedImageView
import com.outgoer.mediapicker.interfaces.PhotoListByAlbumAdapterCallback
import com.outgoer.mediapicker.models.PhotoModel
import com.outgoer.ui.create_story.AddToStoryActivity
import java.util.Locale
import java.util.concurrent.TimeUnit

class MediaListByAlbumAdapter(
    private val context: Context,
    private val photoModelArrayList: ArrayList<PhotoModel>,
    private val photoListByAlbumAdapterCallback: PhotoListByAlbumAdapterCallback
) : RecyclerView.Adapter<MediaListByAlbumAdapter.AdapterVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdapterVH {
        return AdapterVH(LayoutInflater.from(parent.context).inflate(R.layout.item_media_list_by_album, parent, false))
    }

    override fun getItemCount(): Int {
        return photoModelArrayList.size
    }

    override fun onBindViewHolder(holder: AdapterVH, position: Int) {
        val photoModel = photoModelArrayList[position]

        if (context is AddToStoryActivity) {
            if (AddToStoryActivity.isPhotoSelected(photoModel) == -1) {
                holder.ivSelector.setImageResource(R.drawable.ic_unselected_media)
            } else {
                holder.ivSelector.setImageResource(R.drawable.ic_selected_media)
            }
        }

        if(photoModel.type?.contains("video") == true) {
//            holder.durationForVideo.visibility = View.VISIBLE
            holder.videInfoAppCompatImageView.visibility = View.VISIBLE
//            val vDuration = getFormattedVideoDuration(photoModel.path)
//            val retriever = MediaMetadataRetriever()
//            retriever.setDataSource(photoModel.path, hashMapOf())
//            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
//
//
//            val minutes = TimeUnit.MILLISECONDS.toMinutes(duration).toString()
//            val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))
//
//            holder.durationForVideo.text = vDuration
//            retriever.release()
        } else {
//            holder.durationForVideo.visibility = View.GONE
            holder.videInfoAppCompatImageView.visibility = View.GONE
        }


        Glide.with(context)
            .load(photoModel.path)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.ivPhoto)
    }

    private fun getFormattedVideoDuration(videoPath: String?): String {
        val videoDuration = getVideoDuration(videoPath)

        // Convert milliseconds to HH:MM:ss format
        var seconds = videoDuration / 1000
        val hours = seconds / 3600
        val minutes = seconds % 3600 / 60
        seconds %= 60

        return when {
            hours > 0 -> String.format(
                Locale.getDefault(),
                "%02d:%02d:%02d",
                hours,
                minutes,
                seconds
            )

            minutes > 0 -> String.format(Locale.getDefault(), "00:%02d:%02d", minutes, seconds)
            else -> String.format(Locale.getDefault(), "00:00:%02d", seconds)
        }
    }

    private fun getVideoDuration(videoPath: String?): Long {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(videoPath)
        val durationString: String =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toString()
        retriever.release()
        return durationString.toLong()
    }

    inner class AdapterVH(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {

        val ivPhoto: PressedImageView = view.findViewById(R.id.ivPhoto)
        val ivSelector: AppCompatImageView = view.findViewById(R.id.ivSelector)
        val durationForVideo: AppCompatTextView = view.findViewById(R.id.durationForVideo)
        val videInfoAppCompatImageView: AppCompatImageView = view.findViewById(R.id.videInfoAppCompatImageView)

        init {
            ivPhoto.setOnClickListener(this)
            ivSelector.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            v?.let {
                photoListByAlbumAdapterCallback.onPhotoItemClick(adapterPosition)
                notifyItemChanged(adapterPosition)
            }
        }
    }
}