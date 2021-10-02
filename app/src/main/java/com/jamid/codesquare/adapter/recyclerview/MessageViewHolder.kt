package com.jamid.codesquare.adapter.recyclerview

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.data.Message

class MessageViewHolder(val view: View): RecyclerView.ViewHolder(view) {

    fun bind(message: Message?) {
        TODO()
    }

    companion object {
        fun newInstance(parent: ViewGroup): MessageViewHolder {
            TODO()
        }
    }

}