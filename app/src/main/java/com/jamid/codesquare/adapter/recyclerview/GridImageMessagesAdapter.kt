package com.jamid.codesquare.adapter.recyclerview

import android.net.Uri
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.view.SimpleDraweeView
import com.jamid.codesquare.R
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.hide
import com.jamid.codesquare.listeners.MessageListener
import com.jamid.codesquare.show
import com.jamid.codesquare.toast
import java.io.File

class GridImageMessagesAdapter: ListAdapter<Message, GridImageMessagesAdapter.GridImageMessageViewHolder>(comparator) {

    companion object {

        private const val TAG = "GridImageMessage"

        private val comparator = object : DiffUtil.ItemCallback<Message>() {
            override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
                return oldItem.messageId == newItem.messageId
            }

            override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
                return oldItem == newItem
            }
        }
    }

    inner class GridImageMessageViewHolder(val view: View): RecyclerView.ViewHolder(view) {

        private val imageHolder = view.findViewById<SimpleDraweeView>(R.id.grid_image)
        private val imagesDir = view.context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        private val progress = view.findViewById<ProgressBar>(R.id.small_message_image_progress)
        private val messageListener = view.context as MessageListener

        fun bind(message: Message) {

            ViewCompat.setTransitionName(view, message.content)

            if (message.isDownloaded) {
                progress.hide()
                val name = message.content + message.metadata!!.ext
                val destination = File(imagesDir, message.chatChannelId)
                val file = File(destination, name)
                val uri = Uri.fromFile(file)

                if (message.metadata!!.ext == ".webp" || message.metadata!!.ext == ".gif") {
                    val controller = Fresco.newDraweeControllerBuilder()
                        .setUri(uri)
                        .setAutoPlayAnimations(true)
                        .build()

                    imageHolder.controller = controller
                } else {
                    imageHolder.setImageURI(uri.toString())
                }

                view.setOnClickListener {
                    messageListener.onImageClick(view, message, layoutPosition, message.content)
                }

            } else {
                progress.show()
                messageListener.onStartDownload(message) { task, newMessage ->
                    if (task.isSuccessful) {
                        progress.hide()
                        val name = message.content + message.metadata!!.ext
                        val destination = File(imagesDir, message.chatChannelId)
                        val file = File(destination, name)
                        val uri = Uri.fromFile(file)

                        if (message.metadata!!.ext == ".webp") {
                            val controller = Fresco.newDraweeControllerBuilder()
                                .setUri(uri)
                                .setAutoPlayAnimations(true)
                                .build()

                            imageHolder.controller = controller
                        } else {
                            imageHolder.setImageURI(uri.toString())
                        }


                        imageHolder.setOnClickListener {
                            messageListener.onImageClick(view, newMessage, layoutPosition, message.content)
                        }
                    } else {
                        view.context.toast("Something went wrong while downloading media.")
                    }
                }
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridImageMessageViewHolder {
        return GridImageMessageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.grid_image_layout, parent, false))
    }

    override fun onBindViewHolder(holder: GridImageMessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}