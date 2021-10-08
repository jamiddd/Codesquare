package com.jamid.codesquare.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update

@Dao
abstract class BaseDao <T> {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(item: T)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(items: List<T>)

    @Update
    abstract suspend fun update(item: T)

    @Update
    abstract suspend fun update(items: List<T>)

}