package com.jamid.codesquare.data.dao

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import com.jamid.codesquare.data.QUERY_TYPE_POST
import com.jamid.codesquare.data.SearchQuery

@Dao
abstract class SearchQueryDao: BaseDao<SearchQuery>() {
    init {
        Log.d("Something", "Simple: ")
    }
    @Query("SELECT * FROM search_query WHERE type = :type ORDER BY createdAt DESC LIMIT 5")
    abstract fun previousQueries(type: Int = QUERY_TYPE_POST): LiveData<List<SearchQuery>>

    @Query("SELECT * FROM search_query ORDER BY createdAt DESC LIMIT 10")
    abstract fun prevQueries(): LiveData<List<SearchQuery>>

    @Delete
    abstract suspend fun deleteSearchQuery(query: SearchQuery)

    @Query("DELETE FROM search_query")
    abstract suspend fun clearTable()

    @Query("DELETE FROM search_query WHERE id = :id")
    abstract suspend fun deletePreviousSearchByUserId(id: String)

}