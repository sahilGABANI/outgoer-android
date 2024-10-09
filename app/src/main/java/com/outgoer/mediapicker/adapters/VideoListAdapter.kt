package com.outgoer.mediapicker.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.mediapicker.custom.SquareImageView
import com.outgoer.mediapicker.interfaces.VideoListAdapterCallback
import com.outgoer.mediapicker.models.VideoModel
import com.outgoer.mediapicker.utils.DateUtils

class VideoListAdapter(
    private val context: Context,
    private val videoModelArrayList: ArrayList<VideoModel>,
    private val videoListAdapterCallback: VideoListAdapterCallback
) : RecyclerView.Adapter<VideoListAdapter.AdapterVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdapterVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video_list, parent, false)
        return AdapterVH(view)
    }

    override fun onBindViewHolder(holder: AdapterVH, position: Int) {
        val videoModelKT = videoModelArrayList[position]
        Glide.with(context)
            .load(videoModelKT.filePath)
            .into(holder.ivVideoThumb)
        holder.tvVideoDuration.text = videoModelKT.duration
        if (videoModelKT.isSelected) {
            holder.ivCheck.visibility = View.VISIBLE
        } else {
            holder.ivCheck.visibility = View.INVISIBLE
        }
    }

    override fun getItemCount(): Int {
        return videoModelArrayList.size
    }

    inner class AdapterVH(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        val ivVideoThumb = view.findViewById(R.id.ivVideoThumb) as SquareImageView
        val tvVideoDuration = view.findViewById(R.id.tvVideoDuration) as AppCompatTextView
        val ivCheck = view.findViewById(R.id.ivCheck) as AppCompatImageView

        init {
            view.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            val mId = p0?.id
            mId?.let {
                videoListAdapterCallback.onVideoListItemClick(adapterPosition)
            }
        }
    }
}