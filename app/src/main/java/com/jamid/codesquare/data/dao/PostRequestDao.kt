package com.jamid.codesquare.data.dao

import android.util.Log
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import com.jamid.codesquare.data.PostRequest

@Dao
abstract class PostRequestDao: BaseDao<PostRequest>() {
    init {
        Log.d("Something", "Simple: ")
    }
    @Query("SELECT * FROM post_requests WHERE requestId = :requestId")
    abstract suspend fun getPostRequestById(requestId: String): PostRequest?

    @Query("SELECT * FROM post_requests WHERE postId = :postId")
    abstract suspend fun getPostRequestByPost(postId: String): PostRequest?

    @Delete
    abstract suspend fun deletePostRequest(postRequest: PostRequest)

    @Query("SELECT * FROM post_requests WHERE receiverId = :receiverId ORDER BY createdAt DESC")
    abstract fun getPagedPostRequests(receiverId: String): PagingSource<Int, PostRequest>

    @Query("SELECT * FROM post_requests WHERE senderId = :senderId ORDER BY createdAt DESC")
    abstract fun getMyPostRequests(senderId: String): PagingSource<Int, PostRequest>

    @Query("DELETE FROM post_requests")
    abstract suspend fun clearTable()

    @Query("DELETE FROM post_requests WHERE requestId = :id")
    abstract suspend fun deletePostRequestById(id: String)

    @Query("SELECT * FROM post_requests WHERE notificationId = :id")
    abstract suspend fun getPostRequestByNotificationId(id: String): PostRequest?

}