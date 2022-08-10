package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.comparators.Message2Comparator
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.data.Message2
import com.jamid.codesquare.ui.MessageListener3

class MessageAdapter3(
    private val listener: MessageListener3
): PagingDataAdapter<Message2, MessageViewHolder2<Message2>>(Message2Comparator()) {

    private val currentUserId = UserManager.currentUser.id
    var chatChannel: ChatChannel? = null
    init {
        Log.d("Something", "Simple: ")
    }
    override fun onBindViewHolder(holder: MessageViewHolder2<Message2>, position: Int) {
        val item = getItem(position)
        if (item != null) {
            holder.bind(item)
        } else {
            Log.d(TAG, "onBindViewHolder: Null")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder2<Message2> {
        val view = when (viewType) {
            MESSAGE_DEFAULT_ITEM -> {
                LayoutInflater.from(parent.context).inflate(R.layout.message_item_default, parent, false)
            }
            MESSAGE_DEFAULT_IMAGE_ITEM -> {
                LayoutInflater.from(parent.context).inflate(R.layout.message_default_image_item, parent, false)
            }
            MESSAGE_DEFAULT_DOCUMENT_ITEM -> {
                LayoutInflater.from(parent.context).inflate(R.layout.message_default_document_item, parent, false)
            }
            MESSAGE_DEFAULT_VIDEO_ITEM -> {
                LayoutInflater.from(parent.context).inflate(R.layout.message_default_video_item, parent, false)
            }
            MESSAGE_DEFAULT_REPLY_ITEM -> {
                LayoutInflater.from(parent.context).inflate(R.layout.message_default_reply_item, parent, false)
            }
            MESSAGE_MIDDLE_ITEM -> {
                LayoutInflater.from(parent.context).inflate(R.layout.message_middle_item, parent, false)
            }
            MESSAGE_MIDDLE_IMAGE_ITEM -> {
                LayoutInflater.from(parent.context).inflate(R.layout.message_middle_image_item, parent, false)
            }
            MESSAGE_MIDDLE_VIDEO_ITEM -> {
                LayoutInflater.from(parent.context).inflate(R.layout.message_middle_video_item, parent, false)
            }
            MESSAGE_MIDDLE_DOCUMENT_ITEM -> {
                LayoutInflater.from(parent.context).inflate(R.layout.message_middle_document_item, parent, false)
            }
            MESSAGE_MIDDLE_REPLY_ITEM -> {
                LayoutInflater.from(parent.context).inflate(R.layout.message_middle_reply_item, parent, false)
            }
            MESSAGE_DEFAULT_RIGHT_ITEM -> {
                LayoutInflater.from(parent.context).inflate(R.layout.message_item_default_right, parent, false)
            }
            MESSAGE_DEFAULT_IMAGE_RIGHT_ITEM -> {
                LayoutInflater.from(parent.context).inflate(R.layout.message_default_image_right_item, parent, false)
            }
            MESSAGE_DEFAULT_VIDEO_RIGHT_ITEM -> {
                LayoutInflater.from(parent.context).inflate(R.layout.message_default_video_right_item, parent, false)
            }
            MESSAGE_DEFAULT_DOCUMENT_RIGHT_ITEM -> {
                LayoutInflater.from(parent.context).inflate(R.layout.message_default_document_right_item, parent, false)
            }
            MESSAGE_DEFAULT_REPLY_RIGHT_ITEM -> {
                LayoutInflater.from(parent.context).inflate(R.layout.message_default_reply_right_item, parent, false)
            }
            MESSAGE_MIDDLE_RIGHT_ITEM -> {
                LayoutInflater.from(parent.context).inflate(R.layout.message_middle_right_item, parent, false)
            }
            MESSAGE_MIDDLE_IMAGE_RIGHT_ITEM -> {
                LayoutInflater.from(parent.context).inflate(R.layout.message_middle_image_right_item, parent, false)
            }
            MESSAGE_MIDDLE_VIDEO_RIGHT_ITEM -> {
                LayoutInflater.from(parent.context).inflate(R.layout.message_middle_video_right_item, parent, false)
            }
            MESSAGE_MIDDLE_DOCUMENT_RIGHT_ITEM -> {
                LayoutInflater.from(parent.context).inflate(R.layout.message_middle_document_right_item, parent, false)
            }
            MESSAGE_MIDDLE_REPLY_RIGHT_ITEM -> {
                LayoutInflater.from(parent.context).inflate(R.layout.message_middle_reply_right_item, parent, false)
            }
            else -> LayoutInflater.from(parent.context).inflate(R.layout.messages_date_item, parent, false)
        }

        return MessageViewHolder2<Message2>(view, viewType).apply {
            listener = this@MessageAdapter3.listener
        }

    }

    private fun getMessageViewType(currentMessage: Message, topMessage: Message? = null, bottomMessage: Message? = null): Int {
        val isSameBottomSender = bottomMessage?.senderId == currentMessage.senderId
        val isSameTopSender = topMessage?.senderId == currentMessage.senderId

        val isCurrentUserMessage = currentMessage.senderId == UserManager.currentUserId

        return if (isCurrentUserMessage) {
            when {
                isSameTopSender && isSameBottomSender -> {
                    when (currentMessage.type) {
                        image -> MESSAGE_MIDDLE_IMAGE_RIGHT_ITEM
                        document -> MESSAGE_MIDDLE_DOCUMENT_RIGHT_ITEM
                        video -> MESSAGE_MIDDLE_VIDEO_RIGHT_ITEM
                        else -> if (currentMessage.replyTo != null) {
                            MESSAGE_MIDDLE_REPLY_RIGHT_ITEM
                        } else {
                            MESSAGE_MIDDLE_RIGHT_ITEM
                        }
                    }
                }
                isSameTopSender && !isSameBottomSender -> {
                    when (currentMessage.type) {
                        image -> MESSAGE_DEFAULT_IMAGE_RIGHT_ITEM
                        document -> MESSAGE_DEFAULT_DOCUMENT_RIGHT_ITEM
                        video -> MESSAGE_DEFAULT_VIDEO_RIGHT_ITEM
                        else -> if (currentMessage.replyTo != null) {
                            MESSAGE_DEFAULT_REPLY_RIGHT_ITEM
                        } else {
                            MESSAGE_DEFAULT_RIGHT_ITEM
                        }
                    }
                }
                !isSameTopSender && isSameBottomSender -> {
                    when (currentMessage.type) {
                        image -> MESSAGE_MIDDLE_IMAGE_RIGHT_ITEM
                        document -> MESSAGE_MIDDLE_DOCUMENT_RIGHT_ITEM
                        video -> MESSAGE_MIDDLE_VIDEO_RIGHT_ITEM
                        else -> if (currentMessage.replyTo != null) {
                            MESSAGE_MIDDLE_REPLY_RIGHT_ITEM
                        } else {
                            MESSAGE_MIDDLE_RIGHT_ITEM
                        }
                    }
                }
                !isSameTopSender && !isSameBottomSender -> {
                    when (currentMessage.type) {
                        image -> MESSAGE_DEFAULT_IMAGE_RIGHT_ITEM
                        document -> MESSAGE_DEFAULT_DOCUMENT_RIGHT_ITEM
                        video -> MESSAGE_DEFAULT_VIDEO_RIGHT_ITEM
                        else -> if (currentMessage.replyTo != null) {
                            MESSAGE_DEFAULT_REPLY_RIGHT_ITEM
                        } else {
                            MESSAGE_DEFAULT_RIGHT_ITEM
                        }
                    }
                }
                else -> throw Exception("Illegal State exception.")
            }
        } else {
            when {
                isSameTopSender && isSameBottomSender -> {
                    when (currentMessage.type) {
                        image -> MESSAGE_MIDDLE_IMAGE_ITEM
                        document -> MESSAGE_MIDDLE_DOCUMENT_ITEM
                        video -> MESSAGE_MIDDLE_VIDEO_ITEM
                        else -> if (currentMessage.replyTo != null) {
                            MESSAGE_MIDDLE_REPLY_ITEM
                        } else {
                            MESSAGE_MIDDLE_ITEM
                        }
                    }
                }
                isSameTopSender && !isSameBottomSender -> {
                    when (currentMessage.type) {
                        image -> MESSAGE_DEFAULT_IMAGE_ITEM
                        document -> MESSAGE_DEFAULT_DOCUMENT_ITEM
                        video -> MESSAGE_DEFAULT_VIDEO_ITEM
                        else -> if (currentMessage.replyTo != null) {
                            MESSAGE_DEFAULT_REPLY_ITEM
                        } else {
                            MESSAGE_DEFAULT_ITEM
                        }
                    }
                }
                !isSameTopSender && isSameBottomSender -> {
                    when (currentMessage.type) {
                        image -> MESSAGE_MIDDLE_IMAGE_ITEM
                        document -> MESSAGE_MIDDLE_DOCUMENT_ITEM
                        video -> MESSAGE_MIDDLE_VIDEO_ITEM
                        else -> if (currentMessage.replyTo != null) {
                            MESSAGE_MIDDLE_REPLY_ITEM
                        } else {
                            MESSAGE_MIDDLE_ITEM
                        }
                    }
                }
                !isSameTopSender && !isSameBottomSender -> {
                    when (currentMessage.type) {
                        image -> MESSAGE_DEFAULT_IMAGE_ITEM
                        document -> MESSAGE_DEFAULT_DOCUMENT_ITEM
                        video -> MESSAGE_DEFAULT_VIDEO_ITEM
                        else -> if (currentMessage.replyTo != null) {
                            MESSAGE_DEFAULT_REPLY_ITEM
                        } else {
                            MESSAGE_DEFAULT_ITEM
                        }
                    }
                }
                else -> throw IllegalStateException("Shouldn't happen though.")
            }
        }

    }

    override fun getItemViewType(position: Int): Int {
        val itemType =  when (val message2 = getItem(position)) {
            is Message2.DateSeparator -> {
                return DATE_SEPARATOR_ITEM
            }
            is Message2.MessageItem -> {
                val firstMessageFromBottom = position == 0
                val lastMessageFromBottom = position == itemCount - 1
                val isOnlyMessage = itemCount == 1

                when {
                    isOnlyMessage -> {
                        return getMessageViewType(message2.message)
                        /*return if (isCurrentUserMessage) {
                            when (message2.message.type) {
                                image -> MESSAGE_DEFAULT_IMAGE_RIGHT_ITEM
                                document -> MESSAGE_DEFAULT_DOCUMENT_RIGHT_ITEM
                                video -> MESSAGE_DEFAULT_VIDEO_ITEM
                                else -> if (message2.message.replyTo != null) {
                                    MESSAGE_DEFAULT_REPLY_RIGHT_ITEM
                                } else {
                                    MESSAGE_DEFAULT_RIGHT_ITEM
                                }
                            }
                        } else {
                            when (message2.message.type) {
                                image -> MESSAGE_DEFAULT_IMAGE_ITEM
                                document -> MESSAGE_DEFAULT_DOCUMENT_ITEM
                                video -> MESSAGE_DEFAULT_VIDEO_ITEM
                                else -> if (message2.message.replyTo != null) {
                                    MESSAGE_DEFAULT_REPLY_ITEM
                                } else {
                                    MESSAGE_DEFAULT_ITEM
                                }
                            }
                        }*/
                    }
                    firstMessageFromBottom && !isOnlyMessage -> {
                        when (val topMessage2 = getItem(position + 1)) {
                            is Message2.DateSeparator -> {
                                getMessageViewType(message2.message)
                                /*if (isCurrentUserMessage) {
                                    when (message2.message.type) {
                                        image -> MESSAGE_DEFAULT_IMAGE_RIGHT_ITEM
                                        document -> MESSAGE_DEFAULT_DOCUMENT_RIGHT_ITEM
                                        video -> MESSAGE_DEFAULT_VIDEO_RIGHT_ITEM
                                        else -> if (message2.message.replyTo != null) {
                                            MESSAGE_DEFAULT_REPLY_RIGHT_ITEM
                                        } else {
                                            MESSAGE_DEFAULT_RIGHT_ITEM
                                        }
                                    }
                                } else {
                                    when (message2.message.type) {
                                        image -> MESSAGE_DEFAULT_IMAGE_ITEM
                                        document -> MESSAGE_DEFAULT_DOCUMENT_ITEM
                                        video -> MESSAGE_DEFAULT_VIDEO_ITEM
                                        else -> if (message2.message.replyTo != null) {
                                            MESSAGE_DEFAULT_REPLY_ITEM
                                        } else {
                                            MESSAGE_DEFAULT_ITEM
                                        }
                                    }
                                }*/
                            }
                            is Message2.MessageItem -> {
                                getMessageViewType(message2.message, topMessage2.message)
                                /*val isSameTopSender = topMessage2.message.senderId == message2.message.senderId
                                return if (isCurrentUserMessage) {
                                    if (isSameTopSender) {
                                        when (message2.message.type) {
                                            image -> MESSAGE_DEFAULT_IMAGE_RIGHT_ITEM
                                            document -> MESSAGE_DEFAULT_DOCUMENT_RIGHT_ITEM
                                            video -> MESSAGE_DEFAULT_VIDEO_RIGHT_ITEM
                                            else -> if (message2.message.replyTo != null) {
                                                MESSAGE_DEFAULT_REPLY_RIGHT_ITEM
                                            } else {
                                                MESSAGE_DEFAULT_RIGHT_ITEM
                                            }
                                        }
                                    } else {
                                        when (message2.message.type) {
                                            image -> MESSAGE_DEFAULT_IMAGE_RIGHT_ITEM
                                            document -> MESSAGE_DEFAULT_DOCUMENT_RIGHT_ITEM
                                            video -> MESSAGE_DEFAULT_VIDEO_RIGHT_ITEM
                                            else -> if (message2.message.replyTo != null) {
                                                MESSAGE_DEFAULT_REPLY_RIGHT_ITEM
                                            } else {
                                                MESSAGE_DEFAULT_RIGHT_ITEM
                                            }
                                        }
                                    }
                                } else {
                                    if (isSameTopSender) {
                                        when (message2.message.type) {
                                            image -> MESSAGE_DEFAULT_IMAGE_ITEM
                                            document -> MESSAGE_DEFAULT_DOCUMENT_ITEM
                                            video -> MESSAGE_DEFAULT_VIDEO_ITEM
                                            else -> if (message2.message.replyTo != null) {
                                                MESSAGE_DEFAULT_REPLY_ITEM
                                            } else {
                                                MESSAGE_DEFAULT_ITEM
                                            }
                                        }
                                    } else {
                                        when (message2.message.type) {
                                            image -> MESSAGE_DEFAULT_IMAGE_ITEM
                                            document -> MESSAGE_DEFAULT_DOCUMENT_ITEM
                                            video -> MESSAGE_DEFAULT_VIDEO_ITEM
                                            else -> if (message2.message.replyTo != null) {
                                                MESSAGE_DEFAULT_REPLY_ITEM
                                            } else {
                                                MESSAGE_DEFAULT_ITEM
                                            }
                                        }
                                    }
                                }*/
                            }
                            null -> super.getItemViewType(position)
                        }
                    }
                    lastMessageFromBottom -> {
                        when (val bottomMessage2 = getItem(position - 1)) {
                            is Message2.DateSeparator -> {
                                getMessageViewType(message2.message)
                                /*if (isCurrentUserMessage) {
                                    when (message2.message.type) {
                                        image -> MESSAGE_DEFAULT_IMAGE_RIGHT_ITEM
                                        document -> MESSAGE_DEFAULT_DOCUMENT_RIGHT_ITEM
                                        video -> MESSAGE_DEFAULT_VIDEO_RIGHT_ITEM
                                        else -> if (message2.message.replyTo != null) {
                                            MESSAGE_DEFAULT_REPLY_RIGHT_ITEM
                                        } else {
                                            MESSAGE_DEFAULT_RIGHT_ITEM
                                        }
                                    }
                                } else {
                                    when (message2.message.type) {
                                        image -> MESSAGE_DEFAULT_IMAGE_ITEM
                                        document -> MESSAGE_DEFAULT_DOCUMENT_ITEM
                                        video -> MESSAGE_DEFAULT_VIDEO_ITEM
                                        else -> if (message2.message.replyTo != null) {
                                            MESSAGE_DEFAULT_REPLY_ITEM
                                        } else {
                                            MESSAGE_DEFAULT_ITEM
                                        }
                                    }
                                }*/
                            }
                            is Message2.MessageItem -> {
                                getMessageViewType(message2.message, bottomMessage = bottomMessage2.message)
                                /*val isSameBottomSender = bottomMessage2.message.senderId == message2.message.senderId
                                return if (isCurrentUserMessage) {
                                    if (isSameBottomSender) {
                                        when (message2.message.type) {
                                            image -> MESSAGE_DEFAULT_IMAGE_RIGHT_ITEM
                                            document -> MESSAGE_DEFAULT_DOCUMENT_RIGHT_ITEM
                                            video -> MESSAGE_DEFAULT_VIDEO_RIGHT_ITEM
                                            else -> if (message2.message.replyTo != null) {
                                                MESSAGE_DEFAULT_REPLY_RIGHT_ITEM
                                            } else {
                                                MESSAGE_DEFAULT_RIGHT_ITEM
                                            }
                                        }
                                    } else {
                                        when (message2.message.type) {
                                            image -> MESSAGE_DEFAULT_IMAGE_RIGHT_ITEM
                                            document -> MESSAGE_DEFAULT_DOCUMENT_RIGHT_ITEM
                                            video -> MESSAGE_DEFAULT_VIDEO_RIGHT_ITEM
                                            else -> if (message2.message.replyTo != null) {
                                                MESSAGE_DEFAULT_REPLY_RIGHT_ITEM
                                            } else {
                                                MESSAGE_DEFAULT_RIGHT_ITEM
                                            }
                                        }
                                    }
                                } else {
                                    if (isSameBottomSender) {
                                        when (message2.message.type) {
                                            image -> MESSAGE_DEFAULT_IMAGE_ITEM
                                            document -> MESSAGE_DEFAULT_DOCUMENT_ITEM
                                            video -> MESSAGE_DEFAULT_VIDEO_ITEM
                                            else -> if (message2.message.replyTo != null) {
                                                MESSAGE_DEFAULT_REPLY_ITEM
                                            } else {
                                                MESSAGE_DEFAULT_ITEM
                                            }
                                        }
                                    } else {
                                        when (message2.message.type) {
                                            image -> MESSAGE_DEFAULT_IMAGE_ITEM
                                            document -> MESSAGE_DEFAULT_DOCUMENT_ITEM
                                            video -> MESSAGE_DEFAULT_VIDEO_ITEM
                                            else -> if (message2.message.replyTo != null) {
                                                MESSAGE_DEFAULT_REPLY_ITEM
                                            } else {
                                                MESSAGE_DEFAULT_ITEM
                                            }
                                        }
                                    }
                                }*/
                            }
                            null -> super.getItemViewType(position)
                        }
                    }
                    else -> {
                        val topMessage2 = getItem(position + 1)
                        val bottomMessage2 = getItem(position - 1)

                        when (topMessage2) {
                            is Message2.DateSeparator -> {
                                when (bottomMessage2) {
                                    is Message2.DateSeparator -> {
                                        getMessageViewType(message2.message)
                                        /*if (isCurrentUserMessage) {
                                            when (message2.message.type) {
                                                image -> MESSAGE_DEFAULT_IMAGE_RIGHT_ITEM
                                                document -> MESSAGE_DEFAULT_DOCUMENT_RIGHT_ITEM
                                                video -> MESSAGE_DEFAULT_VIDEO_RIGHT_ITEM
                                                else -> if (message2.message.replyTo != null) {
                                                    MESSAGE_DEFAULT_REPLY_RIGHT_ITEM
                                                } else {
                                                    MESSAGE_DEFAULT_RIGHT_ITEM
                                                }
                                            }
                                        } else {
                                            when (message2.message.type) {
                                                image -> MESSAGE_DEFAULT_IMAGE_ITEM
                                                document -> MESSAGE_DEFAULT_DOCUMENT_ITEM
                                                video -> MESSAGE_DEFAULT_VIDEO_ITEM
                                                else -> if (message2.message.replyTo != null) {
                                                    MESSAGE_DEFAULT_REPLY_ITEM
                                                } else {
                                                    MESSAGE_DEFAULT_ITEM
                                                }
                                            }
                                        }*/
                                    }
                                    is Message2.MessageItem -> {
                                        getMessageViewType(message2.message, bottomMessage = bottomMessage2.message)
                                        /*val isSameBottomSender = bottomMessage2.message.senderId == message2.message.senderId
                                        if (isCurrentUserMessage) {
                                            return when {
                                                isSameBottomSender -> {
                                                    when (message2.message.type) {
                                                        image -> MESSAGE_MIDDLE_IMAGE_RIGHT_ITEM
                                                        document -> MESSAGE_MIDDLE_DOCUMENT_RIGHT_ITEM
                                                        video -> MESSAGE_MIDDLE_VIDEO_RIGHT_ITEM
                                                        else -> if (message2.message.replyTo != null) {
                                                            MESSAGE_MIDDLE_REPLY_RIGHT_ITEM
                                                        } else {
                                                            MESSAGE_MIDDLE_RIGHT_ITEM
                                                        }
                                                    }
                                                }
                                                !isSameBottomSender -> {
                                                    when (message2.message.type) {
                                                        image -> MESSAGE_DEFAULT_IMAGE_RIGHT_ITEM
                                                        document -> MESSAGE_DEFAULT_DOCUMENT_RIGHT_ITEM
                                                        video -> MESSAGE_DEFAULT_VIDEO_RIGHT_ITEM
                                                        else -> if (message2.message.replyTo != null) {
                                                            MESSAGE_DEFAULT_REPLY_RIGHT_ITEM
                                                        } else {
                                                            MESSAGE_DEFAULT_RIGHT_ITEM
                                                        }
                                                    }
                                                }
                                                else -> throw Exception("Illegal State exception.")
                                            }
                                        } else {
                                            return when {
                                                isSameBottomSender -> {
                                                    when (message2.message.type) {
                                                        image -> MESSAGE_MIDDLE_IMAGE_ITEM
                                                        document -> MESSAGE_MIDDLE_DOCUMENT_ITEM
                                                        video -> MESSAGE_MIDDLE_VIDEO_ITEM
                                                        else -> if (message2.message.replyTo != null) {
                                                            MESSAGE_MIDDLE_REPLY_ITEM
                                                        } else {
                                                            MESSAGE_MIDDLE_ITEM
                                                        }
                                                    }
                                                }
                                                !isSameBottomSender -> {
                                                    when (message2.message.type) {
                                                        image -> MESSAGE_DEFAULT_IMAGE_ITEM
                                                        document -> MESSAGE_DEFAULT_DOCUMENT_ITEM
                                                        video -> MESSAGE_DEFAULT_VIDEO_ITEM
                                                        else -> if (message2.message.replyTo != null) {
                                                            MESSAGE_DEFAULT_REPLY_ITEM
                                                        } else {
                                                            MESSAGE_DEFAULT_ITEM
                                                        }
                                                    }
                                                }
                                                else -> throw IllegalStateException("Shouldn't happen though.")
                                            }
                                        }*/
                                    }
                                    null -> super.getItemViewType(position)
                                }
                            }
                            is Message2.MessageItem -> {
                                when (bottomMessage2) {
                                    is Message2.DateSeparator -> {
                                        getMessageViewType(message2.message, topMessage2.message)
                                        /*val isSameTopSender = topMessage2.message.senderId == message2.message.senderId
                                        return if (isCurrentUserMessage) {
                                            if (isSameTopSender) {
                                                when (message2.message.type) {
                                                    image -> MESSAGE_DEFAULT_IMAGE_RIGHT_ITEM
                                                    document -> MESSAGE_DEFAULT_DOCUMENT_RIGHT_ITEM
                                                    video -> MESSAGE_DEFAULT_VIDEO_RIGHT_ITEM
                                                    else -> if (message2.message.replyTo != null) {
                                                        MESSAGE_DEFAULT_REPLY_RIGHT_ITEM
                                                    } else {
                                                        MESSAGE_DEFAULT_RIGHT_ITEM
                                                    }
                                                }
                                            } else {
                                                when (message2.message.type) {
                                                    image -> MESSAGE_DEFAULT_IMAGE_RIGHT_ITEM
                                                    document -> MESSAGE_DEFAULT_DOCUMENT_RIGHT_ITEM
                                                    video -> MESSAGE_DEFAULT_VIDEO_RIGHT_ITEM
                                                    else -> if (message2.message.replyTo != null) {
                                                        MESSAGE_DEFAULT_REPLY_RIGHT_ITEM
                                                    } else {
                                                        MESSAGE_DEFAULT_RIGHT_ITEM
                                                    }
                                                }
                                            }
                                        } else {
                                            if (isSameTopSender) {
                                                when (message2.message.type) {
                                                    image -> MESSAGE_DEFAULT_IMAGE_ITEM
                                                    document -> MESSAGE_DEFAULT_DOCUMENT_ITEM
                                                    video -> MESSAGE_DEFAULT_VIDEO_ITEM
                                                    else -> if (message2.message.replyTo != null) {
                                                        MESSAGE_DEFAULT_REPLY_ITEM
                                                    } else {
                                                        MESSAGE_DEFAULT_ITEM
                                                    }
                                                }
                                            } else {
                                                when (message2.message.type) {
                                                    image -> MESSAGE_DEFAULT_IMAGE_ITEM
                                                    document -> MESSAGE_DEFAULT_DOCUMENT_ITEM
                                                    video -> MESSAGE_DEFAULT_VIDEO_ITEM
                                                    else -> if (message2.message.replyTo != null) {
                                                        MESSAGE_DEFAULT_REPLY_ITEM
                                                    } else {
                                                        MESSAGE_DEFAULT_ITEM
                                                    }
                                                }
                                            }
                                        }*/
                                    }
                                    is Message2.MessageItem -> {
                                        getMessageViewType(message2.message, topMessage2.message, bottomMessage2.message)
                                        /*val isSameBottomSender = bottomMessage2.message.senderId == message2.message.senderId
                                        val isSameTopSender = topMessage2.message.senderId == message2.message.senderId
                                        if (isCurrentUserMessage) {
                                            return when {
                                                isSameTopSender && isSameBottomSender -> {
                                                    when (message2.message.type) {
                                                        image -> MESSAGE_MIDDLE_IMAGE_RIGHT_ITEM
                                                        document -> MESSAGE_MIDDLE_DOCUMENT_RIGHT_ITEM
                                                        video -> MESSAGE_MIDDLE_VIDEO_RIGHT_ITEM
                                                        else -> if (message2.message.replyTo != null) {
                                                            MESSAGE_MIDDLE_REPLY_RIGHT_ITEM
                                                        } else {
                                                            MESSAGE_MIDDLE_RIGHT_ITEM
                                                        }
                                                    }
                                                }
                                                isSameTopSender && !isSameBottomSender -> {
                                                    when (message2.message.type) {
                                                        image -> MESSAGE_DEFAULT_IMAGE_RIGHT_ITEM
                                                        document -> MESSAGE_DEFAULT_DOCUMENT_RIGHT_ITEM
                                                        video -> MESSAGE_DEFAULT_VIDEO_RIGHT_ITEM
                                                        else -> if (message2.message.replyTo != null) {
                                                            MESSAGE_DEFAULT_REPLY_RIGHT_ITEM
                                                        } else {
                                                            MESSAGE_DEFAULT_RIGHT_ITEM
                                                        }
                                                    }
                                                }
                                                !isSameTopSender && isSameBottomSender -> {
                                                    when (message2.message.type) {
                                                        image -> MESSAGE_MIDDLE_IMAGE_RIGHT_ITEM
                                                        document -> MESSAGE_MIDDLE_DOCUMENT_RIGHT_ITEM
                                                        video -> MESSAGE_MIDDLE_VIDEO_RIGHT_ITEM
                                                        else -> if (message2.message.replyTo != null) {
                                                            MESSAGE_MIDDLE_REPLY_RIGHT_ITEM
                                                        } else {
                                                            MESSAGE_MIDDLE_RIGHT_ITEM
                                                        }
                                                    }
                                                }
                                                !isSameTopSender && !isSameBottomSender -> {
                                                    when (message2.message.type) {
                                                        image -> MESSAGE_DEFAULT_IMAGE_RIGHT_ITEM
                                                        document -> MESSAGE_DEFAULT_DOCUMENT_RIGHT_ITEM
                                                        video -> MESSAGE_DEFAULT_VIDEO_RIGHT_ITEM
                                                        else -> if (message2.message.replyTo != null) {
                                                            MESSAGE_DEFAULT_REPLY_RIGHT_ITEM
                                                        } else {
                                                            MESSAGE_DEFAULT_RIGHT_ITEM
                                                        }
                                                    }
                                                }
                                                else -> throw Exception("Illegal State exception.")
                                            }
                                        } else {
                                            return when {
                                                isSameTopSender && isSameBottomSender -> {
                                                    when (message2.message.type) {
                                                        image -> MESSAGE_MIDDLE_IMAGE_ITEM
                                                        document -> MESSAGE_MIDDLE_DOCUMENT_ITEM
                                                        video -> MESSAGE_MIDDLE_VIDEO_ITEM
                                                        else -> if (message2.message.replyTo != null) {
                                                            MESSAGE_MIDDLE_REPLY_ITEM
                                                        } else {
                                                            MESSAGE_MIDDLE_ITEM
                                                        }
                                                    }
                                                }
                                                isSameTopSender && !isSameBottomSender -> {
                                                    when (message2.message.type) {
                                                        image -> MESSAGE_DEFAULT_IMAGE_ITEM
                                                        document -> MESSAGE_DEFAULT_DOCUMENT_ITEM
                                                        video -> MESSAGE_DEFAULT_VIDEO_ITEM
                                                        else -> if (message2.message.replyTo != null) {
                                                            MESSAGE_DEFAULT_REPLY_ITEM
                                                        } else {
                                                            MESSAGE_DEFAULT_ITEM
                                                        }
                                                    }
                                                }
                                                !isSameTopSender && isSameBottomSender -> {
                                                    when (message2.message.type) {
                                                        image -> MESSAGE_MIDDLE_IMAGE_ITEM
                                                        document -> MESSAGE_MIDDLE_DOCUMENT_ITEM
                                                        video -> MESSAGE_MIDDLE_VIDEO_ITEM
                                                        else -> if (message2.message.replyTo != null) {
                                                            MESSAGE_MIDDLE_REPLY_ITEM
                                                        } else {
                                                            MESSAGE_MIDDLE_ITEM
                                                        }
                                                    }
                                                }
                                                !isSameTopSender && !isSameBottomSender -> {
                                                    when (message2.message.type) {
                                                        image -> MESSAGE_DEFAULT_IMAGE_ITEM
                                                        document -> MESSAGE_DEFAULT_DOCUMENT_ITEM
                                                        video -> MESSAGE_DEFAULT_VIDEO_ITEM
                                                        else -> if (message2.message.replyTo != null) {
                                                            MESSAGE_DEFAULT_REPLY_ITEM
                                                        } else {
                                                            MESSAGE_DEFAULT_ITEM
                                                        }
                                                    }
                                                }
                                                else -> throw IllegalStateException("Shouldn't happen though.")
                                            }
                                        }*/
                                    }
                                    null -> super.getItemViewType(position)
                                }
                            }
                            null -> super.getItemViewType(position)
                        }
                    }
                }
            }
            null -> super.getItemViewType(position)
        }

        Log.d(TAG, "getItemViewType: $itemType")
        return itemType
    }

}