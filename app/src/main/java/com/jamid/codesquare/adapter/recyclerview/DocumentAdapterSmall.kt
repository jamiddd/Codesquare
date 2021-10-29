package com.jamid.codesquare.adapter.recyclerview

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.jamid.codesquare.data.Metadata

class DocumentAdapterSmall(private val onRemoveClick: (v: View, position: Int) -> Unit): ListAdapter<Metadata, DocumentViewHolderSmall>(comparator) {

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
        return DocumentViewHolderSmall.newInstance(parent, onRemoveClick)
    }

    override fun onBindViewHolder(holder: DocumentViewHolderSmall, position: Int) {
        holder.bind(getItem(position))
    }

}