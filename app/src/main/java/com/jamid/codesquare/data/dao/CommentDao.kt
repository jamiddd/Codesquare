package com.jamid.codesquare.data.dao

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import com.jamid.codesquare.data.Comment

@Dao
abstract class CommentDao: BaseDao<Comment>() {

    @Query("SELECT * FROM comments WHERE commentId = :commentId")
    abstract suspend fun getCommentById(commentId: String): Comment?

    @Query("SELECT * FROM comments WHERE commentChannelId = :commentChannelId ORDER BY createdAt DESC")
    abstract fun getPagedComments(commentChannelId: String): PagingSource<Int, Comment>

    @Query("DELETE FROM comments")
    abstract suspend fun clearTable()

    @Query("DELETE FROM comments WHERE commentId = :commentId")
    abstract suspend fun deleteCommentById(commentId: String)

    @Delete
    abstract suspend fun deleteComment(comment: Comment)

    @Query("SELECT * FROM comments WHERE commentId = :commentId")
    abstract fun getReactiveComment(commentId: String): LiveData<Comment>

    @Query("DELETE FROM comments WHERE senderId = :id")
    abstract suspend fun deleteCommentsByUserId(id: String)

}