package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.material.button.MaterialButton
import com.jamid.codesquare.R

class ImageViewHolder(val view: View, val onClick: (view: View) -> Unit): RecyclerView.ViewHolder(view) {

    private val imageView: SimpleDraweeView = view.findViewById(R.id.project_image)

    fun bind(image: String) {
        imageView.setImageURI(image)

        imageView.setOnClickListener {
            onClick(it)
        }
    }

    companion object {

        private const val TAG = "ImageViewHolder"

        fun newInstance(parent: ViewGroup, onClick: (view: View) -> Unit): ImageViewHolder {
            return ImageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.imageview_layout, parent, false), onClick)
        }

    }

}