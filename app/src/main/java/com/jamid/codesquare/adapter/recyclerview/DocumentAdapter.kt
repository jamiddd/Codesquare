package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.*
import com.jamid.codesquare.data.DocumentHolder
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.listeners.DocumentListener
import com.jamid.codesquare.listeners.MessageListener

class DocumentAdapter: ListAdapter<Message, DocumentAdapter.DocumentViewHolder>(comparator) {

    companion object {
        private val comparator = object : DiffUtil.ItemCallback<Message>() {
            override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
                return oldItem.messageId == newItem.messageId
            }

            override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
                return oldItem == newItem
            }
        }
    }

    inner class DocumentViewHolder(val view: View): RecyclerView.ViewHolder(view) {

        private val messageListener = view.context as MessageListener

        private val documentName = view.findViewById<TextView>(R.id.document_name_text)
        private val documentSize = view.findViewById<TextView>(R.id.document_size_text)
        private val downloadBtn = view.findViewById<Button>(R.id.download_document_btn)
        private val downloadProgress = view.findViewById<ProgressBar>(R.id.download_progress_bar)

        fun bind(message: Message) {
            documentName.text = message.metadata?.name
            documentSize.text = getTextForSizeInBytes(message.metadata?.size ?: 0)

            view.setOnClickListener {
                messageListener.onDocumentClick(message)
            }

            if (!message.isDownloaded) {

                downloadBtn.show()

                downloadBtn.setOnClickListener {
                    downloadBtn.disappear()
                    downloadProgress.show()

                    messageListener.onStartDownload(message) { task, newMessage ->
                        if (task.isSuccessful) {
                            downloadProgress.hide()
                            downloadBtn.hide()

                            view.setOnClickListener {
                                messageListener.onDocumentClick(newMessage)
                            }
                        } else {
                            downloadProgress.hide()
                            downloadBtn.show()

                            view.context.toast("Something went wrong while downloading media.")
                        }
                    }
                }
            } else {

                view.setOnClickListener {
                    messageListener.onDocumentClick(message)
                }

                downloadBtn.hide()
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