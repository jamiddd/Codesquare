package com.jamid.codesquare.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.document
import com.jamid.codesquare.image

@Dao
abstract class MessageDao: BaseDao<Message>() {

    @Query("SELECT * FROM messages ORDER BY createdAt DESC")
    abstract fun getPagedMessages(): PagingSource<Int, Message>

    @Query("SELECT * FROM messages WHERE messageId = :messageId")
    abstract suspend fun getMessage(messageId: String): Message?

    @Query("SELECT * FROM messages WHERE chatChannelId = :channelId ORDER BY createdAt DESC")
    abstract fun getChannelPagedMessages(channelId: String): PagingSource<Int, Message>

    @Query("SELECT * FROM messages WHERE chatChannelId = :channelId AND type = :type ORDER BY createdAt DESC LIMIT :limit")
    abstract suspend fun getLimitedMediaMessages(channelId: String, limit: Int, type: String = image): List<Message>?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertMessages(messages: List<Message>)

    @Query("SELECT * FROM messages WHERE createdAt < :time AND chatChannelId = :channelId ORDER BY createdAt DESC LIMIT :limit")
    abstract suspend fun getMessagesBefore(channelId: String, time: Long, limit: Int): List<Message>

    @Query("SELECT * FROM messages WHERE chatChannelId = :chatChannelId ORDER BY createdAt DESC LIMIT :pageSize")
    abstract suspend fun getMessagesOnRefresh(chatChannelId: String, pageSize: Int): List<Message>?

    @Query("SELECT * FROM messages WHERE createdAt < :nextKey AND chatChannelId = :chatChannelId ORDER BY createdAt DESC LIMIT :pageSize")
    abstract suspend fun getMessagesOnAppend(
        chatChannelId: String,
        pageSize: Int,
        nextKey: Long
    ): List<Message>?

    @Query("SELECT * FROM messages WHERE createdAt > :anchorMessageTimeStart AND chatChannelId = :chatChannelId ORDER BY createdAt DESC LIMIT :pageSize")
    abstract fun getMessagesOnPrepend(
        chatChannelId: String,
        pageSize: Int,
        anchorMessageTimeStart: Long
    ): List<Message>?

    @Query("SELECT * FROM messages WHERE chatChannelId = :chatChannelId AND type = :type ORDER BY createdAt DESC")
    abstract suspend fun getMessages(chatChannelId: String, type: String = document): List<Message>?

}