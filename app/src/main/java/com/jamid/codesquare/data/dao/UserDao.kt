package com.jamid.codesquare.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.jamid.codesquare.data.User

@Dao
abstract class UserDao: BaseDao<User>() {

    @Query("SELECT * FROM users WHERE isCurrentUser = 1")
    abstract fun currentUser(): LiveData<User>

    @Query("DELETE FROM users")
    abstract suspend fun clearTable()

    @Query("SELECT * FROM users WHERE id = :userId")
    abstract suspend fun getUser(userId: String): User?

}