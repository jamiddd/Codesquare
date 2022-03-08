package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.facebook.drawee.backends.pipeline.Fresco
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.comparators.ImageComparator
import com.jamid.codesquare.data.Image
import com.jamid.codesquare.databinding.SmallImageItemBinding
import com.jamid.codesquare.listeners.CommonImageListener
import com.jamid.codesquare.listeners.ImageClickListener
import com.jamid.codesquare.randomId
import com.jamid.codesquare.show

class SmallImagesAdapter(private val imageClickListener: ImageClickListener? = null): ListAdapter<String, SmallImagesAdapter.SmallImageViewHolder>(ImageComparator()) {

    var shouldShowCloseBtn = false
    private var loadingProgress: LottieAnimationView? = null

    inner class SmallImageViewHolder(private val view: View): RecyclerView.ViewHolder(view) {

        fun bind(image: String) {

            val binding = SmallImageItemBinding.bind(view)
            loadingProgress = binding.smallImageLoading

            val listener = CommonImageListener(loadingProgress)

            loadingProgress?.show()

            binding.smallImage.setImageURI(image)

            val builder = Fresco.newDraweeControllerBuilder()
                .setUri(image)
                .setControllerListener(listener)

            binding.smallImage.controller = builder.build()

            val transformation = randomId()
            ViewCompat.setTransitionName(binding.smallImage, transformation)

            binding.smallImage.setOnClickListener {
                imageClickListener?.onImageClick(binding.smallImage, Image(image, listener.finalWidth, listener.finalHeight, transformation))
            }

            if (shouldShowCloseBtn) {
                binding.smallImageCloseBtn.show()
                binding.smallImageCloseBtn.setOnClickListener {
                    imageClickListener?.onCloseBtnClick(binding.smallImage, Image(image, listener.finalWidth, listener.finalHeight, transformation), absoluteAdapterPosition)
                }
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmallImageViewHolder {
        return SmallImageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.small_image_item, parent, false))
    }

    override fun onBindViewHolder(holder: SmallImageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}