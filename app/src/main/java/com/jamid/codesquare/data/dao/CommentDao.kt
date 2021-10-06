package com.jamid.codesquare.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.jamid.codesquare.data.Comment
import com.jamid.codesquare.data.Project

@Dao
abstract class CommentDao: BaseDao<Comment>() {

    @Query("SELECT * FROM comments WHERE commentId = :commentId")
    abstract suspend fun getCommentById(commentId: String): Comment?

    @Query("SELECT * FROM comments WHERE commentChannelId = :commentChannelId ORDER BY createdAt DESC")
    abstract fun getPagedComments(commentChannelId: String): PagingSource<Int, Comment>

    @Query("DELETE FROM comments")
    abstract suspend fun clearTable()

}