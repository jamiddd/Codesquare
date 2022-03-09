package com.jamid.codesquare.adapter.recyclerview

import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.R
import com.jamid.codesquare.data.Metadata
import com.jamid.codesquare.getTextForSizeInBytes
import com.jamid.codesquare.listeners.DocumentClickListener

class DocumentViewHolderSmall(val view: View, private val documentClickListener: DocumentClickListener): RecyclerView.ViewHolder(view) {

    private val name: TextView = view.findViewById(R.id.small_document_name)
    private val size: TextView = view.findViewById(R.id.small_document_size)
    private val removeBtn: Button = view.findViewById(R.id.document_small_remove)

    fun bind(metadata: Metadata) {

        name.text = metadata.name
        if (metadata.size / (1024 * 1024) > 20) {
            val newText = SpannableString("${metadata.name}\n(File size too large)")
            newText.setSpan(ForegroundColorSpan(ContextCompat.getColor(view.context, R.color.error_color)), metadata.name.length + 1, newText.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
            name.text = newText
        }

        size.text = getTextForSizeInBytes(metadata.size)

        removeBtn.setOnClickListener {
            documentClickListener.onCloseBtnClick(it, metadata, absoluteAdapterPosition)
        }

        view.setOnClickListener {
            documentClickListener.onDocumentClick(it, metadata)
        }
    }

    companion object {
        fun newInstance(parent: ViewGroup, documentClickListener: DocumentClickListener): DocumentViewHolderSmall {
            return DocumentViewHolderSmall(LayoutInflater.from(parent.context).inflate(R.layout.document_layout_small, parent, false), documentClickListener)
        }
    }

}