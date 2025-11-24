package com.wellycorp.demolottie

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StyleAdapter(
    private val styleList: List<StyleItem>,
    private var selectedStyle: String,
    private val onStyleSelected: (String) -> Unit
) : RecyclerView.Adapter<StyleAdapter.StyleViewHolder>() {

    private var selectedPosition = styleList.indexOfFirst { it.fileName == selectedStyle }

    inner class StyleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val styleImageView: ImageView = itemView.findViewById(R.id.styleImageView)
        val styleNameTextView: TextView = itemView.findViewById(R.id.styleNameTextView)
        val styleRadioButton: RadioButton = itemView.findViewById(R.id.styleRadioButton)

        fun bind(styleItem: StyleItem, position: Int) {
            styleNameTextView.text = styleItem.displayName
            styleRadioButton.isChecked = position == selectedPosition

            // Load thumbnail preview
            try {
                if (styleItem.thumbnailPath != null) {
                    // Load thumbnail from assets
                    val inputStream = itemView.context.assets.open(styleItem.thumbnailPath)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    styleImageView.setImageBitmap(bitmap)
                    inputStream.close()
                } else {
                    // No thumbnail - show placeholder
                    styleImageView.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            } catch (e: Exception) {
                // Error loading thumbnail - show placeholder
                styleImageView.setImageResource(android.R.drawable.ic_menu_gallery)
                e.printStackTrace()
            }

            // Handle click on entire item
            itemView.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = position
                selectedStyle = styleItem.fileName
                
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
                
                onStyleSelected(styleItem.fileName)
            }

            // Handle click on radio button
            styleRadioButton.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = position
                selectedStyle = styleItem.fileName
                
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
                
                onStyleSelected(styleItem.fileName)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StyleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_style, parent, false)
        return StyleViewHolder(view)
    }

    override fun onBindViewHolder(holder: StyleViewHolder, position: Int) {
        holder.bind(styleList[position], position)
    }

    override fun getItemCount(): Int = styleList.size
}
