package com.outgoer.mediapicker.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.outgoer.R
import com.outgoer.mediapicker.custom.PressedImageView
import com.outgoer.mediapicker.interfaces.VideoListByAlbumAdapterCallback
import com.outgoer.mediapicker.models.VideoModel
import com.outgoer.ui.addvenuemedia.AddVenueMediaActivity
import com.outgoer.ui.createevent.AddMediaEventActivity
import com.outgoer.ui.post.AddNewPostActivity

class VideoListByAlbumAdapter(
    private val context: Context,
    private val videoModelArrayList: ArrayList<VideoModel>,
    private val videoListByAlbumAdapterCallback: VideoListByAlbumAdapterCallback
) : RecyclerView.Adapter<VideoListByAlbumAdapter.AdapterVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdapterVH {
        return AdapterVH(LayoutInflater.from(parent.context).inflate(R.layout.item_video_list_by_album, parent, false))
    }

    override fun getItemCount(): Int {
        return videoModelArrayList.size
    }

    override fun onBindViewHolder(holder: AdapterVH, position: Int) {
        val videoModel = videoModelArrayList[position]

        if (context is AddNewPostActivity) {
            if (AddNewPostActivity.isVideoSelected(videoModel) == -1) {
                holder.ivSelector.setImageResource(R.drawable.outline_check_box_outline_blank_24)
            } else {
                holder.ivSelector.setImageResource(R.drawable.outline_check_box_24)
            }
        } else if (context is AddVenueMediaActivity) {
            if (AddVenueMediaActivity.isVideoSelected(videoModel) == -1) {
                holder.ivSelector.setImageResource(R.drawable.outline_check_box_outline_blank_24)
            } else {
                holder.ivSelector.setImageResource(R.drawable.outline_check_box_24)
            }
        } else if (context is AddMediaEventActivity) {
            if (AddMediaEventActivity.isVideoSelected(videoModel) == -1) {
                holder.ivSelector.setImageResource(R.drawable.outline_check_box_outline_blank_24)
            } else {
                holder.ivSelector.setImageResource(R.drawable.outline_check_box_24)
            }
        }

        Glide.with(context)
            .load(videoModel.filePath)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.ivVideo)
    }

    inner class AdapterVH(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {

        val ivVideo: PressedImageView = view.findViewById(R.id.ivVideo)
        val ivSelector: AppCompatImageView = view.findViewById(R.id.ivSelector)

        init {
            ivVideo.setOnClickListener(this)
            ivSelector.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            v?.let {
                videoListByAlbumAdapterCallback.onVideoItemClick(adapterPosition)
                notifyItemChanged(adapterPosition)
            }
        }
    }
}