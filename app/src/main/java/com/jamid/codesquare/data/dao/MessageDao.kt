package com.jamid.codesquare.data.dao

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.document
import com.jamid.codesquare.image
import com.jamid.codesquare.text
import com.jamid.codesquare.video

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

    @Query("SELECT * FROM messages WHERE createdAt > :anchorMessageTimeStart AND chatChannelId = :chatChannelId ORDER BY createdAt DESC LIMIT :pageSize")
    abstract fun getMessagesOnPrepend(
        chatChannelId: String,
        pageSize: Int,
        anchorMessageTimeStart: Long
    ): List<Message>?

    @Query("SELECT * FROM messages WHERE chatChannelId = :chatChannelId AND type = :type AND isDownloaded = 1 ORDER BY createdAt DESC")
    abstract suspend fun getMessages(chatChannelId: String, type: String = document): List<Message>?

    @Query("SELECT * FROM messages WHERE chatChannelId = :chatChannelId ORDER BY createdAt DESC LIMIT 1")
    abstract suspend fun getLastMessageForChannel(chatChannelId: String): Message?

    @Query("DELETE FROM messages WHERE chatChannelId = :chatChannelId")
    abstract suspend fun deleteAllMessagesInChannel(chatChannelId: String)

   /* @Query("UPDATE messages SET state = :selected WHERE chatChannelId = :chatChannelId")
    abstract suspend fun updateRestOfTheMessagesInChannel(chatChannelId: String, selected: Int)

    @Query("SELECT * FROM messages WHERE state = 1")
    abstract fun onMessagesModeChanged(): LiveData<List<Message>>

    @Query("SELECT * FROM messages WHERE state = 1")
    abstract suspend fun getSelectedMessages(): List<Message>?

    @Query("SELECT * FROM messages WHERE chatChannelId = :chatChannelId AND state = 1 ORDER BY createdAt DESC")
    abstract suspend fun getCurrentlySelectedMessages(chatChannelId: String): List<Message>*/

    @Query("SELECT * FROM messages WHERE chatChannelId = :chatChannelId ORDER BY createdAt DESC LIMIT :limit")
    abstract fun getMessagesForChannel(chatChannelId: String, limit: Int): LiveData<List<Message>>

    @Query("DELETE FROM messages WHERE chatChannelId = :chatChannelId AND senderId = :userId")
    abstract suspend fun deleteAllMessagesByUserInChannel(userId: String, chatChannelId: String?)

    @Query("DELETE FROM messages WHERE senderId = :userId")
    abstract suspend fun deleteAllMessagesByUser(userId: String)

    @Query("DELETE FROM messages")
    abstract suspend fun clearTable()

   /* @Query("UPDATE messages SET state = :state WHERE chatChannelId = :chatChannelId")
    abstract suspend fun updateMessages(chatChannelId: String, state: Int)

    @Query("SELECT * FROM messages WHERE chatChannelId = :channelId AND state = 1")
    abstract fun selectedMessages(channelId: String): LiveData<List<Message>>*/

    @Query("SELECT * FROM messages WHERE chatChannelId = :chatChannelId AND type != :type ORDER BY createdAt DESC LIMIT :limit")
    abstract fun getMediaMessages(chatChannelId: String, limit: Int = 6, type: String = text): LiveData<List<Message>>

    @Query("SELECT * FROM messages WHERE senderId = :id")
    abstract suspend fun getCurrentUserMessages(id: String): List<Message>

    @Query("DELETE FROM messages WHERE createdAt > 1657715865744")
    abstract suspend fun deleteMessagesAfter()

    @Query("SELECT * FROM messages WHERE (chatChannelId = :chatChannelId) AND (type = :t1 OR type = :t2) AND isDownloaded = 1 ORDER BY createdAt DESC LIMIT :limit")
    abstract fun getMultiMediaMessages(
        chatChannelId: String,
        limit: Int,
        t1: String = "image",
        t2: String = "video"
    ): LiveData<List<Message>>

    @Query("SELECT * FROM messages WHERE (chatChannelId = :chatChannelId) AND (type = :t1) ORDER BY createdAt DESC LIMIT :limit")
    abstract fun getDocumentMessages(chatChannelId: String, limit: Int, t1: String = document): LiveData<List<Message>>

    @Query("SELECT * FROM messages WHERE (chatChannelId = :chatChannelId) AND (type = :t1 OR type = :t2) AND (createdAt > :after) AND (isDownloaded = 1) ORDER BY createdAt DESC LIMIT 40")
    abstract suspend fun getMultiMediaMessagesSyncAfter(chatChannelId: String, after: Long, t1: String = image, t2: String = video): List<Message>

    @Query("SELECT * FROM messages WHERE (chatChannelId = :chatChannelId) AND (type = :t1 OR type = :t2) AND (isDownloaded = 1) ORDER BY createdAt DESC LIMIT 40")
    abstract suspend fun getMultiMediaMessagesSync(chatChannelId: String, t1: String = image, t2: String = video): List<Message>

    @Query("SELECT * FROM messages WHERE meta_name = :name LIMIT 1")
    abstract suspend fun getMessageByMediaItemName(name: String): Message?
}