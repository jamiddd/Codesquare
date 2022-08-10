package com.jamid.codesquare.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.jamid.codesquare.data.UserMinimal
import com.jamid.codesquare.data.dao.BaseDao
// something simple
@Dao
abstract class UserMinimalDao: BaseDao<UserMinimal>() {

    @Query("SELECT * FROM user_minimal ORDER BY name ASC")
    abstract fun getPagedUsers(): PagingSource<Int, UserMinimal>

    @Query("DELETE FROM user_minimal")
    abstract suspend fun clearTable()


}