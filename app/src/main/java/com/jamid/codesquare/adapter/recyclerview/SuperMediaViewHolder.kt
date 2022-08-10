package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.*
import com.jamid.codesquare.data.MediaItemWrapper
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.listeners.MediaClickListener

abstract class SuperMediaViewHolder(
    val view: View,
    private val isSelect: Boolean = false,
    val mediaClickListener: MediaClickListener? = null
) : RecyclerView.ViewHolder(view) {

    private var mMediaItemWrapper: MediaItemWrapper? = null
    init {
        Log.d("Something", "Simple: ")
    }
    open fun bind(item: MediaItemWrapper?) {
        mMediaItemWrapper = item
    }

    open fun bind(item: Message) {
        val mediaItem = view.context.getMediaItemsFromMessages(listOf(item))[0]
        bind(mediaItem)
    }

    fun setSelectText(container: ViewGroup, selectText: TextView) {
        if (mMediaItemWrapper != null) {
            val mediaItem = mMediaItemWrapper!!.mediaItem
            if (isSelect) {
                if (mediaItem.sizeInBytes < FILE_SIZE_LIMIT) {
                    container.setOnClickListener {
                        mediaClickListener?.onMediaClick(mMediaItemWrapper!!, bindingAdapterPosition)
                    }

                    if (mMediaItemWrapper!!.isSelected && mMediaItemWrapper!!.selectedCount != -1) {
                        selectText.show()
                        selectText.text = mMediaItemWrapper!!.selectedCount.toString()
                    } else {
                        selectText.hide()
                    }

                } else {
                    container.setOnClickListener {}
                }
            } else {

                container.setSelectableItemBackground()

                container.setOnClickListener {
                    mediaClickListener?.onMediaClick(mMediaItemWrapper!!, bindingAdapterPosition)
                }
            }
        } else {
            container.setOnClickListener {
                mediaClickListener?.onMediaClick(mMediaItemWrapper!!, bindingAdapterPosition)
            }
        }
    }

}