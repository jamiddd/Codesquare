package com.jamid.codesquare.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import com.jamid.codesquare.data.PostInvite

@Dao
abstract class PostInviteDao: BaseDao<PostInvite>() {

    @Query("SELECT * FROM post_invites ORDER BY createdAt DESC")
    abstract fun getPostInvites(): PagingSource<Int, PostInvite>

    @Delete
    abstract suspend fun deletePostInvite(invite: PostInvite)

    @Query("DELETE FROM post_invites")
    abstract suspend fun clearTable()

}