package com.jamid.codesquare.adapter.recyclerview

import android.view.View
import androidx.core.view.ViewCompat
import com.airbnb.lottie.LottieAnimationView
import com.facebook.drawee.backends.pipeline.Fresco
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.databinding.GridImageLayoutBinding
import com.jamid.codesquare.databinding.SmallImageItemBinding
import com.jamid.codesquare.hide
import com.jamid.codesquare.listeners.CommonImageListener
import com.jamid.codesquare.randomId
import com.jamid.codesquare.show
import com.jamid.codesquare.ui.MediaMessageListener

class MessageImageViewHolder(val v: View, private val listener: MediaMessageListener): MediaMessageViewHolder(v) {

    override fun bind(message: Message) {

        val binding = GridImageLayoutBinding.bind(view)
        val loadingProgress = binding.smallMessageImageProgress

        val commonImageListener = CommonImageListener(loadingProgress)

        loadingProgress.show()

        val builder = Fresco.newDraweeControllerBuilder()
            .setUri(message.metadata?.url)
            .setControllerListener(commonImageListener)

        binding.gridImage.controller = builder.build()

        val transformation = randomId()
        ViewCompat.setTransitionName(binding.gridImage, transformation)

        binding.gridImage.setOnClickListener {
            this.listener.onMessageImageClick(binding.gridImage, message)
        }

    }
}