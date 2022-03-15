package com.jamid.codesquare.data.dao

import androidx.lifecycle.LiveData
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import com.firebase.geofire.GeoLocation
import com.jamid.codesquare.data.Project

@Dao
abstract class ProjectDao: BaseDao<Project>() {

    @Query("SELECT * FROM projects WHERE isArchived = 0 ORDER BY createdAt DESC")
    abstract fun getPagedProjects(): PagingSource<Int, Project>

    @Query("DELETE FROM projects")
    abstract suspend fun clearTable()

    @Query("SELECT * FROM projects WHERE isMadeByMe = 1 AND isArchived = 0")
    abstract fun getCurrentUserPagedProjects(): PagingSource<Int, Project>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    abstract suspend fun getProject(projectId: String): Project?

    @Query("SELECT * FROM projects WHERE contributors LIKE :formattedUid AND project_userId != :uid AND isArchived = 0 ORDER BY createdAt DESC")
    abstract fun getPagedCollaborations(formattedUid: String, uid: String): PagingSource<Int, Project>

    @Query("SELECT * FROM projects WHERE project_userId = :id AND isArchived = 0 ORDER BY createdAt DESC")
    abstract fun getPagedOtherUserProjects(id: String): PagingSource<Int, Project>

    @Query("SELECT * FROM projects WHERE contributors LIKE :formattedUid AND project_userId != :id AND isArchived = 0 ORDER BY createdAt DESC")
    abstract fun getOtherUserPagedCollaborations(formattedUid: String, id: String): PagingSource<Int, Project>

    @Query("SELECT * FROM projects WHERE isSaved = 1 AND isArchived = 0 ORDER BY createdAt DESC")
    abstract fun getPagedSavedProjects(): PagingSource<Int, Project>

    @Query("SELECT * FROM projects WHERE chatChannel = :channelId LIMIT 1")
    abstract suspend fun getProjectByChatChannel(channelId: String): Project?

    @Query("SELECT * FROM projects WHERE chatChannel = :channelId LIMIT 1")
    abstract fun getLiveProjectByChatChannel(channelId: String): LiveData<Project>

    @Query("SELECT * FROM projects WHERE tags LIKE :tag AND isArchived = 0 ORDER BY createdAt DESC")
    abstract fun getTagProjects(tag: String): PagingSource<Int, Project>

    @Query("SELECT * FROM projects WHERE id = :id")
    abstract fun getLiveProjectById(id: String): LiveData<Project>

    @Query("DELETE FROM projects WHERE id = :id")
    abstract suspend fun deleteProjectById(id: String)

    @Query("SELECT * FROM projects WHERE isNearMe = 1 AND isArchived = 0")
    abstract fun getProjectsNearMe(): PagingSource<Int, Project>

    @Query("SELECT * FROM projects WHERE isMadeByMe = 1 AND isArchived = 0")
    abstract fun getCurrentUserProjects(): LiveData<List<Project>>

    @Delete
    abstract suspend fun deleteProject(project: Project)

    @Query("SELECT * FROM projects WHERE isArchived = 1 AND isMadeByMe = 1 ORDER BY createdAt DESC")
    abstract fun getArchivedProjects(): PagingSource<Int, Project>

    @Query("DELETE FROM projects WHERE isAd = 1")
    abstract suspend fun deleteAdProjects()

    @Query("SELECT * FROM projects WHERE id = :projectId")
    abstract fun getReactiveProject(projectId: String): LiveData<Project>

    @Query("UPDATE projects SET isNearMe = 0")
    abstract suspend fun disableLocationBasedProjects()


}