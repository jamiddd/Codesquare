package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.jamid.codesquare.R

class GridImagesAdapter: ListAdapter<String, GridImagesAdapter.GridImagesViewHolder>(comparator) {

    companion object {
        private val comparator = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem.length == newItem.length
            }

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }
        }
    }

    inner class GridImagesViewHolder(val view: View): RecyclerView.ViewHolder(view) {

        private val imageHolder = view.findViewById<SimpleDraweeView>(R.id.grid_image)

        fun bind(image: String) {
            imageHolder.setImageURI(image)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridImagesViewHolder {
        return GridImagesViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.grid_image_layout, parent, false))
    }

    override fun onBindViewHolder(holder: GridImagesViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}