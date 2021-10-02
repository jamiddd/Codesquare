package com.jamid.codesquare.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Dao
abstract class BaseDao <T> {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(item: T)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(items: List<T>)

}