package com.outgoer.ui.home.newmap.venueevents.view

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.slider.SliderItem
import com.smarteist.autoimageslider.SliderViewAdapter
import timber.log.Timber

class SliderAdapter(private val context: Context): SliderViewAdapter<SliderAdapter.SliderAdapterVH>() {

    private var mSliderItems: MutableList<SliderItem> = ArrayList()

    fun renewItems(sliderItems: MutableList<SliderItem>) {
        Timber.tag("sliderItems").e("${sliderItems.size}")
        mSliderItems = sliderItems
        notifyDataSetChanged()
    }

    /*
    fun deleteItem(position: Int) {
        mSliderItems.removeAt(position)
        notifyDataSetChanged()
    }

    fun addItem(sliderItem: SliderItem) {
        mSliderItems.add(sliderItem)
        notifyDataSetChanged()
    }
     */

    override fun onCreateViewHolder(parent: ViewGroup): SliderAdapterVH {
        val inflate: View =
            LayoutInflater.from(parent.context).inflate(R.layout.image_slider_layout_item, parent,false)
        return SliderAdapterVH(inflate)
    }

    override fun onBindViewHolder(viewHolder: SliderAdapterVH, position: Int) {
        val (description, imageUrl) = mSliderItems[position]
        viewHolder.textViewDescription.text = description
        viewHolder.textViewDescription.textSize = 16f
        viewHolder.textViewDescription.setTextColor(Color.WHITE)
        Glide.with(viewHolder.itemView)
            .load(imageUrl)
            .fitCenter()
            .into(viewHolder.imageViewBackground)

        viewHolder.itemView.setOnClickListener {
            Timber.tag("GGG").d("onIndicatorClicked: %s", position)
            Toast.makeText(context, "This is item in position $position", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getCount(): Int {
        //slider view count could be dynamic size
        return mSliderItems.size
    }

    class SliderAdapterVH(itemView: View) : ViewHolder(itemView) {
        private var itemViews: View
        var imageViewBackground: ImageView
        private var imageGifContainer: ImageView
        var textViewDescription: TextView

        init {
            imageViewBackground = itemView.findViewById(R.id.iv_auto_image_slider)
            imageGifContainer = itemView.findViewById(R.id.iv_gif_container)
            textViewDescription = itemView.findViewById(R.id.tv_auto_image_slider)
            this.itemViews = itemView
        }
    }
}