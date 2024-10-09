package com.outgoer.ui.music.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.R

class AudioWaveAdapter(private var itemList: List<Unit>) : RecyclerView.Adapter<AudioWaveAdapter.WaveformViewHolder>() {


    fun updateItems(items: List<Unit>) {
        itemList = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WaveformViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.audio_wave_item, parent, false)
        return WaveformViewHolder(view)
    }

    override fun onBindViewHolder(holder: WaveformViewHolder, position: Int) {}

    override fun getItemCount(): Int {
        return itemList.size
    }

    class WaveformViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
