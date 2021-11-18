package com.jamid.codesquare.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.jamid.codesquare.data.ProjectRequest

@Dao
abstract class ProjectRequestDao: BaseDao<ProjectRequest>() {

    @Query("SELECT * FROM project_requests WHERE requestId = :requestId")
    abstract suspend fun getProjectRequestById(requestId: String): ProjectRequest?

    @Query("SELECT * FROM project_requests WHERE projectId = :projectId")
    abstract suspend fun getProjectRequestByProject(projectId: String): ProjectRequest?

    @Query("DELETE FROM project_requests WHERE requestId = :requestId")
    abstract suspend fun deleteProjectRequest(requestId: String)

    @Query("SELECT * FROM project_requests WHERE receiverId LIKE :receiverId ORDER BY createdAt DESC")
    abstract fun getPagedProjectRequests(receiverId: String): PagingSource<Int, ProjectRequest>

}