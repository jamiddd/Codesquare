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

class SmallImagesAdapter(private val imageClickListener: ImageClickListener): ListAdapter<String, SmallImageViewHolder>(ImageComparator()) {

    var shouldShowCloseBtn = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmallImageViewHolder {
        return SmallImageViewHolder(imageClickListener, LayoutInflater.from(parent.context).inflate(R.layout.small_image_item, parent, false), this.shouldShowCloseBtn)
    }

    override fun onBindViewHolder(holder: SmallImageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}