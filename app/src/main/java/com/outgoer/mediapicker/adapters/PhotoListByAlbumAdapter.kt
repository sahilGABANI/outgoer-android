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
import com.outgoer.mediapicker.interfaces.PhotoListByAlbumAdapterCallback
import com.outgoer.mediapicker.models.PhotoModel
import com.outgoer.ui.addvenuemedia.AddVenueMediaActivity
import com.outgoer.ui.createevent.AddMediaEventActivity
import com.outgoer.ui.post.AddNewPostActivity

class PhotoListByAlbumAdapter(
    private val context: Context,
    private val photoModelArrayList: ArrayList<PhotoModel>,
    private val photoListByAlbumAdapterCallback: PhotoListByAlbumAdapterCallback
) : RecyclerView.Adapter<PhotoListByAlbumAdapter.AdapterVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdapterVH {
        return AdapterVH(LayoutInflater.from(parent.context).inflate(R.layout.item_photo_list_by_album, parent, false))
    }

    override fun getItemCount(): Int {
        return photoModelArrayList.size
    }

    override fun onBindViewHolder(holder: AdapterVH, position: Int) {
        val photoModel = photoModelArrayList[position]

        if (context is AddNewPostActivity) {
            if (AddNewPostActivity.isPhotoSelected(photoModel) == -1) {
                holder.ivSelector.setImageResource(R.drawable.outline_check_box_outline_blank_24)
            } else {
                holder.ivSelector.setImageResource(R.drawable.outline_check_box_24)
            }
        } else if (context is AddVenueMediaActivity) {
            if (AddVenueMediaActivity.isPhotoSelected(photoModel) == -1) {
                holder.ivSelector.setImageResource(R.drawable.outline_check_box_outline_blank_24)
            } else {
                holder.ivSelector.setImageResource(R.drawable.outline_check_box_24)
            }
        } else if (context is AddMediaEventActivity) {
            if (AddMediaEventActivity.isPhotoSelected(photoModel) == -1) {
                holder.ivSelector.setImageResource(R.drawable.outline_check_box_outline_blank_24)
            } else {
                holder.ivSelector.setImageResource(R.drawable.outline_check_box_24)
            }
        }

        Glide.with(context)
            .load(photoModel.uri)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.ivPhoto)
    }

    inner class AdapterVH(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {

        val ivPhoto: PressedImageView = view.findViewById(R.id.ivPhoto)
        val ivSelector: AppCompatImageView = view.findViewById(R.id.ivSelector)

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