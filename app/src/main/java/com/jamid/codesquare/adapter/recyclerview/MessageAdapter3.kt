package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.comparators.MessageComparator
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.ui.MessageListenerFragment

class MessageAdapter3(private val fragment: MessageListenerFragment): PagingDataAdapter<Message, MessageViewHolder2<Message>>(MessageComparator()) {

    private val currentUserId = UserManager.currentUser.id
    var chatChannel: ChatChannel? = null

    override fun onBindViewHolder(holder: MessageViewHolder2<Message>, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder2<Message> {
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
            MESSAGE_DEFAULT_REPLY_ITEM -> {
                LayoutInflater.from(parent.context).inflate(R.layout.message_default_reply_item, parent, false)
            }
            MESSAGE_MIDDLE_ITEM -> {
                LayoutInflater.from(parent.context).inflate(R.layout.message_middle_item, parent, false)
            }
            MESSAGE_MIDDLE_IMAGE_ITEM -> {
                LayoutInflater.from(parent.context).inflate(R.layout.message_middle_image_item, parent, false)
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
            MESSAGE_MIDDLE_DOCUMENT_RIGHT_ITEM -> {
                LayoutInflater.from(parent.context).inflate(R.layout.message_middle_document_right_item, parent, false)
            }
            MESSAGE_MIDDLE_REPLY_RIGHT_ITEM -> {
                LayoutInflater.from(parent.context).inflate(R.layout.message_middle_reply_right_item, parent, false)
            }
            else -> throw IllegalStateException("View type is illegal")
        }

        return MessageViewHolder2<Message>(view, viewType).apply {
            fragment = this@MessageAdapter3.fragment
        }

    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)

        val firstMessageFromBottom = position == 0
        val lastMessageFromBottom = position == itemCount - 1
        val isCurrentUserMessage = message?.senderId == currentUserId
        val isOnlyMessage = itemCount == 1

        when {
            isOnlyMessage -> {
                return if (isCurrentUserMessage) {
                    when (message?.type) {
                        image -> MESSAGE_DEFAULT_IMAGE_RIGHT_ITEM
                        document -> MESSAGE_DEFAULT_DOCUMENT_RIGHT_ITEM
                        else -> if (message?.replyTo != null) {
                            MESSAGE_DEFAULT_REPLY_RIGHT_ITEM
                        } else {
                            MESSAGE_DEFAULT_RIGHT_ITEM
                        }
                    }
                } else {
                    when (message?.type) {
                        image -> MESSAGE_DEFAULT_IMAGE_ITEM
                        document -> MESSAGE_DEFAULT_DOCUMENT_ITEM
                        else -> if (message?.replyTo != null) {
                            MESSAGE_DEFAULT_REPLY_ITEM
                        } else {
                            MESSAGE_DEFAULT_ITEM
                        }
                    }
                }
            }
            firstMessageFromBottom && !isOnlyMessage -> {
                val topMessage = getItem(position + 1)
                val isSameTopSender = topMessage?.senderId == message?.senderId
                return if (isCurrentUserMessage) {
                    if (isSameTopSender) {
                        when (message?.type) {
                            image -> MESSAGE_DEFAULT_IMAGE_RIGHT_ITEM
                            document -> MESSAGE_DEFAULT_DOCUMENT_RIGHT_ITEM
                            else -> if (message?.replyTo != null) {
                                MESSAGE_DEFAULT_REPLY_RIGHT_ITEM
                            } else {
                                MESSAGE_DEFAULT_RIGHT_ITEM
                            }
                        }
                    } else {
                        when (message?.type) {
                            image -> MESSAGE_DEFAULT_IMAGE_RIGHT_ITEM
                            document -> MESSAGE_DEFAULT_DOCUMENT_RIGHT_ITEM
                            else -> if (message?.replyTo != null) {
                                MESSAGE_DEFAULT_REPLY_RIGHT_ITEM
                            } else {
                                MESSAGE_DEFAULT_RIGHT_ITEM
                            }
                        }
                    }
                } else {
                    if (isSameTopSender) {
                        when (message?.type) {
                            image -> MESSAGE_DEFAULT_IMAGE_ITEM
                            document -> MESSAGE_DEFAULT_DOCUMENT_ITEM
                            else -> if (message?.replyTo != null) {
                                MESSAGE_DEFAULT_REPLY_ITEM
                            } else {
                                MESSAGE_DEFAULT_ITEM
                            }
                        }
                    } else {
                        when (message?.type) {
                            image -> MESSAGE_DEFAULT_IMAGE_ITEM
                            document -> MESSAGE_DEFAULT_DOCUMENT_ITEM
                            else -> if (message?.replyTo != null) {
                                MESSAGE_DEFAULT_REPLY_ITEM
                            } else {
                                MESSAGE_DEFAULT_ITEM
                            }
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
                            image -> MESSAGE_DEFAULT_IMAGE_RIGHT_ITEM
                            document -> MESSAGE_DEFAULT_DOCUMENT_RIGHT_ITEM
                            else -> if (message?.replyTo != null) {
                                MESSAGE_DEFAULT_REPLY_RIGHT_ITEM
                            } else {
                                MESSAGE_DEFAULT_RIGHT_ITEM
                            }
                        }
                    } else {
                        when (message?.type) {
                            image -> MESSAGE_DEFAULT_IMAGE_RIGHT_ITEM
                            document -> MESSAGE_DEFAULT_DOCUMENT_RIGHT_ITEM
                            else -> if (message?.replyTo != null) {
                                MESSAGE_DEFAULT_REPLY_RIGHT_ITEM
                            } else {
                                MESSAGE_DEFAULT_RIGHT_ITEM
                            }
                        }
                    }
                } else {
                    if (isSameBottomSender) {
                        when (message?.type) {
                            image -> MESSAGE_DEFAULT_IMAGE_ITEM
                            document -> MESSAGE_DEFAULT_DOCUMENT_ITEM
                            else -> if (message?.replyTo != null) {
                                MESSAGE_DEFAULT_REPLY_ITEM
                            } else {
                                MESSAGE_DEFAULT_ITEM
                            }
                        }
                    } else {
                        when (message?.type) {
                            image -> MESSAGE_DEFAULT_IMAGE_ITEM
                            document -> MESSAGE_DEFAULT_DOCUMENT_ITEM
                            else -> if (message?.replyTo != null) {
                                MESSAGE_DEFAULT_REPLY_ITEM
                            } else {
                                MESSAGE_DEFAULT_ITEM
                            }
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
                                image -> MESSAGE_MIDDLE_IMAGE_RIGHT_ITEM
                                document -> MESSAGE_MIDDLE_DOCUMENT_RIGHT_ITEM
                                else -> if (message?.replyTo != null) {
                                    MESSAGE_MIDDLE_REPLY_RIGHT_ITEM
                                } else {
                                    MESSAGE_MIDDLE_RIGHT_ITEM
                                }
                            }
                        }
                        isSameTopSender && !isSameBottomSender -> {
                            when (message?.type) {
                                image -> MESSAGE_DEFAULT_IMAGE_RIGHT_ITEM
                                document -> MESSAGE_DEFAULT_DOCUMENT_RIGHT_ITEM
                                else -> if (message?.replyTo != null) {
                                    MESSAGE_DEFAULT_REPLY_RIGHT_ITEM
                                } else {
                                    MESSAGE_DEFAULT_RIGHT_ITEM
                                }
                            }
                        }
                        !isSameTopSender && isSameBottomSender -> {
                            when (message?.type) {
                                image -> MESSAGE_MIDDLE_IMAGE_RIGHT_ITEM
                                document -> MESSAGE_MIDDLE_DOCUMENT_RIGHT_ITEM
                                else -> if (message?.replyTo != null) {
                                    MESSAGE_MIDDLE_REPLY_RIGHT_ITEM
                                } else {
                                    MESSAGE_MIDDLE_RIGHT_ITEM
                                }
                            }
                        }
                        !isSameTopSender && !isSameBottomSender -> {
                            when (message?.type) {
                                image -> MESSAGE_DEFAULT_IMAGE_RIGHT_ITEM
                                document -> MESSAGE_DEFAULT_DOCUMENT_RIGHT_ITEM
                                else -> if (message?.replyTo != null) {
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
                            when (message?.type) {
                                image -> MESSAGE_MIDDLE_IMAGE_ITEM
                                document -> MESSAGE_MIDDLE_DOCUMENT_ITEM
                                else -> if (message?.replyTo != null) {
                                    MESSAGE_MIDDLE_REPLY_ITEM
                                } else {
                                    MESSAGE_MIDDLE_ITEM
                                }
                            }
                        }
                        isSameTopSender && !isSameBottomSender -> {
                            when (message?.type) {
                                image -> MESSAGE_DEFAULT_IMAGE_ITEM
                                document -> MESSAGE_DEFAULT_DOCUMENT_ITEM
                                else -> if (message?.replyTo != null) {
                                    MESSAGE_DEFAULT_REPLY_ITEM
                                } else {
                                    MESSAGE_DEFAULT_ITEM
                                }
                            }
                        }
                        !isSameTopSender && isSameBottomSender -> {
                            when (message?.type) {
                                image -> MESSAGE_MIDDLE_IMAGE_ITEM
                                document -> MESSAGE_MIDDLE_DOCUMENT_ITEM
                                else -> if (message?.replyTo != null) {
                                    MESSAGE_MIDDLE_REPLY_ITEM
                                } else {
                                    MESSAGE_MIDDLE_ITEM
                                }
                            }
                        }
                        !isSameTopSender && !isSameBottomSender -> {
                            when (message?.type) {
                                image -> MESSAGE_DEFAULT_IMAGE_ITEM
                                document -> MESSAGE_DEFAULT_DOCUMENT_ITEM
                                else -> if (message?.replyTo != null) {
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
        }
    }

}