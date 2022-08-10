package com.jamid.codesquare.data.dao

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import com.jamid.codesquare.data.Post

@Dao
abstract class PostDao: BaseDao<Post>() {

    @Query("SELECT * FROM posts WHERE archived = 0 ORDER BY createdAt DESC")
    abstract fun getPagedPostsByTime(): PagingSource<Int, Post>

    @Query("SELECT * FROM posts WHERE tags LIKE :tag AND archived = 0 ORDER BY createdAt DESC")
    abstract fun getTagPostsByTime(tag: String): PagingSource<Int, Post>

    @Query("SELECT * FROM posts WHERE archived = 0 ORDER BY likesCount DESC")
    abstract fun getPagedPostsByLikes(): PagingSource<Int, Post>

    @Query("SELECT * FROM posts WHERE tags LIKE :tag AND archived = 0 ORDER BY likesCount DESC")
    abstract fun getTagPostsByLikes(tag: String): PagingSource<Int, Post>

    @Query("SELECT * FROM posts WHERE archived = 0 ORDER BY viewsCount DESC")
    abstract fun getPagedPostsByViews(): PagingSource<Int, Post>

    @Query("SELECT * FROM posts WHERE tags LIKE :tag AND archived = 0 ORDER BY viewsCount DESC")
    abstract fun getTagPostsByViews(tag: String): PagingSource<Int, Post>

    @Query("SELECT * FROM posts WHERE archived = 0 ORDER BY contributorsCount DESC")
    abstract fun getPagedPostsByContributors(): PagingSource<Int, Post>

    @Query("SELECT * FROM posts WHERE tags LIKE :tag AND archived = 0 ORDER BY contributorsCount DESC")
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

    @Query("DELETE FROM posts WHERE post_userId = :id")
    abstract suspend fun deletePostsByUserId(id: String)

    @Query("SELECT * FROM posts WHERE rank != -1 ORDER BY rank ASC")
    abstract fun getRankedPosts(): PagingSource<Int, Post>

    @Query("SELECT * FROM posts WHERE likesCount > 5 AND isAd = 0")
    abstract fun getPostMoreLikes(): PagingSource<Int, Post>

    @Query("SELECT * FROM posts WHERE rankCategory = :category ORDER BY rank ASC")
    abstract fun getRankedCategoryPosts(category: String): PagingSource<Int, Post>



















    /* Experimental for new stuff */
    @Query("SELECT * FROM posts WHERE archived = 0 AND createdAt < :time ORDER BY createdAt DESC LIMIT :loadSize")
    abstract suspend fun getPosts(time: Long, loadSize: Int): List<Post>


    @Query("SELECT * FROM posts WHERE archived = 0 AND id = :key LIMIT 1")
    abstract suspend fun getPostById(key: String): Post?


}