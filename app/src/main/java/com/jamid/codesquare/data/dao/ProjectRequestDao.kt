package com.jamid.codesquare.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import com.jamid.codesquare.data.ProjectRequest

@Dao
abstract class ProjectRequestDao: BaseDao<ProjectRequest>() {

    @Query("SELECT * FROM project_requests WHERE requestId = :requestId")
    abstract suspend fun getProjectRequestById(requestId: String): ProjectRequest?

    @Query("SELECT * FROM project_requests WHERE projectId = :projectId")
    abstract suspend fun getProjectRequestByProject(projectId: String): ProjectRequest?

    @Delete
    abstract suspend fun deleteProjectRequest(projectRequest: ProjectRequest)

    @Query("SELECT * FROM project_requests WHERE receiverId = :receiverId ORDER BY createdAt DESC")
    abstract fun getPagedProjectRequests(receiverId: String): PagingSource<Int, ProjectRequest>

    @Query("SELECT * FROM project_requests WHERE senderId = :senderId ORDER BY createdAt DESC")
    abstract fun getMyProjectRequests(senderId: String): PagingSource<Int, ProjectRequest>

    @Query("DELETE FROM project_requests")
    abstract suspend fun clearTable()

}