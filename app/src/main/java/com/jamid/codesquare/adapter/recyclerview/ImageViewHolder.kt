package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.facebook.drawee.backends.pipeline.Fresco
import com.jamid.codesquare.R
import com.jamid.codesquare.data.Image
import com.jamid.codesquare.databinding.ImageviewLayoutBinding
import com.jamid.codesquare.listeners.CommonImageListener
import com.jamid.codesquare.listeners.ImageClickListener
import com.jamid.codesquare.show

class ImageViewHolder(val view: View, private val imageClickListener: ImageClickListener? = null): RecyclerView.ViewHolder(view) {

    private var loadingProgress: LottieAnimationView? = null


    fun bind(image: String) {

        val binding = ImageviewLayoutBinding.bind(view)
        loadingProgress = binding.imageLoading

        val listener = CommonImageListener(loadingProgress)

        loadingProgress?.show()

        val builder = Fresco.newDraweeControllerBuilder()
            .setUri(image)
            .setControllerListener(listener)

        binding.projectImage.controller = builder.build()

        binding.projectImage.setOnClickListener {
            val i = Image(image, listener.finalWidth, listener.finalHeight, ".jpg")
            imageClickListener?.onImageClick(binding.projectImage, i)
        }
    }

    companion object {
        fun newInstance(parent: ViewGroup, imageClickListener: ImageClickListener? = null): ImageViewHolder {
            return ImageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.imageview_layout, parent, false), imageClickListener)
        }
    }

}