package com.jamid.codesquare.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.jamid.codesquare.data.Message

@Dao
abstract class MessageDao: BaseDao<Message>() {

    @Query("SELECT * FROM messages ORDER BY createdAt DESC")
    abstract fun getPagedMessages(): PagingSource<Int, Message>

}