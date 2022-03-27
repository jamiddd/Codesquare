package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.comparators.MessageComparator
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.image
import com.jamid.codesquare.ui.MediaMessageListener
import com.jamid.codesquare.ui.MessageListenerFragment

class MediaMessageAdapter(private val listener: MediaMessageListener): ListAdapter<Message, MediaMessageViewHolder>(MessageComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaMessageViewHolder {
        return if (viewType == 0) {
            MessageImageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.grid_image_layout, parent, false), listener)
        } else {
            DocumentViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.document_layout, parent, false),
                listener
            )
        }
    }

    override fun onBindViewHolder(holder: MediaMessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.type == image) {
            0
        } else {
            1
        }
    }

}