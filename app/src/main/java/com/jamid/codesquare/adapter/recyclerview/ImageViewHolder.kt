package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.view.SimpleDraweeView
import com.jamid.codesquare.FrescoImageControllerListener
import com.jamid.codesquare.R

class ImageViewHolder(val view: View, val onClick: (view: View, controllerListener: FrescoImageControllerListener) -> Unit): RecyclerView.ViewHolder(view) {

    private val imageView: SimpleDraweeView = view.findViewById(R.id.project_image)
    private val controllerListener = FrescoImageControllerListener()

    fun bind(image: String) {

        val builder = Fresco.newDraweeControllerBuilder()
            .setUri(image)
            .setControllerListener(controllerListener)

        imageView.controller = builder.build()

        imageView.setOnClickListener {
            onClick(it, controllerListener)
        }
    }

    companion object {
        fun newInstance(parent: ViewGroup, onClick: (view: View, controllerListener: FrescoImageControllerListener) -> Unit): ImageViewHolder {
            return ImageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.imageview_layout, parent, false), onClick)
        }
    }

}