package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.comparators.MessageComparator
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.databinding.DocumentLayoutBinding
import com.jamid.codesquare.listeners.MessageClickListener

class DocumentAdapter: ListAdapter<Message, DocumentAdapter.DocumentViewHolder>(MessageComparator()) {

    inner class DocumentViewHolder(val view: View): RecyclerView.ViewHolder(view) {

        private val messageListener = view.context as MessageClickListener

        fun bind(message: Message) {

            val binding = DocumentLayoutBinding.bind(view)

            binding.documentNameText.text = message.metadata?.name
            val infoText = "Sent by ${message.sender.name}" + " • " + getTextForTime(message.createdAt) + " • " + getTextForSizeInBytes(message.metadata?.size ?: 0)
            binding.documentInfoText.text = infoText

            view.setOnClickListener {
                messageListener.onDocumentClick(message)
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

                    messageListener.onStartDownload(message) { task, newMessage ->
                        if (task.isSuccessful) {
                            binding.downloadProgressBar.hide()
                            binding.downloadDocumentBtn.hide()

                            view.setOnClickListener {
                                messageListener.onDocumentClick(newMessage)
                            }
                        } else {
                            binding.downloadProgressBar.hide()
                            binding.downloadDocumentBtn.show()

                            view.context.toast("Something went wrong while downloading media.")
                        }
                    }
                }
            } else {

                view.setOnClickListener {
                    messageListener.onDocumentClick(message)
                }

                binding.downloadDocumentBtn.hide()
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        return DocumentViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.document_layout, parent, false))
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}