package com.jamid.codesquare.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.jamid.codesquare.data.ReferenceItem

@Dao
abstract class ReferenceItemDao: BaseDao<ReferenceItem>() {

    @Query("SELECT * FROM liked_item ORDER BY createdAt DESC")
    abstract fun getReferenceItems(): PagingSource<Int, ReferenceItem>

    @Query("DELETE FROM liked_item")
    abstract suspend fun clearTable()

    @Query("DELETE FROM liked_item WHERE id = :itemId")
    abstract suspend fun deleteReferenceItem(itemId: String)

}