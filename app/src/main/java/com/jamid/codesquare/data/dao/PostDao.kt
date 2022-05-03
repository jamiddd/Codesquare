package com.jamid.codesquare.data.dao

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import com.jamid.codesquare.data.Post

@Dao
abstract class PostDao: BaseDao<Post>() {

    @Query("SELECT * FROM posts WHERE archived = 0 ORDER BY isMadeByMe DESC, createdAt DESC")
    abstract fun getPagedPostsByTime(): PagingSource<Int, Post>

    @Query("SELECT * FROM posts WHERE tags LIKE :tag AND archived = 0 ORDER BY isMadeByMe DESC, createdAt DESC")
    abstract fun getTagPostsByTime(tag: String): PagingSource<Int, Post>

    @Query("SELECT * FROM posts WHERE archived = 0 ORDER BY isMadeByMe DESC, likesCount DESC")
    abstract fun getPagedPostsByLikes(): PagingSource<Int, Post>

    @Query("SELECT * FROM posts WHERE tags LIKE :tag AND archived = 0 ORDER BY isMadeByMe DESC, likesCount DESC")
    abstract fun getTagPostsByLikes(tag: String): PagingSource<Int, Post>

    @Query("SELECT * FROM posts WHERE archived = 0 ORDER BY isMadeByMe DESC, viewsCount DESC")
    abstract fun getPagedPostsByViews(): PagingSource<Int, Post>

    @Query("SELECT * FROM posts WHERE tags LIKE :tag AND archived = 0 ORDER BY  isMadeByMe DESC, viewsCount DESC")
    abstract fun getTagPostsByViews(tag: String): PagingSource<Int, Post>

    @Query("SELECT * FROM posts WHERE archived = 0 ORDER BY isMadeByMe DESC, contributorsCount DESC")
    abstract fun getPagedPostsByContributors(): PagingSource<Int, Post>

    @Query("SELECT * FROM posts WHERE tags LIKE :tag AND archived = 0 ORDER BY isMadeByMe DESC, contributorsCount DESC")
    abstract fun getTagPostsByContributors(tag: String): PagingSource<Int, Post>

    @Query("DELETE FROM posts")
    abstract suspend fun clearTable()

    @Query("SELECT * FROM posts WHERE isMadeByMe = 1 AND archived = 0")
    abstract fun getCurrentUserPagedPosts(): PagingSource<Int, Post>

    @Query("SELECT * FROM posts WHERE id = :postId")
    abstract suspend fun getPost(postId: String): Post?

    @Query("SELECT * FROM posts WHERE contributors LIKE :formattedUid AND post_userId != :uid AND archived = 0 ORDER BY createdAt DESC")
    abstract fun getPagedCollaborations(formattedUid: String, uid: String): PagingSource<Int, Post>

    @Query("SELECT * FROM posts WHERE post_userId = :id AND archived = 0 ORDER BY createdAt DESC")
    abstract fun getPagedOtherUserPosts(id: String): PagingSource<Int, Post>

    @Query("SELECT * FROM posts WHERE contributors LIKE :formattedUid AND post_userId != :id AND archived = 0 ORDER BY createdAt DESC")
    abstract fun getOtherUserPagedCollaborations(formattedUid: String, id: String): PagingSource<Int, Post>

    @Query("SELECT * FROM posts WHERE isSaved = 1 AND archived = 0 ORDER BY createdAt DESC")
    abstract fun getPagedSavedPosts(): PagingSource<Int, Post>

    @Query("SELECT * FROM posts WHERE chatChannel = :channelId LIMIT 1")
    abstract suspend fun getPostByChatChannel(channelId: String): Post?

    @Query("SELECT * FROM posts WHERE chatChannel = :channelId LIMIT 1")
    abstract fun getLivePostByChatChannel(channelId: String): LiveData<Post>

    @Query("SELECT * FROM posts WHERE tags LIKE :tag AND archived = 0 ORDER BY viewsCount DESC")
    abstract fun getTagPosts(tag: String): PagingSource<Int, Post>






    @Query("SELECT * FROM posts WHERE id = :id")
    abstract fun getLivePostById(id: String): LiveData<Post>

    @Query("DELETE FROM posts WHERE id = :id")
    abstract suspend fun deletePostById(id: String)

    @Query("SELECT * FROM posts WHERE isNearMe = 1 AND archived = 0")
    abstract fun getPostsNearMe(): PagingSource<Int, Post>

    @Query("SELECT * FROM posts WHERE isMadeByMe = 1 AND archived = 0")
    abstract fun getCurrentUserPosts(): LiveData<List<Post>>

    @Delete
    abstract suspend fun deletePost(post: Post)

    @Query("SELECT * FROM posts WHERE archived = 1 AND isMadeByMe = 1 ORDER BY createdAt DESC")
    abstract fun getArchivedPosts(): PagingSource<Int, Post>

    @Query("DELETE FROM posts WHERE isAd = 1")
    abstract suspend fun deleteAdPosts()

    @Query("SELECT * FROM posts WHERE id = :postId")
    abstract fun getReactivePost(postId: String): LiveData<Post>

    @Query("UPDATE posts SET isNearMe = 0")
    abstract suspend fun disableLocationBasedPosts()


}