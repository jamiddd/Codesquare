package com.jamid.codesquare.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jamid.codesquare.data.InterestItem

@Dao
abstract class InterestItemDao: BaseDao<InterestItem>() {

    @Query("SELECT * FROM interest_item ORDER BY isChecked DESC, weight DESC")
    abstract fun getPagedInterestItems(): PagingSource<Int, InterestItem>

    @Query("DELETE FROM interest_item")
    abstract suspend fun clearInterestsTable()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertInterests(interestItems: List<InterestItem>)

}