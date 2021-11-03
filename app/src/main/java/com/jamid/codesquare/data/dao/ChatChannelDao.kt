package com.jamid.codesquare.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.jamid.codesquare.data.ChatChannel

@Dao
abstract class ChatChannelDao: BaseDao<ChatChannel>() {

    @Query("DELETE FROM chat_channels")
    abstract suspend fun clearTable()

    @Query("SELECT * FROM chat_channels ORDER BY updatedAt DESC")
    abstract fun getPagedChatChannels(): PagingSource<Int, ChatChannel>

    @Query("SELECT * FROM chat_channels WHERE chatChannelId = :chatChannel")
    abstract suspend fun getChatChannel(chatChannel: String): ChatChannel?

    @Query("SELECT * FROM chat_channels")
    abstract suspend fun allChannels(): List<ChatChannel>?

    @Query("SELECT * FROM chat_channels WHERE chatChannelId != :chatChannelId")
    abstract suspend fun getForwardChannels(chatChannelId: String): List<ChatChannel>?

}