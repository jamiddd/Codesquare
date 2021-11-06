package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.jamid.codesquare.*
import com.jamid.codesquare.data.Message

class MessageAdapter2(private val currentUsrId: String, private val contributorsSize: Int): PagingDataAdapter<Message, MessageViewHolder>(comparator) {

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

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        return when (viewType) {
            msg_at_start, msg_at_start_image, msg_at_start_doc, msg_at_middle, msg_at_middle_image, msg_at_middle_doc, msg_at_end, msg_at_end_image, msg_at_end_doc, msg_single, msg_single_image, msg_single_doc -> {
                MessageViewHolder.newInstance(parent, R.layout.chat_balloon_left, currentUsrId, contributorsSize, viewType)
            }
            msg_at_start_alt, msg_at_start_alt_image, msg_at_start_alt_doc, msg_at_middle_alt, msg_at_middle_alt_image, msg_at_middle_alt_doc, msg_at_end_alt, msg_at_end_alt_image, msg_at_end_alt_doc, msg_single_alt, msg_single_alt_image, msg_single_alt_doc -> {
                MessageViewHolder.newInstance(parent, R.layout.chat_balloon_right, currentUsrId, contributorsSize, viewType)
            }
            else -> throw IllegalStateException("View type is illegal")
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)

        val firstMessageFromBottom = position == 0
        val lastMessageFromBottom = position == itemCount - 1
        val isCurrentUserMessage = message?.senderId == currentUsrId
        val isOnlyMessage = itemCount == 1

        Log.d("MessageAdapter", "isCurrentUserMessage = $isCurrentUserMessage + currentUser -> $currentUsrId + ${message?.sender}")

        when {
            isOnlyMessage -> {
                return if (isCurrentUserMessage) {
                    when (message?.type) {
                        image -> msg_single_alt_image
                        document -> msg_single_alt_doc
                        else -> msg_single_alt
                    }
                } else {
                    when (message?.type) {
                        image -> msg_single_image
                        document -> msg_single_doc
                        else -> msg_single
                    }
                }
            }
            firstMessageFromBottom && !isOnlyMessage -> {
                val topMessage = getItem(position + 1)
                val isSameTopSender = topMessage?.senderId == message?.senderId
                return if (isCurrentUserMessage) {
                    if (isSameTopSender) {
                        when (message?.type) {
                            image -> msg_at_end_alt_image
                            document -> msg_at_end_alt_doc
                            else -> msg_at_end_alt
                        }
                    } else {
                        when (message?.type) {
                            image -> msg_single_alt_image
                            document -> msg_single_alt_doc
                            else -> msg_single_alt
                        }
                    }
                } else {
                    if (isSameTopSender) {
                        when (message?.type) {
                            image -> msg_at_end_image
                            document -> msg_at_end_doc
                            else -> msg_at_end
                        }
                    } else {
                        when (message?.type) {
                            image -> msg_single_image
                            document -> msg_single_doc
                            else -> msg_single
                        }
                    }
                }
            }
            lastMessageFromBottom -> {
                val bottomMessage = getItem(position - 1)
                val isSameBottomSender = bottomMessage?.senderId == message?.senderId
                return if (isCurrentUserMessage) {
                    if (isSameBottomSender) {
                        when (message?.type) {
                            image -> msg_at_start_alt_image
                            document -> msg_at_start_alt_doc
                            else -> msg_at_start_alt
                        }
                    } else {
                        when (message?.type) {
                            image -> msg_single_alt_image
                            document -> msg_single_alt_doc
                            else -> msg_single_alt
                        }
                    }
                } else {
                    if (isSameBottomSender) {
                        when (message?.type) {
                            image -> msg_at_start_image
                            document -> msg_at_start_doc
                            else -> msg_at_start
                        }
                    } else {
                        when (message?.type) {
                            image -> msg_single_image
                            document -> msg_single_doc
                            else -> msg_single
                        }
                    }
                }
            }
            else -> {
                val topMessage = getItem(position + 1)
                val bottomMessage = getItem(position - 1)
                val isSameBottomSender = bottomMessage?.senderId == message?.senderId
                val isSameTopSender = topMessage?.senderId == message?.senderId
                if (isCurrentUserMessage) {
                    return when {
                        isSameTopSender && isSameBottomSender -> {
                            when (message?.type) {
                                image -> msg_at_middle_alt_image
                                document -> msg_at_middle_alt_doc
                                else -> msg_at_middle_alt
                            }
                        }
                        isSameTopSender && !isSameBottomSender -> {
                            when (message?.type) {
                                image -> msg_at_end_alt_image
                                document -> msg_at_end_alt_doc
                                else -> msg_at_end_alt
                            }
                        }
                        !isSameTopSender && isSameBottomSender -> {
                            when (message?.type) {
                                image -> msg_at_start_alt_image
                                document -> msg_at_start_alt_doc
                                else -> msg_at_start_alt
                            }
                        }
                        !isSameTopSender && !isSameBottomSender -> {
                            when (message?.type) {
                                image -> msg_single_alt_image
                                document -> msg_single_alt_doc
                                else -> msg_single_alt
                            }
                        }
                        else -> throw Exception("Illegal State exception.")
                    }
                } else {
                    return when {
                        isSameTopSender && isSameBottomSender -> {
                            when (message?.type) {
                                image -> msg_at_middle_image
                                document -> msg_at_middle_doc
                                else -> msg_at_middle
                            }
                        }
                        isSameTopSender && !isSameBottomSender -> {
                            when (message?.type) {
                                image -> msg_at_end_image
                                document -> msg_at_end_doc
                                else -> msg_at_end
                            }
                        }
                        !isSameTopSender && isSameBottomSender -> {
                            when (message?.type) {
                                image -> msg_at_start_image
                                document -> msg_at_start_doc
                                else -> msg_at_start
                            }
                        }
                        !isSameTopSender && !isSameBottomSender -> {
                            when (message?.type) {
                                image -> msg_single_image
                                document -> msg_single_doc
                                else -> msg_single
                            }
                        }
                        else -> throw IllegalStateException("Shouldn't happen though.")
                    }
                }
            }
        }
    }

}