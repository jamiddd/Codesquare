package com.jamid.codesquare.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.jamid.codesquare.data.User

@Dao
abstract class UserDao: BaseDao<User>() {

    @Query("SELECT * FROM users WHERE isCurrentUser = 1")
    abstract fun currentUser(): LiveData<User>

    @Query("DELETE FROM users")
    abstract suspend fun clearTable()

    @Query("SELECT * FROM users WHERE id = :userId")
    abstract suspend fun getUser(userId: String): User?

    @Query("SELECT * FROM users WHERE chatChannels LIKE :chatChannel ORDER BY name ASC")
    abstract suspend fun getChannelContributors(chatChannel: String): List<User>?

    @Query("DELETE FROM users WHERE id = :userId")
    abstract suspend fun deleteUserById(userId: String)

    @Transaction
    @Query("UPDATE users SET isLiked = 1 AND likesCount = likesCount + 1 WHERE id = :userId")
    abstract suspend fun likeLocalUserById(userId: String)

    @Transaction
    @Query("UPDATE users SET isLiked = 0 AND likesCount = likesCount - 1 WHERE id = :userId")
    abstract suspend fun dislikeLocalUserById(userId: String)

    @Query("SELECT * FROM users WHERE chatChannels LIKE :formattedChannelId ORDER BY name ASC")
    abstract fun getChannelContributorsLive(formattedChannelId: String): LiveData<List<User>>

    @Query("SELECT * FROM users WHERE id = :userId")
    abstract fun liveLocalUser(userId: String): LiveData<User>

    @Query("SELECT * FROM users WHERE id = :userId")
    abstract fun getReactiveUser(userId: String): LiveData<User>

}