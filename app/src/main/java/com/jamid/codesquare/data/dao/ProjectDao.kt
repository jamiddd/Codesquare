package com.jamid.codesquare.data.dao

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.jamid.codesquare.data.Project

@Dao
abstract class ProjectDao: BaseDao<Project>() {

    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    abstract fun getPagedProjects(): PagingSource<Int, Project>

    @Query("DELETE FROM projects")
    abstract suspend fun clearTable()

    @Query("SELECT * FROM projects WHERE isMadeByMe = 1")
    abstract fun getCurrentUserPagedProjects(): PagingSource<Int, Project>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    abstract suspend fun getProject(projectId: String): Project?

}