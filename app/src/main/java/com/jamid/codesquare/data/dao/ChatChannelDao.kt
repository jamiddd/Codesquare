package com.jamid.codesquare.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.data.ChatChannelWrapper

@Dao
abstract class ChatChannelDao: BaseDao<ChatChannel>() {

    @Query("DELETE FROM chat_channels")
    abstract suspend fun clearTable()

    @Query("SELECT * FROM chat_channels WHERE archived = 0 AND (authorized = 1 OR message_data1_userId = :currentUserId) ORDER BY updatedAt DESC")
    abstract fun chatChannels(currentUserId: String): LiveData<List<ChatChannel>>

    @Query("SELECT * FROM chat_channels WHERE type = :type AND archived = 0 AND authorized = 0 AND message_data1_userId != :currentUserId")
    abstract fun messageRequests(currentUserId: String, type: String = "private"): LiveData<List<ChatChannel>>

    @Query("SELECT * FROM chat_channels WHERE chatChannelId = :chatChannel")
    abstract suspend fun getChatChannel(chatChannel: String): ChatChannel?

    @Query("SELECT * FROM chat_channels WHERE contributors LIKE :userId AND archived = 0")
    abstract fun getForwardChannels(userId: String): LiveData<List<ChatChannel>>

    @Query("SELECT * FROM chat_channels WHERE chatChannelId = :chatChannelId")
    abstract fun getCurrentChatChannel(chatChannelId: String): LiveData<ChatChannel>

    @Query("DELETE FROM chat_channels WHERE chatChannelId = :chatChannelId")
    abstract suspend fun deleteChatChannelById(chatChannelId: String)

    @Query("SELECT * FROM chat_channels WHERE chatChannelId = :chatChannelId")
    abstract fun getReactiveChatChannel(chatChannelId: String): LiveData<ChatChannel>

    @Query("SELECT * FROM chat_channels WHERE archived = 1 AND authorized = 1 ORDER BY updatedAt DESC")
    abstract fun archivedChannels(): LiveData<List<ChatChannel>>

}

@Dao
abstract class ChatChannelWrapperDao: BaseDao<ChatChannelWrapper>() {
    @Query("SELECT * FROM chat_channel_wrapper WHERE chatChannel_archived = 0 AND (chatChannel_authorized = 1 OR chatChannel_message_data1_userId = :currentUserId) ORDER BY chatChannel_updatedAt DESC")
    abstract fun chatChannelWrappers(currentUserId: String): LiveData<List<ChatChannelWrapper>>

    @Query("SELECT * FROM chat_channel_wrapper WHERE chatChannel_type = :type AND chatChannel_archived = 0 AND chatChannel_authorized = 0 AND chatChannel_message_data1_userId != :currentUserId")
    abstract fun messageRequests(currentUserId: String, type: String = "private"): LiveData<List<ChatChannelWrapper>>

    @Query("SELECT * FROM chat_channel_wrapper WHERE chatChannel_chatChannelId = :chatChannel")
    abstract suspend fun getChatChannel(chatChannel: String): ChatChannelWrapper?

    @Query("DELETE FROM chat_channel_wrapper WHERE chatChannel_chatChannelId = :chatChannelId")
    abstract suspend fun deleteChatChannelById(chatChannelId: String)

    @Query("SELECT * FROM chat_channel_wrapper WHERE chatChannel_archived = 0 AND chatChannel_authorized = 1 AND isRead = 0")
    abstract fun getUnreadChatChannels(): LiveData<List<ChatChannelWrapper>>

    @Query("SELECT * FROM chat_channel_wrapper WHERE chatChannel_archived = 1 AND chatChannel_authorized = 1 ORDER BY chatChannel_updatedAt DESC")
    abstract fun archivedChannels(): LiveData<List<ChatChannelWrapper>>

    @Query("DELETE FROM chat_channel_wrapper")
    abstract suspend fun clearTable()

    @Query("SELECT * FROM chat_channel_wrapper WHERE chatChannel_chatChannelId = :chatChannelId")
    abstract fun getReactiveChatChannel(chatChannelId: String): LiveData<ChatChannelWrapper>

}