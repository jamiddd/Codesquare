package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.jamid.codesquare.data.Metadata
import com.jamid.codesquare.listeners.DocumentClickListener

class SmallDocumentsAdapter(private val documentClickListener: DocumentClickListener): ListAdapter<Metadata, DocumentViewHolderSmall>(comparator) {
    init {
        Log.d("Something", "Simple: ")
    }
    companion object {
        val comparator = object : DiffUtil.ItemCallback<Metadata>() {
            override fun areItemsTheSame(oldItem: Metadata, newItem: Metadata): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: Metadata, newItem: Metadata): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolderSmall {
        return DocumentViewHolderSmall.newInstance(parent, documentClickListener)
    }

    override fun onBindViewHolder(holder: DocumentViewHolderSmall, position: Int) {
        holder.bind(getItem(position))
    }

}