package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.R
import com.jamid.codesquare.data.Metadata
import com.jamid.codesquare.getTextForSizeInBytes

class DocumentViewHolderSmall(val view: View, val onClick: (view: View, position: Int) -> Unit): RecyclerView.ViewHolder(view) {

    private val name: TextView = view.findViewById(R.id.small_document_name)
    private val size: TextView = view.findViewById(R.id.small_document_size)
    private val removeBtn: Button = view.findViewById(R.id.document_small_remove)

    fun bind(metadata: Metadata) {

        name.text = metadata.name
        size.text = getTextForSizeInBytes(metadata.size)

        removeBtn.setOnClickListener {
            onClick(it, layoutPosition)
        }
    }

    companion object {

        private const val TAG = "DocumentViewHolder"

        fun newInstance(parent: ViewGroup, onClick: (view: View, position: Int) -> Unit): DocumentViewHolderSmall {
            return DocumentViewHolderSmall(LayoutInflater.from(parent.context).inflate(R.layout.document_layout_small, parent, false), onClick)
        }

    }

}