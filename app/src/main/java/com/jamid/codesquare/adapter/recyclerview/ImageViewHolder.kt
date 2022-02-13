package com.jamid.codesquare.adapter.recyclerview

import android.graphics.drawable.Animatable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.imagepipeline.image.ImageInfo
import com.jamid.codesquare.R
import com.jamid.codesquare.databinding.ImageviewLayoutBinding
import com.jamid.codesquare.hide
import com.jamid.codesquare.show

class ImageViewHolder(val view: View, val onClick: (view: View, controllerListener: BaseControllerListener<ImageInfo>) -> Unit): RecyclerView.ViewHolder(view) {

    private var loadingProgress: LottieAnimationView? = null

    inner class MyImageListener: BaseControllerListener<ImageInfo>() {

        var finalWidth = 0
        var finalHeight = 0

        override fun onFinalImageSet(id: String?, imageInfo: ImageInfo?, animatable: Animatable?) {
            super.onFinalImageSet(id, imageInfo, animatable)

            loadingProgress?.hide()

            if (imageInfo != null) {
                finalWidth = imageInfo.width
                finalHeight = imageInfo.height
            }

        }

        override fun onFailure(id: String?, throwable: Throwable?) {
            super.onFailure(id, throwable)

            loadingProgress?.hide()

        }
    }

    fun bind(image: String) {

        val listener = MyImageListener()
        val binding = ImageviewLayoutBinding.bind(view)

        loadingProgress = binding.imageLoading
        loadingProgress?.show()

        val builder = Fresco.newDraweeControllerBuilder()
            .setUri(image)
            .setControllerListener(listener)

        binding.projectImage.controller = builder.build()

        binding.projectImage.setOnClickListener {
            onClick(it, listener)
        }
    }

    companion object {
        fun newInstance(parent: ViewGroup, onClick: (view: View, controllerListener: BaseControllerListener<ImageInfo>) -> Unit): ImageViewHolder {
            return ImageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.imageview_layout, parent, false), onClick)
        }
    }

}