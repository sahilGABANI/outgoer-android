package com.outgoer.ui.create_story.view

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.R

class ColorPickerAdapter(private val context: Context, private val colorPickerColors: List<Int>) :
    RecyclerView.Adapter<ColorPickerAdapter.ViewHolder>() {
    private var onColorPickerClickListener: OnColorPickerClickListener? = null

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        buildColorPickerView(holder.colorPickerView, colorPickerColors!![position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View =
            LayoutInflater.from(context).inflate(R.layout.color_picker_item_list, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return colorPickerColors!!.size
    }

    private fun buildColorPickerView(view: View, colorCode: Int) {
        view.visibility = View.VISIBLE
        val biggerCircle = ShapeDrawable(OvalShape())
        biggerCircle.intrinsicHeight = 20
        biggerCircle.intrinsicWidth = 20
        biggerCircle.bounds = Rect(0, 0, 8, 12)
        biggerCircle.paint.color = colorCode
        val smallerCircle = ShapeDrawable(OvalShape())
        smallerCircle.intrinsicHeight = 5
        smallerCircle.intrinsicWidth = 5
        smallerCircle.bounds = Rect(0, 0, 5, 5)
        smallerCircle.paint.color = Color.WHITE
        smallerCircle.setPadding(4, 4, 4, 4)
        val drawables = arrayOf<Drawable>(smallerCircle, biggerCircle)
        val layerDrawable = LayerDrawable(drawables)
        view.setBackgroundDrawable(layerDrawable)
    }

    fun setOnColorPickerClickListener(onColorPickerClickListener: OnColorPickerClickListener?) {
        this.onColorPickerClickListener = onColorPickerClickListener
    }


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var colorPickerView: View

        init {
            colorPickerView = itemView.findViewById<View>(R.id.color_picker_view)
            itemView.setOnClickListener {
                if (onColorPickerClickListener != null)
                    onColorPickerClickListener?.onColorPickerClickListener(
                        colorPickerColors.get(adapterPosition)
                    )
            }
        }
    }


    interface OnColorPickerClickListener {
        fun onColorPickerClickListener(colorCode: Int)
    }

}