package com.jamid.codesquare.adapter.recyclerview

import android.view.View
import androidx.core.content.ContextCompat
import com.jamid.codesquare.*
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.databinding.DocumentLayoutBinding
import com.jamid.codesquare.ui.MediaMessageListener

class DocumentViewHolder(val v: View, val listener: MediaMessageListener): MediaMessageViewHolder(v) {

    override fun bind(message: Message) {

        val binding = DocumentLayoutBinding.bind(view)

        binding.documentNameText.text = message.metadata?.name
        val infoText = "Sent by ${message.sender.name}" + " • " + getTextForTime(message.createdAt) + " • " + getTextForSizeInBytes(message.metadata?.size ?: 0)
        binding.documentInfoText.text = infoText

        view.setOnClickListener {
            listener.onMessageDocumentClick(message)
        }

        val icon = when (message.metadata?.ext) {
            ".pdf" -> ContextCompat.getDrawable(view.context, R.drawable.ic_pdf)
            ".docx" -> ContextCompat.getDrawable(view.context, R.drawable.ic_docx)
            ".pptx" -> ContextCompat.getDrawable(view.context, R.drawable.ic_pptx)
            else -> ContextCompat.getDrawable(view.context, R.drawable.ic_document)
        }

        binding.documentImg.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null)

        if (!message.isDownloaded) {

            binding.downloadDocumentBtn.show()

            binding.downloadDocumentBtn.setOnClickListener {
                binding.downloadDocumentBtn.disappear()
                binding.downloadProgressBar.show()

                listener.onMessageNotDownloaded(message) { newMessage ->
                    bind(newMessage)
                }
            }
        } else {

            view.setOnClickListener {
                listener.onMessageDocumentClick(message)
            }

            binding.downloadDocumentBtn.hide()
        }

    }
}