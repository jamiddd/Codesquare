package com.jamid.codesquare.adapter.recyclerview

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.R

class ImageViewHolderSmall(val view: View, val onClick: (view: View) -> Unit): RecyclerView.ViewHolder(view) {

    private val imageView: ImageView = view.findViewById(R.id.image_small)
    private val removeBtn: Button = view.findViewById(R.id.image_small_remove)

    fun bind(image: Uri) {
        imageView.setImageURI(image)

        removeBtn.setOnClickListener {
            onClick(it)
        }
    }

    companion object {
        fun newInstance(parent: ViewGroup, onClick: (view: View) -> Unit): ImageViewHolderSmall {
            return ImageViewHolderSmall(LayoutInflater.from(parent.context).inflate(R.layout.imageview_layout_small, parent, false), onClick)
        }
    }

}