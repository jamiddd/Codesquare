package com.jamid.codesquare.data.dao

import android.util.Log
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.jamid.codesquare.data.LikedBy

@Dao
abstract class LikedByDao: BaseDao<LikedBy>() {
    init {
        Log.d("Something", "Simple: ")
    }
    @Query("DELETE FROM liked_by")
    abstract suspend fun clearTable()

    @Query("SELECT * FROM liked_by ORDER BY createdAt DESC")
    abstract fun getLikedBy(): PagingSource<Int, LikedBy>
}