package com.jamid.codesquare.adapter.recyclerview

import android.content.Context
import android.net.Uri
import android.os.Environment
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
import com.jamid.codesquare.*
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.ui.MessageListenerFragment
import java.io.File

class GridImageMessagesAdapter(private val fragment: MessageListenerFragment): ListAdapter<Message, GridImageMessagesAdapter.GridImageMessageViewHolder>(comparator) {

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

    inner class GridImageMessageViewHolder(val view: View): RecyclerView.ViewHolder(view) {

        private val imageHolder = view.findViewById<SimpleDraweeView>(R.id.grid_image)
        private val progress = view.findViewById<ProgressBar>(R.id.small_message_image_progress)
        private val controllerListener = FrescoImageControllerListener()

        fun bind(message: Message) {
            if (message.isDownloaded) {
                progress.hide()

                val uri = getImageUriFromMessage(message, view.context)

                setMessageImageBasedOnExtension(imageHolder, uri, message)

                imageHolder.setOnClickListener {
                    message.metadata!!.height = controllerListener.finalHeight.toLong()
                    message.metadata!!.width = controllerListener.finalWidth.toLong()
                    fragment.onMessageImageClick(imageHolder, message)
                }

            } else {
                progress.show()

                fragment.onMessageNotDownloaded(message) { newMessage ->
                    bind(newMessage)
                }
            }
        }

        private fun getImageUriFromMessage(message: Message, context: Context): Uri {
            val imagesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val name = message.content + message.metadata!!.ext
            val destination = File(imagesDir, message.chatChannelId)
            val file = File(destination, name)
            return Uri.fromFile(file)
        }

        private fun setMessageImageBasedOnExtension(
            imageHolder: SimpleDraweeView,
            imageUri: Uri,
            message: Message
        ) {
            ViewCompat.setTransitionName(imageHolder, message.content)
            val metadata = message.metadata
            if (metadata != null) {
                val builder = Fresco.newDraweeControllerBuilder()
                    .setUri(imageUri)
                    .setControllerListener(controllerListener)

                if (metadata.ext == ".webp") {
                    builder.autoPlayAnimations = true
                }

                imageHolder.controller = builder.build()

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