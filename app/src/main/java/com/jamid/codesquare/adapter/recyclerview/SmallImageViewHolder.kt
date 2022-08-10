package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.View
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.facebook.drawee.backends.pipeline.Fresco
import com.jamid.codesquare.data.Image
import com.jamid.codesquare.databinding.SmallImageItemBinding
import com.jamid.codesquare.listeners.CommonImageListener
import com.jamid.codesquare.listeners.ImageClickListener
import com.jamid.codesquare.randomId
import com.jamid.codesquare.show

class SmallImageViewHolder(
    private val imageClickListener: ImageClickListener,
    private val view: View,
    private val shouldShowCloseBtn: Boolean
): RecyclerView.ViewHolder(view) {
    init {
        Log.d("Something", "Simple: ")
    }
    private lateinit var loadingProgress: LottieAnimationView

    fun bind(image: String) {

        val binding = SmallImageItemBinding.bind(view)
        loadingProgress = binding.smallImageLoading

        val listener = CommonImageListener(loadingProgress)

        loadingProgress.show()

        binding.smallImage.setImageURI(image)

        val builder = Fresco.newDraweeControllerBuilder()
            .setUri(image)
            .setControllerListener(listener)

        binding.smallImage.controller = builder.build()

        val transformation = randomId()
        ViewCompat.setTransitionName(binding.smallImage, transformation)

        binding.smallImage.setOnClickListener {
            imageClickListener.onImageClick(binding.smallImage, Image(image, listener.finalWidth, listener.finalHeight, transformation))
        }

        if (shouldShowCloseBtn) {
            binding.smallImageCloseBtn.show()
            binding.smallImageCloseBtn.setOnClickListener {
                imageClickListener.onCloseBtnClick(binding.smallImage, Image(image, listener.finalWidth, listener.finalHeight, transformation), absoluteAdapterPosition)
            }
        }

    }
}