package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.updateLayoutParams
import com.jamid.codesquare.*
import com.jamid.codesquare.data.MediaItemWrapper
import com.jamid.codesquare.databinding.MediaItemAltBinding
import com.jamid.codesquare.listeners.MediaClickListener

class GridMediaAdapter(
    private val isSelect: Boolean = false,
    private val mediaClickListener: MediaClickListener? = null
): SuperMediaAdapter() {
    init {
        Log.d("Something", "Simple: ")
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuperMediaViewHolder {
        return GridMediaViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.media_item_alt, parent, false),
            isSelect,
            mediaClickListener
        )
    }

    override fun onBindViewHolder(holder: SuperMediaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class GridMediaViewHolder(
    view: View,
    isSelect: Boolean = false,
    mediaClickListener: MediaClickListener? = null
): SuperMediaViewHolder(view, isSelect, mediaClickListener) {

    private val binding: MediaItemAltBinding by lazy { MediaItemAltBinding.bind(view) }

    override fun bind(item: MediaItemWrapper?) {
        super.bind(item)
        val mediaItemWrapper = item ?: return
        val mediaItem = mediaItemWrapper.mediaItem

        val width = getWindowWidth()
        val h = width / 3

        binding.root.updateLayoutParams<ViewGroup.LayoutParams> {
            height = h
        }

        if (mediaItem.thumbnail != null) {
            binding.mediaAltImage.setImageBitmap(mediaItem.thumbnail)
        } else {
            val thumb = binding.root.context.getObjectThumbnail(mediaItem.url.toUri())
            if (thumb != null) {
                binding.mediaAltImage.setImageBitmap(thumb)
            } else {
                binding.mediaAltImage.setImageURI(mediaItem.url.toUri())
            }
        }

        if (mediaItem.type == video) {
            binding.mediaTypeIcon.show()
        } else {
            binding.mediaTypeIcon.hide()
        }

        setSelectText(binding.root, binding.mediaSelectCount)
    }

    companion object {
        private const val TAG = "GridMediaAdapter"
    }

}